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
astranaut generator is unchanged. Tag-preserving reconstruction across whole-tree
algorithms, normalization and JSON remains part of their respective follow-up tasks;
the mutable pattern/diff representations and Optional-root migration are also separate.
