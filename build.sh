#!/usr/bin/env bash
# Build MargyT from an official TikTok apk.
#
#   ./build.sh path/to/tiktok.apk
#
# The apk is not in this repository; bring your own. It has to be a universal
# one -- type APK, not BUNDLE -- arm64-v8a, nodpi. A split bundle has no
# resource table of its own to read.
#
# Everything else -- smali, d8, android.jar, the signer -- is downloaded into
# tools/ the first time and reused after that. All you need installed is a JDK
# and Python 3.

set -euo pipefail
cd "$(dirname "$0")"
exec python3 -m margyt "$@"
