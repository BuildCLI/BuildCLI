#!/bin/sh
# Builds target/buildcli-native, a GraalVM native executable of BuildCLI (a spike: see docs/native-image.md).
#
#   scripts/build-native.sh            uses native-image from the PATH (or NATIVE_IMAGE=/path/to/native-image)
#   NATIVE_IMAGE_XMX=4g scripts/build-native.sh   heap for the build itself (default 3g; it needs about 2-3 GB)
#
# Needs a GraalVM JDK (21 or newer) and a C toolchain (gcc/clang, zlib headers on Linux). Takes 5-10 minutes.
set -eu
cd "$(dirname "$0")/.."

NI="${NATIVE_IMAGE:-native-image}"
if ! command -v "$NI" >/dev/null 2>&1; then
  echo "error: native-image not found. Install GraalVM (e.g. 'sdk install java 25.0.2-graalce') or set NATIVE_IMAGE." >&2
  exit 1
fi
# the jar tool of the same JDK (native-image itself is a link into a private folder, so look next to java)
if command -v jar >/dev/null 2>&1; then
  JAR_TOOL="$(command -v jar)"
else
  JAR_TOOL="$(dirname "$(readlink -f "$(command -v java)")")/jar"
fi

[ -f target/buildcli.jar ] || mvn -q -B package -DskipTests

# The jar carries multi-release classes from its dependencies (the SQLite driver's native-image feature is one), which the
# builder only sees when the manifest says Multi-Release. The normal jar is left as it is, because that flag changes which
# classes the JVM picks.
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
cp target/buildcli.jar "$TMP/buildcli.jar"
printf 'Multi-Release: true\n' > "$TMP/extra.mf"
"$JAR_TOOL" ufm "$TMP/buildcli.jar" "$TMP/extra.mf"

"$NI" -jar "$TMP/buildcli.jar" -o target/buildcli-native "-J-Xmx${NATIVE_IMAGE_XMX:-3g}" -H:+ReportExceptionStackTraces
echo "built: $(pwd)/target/buildcli-native ($(du -h target/buildcli-native | cut -f1))"
