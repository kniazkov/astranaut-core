# Stabilization, cycles and cancellation

`RuleEngine.apply(tree)` performs one bottom-up pass. `RuleStabilizer` repeats the
same engine until two consecutive results are structurally equal. `DefaultTransformer`
uses this policy too, so generated converters no longer loop silently on cycles.
`DefaultPatcher` continues to perform one pass.

```java
RuleEngine engine = new RuleEngine(List.of(
    new TemplateRule(DraftNode.create("A"), DraftNode.create("B")),
    new TemplateRule(DraftNode.create("B"), DraftNode.create("C"))
));
Tree result = new RuleStabilizer(engine)
    .apply(Tree.createDraft("Root(A)"));
// Root(C)
```

Each pass preserves the engine's declaration priority, direction, bottom-up traversal,
local rollback and independent successful changes. Accepted outputs become candidates
again on the next pass. Empty trees are stable. Non-applicable rules and structural
no-ops are ordinary outcomes, regardless of how many rules were attempted.

## State and determinism

A checkpoint is taken only **between complete passes**. At that boundary the traversal
stack, candidate position, settled markers and priority cursor are reset. The fixed
rule list and engine configuration, together with the tree, define the next pass.
No unfinished worker state participates in cycle detection. Parallel and sequential
engines use the same checkpoint policy and structural comparisons.

Repeated execution requires a deterministic structural transition: applicability,
outputs and builder acceptance must depend on type, data and ordered children with
their tags. Rules depending on invocation counts, time, object identity, properties
or source metadata do not satisfy this contract. Normalize relevant metadata into
structure first. The engine cannot establish determinism for arbitrary user code.
This requirement applies even to rules that are not declared concurrent; concurrent
rules additionally satisfy the thread-safety/speculation contract of `RewriteRule`.

Equality uses `Tree.deepCompare` / `NodeStructure`, including edge tags, and ignores
sharing topology and source metadata. Hashes are not used as proof of equality (or
as a prefilter here). Thus a hash collision cannot cause a false cycle. A tag-only
change is progress, and a tag-only cycle is detected.

## Detection and diagnostics

Brent's cycle algorithm retains a checkpoint and advances it at exponentially spaced
pass boundaries. It executes exactly one engine pass per iteration, without replaying
rules on a second traversal. Adjacent equality returns the stable result before any
cycle check. A repeated nonadjacent checkpoint raises `RewriteCycleException`.

The exception exposes `getCheckpointPass()` (zero-based earlier equal boundary) and
`getPeriod()` (distance between repeated states). These are `BigInteger` counters:
there is no integer overflow that becomes an accidental pass limit. The checkpoint
is not necessarily the first entry into the cycle; no extra replay is performed to
find that entry. Diagnostics retain counters, not trees. For deterministic transitions
the reported distance is the cycle length. Detection may occur later than the first
repeat, but requires O(prefix length + cycle length) completed passes.

There is **no fixed step limit**. A nonrepeating infinite sequence, including a tree
that keeps growing, cannot in general be recognized as nonterminating. Use cancellation
when the caller no longer wants to continue.

## Memory and cost

History storage is a constant number of tree roots: the checkpoint and current/next
states (plus the caller's input if still retained). Unchanged immutable subtrees may
be shared. Superseded checkpoints and intermediate states are not kept in a collection.
The counters occupy O(log completed passes) bits, not a bounded-width step budget.

This is not constant total memory: retained trees can grow, a checkpoint can pin old
subtrees, and each engine pass has its own builders, frames and speculative proposals.
Each structural comparison has temporary identity-pair memoization and traversal state;
its cost follows the compared graphs/pairs, rather than just the number of passes.
There are at most two whole-tree comparisons per completed pass. The regression suite
covers 30,000 finite passes and wide/shared and deep trees with 20,000 occurrences.

## Cooperative cancellation

Interrupt the thread calling `apply`, for example with `Future.cancel(true)` when the
whole operation is submitted to a caller-owned executor. The coordinator must run
**outside** the bounded worker pool supplied to `ParallelOccurrenceWalker`.

The engine checks interruption between traversal frames, rule candidates and ancestor
rebuilds, before/after rule callbacks, and at pass/comparison boundaries. Cancellation
throws `java.util.concurrent.CancellationException` and preserves the interrupt flag.
No partial result is returned; the immutable input remains available to the caller.
An already interrupted call is cancelled even for an empty input.

When the coordinator is interrupted while waiting for workers, the start gate closes,
queued futures are cancelled and running callbacks are interrupted and joined. Worker
failures are observed in completion order, so a later failing worker cannot hide behind
an earlier still-running worker. Accepted proposals still follow tree/rule order. The
external executor is never closed. Unexpected errors propagate; construction refusal
retains its normal local semantics.

Cancellation is cooperative, not a hard deadline. User matchers/builders must return
or honor interruption; the library cannot stop a callback that ignores it. A currently
running structural comparison or individual reconstruction completes before the next
checkpoint. `Future.cancel(true)` itself does not join the coordinator; callers needing
a completion barrier must arrange one around the operation. The operation itself joins
its workers before unwinding. No background work is created by the stabilizer.

## Reproducible bounded-heap probe

After compiling the project with Java 21:

```sh
mkdir -p target/bench
javac -cp target/classes -d target/bench scripts/StabilizationBenchmark.java
java -Xmx64m -cp target/classes:target/bench StabilizationBenchmark
```

The probe verifies 100,000 changed passes on a leaf and 1,000 changed passes on a
root with 10,000 shared leaf occurrences, under the same 64 MiB heap cap. Both completed
on the development environment (about 0.4 s and 0.7 s respectively; illustrative timings,
not CI performance assertions). This exercises bounded history on long runs and large
snapshots; it is not a bound on memory for arbitrary growing trees or user callbacks.

The engine now reuses an immutable sibling-list snapshot while trying candidates and
refreshes it after every accepted edit. This avoids repeatedly copying a wide sibling
list for unsuccessful attempts, while preserving invalidation after changes.
