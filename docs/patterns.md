# Immutable patterns and captures

`Insert`, `Delete`, `Replace`, `Hole` and `PatternOperator` are immutable `Node`s.
Their children participate in ordinary traversal, structural equality, cutting
and reconstruction. Their types create independent builders. The hole's type is
its **service type**, not the type accepted by the hole: `getConstraint()` returns
an optional `AbstractType`, and `accepts(node)` uses `isSubtypeOf`.

`PatternNode` snapshots an ordinary node into an editable-by-copy template.
`withHole(index, number)` and `withChildren(children)` return new nodes and retain
parent-edge tags. `PatternBuilder` is mutable mining state, but each `getPattern()`
publishes an independent snapshot. Previously published patterns never change.
The target descriptor is retained by `getTargetType()`, including after rebuilding
through the pattern's own builder; substitution still uses the concrete target's
validation. Derived properties are not transferred through builders.

## Matching

`PatternMatcher.match(pattern, node)` returns optional attempt-local `Extracted`.
`find(pattern, nodes, reverse)` returns the first successful nonempty occurrence,
its half-open indexes, and its captures. It does not modify the input tree.

```java
Node sequence = new PatternOperator(PatternOperator.Kind.SEQUENCE, "", List.of(
    new Hole(expressionType, 1),
    DraftNode.create("Stop")
));
Optional<PatternMatcher.Match> match = new PatternMatcher()
    .find(sequence, root.getChildrenList(), false);
```

A `Hole(number)` accepts any node. `Hole(AbstractType, number)` accepts that type
and its subtypes, including multiple inheritance. The `Hole(Node, number)` overload
is for mining a constraint from a prototype. Holes with the same slot accumulate
nodes in matching order, without an equality test. Captured objects are retained.

`PatternOperator` supplies immutable expressions:

| Kind | Data | Operands / behavior |
| --- | --- | --- |
| `ANY` | empty | No operands; one arbitrary node |
| `DATA` | literal | No operands; exact data equality |
| `DATA_HOLE` | decimal slot | No operands; append candidate data to the slot |
| `RANGE` | two UTF-16 bounds | No operands; inclusive single-character range |
| `AND` | empty | All operands on the same node, left to right |
| `OR` | empty | First successful operand, left to right |
| `NOT` | empty | One condition; negate and discard its captures |
| `OPTIONAL` | empty | One sequence element, greedily zero or one occurrence |
| `REPEAT` | empty | One sequence element, greedily zero or more occurrences |
| `SEQUENCE` | empty | Ordered elements, possibly empty |
| `CHILDREN` | empty | Match the candidate's entire child sequence |
| `WITH_DATA` | empty | Output-only node template followed by data expressions |

Ordinary node patterns compare type (accepting subtypes), data and the complete
ordered child sequence. Explicit pattern tags must match the candidate edges.
To constrain only a type, use a typed hole. Conditions can be combined, for example
`AND(typedHole, dataHole, CHILDREN(...))`. A negative typed capture is expressed as
`AND(NOT(typedHole), untypedHole)`; the negated trial never contributes captures.

Semantics follow the [pinned Astranaut DSL README](https://github.com/kniazkov/astranaut/blob/6a418bbb4851eabd391cfcf33ed0ba255ffa29b7/README.md)
and its generator: optional/repeated elements are greedy **without backtracking**.
Reverse search changes the order of candidate starts, not the order of elements
within a candidate or the order of captured values. Zero-width matches do not
advance search or repetition. A failed sequence, conjunction, alternative or
candidate start commits nothing.

Node slots and data slots are independent. Data concatenates in matching order;
repeated numbers do not require equal strings. `Extracted.getNodes(int...)` returns
an immutable snapshot, grouping slots in the requested order. `fork()` isolates
speculation and `replaceWith(successful)` commits it. The persistent immutable log
makes appending, forking and committing constant-time; reading a slot scans the log.

The same immutable pattern and stateless matcher can be shared by threads. Each
match owns its mutable `Extracted`; do not concurrently mutate that result. This
API does not parallelize competing alternatives or change their priority.

## Substitution

`TemplateSubstitutor.substitute(template, captures)` returns `Optional<Tree>`:

- A hole expands to **all** nodes in its node slot, retaining object identity,
  type and fragment. Repeating an output slot intentionally creates sharing.
- The parent builder receives the resulting complete child sequence. Tagged
  edges are retained; a named single-child edge cannot expand to several nodes.
  Refusal, missing required children, incompatible types or lost tags reject the
  whole substitution. Unexpected implementation exceptions propagate.
- At the root, zero output nodes produce a present empty tree, one node produces
  a root, and multiple nodes return `Optional.empty()` (refusal).
- `Insert` emits its after node; `Delete` emits nothing; `Replace` emits its after
  subtree. The before subtree is not rebuilt. `SEQUENCE` concatenates output.
- `WITH_DATA(template, DATA(...), DATA_HOLE(...), ...)` concatenates literal and
  captured strings, then builds the template with that data and expanded children.
  The target builder validates the concrete value.

Substitution is iterative and memoizes shared template dependencies by identity.
It builds new parent nodes through their builders. It shares captured nodes rather
than rebuilding or coercing them. Reconstruction uses the same implementation as
`OccurrenceReplacer`, including the checks for accidental reordering or retagging.

## Migration boundary

This is the core representation and single-attempt API for issue #16. The existing
mined-patch matcher keeps its historic child-subsequence search behind the common
rule engine; it isolates candidate trials and no longer interprets
repeated hole numbers as data equality. It is not the DSL-expression interpreter.

Astranaut itself is unchanged in this PR. Its generator should emit these immutable
expressions and call the transactional matcher, or explicitly use `fork()`/commit
around speculative generated conditions. Existing generated direct `&&`/`||`
mutations of one `Extracted` do not gain transactional semantics automatically.
Whole-tree bottom-up application and ancestor rollback are implemented by the
[common rule engine](rule-engine.md). Stabilization is described in
[its guide](stabilization.md); [JSON version 1](json.md) preserves pattern scaffolds,
operations and typed/untyped holes.
