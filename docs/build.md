# Building ASTranaut Core

Requires JDK 21+ and Maven 3.6.3+. Run `mvn clean verify` from the repository root.
The reactor first compiles/tests `quality-rules`, then builds/tests/analyzes the library.
The public library is `com.kniazkov:astranaut-core:0.0.1`. Its sources are in
`src`; Java packages are now `com.kniazkov.astranaut.core.*`.

The library JAR, sources JAR and Javadoc JAR are in `target`.
The parent POM and `astranaut-quality-rules` are supporting artifacts of the same
reactor; consumers only need the library dependency. Checks are plugin dependencies,
never runtime dependencies of the library.
The imported helper has BSD-3-Clause notices and regression tests; core keeps its MIT
license and strict style policy. Intentionally malformed checker fixtures are not
subject to the core style policy. VERSION.txt resources and manifest entries retain
version metadata; wall-clock timestamps and VCS revision injection were removed for
reproducible artifacts.

## Quality contract

- Checkstyle: explicit legacy configuration, production and test Java; the 20
  custom checks are maintained locally under `quality-rules`.
- PMD 7: explicit rules migrated from Qulice 0.22.0, including custom XPath rules.
- Dependency Plugin: rejects unused compile and undeclared dependencies. Runtime
  logging providers are explicitly runtime-scoped rather than globally suppressed.
- Duplicate Finder: rejects class/resource collisions in compile/runtime classpaths.
- Enforcer: JDK/Maven minimums, explicit release plugin versions, no direct SNAPSHOT
  dependencies for release builds.
- JUnit Jupiter and JaCoCo: all tests execute; HTML/XML coverage is available under
  each module's `target/site/jacoco`. No remote coverage token is required.

See [the migration ledger](build-migration.md) for the old rules and replacements.
Run `mvn clean install` and then `python3 scripts/check-build-gates.py` to verify
that representative violations fail the Maven gates and inspect the built artifact.
CI also saves reports when a check fails. No automatic source formatter rewrites files.

## Maven Central release

The release profile follows `kniazkov/json`: GPG plugin 3.2.7 and Sonatype Central
publishing plugin 0.11.0, server ID `central`, `autoPublish=false`,
`waitUntil=validated`. No credentials or keys are stored in the repository.

1. Configure your Central user token in your local Maven `settings.xml` under server
   ID `central`, and make your signing key available to GPG.
2. Verify the version (including `Info.VERSION`) and release SCM tag, run tests,
   and create the release tag after review.
3. Run `mvn -Prelease clean deploy`. The reactor signs and uploads the parent POM,
   helper artifact and library, with sources and Javadoc.
4. Inspect the validated deployment in Central Portal and publish it manually.

Ordinary `verify`/`install` needs no credentials, GPG key or external publishing service.
A migration PR never publishes a release.

## Repository layout

Library sources and tests live in the root `src/`, and library artifacts in root
`target/`. The root POM orchestrates the build. `build-support/library/pom.xml` is
only a reactor descriptor pointing to those directories, so locally compiled
Checkstyle rules are available before library verification. `quality-rules` is
build tooling, not part of the library. Use `-pl :astranaut-core` to select the
library after installing the helper.

All library Java packages now start with `com.kniazkov.astranaut.core`; consumers
must update their imports. Project copyright is 2026. Original BSD copyright
notices on imported Qulice code/configuration are retained.
