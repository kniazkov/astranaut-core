# Building ASTranaut Core

Requires JDK 21+ and Maven 3.6.3+. Run `mvn clean verify` from the repository root.
This is a single-module library: `com.kniazkov:astranaut-core:0.0.1`.
Sources and tests live in `src/`; the library, sources and Javadoc JARs are built
in `target/`. Java packages start with `com.kniazkov.astranaut.core`.
Maven writes the project version to the JAR manifest as `Implementation-Version`.
`Info.VERSION` reads this value at runtime; it is `unknown` when running directly
from classes without manifest metadata (for example, in an IDE). The version is
maintained only in `pom.xml`.

## Shared build and quality checks

The published `com.kniazkov:astranaut-parent:0.0.1` supplies Java 21 defaults,
plugin versions, quality checks and the release profile. Maven downloads it and
`com.kniazkov:astranaut-quality-rules:0.0.1` from Central; no sibling checkout or
local installation of build tooling is needed. The checker JAR is a plugin
dependency and is not a runtime dependency of this library.

- Checkstyle checks production and test Java, including the 20 custom checks.
- PMD 7 applies the migrated rules, including custom XPath rules.
- Dependency Plugin rejects unused compile and undeclared dependencies.
- Duplicate Finder rejects compile/runtime class and resource collisions.
- Enforcer checks Java/Maven minimums, explicit release plugin versions and
  direct SNAPSHOT dependencies for release builds.
- JUnit Jupiter runs the tests; JaCoCo writes HTML/XML coverage to
  `target/site/jacoco`. No remote service or token is required, and no minimum
  coverage threshold is enforced.
- Source and Javadoc JARs are generated, with Javadoc errors failing the build.

Check implementations, configurations and their regression/rejection tests are
maintained in [astranaut-build](https://github.com/kniazkov/astranaut-build).
See its [policy ledger](https://github.com/kniazkov/astranaut-build/blob/master/docs/legacy-policy.md)
for the old rules and replacements. To change the shared policy, release a new
build version there, then update this project's parent version.

CI runs `mvn clean verify` on Java 21 and saves test, coverage and analysis reports,
including when verification fails. It resolves dependencies without restoring the
old Maven cache, which could contain locally installed pre-extraction checkers.
No automatic source formatter rewrites files.

## Maven Central release

The inherited release profile follows `kniazkov/json`: GPG plugin 3.2.7 and
Sonatype Central publishing plugin 0.11.0, server ID `central`,
`autoPublish=false`, `waitUntil=validated`.

1. Configure your Central user token in Maven `settings.xml` under server ID
   `central`, and make your signing key available to GPG.
2. Verify the project version in `pom.xml` and release SCM tag, run tests,
   and create the release tag after review.
3. Run `mvn -Prelease clean deploy`. This signs and uploads only the core POM
   and library artifacts, including sources and Javadoc. The shared parent and
   checks have their own release lifecycle.
4. Inspect the validated deployment in Central Portal and publish it manually.

Ordinary `verify`/`install` needs no credentials or GPG key.
