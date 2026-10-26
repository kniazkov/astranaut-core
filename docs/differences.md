# Immutable reversible differences

A `DiffStrategy` compares two immutable `Tree` values and produces a `DiffTree`.
Both `getBefore()` and `getAfter()` restore the corresponding structural state:
type, string data, ordered children and tags. Empty states are supported. Readonly
properties are supplied by reconstructed nodes; their exact recovery is not promised.

```java
Tree before = Tree.createDraft("Root(A,Z)");
Tree after = Tree.createDraft("Root(A,B,C,Z)");
DiffTree difference = new PositionalDiff().compare(before, after);
assert before.deepCompare(difference.getBefore());
assert after.deepCompare(difference.getAfter());
```

`Insert`, `Delete` and `Replace` remain ordinary immutable nodes. A `DiffNode` is an
immutable parent scaffold containing unchanged branches and operation nodes. It has
no parent pointer or mutation methods. Its builder is the shared structural pattern
builder: ordinary occurrence replacement can rebuild a scaffold as a `PatternNode`.
Wrapping that rebuilt root in `DiffTree` preserves projection behavior.

An operation's selected payload is **opaque**: projection does not execute operations
inside that payload. This lets the inputs themselves contain insertions, deletions,
replacements, holes or entire patterns. The positional strategy quotes service-node
inputs with `Replace(before, after)`, even when their structures are equal. Ordinary
unchanged subtrees are reused. Difference/pattern scaffolds and legacy prototype views
are rebuilt iteratively through their concrete target builders. No `DummyNode` fallback
is used: a refused projection throws `ConstructionException`; unexpected errors propagate.

## Strategies

`PositionalDiff` computes structural hashes once as candidate filters, and always checks
actual equality after matching hashes. It retains equal prefixes and suffixes, then
pairs the remaining child occurrences in order. Unpaired children become insertions or
deletions in that same order. Different local types/data replace the affected subtree.
This is deterministic, but does not promise a minimum edit script or detect moves.

Named layouts are paired by position. If tags themselves change, or a named layout
changes arity, the whole parent becomes a replacement whose two payloads retain the
exact layouts. Thus a right-only optional child cannot silently become a left child.
The strategy works without schemas; projection uses the common checked reconstruction
helper, including explicit named bindings and validation of the final child order/tags.

`WholeTreeDiff` is a separate coarse strategy: it records a root replacement, insertion,
deletion or empty-to-empty result. It keeps original immutable endpoint objects and
requires no matching or reconstruction. Different strategies need not produce equal
difference trees; their endpoint guarantees are identical.

The former `Mapping`/`Mapper` node-identity tables and their recursive implementation
have been removed. `TopDownMapper.INSTANCE` remains a `DiffStrategy` facade for
`PositionalDiff`. Call `compare(Tree, Tree)` rather than `map(Node, Node)`. Consumers
needing their own alignment policy implement `DiffStrategy`; they need not manufacture
identity-keyed action maps. Existing input examples, including real parser trees,
now test the reversible contract instead of the old heuristic's exact edit counts.

## Editing and publication

```java
DiffTreeBuilder editor = new DiffTreeBuilder(before);
Node root = editor.getCurrentTree().getRoot().orElseThrow();
NodeOccurrence child = new NodeOccurrence(root).getChild(0);
boolean accepted = editor.replaceNode(child, DraftNode.create("Changed"));
DiffTree snapshot = editor.getDiffTree();
```

The builder is mutable and not thread-safe. Every returned diff and every input tree
remain unchanged after later edits. Explicit addresses refer to **the current after
snapshot** returned by `getCurrentTree()`. A stale or foreign root identity is rejected;
obtain a fresh address after an accepted edit. Two occurrences of one shared object
can be edited independently. Root replacement/deletion and empty-to-nonempty `build`
are supported.

`insertNode(parentOccurrence, index, node)` inserts an untagged child before the current
index (including the end boundary). Deletion compacts children and retains remaining
tags. To change a named layout, replace its parent with a valid constructed node.
Every direct edit rebuilds all affected ancestors, validates the resulting diff and
commits atomically. A false result changes neither current state nor published snapshot.
`build(target, strategy)` likewise verifies both projections before publication.

