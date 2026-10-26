# Common atomic rule engine

`RuleEngine` executes **one pass**. It is the shared coordinator for ordinary
`RewriteRule` implementations, `TemplateRule`, generated `ConverterRule`, and mined
`PatternRule`. `DefaultPatcher` delegates to this engine. `DefaultTransformer`
also delegates, retaining its compatibility loop until structural stability.
The reusable stabilization/cycle policy remains issue #18.

```java
RewriteRule rule = new TemplateRule(
    DraftNode.create("A"),
    DraftNode.create("B")
);
Tree result = new RuleEngine(List.of(rule))
    .apply(Tree.createDraft("Root(Inner(A))"));
// Root(Inner(B))
```

## Traversal and priority

The engine visits child lists bottom-up, from left branches to right branches.
All descendants of a sibling sequence are processed before rules on that sequence.
The root is treated as a one-element sequence after its descendants. Empty trees
invoke no rules. Traversal and ancestor reconstruction use explicit stacks.
Structural matching of deeply nested ordinary node patterns is iterative as well;
DSL combinator evaluation and the specialized legacy mined matcher retain their
own interpretation algorithms.

Within each sibling list, rules scan in **declaration priority order**, each in
its own direction. A right-to-left rule visits descending candidate starts; the
matched sequence itself remains ordered left-to-right. The first accepted rule
owns its consumed occurrences. Outputs are marked as processed for this pass,
so lower-priority rules cannot rewrite those outputs or overlap consumed inputs.
A subsequent pass can process them. A refused proposal leaves occurrences available
for lower-priority rules. A structurally unchanged proposal, including unchanged
edge tags, is not progress and does not block another rule.

`RewriteRule.apply(nodes, index)` attempts exactly one start on the current
immutable sibling list. It returns an optional `Replacement`: a positive consumed
length, immutable output nodes, and optional explicit output tags. Deletion is an
empty output sequence. Consuming past the input is a rule implementation error,
not normal non-applicability. Rules must return valid nodes; specialized generated
matchers/builders need not be replaced by a generic interpreter.

## Atomicity and bindings

Each proposal is built in temporary storage through the current parent and **every
ancestor to the root**. Only if all builders accept does the coordinator publish
the whole chain. Rejected setters, invalid builder state, or `ConstructionException`
refuse the entire local application. Earlier independent successful applications
remain. Other exceptions propagate unchanged; routine refusal is not logged.

The reconstruction implementation is shared with `OccurrenceReplacer` and
`NodeReplacer`. Parent data, fragments and unaffected edge tags are retained.
Named bindings use `setChild(tag, child)` and are not subsequently erased by a
positional reset. Adapter reconstruction can use `setChildren`. Every built result
is checked for accidental reordering, lost tags and changed provenance. Properties
are supplied by the rebuilt node, not copied into its builder.

Default replacement bindings preserve positional tags when input/output arities
match. Deletion removes the consumed edges. Changed nonempty arity involving tagged
inputs requires explicit output bindings, rather than guessing from shared child
identity. Explicit bindings are still validated by the parent builder. A root must
have no tag and contain zero or one node; multiple roots refuse the proposal.

A tag-only edit is a structural change. Two references to the same immutable node
remain separate occurrences. The mined-patch adapter now stores local commands by
`NodeOccurrence`; replacing one occurrence and deleting another cannot collapse
into two commands against the same object. Its local pattern keeps the historic
child-subsequence matching semantics. The legacy identity-based `ActionList` API
used by diff code remains separate; it is no longer the patcher's execution path.

## Provenance and adapters

`TemplateRule` computes `Fragment.fromNodes` over the entire matched sequence,
including discarded separators. `TemplateSubstitutor(fragment)` gives that span
to newly constructed template nodes while retaining captured objects and their
fragments. Incompatible sources produce absent provenance.

`ConverterRule` retains the converter's matcher, minimum consumption, scan direction
and factory. Its new output root is reconstructed with the complete matched span;
a returned captured input/descendant is reused unchanged. A generated converter
that constructs additional nested nodes is responsible for their construction and
provenance under the same contract. Legacy `NullNode` conversion results are adapted
to deletion at this boundary only.

The tree overload of `DefaultTransformer` supports an empty result. Its node-only
overload cannot represent an empty tree and throws `ConstructionException` if the
root is deleted. Builder refusal now preserves the input instead of returning a
`DummyNode`. `PatternRule` validates its entire local patch before the common engine
attempts to rebuild outer ancestors.

## Explicit parallel proposals

Pass a caller-owned `ParallelOccurrenceWalker` strategy to the engine to enable
bounded speculation. Only rules explicitly returning `isConcurrent() == true`
are invoked concurrently. This declaration includes every matcher and output
factory they call: they must be deterministic, side-effect-free and safe for
speculative execution. Other rules remain on the calling thread. Do not invoke
this operation from inside its own bounded pool.

The configured threshold and worker count are reused; no pool is created per node
and no nested worker submission occurs. Workers calculate proposals on one immutable
sibling snapshot. The coordinator accepts them in declared rule/scan order and
calls ancestor builders sequentially. Every accepted edit invalidates the complete
proposal cache before further matching, including proposals depending on neighbors.
Thus completion order cannot alter priority or silently apply stale matches.

Unexpected worker errors propagate. On cancellation/failure, the start gate closes,
queued work is cancelled and running operation-owned callbacks are joined. Callbacks
must cooperate with interruption. The external executor remains open.

This conservative strategy favors correctness over speculative reuse. It can perform
extra matching and may be slower on small lists or cheap rules. Full ancestor
validation also means repeated edits of very wide immutable parents can be costly.
Parallel execution is opt-in; no speedup is promised for chains or small trees.
