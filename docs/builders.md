# Builders and allocation

`Builder` remains an interface. Implementations may use their own state and validation,
with or without a schema. `SchemaBuilder` and `ChildBindings` are optional helpers,
not requirements for generated builders or parser adapters.

- `setChild(tag, child)` fills or replaces a known tagged position. New children are
  appended to the builder's insertion order; replacing a child retains its position.
- `setChild(index, child)` replaces an existing occurrence in that insertion order,
  preserving its binding. It never appends. Invalid indexes and unknown tags return false.
- `removeChild(tag)` and `removeChild(index)` remove an occurrence; later indexes shift.
  Removing a required child succeeds and makes construction invalid.
- `setChildren(list)` resets all bindings. Success requires a complete, unambiguous
  child allocation, even if data is still incomplete. Input order becomes the builder's
  insertion order; the built node uses schema order. Missing optional children are omitted.
- A false return changes nothing. `isValid()` is a pure check of complete construction
  requirements, including implementation-specific data validation.
- `build()` produces a detached valid node, or throws `ConstructionException` with a
  diagnostic reason. Reusing the builder cannot change already published nodes.
  Unexpected programming errors are not wrapped as construction refusals.

For example, adding `right` before `left` is valid as an intermediate state. Index zero
then refers to `right`. A built Addition still exposes children in `left`, `right` order.
`Builder` defaults reject individual child edits: sufficient for leaves; composite
implementations override the supported operations. Nulls are not child or data values.

## NodeAllocator

The allocator works with occurrences, including repeated references to the same Node.
`allocate(List<Node>)` returns an optional immutable list of per-schema-position lists,
including empty optional/repeated slots. The array overload supports single-valued
schemas, clears absent optional slots on success and leaves the entire array unchanged
on rejection. Repeated schemas use the list overload.

An allocation is acceptable when types and cardinalities match and every input is used
exactly once. If two occurrences fit both of their positions, their relative source
order must be preserved. A remaining second allocation is an ambiguity and is rejected:
for example, one Expression cannot choose between two optional Expression positions.
No lexical type-name ordering, Map iteration order or object equality chooses a slot.

Search is iterative, stops at a second solution and prunes unreachable required slots.
It has exponential worst-case complexity for overlapping schemas; there is no silent
step limit. Single repeated slots support wide input without recursion. Invocation state
is local, so even a shared allocator can serve concurrent callers; builders and bindings
remain mutable objects owned by individual tasks.

Tests compare allocation with exhaustive enumeration for all schemas of up to three
positions, two overlapping types and all three cardinalities, and inputs of up to three
occurrences. Separate tests cover 30,000 repeated children, shared references, atomic
failure, concurrent calls, incomplete builders, tagged/indexed edits and snapshot reuse.

## Scope

This stage migrates core and its consumer fixtures to `setChildren` / `build`. The external
astranaut generator is unchanged. The shared reconstruction helper now serves
occurrence edits, pattern/difference projection and [JSON reading](json.md). Draft
builders also accept complete tagged layouts. Pattern/difference snapshots and
[optional tree roots](optional-root.md) have been migrated in their respective stages.

## Contract acceptance (#12 and #26)

The common pair contract in `SharedBuilderContractTest` runs unchanged against two
implementations: the generated-style `Addition` fixture (using `SchemaBuilder` and
`NodeAllocator`) and `IndependentPair` (a hand-written schema-free adapter with its own
insertion-ordered state). The latter does not delegate to core binding/allocation helpers.
Both expose the same two required Expression positions, so the same assertions apply.
This is a unit-level consumer fixture, not generated output or an external JAR test.

Each rejected operation is tested on a fresh, reverse-filled builder. Tests compare the
published node and child identities, then edit index zero to expose any hidden damage to
insertion order. They also test incomplete construction, deletion and index shifts,
repeated build, full replacement, mutable caller lists, and shared child occurrences.
The same construction scripts run sequentially and in a four-thread pool with separate
builders; output and expected refusals must agree. Factories used by that test are stateless.
This does not authorize sharing arbitrary mutable builders or consumer factories.

| Contract | Regression coverage |
| --- | --- |
| Named edits, insertion-order indexes, required deletions, atomic false, snapshots | `SharedBuilderContractTest`, `BuilderContractTest` |
| Data validation and expected construction refusal | `BuilderContractTest` (Char and generated-style nodes) |
| Reordered input, subtypes, required/optional/repeated cardinalities and ambiguity | `NodeAllocatorTest`, `AllocationOracleTest` |
| Distinct but structurally equal input occurrences retain identity/order | `AllocationOracleTest` compares each allocated object with `assertSame` |
| Repeated source object, immutable output, no partial writes, concurrent success/refusal | `AllocationSafetyTest` |
| Typed, named and indexed getters; optional gaps; schema-free adapters; wildcard Optional | `NodeTagsTest`, `SharedBuilderContractTest` |
| Tags survive reconstruction, edits, and both JSON modes | `DraftTagsTest`, occurrence/reconstruction and JSON suites |
| Concurrent tag reads of shared children and views | `NodeTagsTest`, `ImmutableViewsTest` |

The exhaustive allocation reference covers 259 schemas and 15 input sequences per schema
(3,885 comparisons). It enumerates complete assignments independently of the production
search and checks type, cardinality and ordering constraints. Structural equality alone
cannot verify an allocation: equal-looking source nodes must keep their occurrence identity.

These suites complete the in-repository Builder/allocation and named-child contracts.
Task #24 remains responsible for a separate Maven consumer of the built JAR and, after
migration of astranaut, a real DSL-to-generated-code integration test.
