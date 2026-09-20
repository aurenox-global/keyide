#!/usr/bin/env bash
# Descarga nodejs-mobile (libnode.so) y las cabeceras para el motor Node de KeyIDE.
# Los binarios están en .gitignore por tamaño (~130 MB); este script los restaura.
#
# Uso:  scripts/fetch-nodejs-mobile.sh
set -euo pipefail

VER="nodejs-mobile-v0.3.3"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

echo "→ Descargando $VER ..."
curl -fL -o "$TMP/njm.zip" \
  "https://github.com/janeasystems/nodejs-mobile/releases/download/${VER}/${VER}-android.zip"

echo "→ Extrayendo ..."
unzip -q "$TMP/njm.zip" -d "$TMP/x"

for abi in arm64-v8a armeabi-v7a x86_64; do
  mkdir -p "$ROOT/app/src/main/jniLibs/$abi"
  cp "$TMP/x/bin/$abi/libnode.so" "$ROOT/app/src/main/jniLibs/$abi/"
  echo "  ✓ $abi/libnode.so"
done

mkdir -p "$ROOT/app/src/main/cpp/node"
cp -r "$TMP/x/include" "$ROOT/app/src/main/cpp/node/"
echo "→ Listo. Ya puedes compilar (./gradlew assembleRelease)."
