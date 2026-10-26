# Immutable model and views

A published node is a stable value: type, string data, ordered child occurrences and their
tags do not change after construction. Readonly properties and the complete provenance
chain (`Fragment` → `Position` → `Source`) must also remain stable. Empty roots are
represented by `Optional<Node>`, not by null children or hidden placeholder nodes.
Shared children are permitted; cycles along child edges are not valid input.

Builders, allocators' invocation state, selection sets, iterators, action lists and rule
execution state are mutable working objects. They are not part of the published node
model and must not be shared between tasks without an explicit concurrency contract.
A later builder edit cannot modify an earlier node. Returning an unmodifiable collection
backed by a mutable external collection is insufficient to meet this contract.

## Implementation audit

| Model component | Ownership and publication |
| --- | --- |
| `DraftNode`, `Char`, action nodes | Constructors retain immutable values or snapshot builder collections; subsequent edits stay in the builder |
| `PatternNode`, `PatternOperator`, `Hole`, `DiffNode` | Independent immutable descriptors/content; captured source nodes remain immutable references, not mutable builder state |
| `SimpleAbstractType`, child descriptors and schemas | Own properties, bases and schema collections are copied; referenced descriptors must themselves honor the contract |
| Normalized nodes, edges and properties | Immutable snapshots and completed child lists; no shared invocation caches |
| `Tree`, `NodeOccurrence` | Final root/address state; parent/index belongs to an occurrence, never to a shared source node |
| `DefaultFragment`, `DefaultPosition` | Final references and coordinates; custom Position/Source implementations remain the producer's responsibility |
| `StringSource`, `FileSource` | In-memory immutable source snapshots; private line offsets are not exposed; FileSource reads contents once |
| Subtree and labeled views | Completed child/index lists before publication; no lazy writes from getters; labeled property maps are copied |
| Extended views | Separate wrappers for separate occurrences; final position/hash fields; private child arrays are linked completely within the creator, before return, and never mutated afterwards |

The audit is about the built-in implementations and the stated contracts. Interfaces
cannot freeze a mutable third-party parser, Source, Type or Map supplied by an adapter.
An adapter must snapshot such state or refer to an independently immutable object graph.

## Subtree and labeled views

Both builders use one iterative postorder implementation. A task-local identity map stores
completed wrappers; an active-ancestor identity set rejects cycles in selected edges.
The map and unfinished frames never escape construction. Every published wrapper owns an
immutable list of already completed children. The same source object may reuse its wrapper
at several parent positions; each position keeps its own tag/index mapping.

`SubtreeBuilder` no longer accumulates child indexes when the same source subtree is visited
again. It no longer constructs children on first `getChild` access. Selection is evaluated
once per unique source parent's child edge; callbacks must be pure and selection sets must
remain stable during `create`. The resulting view retains neither the mutable selection set
nor mutable traversal state. Prototype-chain membership lookup is iterative as well.

The historical selection semantics are retained: the root is a container, rejected child
branches are pruned, and a root with no selected children returns `DummyNode`. This API is
not the Optional-root transformation API. A cut view retains the original type and
provenance; cutting does not assert that the selected children satisfy the original
concrete schema. Consumers requiring a concrete node must materialize through its Builder
and handle refusal. This boundary is separate from the immutability of the view itself.

`LabeledTreeBuilder` builds its descendants iteratively, snapshots both labeled and unlabeled
property maps, and preserves the original fragment. Changing a selection set after the call
or using the builder again does not change an earlier result. This wrapper does not copy
readonly properties through a concrete Builder; it adds presentation metadata to a view.

The work performed by the common construction traversal is linear in selected unique nodes
and their outgoing edges, excluding callback, Set membership, metadata-copy and source
accessor costs. It uses heap frames rather than recursive Java calls. For a DAG, output
sharing is retained by identity; no structural-equality merging occurs.

## Concurrent readers

After construction, reads do not initialize children, mutate properties or update traversal
markers. Extended views derive neighbor links from their immutable parent/index position;
a shared prototype never receives parent, sibling, index or visitation state.

Publish completed roots through normal Java concurrency mechanisms (executor submission,
a concurrent collection, a final owner field, or volatile/locking handoff). Extended graph
construction intentionally has a private linking phase; neither the root nor a child may
escape that phase. There are no concurrent callbacks during construction. The factory
retains no instance state after returning.

Safe concurrent reads do not imply that a user-defined selector, factory, builder or rule
can be invoked concurrently. This remains an explicit caller/implementation obligation.

## Verification and remaining boundaries

`ImmutableViewsTest` covers shared-branch duplication, immutable selection snapshots,
20,000-deep and 20,000-wide inputs, cycle refusal, and concurrent reading of children,
named/indexed bindings, types, properties, fragments and extended occurrence links.
The existing model suites cover independent type implementations, multiple inheritance,
deep hierarchies, wildcard Optional interfaces, schema-free adapters, builder reuse,
source snapshots and structural tagged JSON/diff round trips.

These checks establish the built-in model contracts and close the unsafe lazy-view part
of the model migration. Independent JAR-consumer contracts and the later real
DSL → generated Java → core integration remain task #24; the generator is unchanged.
