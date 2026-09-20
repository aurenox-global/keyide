// PTY nativo para KeyIDE: abre un pseudo-terminal y lanza una shell
// interactiva dentro. Devuelve el fd maestro, o -1 si algo falla
// (entonces la app usa el shell persistente como fallback).

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <termios.h>
#include <sys/ioctl.h>

JNIEXPORT jint JNICALL
Java_com_keyide_app_terminal_Pty_nativeOpenPt(JNIEnv *env, jobject thiz, jstring jcwd) {
    // Copiamos el cwd ANTES del fork (usar el JNIEnv en el hijo no es seguro).
    char cwd[4096];
    cwd[0] = '\0';
    if (jcwd != NULL) {
        const char *c = (*env)->GetStringUTFChars(env, jcwd, NULL);
        if (c != NULL) {
            strncpy(cwd, c, sizeof(cwd) - 1);
            cwd[sizeof(cwd) - 1] = '\0';
            (*env)->ReleaseStringUTFChars(env, jcwd, c);
        }
    }

    int master = posix_openpt(O_RDWR | O_NOCTTY);
    if (master < 0) return -1;
    if (grantpt(master) != 0 || unlockpt(master) != 0) {
        close(master);
        return -1;
    }
    char *slave = ptsname(master);
    if (slave == NULL) {
        close(master);
        return -1;
    }
    char slavePath[256];
    strncpy(slavePath, slave, sizeof(slavePath) - 1);
    slavePath[sizeof(slavePath) - 1] = '\0';

    pid_t pid = fork();
    if (pid < 0) {
        close(master);
        return -1;
    }
    if (pid == 0) {
        // Proceso hijo: nueva sesión y terminal de control.
        setsid();
        int sfd = open(slavePath, O_RDWR);
        if (sfd < 0) _exit(127);
        ioctl(sfd, TIOCSCTTY, 0);
        dup2(sfd, 0);
        dup2(sfd, 1);
        dup2(sfd, 2);
        if (sfd > 2) close(sfd);
        close(master);
        if (cwd[0] != '\0') chdir(cwd);
        execl("/system/bin/sh", "sh", "-i", (char *) NULL);
        _exit(127);
    }
    return (jint) master;
}

JNIEXPORT void JNICALL
Java_com_keyide_app_terminal_Pty_nativeSetWin(JNIEnv *env, jobject thiz, jint fd, jint rows, jint cols) {
    struct winsize ws;
    memset(&ws, 0, sizeof(ws));
    ws.ws_row = (unsigned short) rows;
    ws.ws_col = (unsigned short) cols;
    ioctl((int) fd, TIOCSWINSZ, &ws);
}
