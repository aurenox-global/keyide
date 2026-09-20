// Puente JNI a libnode (nodejs-mobile). Arranca Node en un hilo aparte,
// con stdout/stderr redirigidos a una tubería que lee Kotlin, y mantiene un
// "servicio" Node vivo: el bootstrap lee rutas de scripts por stdin y las ejecuta.

#include <jni.h>
#include <unistd.h>
#include <pthread.h>
#include <string>

// Declaramos node::Start manualmente (símbolo _ZN4node5StartEiPPc) para no
// depender de las cabeceras completas de Node.
namespace node {
int Start(int argc, char** argv);
}

static int g_write_fd = -1;

struct Args {
    std::string bootstrap;
    std::string cwd;
    int writeFd;
};

static void* node_main(void* p) {
    Args* a = static_cast<Args*>(p);
    dup2(a->writeFd, STDOUT_FILENO);
    dup2(a->writeFd, STDERR_FILENO);
    close(a->writeFd);
    if (!a->cwd.empty()) {
        chdir(a->cwd.c_str());
    }
    char* argv[3];
    argv[0] = const_cast<char*>("node");
    argv[1] = const_cast<char*>(a->bootstrap.c_str());
    argv[2] = nullptr;
    node::Start(2, argv);
    return nullptr;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_keyide_app_run_NodeRunner_nativeStart(JNIEnv* env, jobject thiz, jstring jbootstrap, jstring jcwd) {
    int fds[2];
    if (pipe(fds) != 0) return -1;

    Args* a = new Args();
    a->writeFd = fds[1];
    const char* b = env->GetStringUTFChars(jbootstrap, nullptr);
    if (b != nullptr) {
        a->bootstrap = b;
        env->ReleaseStringUTFChars(jbootstrap, b);
    }
    const char* c = env->GetStringUTFChars(jcwd, nullptr);
    if (c != nullptr) {
        a->cwd = c;
        env->ReleaseStringUTFChars(jcwd, c);
    }

    g_write_fd = fds[1];

    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setstacksize(&attr, 8 * 1024 * 1024);
    pthread_t t;
    if (pthread_create(&t, &attr, node_main, a) != 0) {
        close(fds[0]);
        close(fds[1]);
        g_write_fd = -1;
        return -1;
    }
    pthread_detach(t);
    return fds[0];
}

extern "C" JNIEXPORT jint JNICALL
Java_com_keyide_app_run_NodeRunner_nativeWriteFd(JNIEnv* env, jobject thiz) {
    return g_write_fd;
}
