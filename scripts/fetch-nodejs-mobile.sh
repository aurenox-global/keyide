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

echo "→ Descargando npm 6.14.18 (CLI para el Node embebido) ..."
curl -sfL -o "$TMP/npm.tgz" https://registry.npmjs.org/npm/-/npm-6.14.18.tgz
tar xzf "$TMP/npm.tgz" -C "$TMP"
mkdir -p "$ROOT/app/src/main/assets"
rm -rf "$ROOT/app/src/main/assets/npm"
cp -r "$TMP/package" "$ROOT/app/src/main/assets/npm"
rm -rf "$ROOT/app/src/main/assets/npm/man" "$ROOT/app/src/main/assets/npm/docs" "$ROOT/app/src/main/assets/npm/changelogs"
echo "  ✓ assets/npm"

echo "→ Listo. Ya puedes compilar (./gradlew assembleRelease)."
