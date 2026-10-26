# Using and verifying core as a dependency

Core targets Java 21. Add `com.kniazkov:astranaut-core:0.0.1` as an ordinary Maven
dependency. Consumer code uses `com.kniazkov.astranaut.core` packages. It does not need
the core sources, test fixtures, or the astranaut-build parent in its own POM.
Visualization is a separate opt-in dependency; core has no Graphviz/V8/Logback dependency.

## Independent Maven acceptance project

From the repository root, with Java 21 and Maven:

```sh
mvn -B -ntp clean install
mvn -B -ntp -f integration-tests/consumer/pom.xml clean verify
```

The first command builds, tests, checks and installs core once. `install` is local Maven
installation, not a release or deployment. The second starts a separate Maven build:
no reactor module, parent relationship, test JAR, source-directory inclusion or dependency
on `target/classes`. Its own compiler and JUnit configuration is intentionally minimal.
All consumer classes are in package `consumer`, outside core's package-private boundary.

The consumer checks that `Node` was loaded from a JAR and compares its SHA-256 with
`target/astranaut-core-0.0.1.jar`. A stale installed artifact or a classes-directory
substitution fails the test. To verify another build directory/version, supply
`-Dcore.build.directory=/absolute/path/to/target -Dcore.version=VERSION` to the second
command. Both builds must use the same local Maven repository/settings if overridden.
A root-only `mvn verify` does not run the separate consumer; CI executes both commands.

## Contracts at the public boundary

`PublicContractsTest` applies common snapshot/build assertions to both DraftNode and an
independent schema-free Adapter. The adapter implements its own Builder and stores
immutable data, optional child and provenance. Its descriptor has multiple abstract bases;
a compiled base interface returns `Optional<? extends Node>` and its implementation
narrows that to `Optional<Adapter>`. These are handwritten consumer examples, not claims
about the current generator's output.

The external suite checks:

- Type identity by global name, multiple bases and common-base ambiguity; unknown child
  schema; allocation ambiguity, repeated occurrences and reference order.
- Builder refusal, missing data, full replacement, optional/tagged/indexed edits,
  unchanged prior snapshots and caller-list ownership.
- Structural equality/hash agreement and a real string hash collision.
- Both JSON modes with a strict resolver, preservation of edge tags, empty roots, and
  both restored endpoints of a serialized diff.
- Atomic rejection by a concrete ancestor and propagation of unexpected rule errors.
- UTF-16 fragment ranges for built-in and independent Source implementations, immutable
  file snapshots, and provenance retained after Builder reuse.

Detailed built-in regressions remain in the ordinary core suites: the [builder acceptance
map](builders.md#contract-acceptance-12-and-26), [immutable model audit](immutable-model.md),
[source contract](source-contract.md), [rule engine](rule-engine.md), [cycle detection](stabilization.md),
[diff](differences.md), [normalization](normalization.md) and [JSON](json.md).
The consumer tests complement those regressions without copying their entire fixtures.

## Adapter obligations

Names uniquely identify types; equal names must describe compatible definitions.
`AbstractType` holds direct bases and own properties. Only concrete `Type` supplies a
Builder. Unknown child schema is distinct from a known empty schema. Properties remain
readonly metadata; they are not a hidden registry of child bindings.

Published nodes, types, properties, fragments and sources must remain stable. Copy mutable
parser state before exposing it. Shared children are allowed; child cycles are invalid.
An occurrence's tag belongs to its parent edge. Optional children do not reserve null
slots. Named and indexed getters must return the same object for the same occurrence.

Builders own mutable working state. False must preserve all state, including insertion
order. `build` either creates a valid immutable snapshot or throws `ConstructionException`.
Implementations validate their own string data. Unexpected errors must propagate. A
schema-free adapter must reconstruct its own node from data and its ordered child list;
if it exposes tags, reconstruction must preserve those bindings as well.

Structural equality includes type, nonnull string data, ordered children and tags. It
excludes properties, provenance and sharing arrangement. JSON restores structural content
through builders; it deliberately ignores metadata. Diff restores structural endpoints,
not original parser objects. Normalization makes properties structural, with no inverse
conversion guarantee. See the linked guides for service-node and error semantics.

## Load probes and limits

`ConsumerLoadTest` round-trips chains and wide shared trees of over 20,000 occurrences,
checks 512 normally nonmatching rules, and compares normalization across these shapes:
small, 20,000-wide shared input, a 20,000-deep chain, and eight separate 2,500-deep branches.
Sequential mode and caller-owned pools of 1/2/4 threads must produce equal structures and
hashes; the algorithm must leave the caller's pool open. Existing core suites separately
cover cancellation, worker errors, rule priority and ordered acceptance.

CI saves Surefire reports and `consumer-reports/normalization.csv` plus `environment.txt`.
Each configuration gets one unrecorded warmup and three recorded samples. Every sample
uses a fresh Normalizer invocation: invocation-local memoization cannot leak between
samples. Identical source objects within one DAG may legitimately share work.

The CSV records elapsed nanoseconds, submitted executor tasks, and the sum of JVM heap
pool peak-used readings after resetting peak counters. That sum is an approximate
process-heap observation, not allocated bytes, retained output size, RSS, or a guarantee
that the pools peaked simultaneously. It includes previously live objects and JVM/GC
effects. Tests run with a 512 MiB maximum heap. Timings exclude equality verification and
pool construction; results depend on JVM warmup, hardware and GC. Compare distributions,
not a single number. No CI assertion requires a speedup or imposes a rewrite-step limit.
The sequential structural hash is not presented as a parallel hash implementation.

## Later migration of astranaut (#24 remains open)

The generator/interpreter end-to-end phase needs an explicit transition to that repository.
Before declaring it complete:

1. Change imports and dependency coordinates to Java 21/current core.
2. Generate separate AbstractType descriptors for abstract Java interfaces; Type descriptors
   for concrete classes, direct base Sets, own properties and complete optional schemas.
3. Generate nonnull string data access, wildcard Optional base getters, and both child-tag
   methods. Check `Addition <- left@Expression, right@Expression;`, absent optional children,
   and shared/equal objects at distinct tagged occurrences.
4. Generate independent Builders implementing atomic boolean edits, insertion-order indexes,
   complete `setChildren`, repeatable `build` and expected construction refusal. Preserve
   readonly property/provenance rules and the concrete data validator.
5. Migrate converters to the common rule/stabilization contracts, Optional roots and the
   versioned JSON type resolver. Do not reintroduce namespace/property-based type identity.
6. Run real DSL input through generation, compile the resulting Java against this JAR,
   and compare execution with the interpreter, including refusals, tags and fragments.

This PR completes the independent core-consumer stage. Handwritten adapters do not fulfill
step 6, and new DSL datatype/validator decisions must be included during generator migration.
