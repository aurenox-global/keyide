# Compilar

## Requisitos
- JDK 17
- Android SDK (compileSdk 35, build-tools y platform-tools)
- Gradle 8.13 (vía wrapper)

`local.properties` debe apuntar al SDK:
```
sdk.dir=/ruta/a/Android/Sdk
```

## Compilar el APK

```bash
./gradlew assembleDebug
# salida: app/build/outputs/apk/debug/app-debug.apk
```

Release firmado (requiere keystore propio):
```bash
./gradlew assembleRelease
```

## Dependencias Python (pip)

Chaquopy **no** permite instalar paquetes en tiempo de ejecución: los paquetes se
**descargan al compilar** y se empaquetan en el APK (wheels por ABI).

Se declaran en `app/build.gradle.kts`, **con el bloque de nivel de proyecto** `chaquopy`
(el `python { }` anidado dentro de `android.defaultConfig` NO compila en Kotlin DSL):

```kotlin
chaquopy {
    defaultConfig {
        pip {
            install("requests")
            // install("numpy")
            // install("pandas")
            // install("beautifulsoup4")
        }
    }
}
```

- **Pure-Python** (requests, flask, bs4…): cualquier paquete.
- **Nativos** (numpy, pandas, scipy…): requieren *wheel* para la ABI Android; el repositorio
  de Chaquopy incluye muchos precompilados.

ABIs objetivo (`android.defaultConfig.ndk.abiFilters`): `arm64-v8a`, `armeabi-v7a`,
`x86_64`. Cada ABI añade peso (~4-6 MB por runtime Python).

### Notas
- El aviso `Failed to compile to .pyc format: buildPython version … is incompatible` es
  inofensivo (no se precompila bytecode).
- El helper de ejecución es `app/src/main/python/runner.py`.

## Estructura de salida
```
app/build/outputs/apk/debug/app-debug.apk
app/build/python/…           entorno Python de build
```
