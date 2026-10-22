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
`Pattern` and `DiffTree` still require a non-null root in their constructors;
they inherit `Optional<Node>` rather than overriding it with incompatible
`Optional<PatternNode>` or `Optional<DiffNode>` return types.

## Empty-tree behavior

| Consumer | Empty-tree behavior |
| --- | --- |
| `Tree.toString()` / `getLanguage()` | Empty string |
| `Tree.deepCompare(Tree)` | Two empty trees match; empty and non-empty do not |
| `Transformer.transform(Tree)` | Empty result without invoking the Node callback |
| `LabeledTreeBuilder` | Empty result |
| Pattern matching / default patcher | No matches; input remains empty |
| JSON serializer | Empty object `{}` without a root member |
| JSON deserializer | Missing/null root yields an empty tree without requesting a factory |
| DOT / PNG / SVG | Empty graph, with no placeholder node |
| `PatternBuilder`, `DiffTreeBuilder`, `SubtreeBuilder` | Constructor rejects empty input with `IllegalArgumentException` |
| `DiffTreeBuilder.build(Tree, Mapper)` | Empty target returns false without changing the builder |

A Node transformer returning null violates its contract and throws; it does not
implicitly delete a tree. Root insertion/deletion in difference trees belongs to
#19. The legacy deserializer's error/fallback policy is unchanged; invalid input
may still become an empty tree or a DummyNode tree until #20 replaces that policy.
An empty JSON object does not represent a DummyNode tree.

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

This is the next implementation stage of #11, not completion of the entire issue.
Legacy PatternNode/DiffNode mutations and lazy wrapper state still require their
model migration, coordinated with #14, #16 and #19. The whole object graph must
satisfy the immutable contract before general concurrent access is promised.
