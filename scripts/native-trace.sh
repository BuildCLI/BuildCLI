#!/bin/sh
# Regenerates src/main/resources/META-INF/native-image/dev.buildcli/buildcli/reachability-metadata.json by running BuildCLI on a
# GraalVM JVM with the tracing agent while it does real work. Run it after a dependency update or a new feature that uses
# reflection, resources or JNI. See docs/native-image.md.
#
# The command line part below is automatic. The terminal UI cannot be scripted from here: run it once yourself under the same
# agent (see the end of the output) and use every screen you want in the native build.
#
# Needs: GraalVM java on the PATH (JAVA_HOME), git, and for the model calls OPENROUTER_API_KEY in the environment (optional:
# without it the calls to a real provider are skipped, and the metadata for the HTTP client path will be missing).
set -eu
cd "$(dirname "$0")/.."
# always package: a jar left by an earlier build would trace old code (Maven only redoes what changed)
mvn -q -B package -DskipTests

OUT="${1:-src/main/resources/META-INF/native-image/dev.buildcli/buildcli}"
# kept after the run: the terminal UI is traced by hand from it (see the end of the output)
WORK="$(mktemp -d)"
mkdir -p "$WORK/project" "$WORK/home" "$OUT"
BIN_DIR="$(dirname "$(readlink -f "$(command -v java)")")"
cp target/buildcli.jar "$WORK/buildcli.jar"
printf 'Multi-Release: true\n' > "$WORK/extra.mf"
"$BIN_DIR/jar" ufm "$WORK/buildcli.jar" "$WORK/extra.mf"

export BUILDCLI_HOME="$WORK/home"
cd "$WORK/project"
git init -q . && git -c user.email=a@b -c user.name=t commit -q --allow-empty -m base
OLDPWD_OUT="$(cd "$OLDPWD" && cd "$OUT" && pwd)"
# A command that fails is reported, because the metadata would silently miss what it should have traced. Only the commands
# in EXPECTED_FAIL may exit with an error on a machine without a model (doctor checks the model provider).
FAILED=""
EXPECTED_FAIL="doctor"
bc() {
  if ! java "-agentlib:native-image-agent=config-merge-dir=$OLDPWD_OUT" -jar "$WORK/buildcli.jar" "$@" >"$WORK/last.log" 2>&1; then
    case " $EXPECTED_FAIL " in
      *" $* "*) ;;
      *) FAILED="$FAILED
  buildcli $*: $(tail -n 1 "$WORK/last.log")" ;;
    esac
  fi
}

for args in "--version" "--help" "init" "agent list" "agent show wheslley" "agent create dora --role tester" "provider list" \
            "provider add foo --url http://localhost:9999/v1" "config" "doctor" "runs" "task list" "usage"; do
  echo "trace: buildcli $args"
  # shellcheck disable=SC2086
  bc $args </dev/null
done
echo "fake-key-0123456789abcdef" | bc provider login openrouter --stdin
bc provider logout openrouter </dev/null

if [ -n "${OPENROUTER_API_KEY:-}" ]; then
  echo "trace: real model calls through OpenRouter"
  bc provider test openrouter:openrouter/free </dev/null
  bc run --headless --approve all --agent bruno --model openrouter:openrouter/free "Create the file src/Hello.java containing exactly: class Hello {}" </dev/null
  bc run --headless --agent carla --model openrouter:openrouter/free "Use git to see the last commit and tell me its message." </dev/null
  bc runs </dev/null
else
  echo "skipped: OPENROUTER_API_KEY is not set, so the HTTP client and the tools were not traced"
fi

if [ -n "$FAILED" ]; then
  echo "error: these traced commands failed, so the metadata may be incomplete (log of the last one: $WORK/last.log):$FAILED" >&2
  exit 1
fi

cat <<MSG

Done: $OUT/reachability-metadata.json
Now trace the terminal UI the same way, by hand, in the sample project this script made (it has the agents of 'buildcli init'):

  cd $WORK/project && BUILDCLI_HOME=$WORK/home java -agentlib:native-image-agent=config-merge-dir=$OLDPWD_OUT -jar $WORK/buildcli.jar run

and use every screen: send a message, F2 settings, /connect, /model, Ctrl+F, /copy, /undo, a dialog, a group. Quit with Ctrl+C.
When you are done, delete the working folder: rm -rf $WORK
MSG