Legacy `replaceNode(Node, Node)`, `deleteNode(Node)` and `insertNode(Insertion)` remain
convenience adapters. They require unique object/prototype handles, never choose an
arbitrary occurrence of a shared object, and keep handles for rebuilt ancestors.
Repeated insertions at one legacy anchor retain declaration order. Legacy prototype
views are materialized into valid concrete input nodes before editing; use
`getCurrentTree()` to obtain their current addresses. Handle tables are builder-local
and may grow with the edit history; use occurrence APIs and short-lived editors for
large workloads. They are never stored in published diff nodes.

`ActionList.convertTreeToDiffTree` now throws `ConstructionException` when any requested
operation is refused. It cannot silently publish a partially applied command list.
This compatibility API applies commands in its existing insertion/replacement/deletion
order and requires valid intermediate states; atomic template/rule application uses the
separate common rule engine. Object-keyed ActionList is not an occurrence edit API.

## Parallel execution and cost

```java
try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
    ParallelOccurrenceWalker workers = new ParallelOccurrenceWalker(pool, 4, 1024);
    DiffStrategy strategy = new PositionalDiff(workers);
    DiffTree difference = strategy.compare(before, after);
}
```

Only independent paired branches at the top sibling frontier are submitted. Each worker
uses an iterative sequential traversal; no nested submissions occur. The supplied
strategy controls task count and splitting threshold. Results are collected in child
order, so scheduling cannot alter operation order. Input accessors must be safe for
concurrent reads. Projection calls user builders sequentially; it does not implicitly
parallelize external factories. Invoke from outside the supplied worker pool.

Hash maps are constructed once, then only read by workers. Frames and operation lists
are task-local. On failure/cancellation, the shared execution strategy cancels and joins
cooperative work, without closing the external pool. Traversal/projection check interruption;
individual hash/equality computations and user callbacks must finish before subsequent
checks. Parallelism is opt-in and is not guaranteed to help small inputs or chains.

Hash storage follows unique input nodes. Traversal and the generated difference follow
occurrences: shared subtrees at different positions may differ independently. Frame/list
storage and output size are proportional to the visited occurrence plans; equality has
its own temporary identity-pair memoization. Equal large candidates may be compared more
than once, so no strict linear-time guarantee is made for arbitrary DAGs. Projection
memoizes immutable scaffold results by identity while retaining every parent edge.
Neither strategy stores recursive Java frames proportional to tree depth.

Tests cover 20,000-deep chains, wide trees with 20,000 shared occurrences, hash collisions,
root transitions, operation payloads, schema-free tagged adapters, snapshots, stale
addresses, builder refusals and 300 generated graph pairs across pool sizes 1/2/4.
`DiffBenchmark.java` provides a separate timing/heap probe; it does not enforce a speedup
in CI. JSON's new structural format, including these scaffolds, remains the next task #20.


## Reproducible timing probe

Run with Java 21 after compiling the project:

```sh
mkdir -p /tmp/diff-probe
javac -cp target/classes -d /tmp/diff-probe scripts/DiffBenchmark.java
java -Xmx128m -cp target/classes:/tmp/diff-probe DiffBenchmark
```

The probe compares eight independent chains, then restores and checks both endpoints.
After four warmups it reports the median of five runs. One run on OpenJDK 21.0.13
with a four-thread pool and a 1,024-occurrence splitting threshold produced:

| Input occurrences | Sequential, ms | Parallel, ms |
| ---: | ---: | ---: |
| 137 | 2.405 | 2.576 |
| 1,033 | 6.840 | 7.890 |
| 8,201 | 11.210 | 15.212 |

The entire probe completed within the 128 MiB Java heap limit. Linux `ru_maxrss`
reported 179,440 KiB peak resident memory for the JVM process, including runtime/native
memory as well as heap. This is a process-wide measurement, not per-operation allocation. Timings depend on the machine and
warmup; the larger case did not benefit from parallelism. Sequential execution remains
the default, and callers should measure their own input shapes before enabling workers.
