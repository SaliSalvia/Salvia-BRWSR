#!/usr/bin/env bash
# Bundles the Vazirmatn TTFs into core/ui — required by theme/Type.kt.
# TTFs are binaries and cannot be committed via the text-based Git API,
# so run this once after cloning and commit the files it produces:
#
#   bash scripts/download-fonts.sh
#   git add core/ui/src/main/res/font/*.ttf
#   git commit -m "feat(ui): bundle Vazirmatn v33.003 TTFs"
set -euo pipefail

VERSION="v33.003"
FONT_DIR="core/ui/src/main/res/font"
ZIP_URL="https://github.com/rastikerdar/vazirmatn/releases/download/$VERSION/vazirmatn-$VERSION.zip"

command -v curl  >/dev/null || { echo "error: curl is required";  exit 1; }
command -v unzip >/dev/null || { echo "error: unzip is required"; exit 1; }

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

echo "Downloading Vazirmatn $VERSION ..."
curl -fL -o "$TMP/vazirmatn.zip" "$ZIP_URL"
unzip -q "$TMP/vazirmatn.zip" -d "$TMP/vazirmatn"

REG="$(find "$TMP/vazirmatn" -name 'Vazirmatn-Regular.ttf' -print -quit)"
if [ -z "$REG" ]; then
  echo "error: Vazirmatn-Regular.ttf not found in the downloaded archive"
  exit 1
fi
SRC_DIR="$(dirname "$REG")"

mkdir -p "$FONT_DIR"
cp "$SRC_DIR/Vazirmatn-Regular.ttf"  "$FONT_DIR/vazirmatn_regular.ttf"
cp "$SRC_DIR/Vazirmatn-Medium.ttf"   "$FONT_DIR/vazirmatn_medium.ttf"
cp "$SRC_DIR/Vazirmatn-SemiBold.ttf" "$FONT_DIR/vazirmatn_semibold.ttf"
cp "$SRC_DIR/Vazirmatn-Bold.ttf"     "$FONT_DIR/vazirmatn_bold.ttf"
cp "$SRC_DIR/Vazirmatn-Black.ttf"    "$FONT_DIR/vazirmatn_black.ttf"

echo
ls -lh "$FONT_DIR"
echo
echo "Done. Commit the binaries:"
echo "  git add $FONT_DIR/*.ttf && git commit -m 'feat(ui): bundle Vazirmatn v33.003 TTFs'"
