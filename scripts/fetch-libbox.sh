#!/usr/bin/env bash
# Download official-compatible libbox.aar at build time (not redistributed in git).
# Source: JitPack-published artifact matching sing-box 1.13.14 (gomobile libbox).
# Alternative: place your own AAR at app/libs/libbox.aar (e.g. built via
#   go run ./cmd/internal/build_libbox -target android  in SagerNet/sing-box).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/app/libs/libbox.aar"
VERSION="${LIBBOX_VERSION:-1.13.14}"
URL="${LIBBOX_AAR_URL:-https://jitpack.io/com/github/singbox-android/libbox/${VERSION}/libbox-${VERSION}.aar}"

mkdir -p "$(dirname "$DEST")"
if [[ -f "$DEST" && "${FORCE_LIBBOX_FETCH:-}" != "1" ]]; then
  echo "libbox.aar already present: $DEST ($(du -h "$DEST" | cut -f1))"
  exit 0
fi

echo "Fetching libbox $VERSION ..."
echo "  URL: $URL"
TMP="$(mktemp)"
if ! curl -fL --retry 3 --connect-timeout 30 -o "$TMP" "$URL"; then
  rm -f "$TMP"
  echo "ERROR: download failed."
  echo "Place a user-built libbox.aar at: $DEST"
  echo "Build from sing-box: make lib_install && make lib_android"
  exit 1
fi
# Basic sanity: AAR is a zip containing jni/*/libbox.so
if ! unzip -l "$TMP" | grep -q 'jni/.*/libbox.so'; then
  rm -f "$TMP"
  echo "ERROR: downloaded file does not look like libbox.aar"
  exit 1
fi
mv "$TMP" "$DEST"
echo "OK -> $DEST ($(du -h "$DEST" | cut -f1))"
