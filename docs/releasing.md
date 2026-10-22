# Releasing

1. **Main is green**: the `CI` workflow passes on Linux, macOS and Windows (build, tests, architecture rules, Checkstyle, CLI
   smoke test, both installers).
2. **Before a release, verify by hand what CI cannot** (record the result in the release notes):
   - drive the TUI on a **macOS** terminal and a **Windows** terminal (`buildcli init`, then `buildcli`: request input, an
     approval with a diff, a denial, an escalation);
   - run a few real tasks (`buildcli run --headless --temperature 0.5 ...`, repeated) against each model you claim support for;
   - follow the README quick start on a clean machine.
3. **Update `CHANGELOG.md`** and set the version: `mvn versions:set -DnewVersion=1.0.0 -DgenerateBackupPoms=false`.
4. **Tag and publish**: commit, tag `v1.0.0`, and publish a GitHub release for the tag. The `Release` workflow builds the
   jar, runs the full verification, and attaches `buildcli.jar`, `buildcli.jar.sha256`, `install.sh` and `install.ps1`.
5. **Verify the published artefacts**: run the installer against the release
   (`curl -fsSL https://github.com/BuildCLI/BuildCLI/releases/latest/download/install.sh | sh`) and check
   `buildcli --version`.
6. Set the next development version (`1.0.1-SNAPSHOT` or `1.1.0-SNAPSHOT`).

The jar's version comes from the Maven `project.version` through the manifest (`buildcli --version`).
