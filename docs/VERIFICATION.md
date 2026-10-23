# Verification report

Verified on 23 October 2026. Results below describe this portfolio edition, not the original coursework submission.

## Environment and commands

Local verification used macOS on Apple Silicon, Microsoft OpenJDK 17.0.16, JavaFX 21.0.2, and the committed Gradle 7.4.2 wrapper.

```bash
./gradlew clean build seedDemo installDist --no-daemon
./gradlew build installDist -PcaptureScreenshots --no-daemon
```

The first command was run against a fresh curated working copy with no data directory. It compiled the application, ran the default suite, built ZIP/TAR distributions, created synthetic demo data, and installed local launch scripts. The second command additionally rendered the optional portfolio screenshots.

| Check | Result |
| --- | --- |
| Default JUnit suite | **184 passed**, 0 failures/errors; 1 optional screenshot test skipped |
| Controller integration | **52 cases** using production FXML and temporary JSON/files |
| FXML loading | All **13 screens** loaded with their actual controllers |
| Synthetic demo setup | Two accounts and three documents created; overwrite refusal covered by tests |
| Packaging | ZIP/TAR distributions and installed launch scripts generated |
| Installed launcher | Stayed running for a six-second startup smoke check without launch errors; process then terminated |
| Optional screenshot suite | **185 passed**, 0 failures/errors/skips with capture enabled |
| Coverage, default suite | **70.84% lines** (1,188/1,677); **55.77% branches** (638/1,144) |

JaCoCo numbers cover production classes, including the demo setup and JavaFX controllers; they are not a claim of complete behavioral coverage. Screenshot capture adds a separate optional test and is not required in CI.

## Regressions exercised

- JavaFX-thread assertion errors and runtime exceptions must reach JUnit.
- Register/login/password changes, duplicate identities, malformed hashes, BCrypt hashing, and legacy hash migration.
- Legacy passwords over 72 UTF-8 bytes remain valid without BCrypt migration until changed to a supported length.
- Duplicate upload basenames retain independent bytes; deletion and empty-catalogue persistence; corrupt metadata rejection; restricted file paths.
- Stable user IDs across role changes, stale-edit counter preservation, and exact UTF-8 download contents.
- Guest restrictions, session clearing, admin action guards, ownership checks, filter/sort behavior, forms, and navigation.
- Synthetic seeding refuses existing files and nonempty directories.

## Why the original and current counts differ

The original baseline reported **403 passing tests**. Its XML output contained **12 occurrences of assertion failures** that were not counted as failed tests because the JavaFX helper swallowed errors on the application thread. Several controller checks also caught exceptions without failing and only inspected private fields.

This edition propagates asynchronous errors, replaces weak controller checks with production-FXML fixtures, and replaces weak file-service checks with isolated persistence regressions. The lower test count is deliberate: comparing raw counts or the old claimed coverage would be misleading. The source snapshot and history remain available locally for comparison.

## Reproduce and inspect

Default reports:

- `build/reports/tests/test/index.html`
- `build/reports/jacoco/test/html/index.html`
- `build/reports/jacoco/test/jacocoTestReport.xml`

On Linux without a display:

```bash
xvfb-run -a ./gradlew clean build --no-daemon
```

The [Java CI workflow](https://github.com/MakenzieAhern/scrollvault-desktop/actions/workflows/ci.yml) repeats that command on Ubuntu 24.04 / Java 17 and uploads test/coverage reports.

## Visual verification and limits

The screenshots in `docs/images/` are rendered from production FXML/CSS using synthetic data, not mockups. Capture checks the library's three records, text preview, admin controls, and statistics. The packaged classpath launcher emits JavaFX's unnamed-module configuration warning; it successfully starts, but this is not a modular runtime-image distribution. The desktop window manager can slightly reduce the requested 1,200 × 800 viewport.

Tests directly exercise controller methods and substitute temporary paths for native download dialogs. They do not exhaustively automate native file choosers, every image format, multi-instance writes, packaging on every operating system, or all accessibility behaviors. The application remains a local desktop prototype.
