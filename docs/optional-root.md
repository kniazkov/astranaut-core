# Optional tree roots

`Tree.getRoot()` returns `Optional<Node>`. `new Tree()` and
`EmptyTree.INSTANCE` have no root. `new Tree(node)` requires a non-null root and
preserves its identity; `new Tree(null)` fails immediately. A tree containing
`DummyNode.INSTANCE` is non-empty. No hidden placeholder is inserted into an
empty tree's child traversal, JSON or DOT output.

```java
Tree empty = new Tree();
Tree populated = Tree.createDraft("Addition(Left,Right)");
populated.getRoot().ifPresent(node -> System.out.println(node.getTypeName()));
Node required = populated.getRoot().orElseThrow();
```

Unwrap with `orElseThrow()` only when the surrounding contract guarantees a root.
General consumers should handle absence explicitly. `getRoot()` is final: a Tree
subclass cannot replace the stored root or change the emptiness contract.
`Pattern` requires a non-null root. `DiffTree()` supports empty-to-empty differences;
its other constructor accepts a non-null ordinary, scaffold or operation root.
Both inherit `Optional<Node>`.

## Empty-tree behavior

| Consumer | Empty-tree behavior |
| --- | --- |
| `Tree.toString()` / `getLanguage()` | Empty string |
| `Tree.deepCompare(Tree)` | Two empty trees match; empty and non-empty do not |
| `Transformer.transform(Tree)` | Empty result without invoking the Node callback |
| `LabeledTreeBuilder` | Empty result |
| Pattern matching / default patcher | No matches; input remains empty |
| JSON serializer | Versioned empty table with explicit `root: null` |
| JSON deserializer | Explicit null root and empty table yield an empty tree without resolving types |
| DOT / PNG / SVG | Empty graph, with no placeholder node |
| `PatternBuilder`, `SubtreeBuilder` | Constructor rejects empty input with `IllegalArgumentException` |
| `DiffTreeBuilder` / `build(Tree, DiffStrategy)` | Both empty states and root insertion/deletion are supported |

A Node transformer returning null violates its contract and throws; it does not
implicitly delete a tree. [Difference trees](differences.md) represent root insertion/deletion
explicitly. The [JSON reader](json.md) rejects malformed input with `TreeJsonException`.
A missing root member or an empty JSON object is invalid; neither silently creates a tree.

## Immutability boundaries

The root reference is final and safely published with its Tree. Tree does not
clone or normalize its root: ordinary nodes and shared subtrees retain their
identity. The producer must supply nodes whose data, ordered children, tags,
type descriptors, properties and complete source provenance remain stable.
A read-only view over a changing parser object does not meet that contract.
Builders and user callbacks are not automatically thread-safe.

Tests cover empty/non-empty comparisons, null rejection, transformations,
labeling, JSON, DOT, pattern matching and explicit builder refusals. Concurrent
reads of a published tree with a shared child are tested after its builder has
been reused; the built children list cannot be modified through the Node API.
Existing type/schema and wildcard-Optional getter tests continue to apply.

PatternNode, DiffNode and the built-in subtree/labeled views now publish immutable
snapshots without lazy child mutation. See [the model audit](immutable-model.md) for
ownership, publication, adapter obligations and the separate consumer/generator work.
