#!/bin/bash
# Bundles the Fichero printer TypeScript library into a single ES module JS file.
#
# Usage:
#   cd web && bash build-fichero.sh
#
# When the upstream fichero-printer repo is updated:
#   cd lib/fichero-printer && git pull
#   cd ../../web && bash build-fichero.sh
#
# This produces public/fichero.js which the web app imports.

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FICHERO_SRC="$SCRIPT_DIR/../lib/fichero-printer/web/src/lib/fichero"
OUT="$SCRIPT_DIR/public/fichero.js"

if [ ! -d "$FICHERO_SRC" ]; then
    echo "ERROR: Fichero source not found at $FICHERO_SRC"
    echo "Clone the repo first:  git clone https://github.com/0xMH/fichero-printer.git lib/fichero-printer"
    exit 1
fi

echo "Bundling Fichero library from: $FICHERO_SRC"
npx esbuild "$FICHERO_SRC/index.ts" \
    --bundle \
    --format=esm \
    --outfile="$OUT" \
    --target=es2020 \
    --minify=false

echo "Done: $OUT ($(wc -c < "$OUT") bytes)"
