# Occurrences and traversal

A `Node` is immutable and compares structurally. The same object may occur more than
once. `NodeOccurrence` addresses one position using **root object identity and child
indices**. Equal nodes, or two edges pointing to the same node, do not merge addresses.
Addresses from separately constructed wrappers around the same root and path are equal.
Addresses are relative to that immutable root; obtain new addresses after rebuilding it.

```java
NodeOccurrence root = new NodeOccurrence(node);
NodeOccurrence right = root.getChild(1);
Optional<Node> changed = new OccurrenceReplacer().replace(right, replacement);
```

Parent, sibling index and tag belong to the occurrence. `getTag()` reads
`parent.getNode().getChildTag(index)`; source fragments remain on the node.
The root has depth zero, index minus one and no parent, sibling or tag.
Ancestors are nearest-first and exclude the addressed node. Their iterators can be
retained safely after a traversal callback returns.

Each address stores one parent link, an index and a cached address hash. Navigating to
a child takes constant space and time; address equality takes at most the path length
and uses no recursive calls. Traversing a chain does not store quadratic path copies.

## Sequential operations

`OccurrenceWalker` uses iterative preorder: parent, then children left to right.
`findFirst` stops immediately at the first match; `findAll` **prunes below matches**;
`collectAll` visits every occurrence. Callbacks run sequentially on the calling thread.
Traversal frames use O(depth) space, excluding returned addresses/results. A shared
DAG is unfolded by occurrence, so the number of visits can exceed the object count.
Inputs must be acyclic and remain unchanged during the operation.

`DepthFirstWalker` retains its node-facing search semantics and delegates to the same
traversal. `NodeSelector.selectOccurrences` visits all descendants, even below matches.
Its legacy `select` returns an identity set and therefore intentionally collapses
multiple occurrences of the same object. Use occurrence results for navigation/edits.

`DepthCalculator` uses iterative postorder and an operation-local identity cache.
It computes a shared subtree once, rejects cycles, retains no input trees between
calls, and counts a leaf as depth one. `ExtNodeCreator` is also iterative and creates
a separate wrapper with parent/sibling links for each occurrence; structural hash
memoization does not merge these wrappers. Hash collisions still require equality checks.

`Node.getChildrenList()` now returns a standard immutable, non-null `List` snapshot.
The old manual `ChildrenList` and `ChildrenIterator` implementations have been removed.
Obtaining this snapshot costs O(child count); indexed node traversal need not allocate it.

## Editing

`OccurrenceReplacer` rebuilds only the addressed ancestor chain through builders and
reuses all other children. It preserves type, data, fragment, child order and edge tags.
Named children are bound explicitly where supported; otherwise allocation is delegated
to `setChildren`. Reconstruction is checked before publishing the new root: builders
that reorder, retag or clone untouched children do not silently change the requested edit.
A refusal (including `ConstructionException`) returns `Optional.empty()`. Unexpected
programming exceptions propagate. No partial rebuilt tree is returned on refusal.
Readonly derived properties are supplied by rebuilt nodes, not copied into builders.

Builders without enough binding support may refuse reconstruction even when a manually
constructed node would be valid. For example, ambiguous mixed named and repeated slots
require a capable builder. The algorithm never guesses a different edge binding.

Legacy `NodeReplacer` intentionally replaces **all reference-identical occurrences**.
It is now iterative, preserves metadata and tags, and throws `ConstructionException`
if an ancestor refuses rebuilding. Its legacy `Pair`/`DummyNode` result protocol remains
for existing callers. New code should use occurrence addresses instead.

## Explicit parallel strategy

`ParallelOccurrenceWalker(executor, parallelism, threshold)` is opt-in. The caller owns
the executor and calls the operation from outside that pool. At most `parallelism`
tasks split independent root-child branches into stripes; workers never submit tasks.
Small trees and roots with at most one child take the sequential path. A bounded probe
counts occurrences, not unique objects. This strategy does not promise acceleration
for a long trunk that branches only far below its root.

`collectAll`, `findAll`, `findFirst` and `depth` preserve their sequential results.
Results are assembled in child order, regardless of task completion order. Matching
parents prune descendants. Each shared occurrence is searched separately; depth caches
are local to individual branch computations.

Predicates must explicitly support concurrent calls and must not depend on invocation
order. Parallel `findFirst` can speculatively evaluate later branches, including their
exceptions, but returns the first match in **tree order**, not the fastest match.
Use the sequential walker when callback side effects or immediate stopping matter.
Failure/interruption cancels submitted futures, and interruption restores the caller's
interrupt flag. Cancellation is cooperative: a user callback that ignores interruption
can continue running. The executor is never closed by the algorithm.

Tests cover 30,000-level chains, broad shared trees, different worker counts, forced
out-of-order completion, pruning, stable ancestors, tagged single-occurrence replacement,
metadata preservation, cycle rejection and standard Java List contracts.
