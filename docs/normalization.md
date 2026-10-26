# Immutable normalization

`new Normalizer().normalize(root)` converts readonly properties into ordinary tree
nodes. `new NormalizedNode(root)` is a convenience constructor for the same operation.
The result is an independent representation: **denormalization and recovery of the
original concrete classes or writable properties are not promised**.

Normalization never calls an original `Type.createBuilder()`. This includes adapters
that have no child schema or cannot construct their original parser nodes.

## Representation

Each normalized source node has its original data string and fragment, an independent
normalized type, and these children:

| Position | Node | Meaning |
| --- | --- | --- |
| 0 | `Properties` | Original node's `getProperties()` |
| 1 | `Properties` | Original type's own `getProperties()` |
| 2 onwards | `NormalizedEdge` | Original child occurrences, in source order |

Both property groups exist even when empty. They are untagged, so they cannot collide
with arbitrary source tags. Type properties are not merged with node properties or
inherited base-type properties. A node whose default properties come from its type
will intentionally have equal but separately meaningful groups.

A `Properties` node contains `Property` leaves sorted by their original key using
`String.compareTo` (case-sensitive UTF-16 order). Its builder rejects duplicate keys.
`Property.getName()` returns the original key and `getData()` returns the value.
Property order is independent of the source map's insertion or iteration order.
Changing properties therefore changes structural equality and structural hashes.

Each `NormalizedEdge` contains exactly one target node and the source occurrence's
optional tag. The tag is explicit ordinary node data:

- `N`: no tag;
- `T` followed by the exact tag: a present tag (`T` alone is a present empty string).

`NormalizedNode.getChildTag(i + 2)` returns the original tag at index `i`.
`getChild(tag)` returns the corresponding **edge node**; its `getChild(0)` is the
normalized target. Edges have no tag on their own single child. This extra node
makes occurrence metadata editable and serializable without storing tags on a shared
target object. Distinct edges may point to the same immutable target.

For example:

```java
NormalizedNode normalized = new Normalizer().normalize(original);
Node leftEdge = normalized.getChild("left").orElseThrow();
Node leftTarget = leftEdge.getChild(0);
Properties nodeProperties = (Properties) normalized.getChild(0);
Map<String, String> values = nodeProperties.collectAllProperties();
```

The following globally reserved type names/prefixes identify the representation:

- `AstranautProperties`: a property group;
- `AstranautProperty` + hexadecimal UTF-16 key: one named property;
- `AstranautNormalized` + hexadecimal UTF-16 original type name: a source node;
- `AstranautNormalizedEdge`: a child occurrence.

The hexadecimal encoding uses four lowercase digits per UTF-16 code unit. It is
injective even for empty strings and unpaired surrogates, and produces identifier-safe
names. The edge suffix is outside the hexadecimal alphabet. Applications must not
reuse these reserved service names for unrelated types. No namespace property is
used for identity. `NormalizedNode.getOriginalTypeName()` provides a readable source
name for diagnostics. Tags, values, child counts and property sets do not change
normalized type identity; adding or removing children does not turn it into a new type.

## Construction and ordinary algorithms

All published state is final and all child collections are immutable snapshots.
Builders remain mutable and independent. Changes to a builder or its input list do
not affect a previously built node.

Normalized-node builders accept exactly two leading `Properties` nodes followed by
zero or more `NormalizedEdge` nodes, with unique present tags. They accept any data
string and never check the source type's child/data constraints. Removing an edge is
valid; removing a required metadata group leaves an invalid builder until a complete
`setChildren` succeeds. Rejected edits are atomic. Index edits use the current child
list and preserve the existing tag; a deliberate retagging uses `setChildren` with
new edges. Unknown tagged edits return false.

An edge builder accepts exactly one target and valid encoded tag data. A property
builder accepts string data and no children. A properties builder accepts unique
property leaves and publishes them sorted by key. Synthetic property metadata has
an empty fragment; normalized source nodes and their occurrence edges retain source
provenance. Generic rebuilds preserve fragments through their builders.

The representation can be traversed, compared, edited, diffed and serialized as
ordinary nodes. Tests exercise `OccurrenceReplacer`, the existing top-down diff and
JSON serialization. Edge data retains normalized tags, while the
[general JSON format](json.md) also stores ordinary occurrence tags explicitly and
supports configurable unknown-type handling.
Restoring normalization classes requires a factory registering their types, just as
with other concrete node types.

The builder accepts inserted edge targets as supplied; it does not silently normalize
or coerce them again. To insert another normalized subtree, normalize it explicitly:

```java
Node target = new Normalizer().normalize(other);
NormalizedEdge edge = new NormalizedEdge(Optional.of("right"), target);
Builder builder = normalized.getType().createBuilder();
builder.setData(normalized.getData());
builder.setFragment(normalized.getFragment());
List<Node> children = new ArrayList<>(normalized.getChildrenList());
children.add(edge);
if (!builder.setChildren(children)) {
    throw new IllegalArgumentException("Incompatible children or duplicate tag");
}
Node updated = builder.build();
```

## Depth, sharing and parallel execution

Sequential normalization uses an explicit postorder stack, identity memoization and
an active-path identity set. Each distinct source object is normalized once per call;
shared targets remain shared, and different occurrence tags stay on different edges.
Cycles and duplicate source tags are rejected. A 30,000-edge input chain is covered
without recursive Java calls. An already normalized root is returned unchanged by
`Normalizer.normalize`; there is no recursive normalization of later manual edits.
The result retains no original node or type descriptor, only immutable provenance.

Parallel execution is explicitly selected by the caller:

```java
try (var pool = Executors.newFixedThreadPool(4)) {
    ParallelOccurrenceWalker strategy = new ParallelOccurrenceWalker(pool, 4, 32768);
    NormalizedNode result = new Normalizer().normalize(root, strategy);
}
```

The existing branch mapper supplies bounded striped tasks, deterministic result order,
threshold fallback, cancellation and exception propagation. The caller owns the pool
and must invoke the operation from outside that pool. Source adapters must support
concurrent reads. No nested tasks are submitted and no builders are shared.

Memoization is local to each parallel branch: a target shared across root branches
may be normalized more than once. This changes sharing topology, not structure, tags,
properties or provenance. Prefer sequential normalization for graphs with extensive
cross-branch sharing. Single-child roots and inputs below the threshold stay sequential.
The default `normalize(root)` is always sequential.

The manual performance probe is reproducible with Java 21 after compiling core:

```sh
java -Xmx1g -cp target/classes scripts/NormalizationBenchmark.java
```

It measures 32 independent branches using four platform threads, six paired warmups
and medians of nine paired samples. Timing is diagnostic and is never a CI assertion.

On the development container (OpenJDK 21.0.13, `-Xmx1g`, four workers), the observed
medians for this implementation were:

| Source nodes | Sequential, ms | Parallel, ms |
| ---: | ---: | ---: |
| 129 | 0.426 | 0.770 |
| 1,025 | 0.594 | 2.273 |
| 8,193 | 2.451 | 7.545 |
| 32,769 | 9.077 | 2.912 |
| 65,537 | 33.002 | 8.102 |

These are single-environment measurements, not portable speed guarantees. Based on
this probe, **32,768 source occurrences** is a conservative starting threshold for
independent branches; tune it for the actual property volume, DAG sharing and machine.
The threshold and parallelism remain caller choices. Small trees should use the
sequential path; parallel execution is not automatically enabled.
