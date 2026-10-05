#!/bin/sh
# Installs BuildCLI for the current user (Linux and macOS).
#
#   curl -fsSL https://github.com/BuildCLI/BuildCLI/releases/latest/download/install.sh | sh
#
# It downloads buildcli.jar from a GitHub release, verifies its SHA-256, puts it in ~/.buildcli/bin and creates a
# 'buildcli' launcher in ~/.local/bin. Nothing outside those two places is touched and no sudo is needed.
#
# Options (or environment variables):
#   --from-file PATH      install a local jar instead of downloading (offline installs, CI)
#   --version TAG         a release tag such as v1.0.0 (default: the latest release)
#   BUILDCLI_REPO         GitHub repository to download from (default: BuildCLI/BuildCLI)
#   BUILDCLI_BASE_URL     download from this URL instead (a mirror; must serve buildcli.jar and buildcli.jar.sha256)
#   BUILDCLI_HOME         install directory (default: ~/.buildcli)
#   BUILDCLI_BIN_DIR      where the launcher goes (default: ~/.local/bin)
set -eu

REPO="${BUILDCLI_REPO:-BuildCLI/BuildCLI}"
HOME_DIR="${BUILDCLI_HOME:-$HOME/.buildcli}"
BIN_DIR="${BUILDCLI_BIN_DIR:-$HOME/.local/bin}"
FROM_FILE=""
VERSION="latest"

while [ $# -gt 0 ]; do
  case "$1" in
    --from-file) FROM_FILE="$2"; shift 2 ;;
    --version) VERSION="$2"; shift 2 ;;
    -h|--help) sed -n '2,18p' "$0"; exit 0 ;;
    *) echo "unknown option: $1" >&2; exit 2 ;;
  esac
done

say() { printf '%s\n' "$*"; }
die() { printf 'error: %s\n' "$*" >&2; exit 1; }

# --- Java 21+ ---
command -v java >/dev/null 2>&1 || die "Java 21 or newer is required but 'java' was not found. Install a JDK 21+ first."
JAVA_MAJOR=$(java -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -n 1)
[ -n "$JAVA_MAJOR" ] && [ "$JAVA_MAJOR" -ge 21 ] || die "Java 21 or newer is required (found: ${JAVA_MAJOR:-unknown})."

mkdir -p "$HOME_DIR/bin" "$BIN_DIR"
JAR="$HOME_DIR/bin/buildcli.jar"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

sha256_of() {
  if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | cut -d ' ' -f 1
  elif command -v shasum >/dev/null 2>&1; then shasum -a 256 "$1" | cut -d ' ' -f 1
  else die "neither sha256sum nor shasum is available to verify the download"
  fi
}

if [ -n "$FROM_FILE" ]; then
  [ -f "$FROM_FILE" ] || die "no such file: $FROM_FILE"
  cp "$FROM_FILE" "$TMP/buildcli.jar"
  say "Installing from $FROM_FILE (checksum not verified for local files)"
else
  if [ -n "${BUILDCLI_BASE_URL:-}" ]; then BASE="$BUILDCLI_BASE_URL"
  elif [ "$VERSION" = "latest" ]; then BASE="https://github.com/$REPO/releases/latest/download"
  else BASE="https://github.com/$REPO/releases/download/$VERSION"; fi
  command -v curl >/dev/null 2>&1 || die "curl is required to download BuildCLI"
  say "Downloading $BASE/buildcli.jar"
  curl -fsSL "$BASE/buildcli.jar" -o "$TMP/buildcli.jar" || die "download failed; is there a release in $REPO?"
  curl -fsSL "$BASE/buildcli.jar.sha256" -o "$TMP/buildcli.jar.sha256" || die "could not download the checksum file"
  EXPECTED=$(cut -d ' ' -f 1 "$TMP/buildcli.jar.sha256")
  ACTUAL=$(sha256_of "$TMP/buildcli.jar")
  [ "$EXPECTED" = "$ACTUAL" ] || die "checksum mismatch (expected $EXPECTED, got $ACTUAL); not installing"
  say "Checksum verified"
fi

mv "$TMP/buildcli.jar" "$JAR"
LAUNCHER="$BIN_DIR/buildcli"
# A lean JVM for a CLI: serial GC, quick JIT, small stacks, and a class-data archive the JVM creates on the first run
# next to the jar (measured: 0.3 s -> 0.17 s to start, 90 -> 70 MB; the first run takes ~2 s once). Native access for
# SQLite is granted by the jar's manifest. BUILDCLI_JAVA_OPTS adds or overrides options.
cat > "$LAUNCHER" <<LAUNCH
#!/bin/sh
exec java -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:+AutoCreateSharedArchive \\
  -XX:SharedArchiveFile="$JAR.jsa" -Xlog:disable \$BUILDCLI_JAVA_OPTS -jar "$JAR" "\$@"
LAUNCH
chmod +x "$LAUNCHER"

say "Installed: $LAUNCHER"
"$LAUNCHER" --version
case ":$PATH:" in
  *":$BIN_DIR:"*) ;;
  *) say ""; say "Add $BIN_DIR to your PATH to run 'buildcli' from anywhere, for example:"
     say "  echo 'export PATH=\"$BIN_DIR:\$PATH\"' >> ~/.profile" ;;
esac
say ""
say "Next: cd into a project and run 'buildcli init', then 'buildcli doctor'."
