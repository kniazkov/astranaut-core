# Type descriptors and child tags

This is the first implementation stage of issues #11 and #26. The interfaces below
are available in core; migrating the Astranaut generator is a separate step.

## Abstract and concrete types

`AbstractType` describes a globally named type, including abstract types that
cannot create nodes. `Type extends AbstractType` adds a builder factory and an
optional child schema. `Node.getType()` returns `Type`.

`getBaseTypes()` returns an immutable set of immediate bases. It excludes the type
itself. Bases have equal priority, and set iteration order has no semantic meaning.
The producer must provide an acyclic graph and consistent definitions for each
name. Core permits concrete types as bases even when a particular DSL does not.

```java
AbstractType expression = new SimpleAbstractType("Expression");
AbstractType binary = new SimpleAbstractType(
    "BinaryExpression",
    Set.of(expression),
    Map.of()
);

boolean subtype = binary.isSubtypeOf(expression); // true
Optional<AbstractType> common = binary.findCommonBaseType(expression);
```

`isSubtypeOf` includes the type itself and traverses all ancestors.
`findCommonBaseType` returns the unique most specific common base, including the
operands. It returns empty for unrelated types or several incomparable candidates.
For example, if both X and Y derive from unrelated A and B, neither A nor B is
chosen arbitrarily. Both algorithms use iterative traversal and local state; they
support deep graphs without consuming the Java call stack or maintaining a global
cache.

Type equality and hash codes depend only on the globally unique name. Extend
`TypeWithNameBasedEquality` to reuse this implementation. A direct implementation of
`AbstractType` must implement the same contract, including equality across Java
implementation classes. Java does not permit an interface default implementation
of `Object.equals` or `Object.hashCode`.

`SimpleAbstractType` copies its base set and property map. Supplied base descriptors
must themselves be immutable. Properties are **own properties only**; there is no
automatic merging of inherited properties.

Nodes implement `Node` and return independent `Type` descriptors. A descriptor
must not retain a node instance or its subtree: its builder factory depends only
on the type definition. Fixed types can share a static descriptor; dynamic types
store the required definition data in a separate descriptor. Node properties do
not automatically become type properties. Prototype wrappers retain the prototype
type properties; per-node overrides, such as a hole's display color, stay on the node.

## Child schemas

`Type.getChildDescriptors()` returns `Optional<List<ChildDescriptor>>`:

- Empty Optional: the schema is unknown; a custom builder handles construction.
- Present empty list: the type is a leaf; no children are allowed.
- Present list: the complete ordered schema of this concrete type. Consumers do
  not assemble it by traversing the base types.

Each descriptor references an `AbstractType` and permits that type and its
subtypes. The three cardinalities are `REQUIRED` (one), `OPTIONAL` (zero or one),
and `REPEATED` (zero or more). Only the first two may have a tag. All tags in one
schema must be unique.

```java
List<ChildDescriptor> schema = ChildDescriptor.create()
    .required(expression, "left")
    .optional(expression, "right")
    .required(statement)
    .optional(statement, Optional.empty())
    .repeated(statement)
    .build();
```

`required` and `optional` accept a type alone, a type and a non-null string tag,
or a type and an `Optional<String>` tag. `repeated` accepts only a type.
Descriptors have a single private constructor: create schemas through
`ChildDescriptor.create()...build()`. Existing descriptors can be reused with
`add(descriptor)`; `build()` validates tag uniqueness across the entire list.
Null types, tags, Optional containers and descriptors are rejected immediately.

`build()` returns an immutable snapshot; changing the builder afterwards does
not change a previously produced list. An empty builder produces an empty leaf
schema. A custom `Type` must likewise expose a validated immutable schema,
not a live view of mutable state.

The current `NodeAllocator` consumes the new type references for its existing
single/optional allocation algorithm. It explicitly rejects `REPEATED` descriptors;
repeated allocation and the revised Builder setters belong to #12. Builders that
implement their own list handling can already use repeated schemas as metadata.

## Tags belong to child occurrences

```java
Optional<Node> left = addition.getChild("left");
Optional<String> tag = addition.getChildTag(0);
```

Both methods work independently of a schema. By default, they return empty;
`getChildTag` first checks the index against the compact child list and throws
`IndexOutOfBoundsException` for an invalid index. Null tags are invalid arguments.

For every present tag at index i, `getChild(tag)` must return the **same object**
as `getChild(i)`. Unknown tags and absent optional children return empty. An absent
optional child occupies no index; a tag's index is not necessarily its descriptor's
index. One object may occur in both `left` and `right`; lookup must distinguish
those occurrences instead of searching by node equality or object identity.

Optional typed getters in base interfaces use `Optional<? extends Base>` so
concrete implementations can return `Optional<Derived>`.

The hand-written generated-style fixtures in core exercise Addition's left/right
tags. Independent adapter tests cover a missing left child, equal-looking children,
a shared child, concurrent reads and wildcard Optional getters.

## Migration boundaries

`getHierarchy()` and `getChildTypes()` are replaced by `getBaseTypes()` and
`getChildDescriptors()`. Nodes expose `isSubtypeOf(AbstractType)` as a convenience
for `node.getType().isSubtypeOf(type)`. Hierarchy queries and allocator type matching
accept descriptors; the former string-based group query has been removed. Graph traversal uses sets of descriptors and their equals/hashCode contract.
Names remain the identifiers in serialization and the basis of type equality.

Immutable patterns and differences, occurrence-based transformations and
[tag-preserving JSON round trips](json.md) now implement these contracts. Issues #11
and #26 retain their remaining model and external-consumer integration checks;
this does not claim that every legacy wrapper or the external generator is migrated.
Structural equality and hashing with tags are implemented as described in
[the structural equality guide](structural-equality.md).

Tree roots now use `Optional<Node>`; see [the root migration guide](optional-root.md)
for empty-tree behavior and boundaries of the remaining immutable-model work.
