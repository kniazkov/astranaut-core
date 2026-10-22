# Structural equality and hashing

Two nodes are equal when their types have the same globally unique name, their
string data match, and their ordered child occurrences match recursively. Each
occurrence includes its `Optional<String>` tag: absence and an empty string differ.
Properties, source fragments, Java implementation classes and sharing topology are
excluded. One shared subtree in two positions equals two independent equal copies.
Inputs must be valid acyclic graphs, with consistent child counts and indexed access.

## Implementing Node

Built-in nodes extend `NodeWithStructuralEquality`, whose final `equals` and
`hashCode` delegate to `NodeStructure`. Generated classes can use the same base.
An adapter that already extends another class must implement both methods explicitly:

```java
@Override
public final boolean equals(final Object other) {
    return other instanceof Node node && NodeStructure.equal(this, node);
}

@Override
public final int hashCode() {
    return NodeStructure.hash(this);
}
```

See `StructuralNodeFixture` in the tests for an independent implementation with
children, tags and metadata. Type descriptors must also obey the name-based
`AbstractType.equals/hashCode` contract. A foreign implementation retaining
`Object.equals` cannot participate in symmetric structural equality; Java cannot
supply default implementations of these Object methods through an interface.

`Node.deepCompare` delegates to structural equality. It no longer compares
properties. Standard child-list searches now use structural equality; algorithms
looking for a particular object use reference identity explicitly.

## Algorithms and cache lifetime

`NodeStructure.equal` uses an explicit work stack and memoizes pairs of object
identities. It compares actual type/data/tag values, never infers equality from
hashes. `NodeStructure.hash` uses iterative postorder traversal, memoizing each
object's hash and folding every child occurrence in order. Both support deep trees
without growing the Java call stack. Tests include a chain of 30,001 nodes and a
70-level shared diamond. Hashing rejects cycles; equality assumes valid inputs and
is not a graph validator.

For constant-time node access, sequential hashing takes O(V + E) work and O(V)
auxiliary storage for reachable objects/edges. Equality visits distinct object
pairs and their corresponding edges; its cost depends on both sharing layouts.
No recursive Node.hashCode call is used to key either cache. The state belongs to
one invocation. `NodeStructure.hashes` additionally exposes an unmodifiable,
identity-keyed snapshot for algorithms needing every subtree hash. Discard this
snapshot when the operation ends; retaining it retains its nodes.

`NodeStructure.hash` retains no trees between calls.
`ExtNodeCreator` calculates one snapshot per conversion. Absolute hashes and
`Node.hashCode` agree, but neither is a stable persistence format or a unique ID.
Equal hashes require a structural comparison. Top-down mapping and pair finding
now verify their candidates; `IdenticalNodeFinder` preserves its local
(type + nonempty data) grouping semantics and verifies those values within buckets.

Legacy mutable pattern/diff wrappers still exist. Hashes are deliberately not
cached on node instances. Do not mutate nodes during an operation or while they
are keys in ordinary hash collections. Mapping, patching and selection bookkeeping
uses identity collections where it addresses concrete objects. This prevents
separate equal siblings from being conflated; a complete occurrence/path model for
shared objects remains the traversal/mapping work in #14. Serialization,
normalization and diff tag preservation remain separate tasks. This change does
not make every older algorithm iterative or immutable.

## Explicit parallel strategy

```java
try (var executor = Executors.newFixedThreadPool(4)) {
    var strategy = new ParallelNodeStructure(executor, 4, 5000);
    int hash = strategy.hash(root);
    boolean same = strategy.equal(root, other);
}
```

The caller owns and closes the bounded executor. Ordinary `equals`, `hashCode`
and `NodeStructure.hash` remain sequential and never submit tasks or use a global pool.
Invoke the strategy outside its executor to avoid starvation on a saturated pool.
Node implementations and their callbacks must allow concurrent reads.

The threshold counts reachable objects. Small trees, one-worker configurations
and roots with only one child use sequential operations. Larger roots distribute
child branches over at most the requested worker count, without nested task
submission. Results are folded in original child order, including tags. Each
hash worker owns an identity cache; shared objects may be computed again by other
workers. No shared mutable cache or historical-tree cache exists. Root-level
partitioning can be unbalanced and does not accelerate chains.

After a comparison mismatch, workers stop starting subsequent branch comparisons;
an already running branch comparison finishes. Worker exceptions propagate without
being converted into inequality. Caller interruption restores the interrupt flag
and throws `IllegalStateException`; outstanding futures receive best-effort
cancellation. Cancellation does not guarantee that an already running computation
or a caller-provided callback stops immediately. The executor remains caller-owned.
Tests exercise 1/2/4 workers repeatedly, collision data/tags, shared branches,
sequential fallback, worker failure and caller interruption.

## Diagnostic measurements

Run with Java 21 after building the project:

```sh
mvn -B -ntp clean verify
mkdir -p target/benchmark-classes
javac -cp target/classes -d target/benchmark-classes benchmarks/StructuralBenchmark.java
java -Xms256m -Xmx1g -cp target/classes:target/benchmark-classes StructuralBenchmark
```

Measured on 2026-10-15 in a Linux container with 8 available processors, OpenJDK
21.0.12.1, 256 MiB initial / 1 GiB maximum heap. Entries are median microseconds per
operation, six warm-up rounds and nine measured rounds. Tiny inputs use batches
of 1,000. Equality compares separate, equal structures. Threshold: 5,000 objects.

| Shape | Mode | Hash, µs | Equality, µs |
| --- | --- | ---: | ---: |
| 3 nodes | Sequential | 1.044 | 0.731 |
| 3 nodes | 1 worker | 0.789 | 0.934 |
| 3 nodes | 2 workers | 0.485 | 0.363 |
| 3 nodes | 4 workers | 0.971 | 0.595 |
| Chain, 30,001 nodes | Sequential | 4649.918 | 5577.552 |
| Chain, 30,001 nodes | 1 worker | 5952.376 | 5753.046 |
| Chain, 30,001 nodes | 2 workers | 5215.458 | 3072.858 |
| Chain, 30,001 nodes | 4 workers | 4090.146 | 3286.259 |
| 64 branches, 131,009 nodes | Sequential | 14357.867 | 16615.790 |
| 64 branches, 131,009 nodes | 1 worker | 15537.570 | 21582.150 |
| 64 branches, 131,009 nodes | 2 workers | 14245.563 | 5575.991 |
| 64 branches, 131,009 nodes | 4 workers | 6716.986 | 3356.630 |

In this run, four workers improved the large independent-branch case by about
2.1x for hashing and 5.0x for equality. Two workers barely improved hashing.
Tiny/chain modes all run sequentially: their differences reflect wrapper/probe
overhead, JIT, allocation/GC and measurement noise, not parallel speedup. These
are diagnostic single-process measurements, not a JMH study or performance
promises. There are no timing assertions in CI. Tune the threshold for actual
node implementations, sharing patterns, branch balance and available resources.
