# Tree JSON, version 1

`JsonSerializer` writes structure by default. `Mode.EXTENDED` additionally writes
readonly metadata. Both forms are read by `JsonDeserializer` through node builders;
reading deliberately ignores metadata. The serialized structure includes global type
names, string data, ordered child occurrences and their tags, and service-node parameters.

```java
Tree original = Tree.createDraft("Root(A,B)");
String text = new JsonSerializer(original).serialize();
TypeResolver resolver = name -> Optional.ofNullable(factory.getType(name));
Tree restored = new JsonDeserializer(text, resolver).convert();
assert original.deepCompare(restored);
```

For extended output:

```java
String text = new JsonSerializer(original, JsonSerializer.Mode.EXTENDED).serialize();
Tree restored = new JsonDeserializer(
    text,
    resolver,
    UnknownTypes.REJECT
).convert();
```

## Container and node table

The wire format uses a flat, dependency-ordered node table. Children refer to earlier
entries by zero-based index. The root is an index, or explicit JSON `null` for an empty
tree. This keeps JSON nesting shallow even for a chain containing tens of thousands of
nodes. Object member order is irrelevant; node-table and child-array order are meaningful.

```json
{
  "version": 1,
  "kind": "tree",
  "nodes": [
    {"kind": "node", "type": "Literal", "data": "x", "children": []},
    {
      "kind": "node",
      "type": "Pair",
      "data": "",
      "children": [{"node": 0, "tag": "left"}, {"node": 0, "tag": "right"}]
    }
  ],
  "root": 1
}
```

`version`, container `kind`, `nodes` and `root` are required. The container kind is
`tree`, `diff` or `pattern`. Empty ordinary trees and empty differences use an empty
node table and `root: null`; patterns require a root. `{}` is invalid.

Each node requires `kind`, `type`, `data` (including the empty string), and `children`
(including an empty array). Type names are globally unique strings; there is no namespace
or language-based identity. Every child entry requires `node`; absent `tag` means an
unnamed occurrence. A present tag must be a string, unique within its parent, and accepted
by the target builder. Tags are taken from `getChildTag(index)`, never inferred from an
equal child object or from a schema. A shared child may therefore have different tags on
its different incoming edges.

References must be nonnegative integers strictly less than the containing record's index.
Forward references, self references and cycles are rejected. The root must reference an
existing record. Unreferenced records are still structurally validated. The writer reuses
records by object identity; readers need not preserve the original sharing arrangement.
Equality after round trip means structural equality, not object identity.

## Service nodes

The `kind` discriminator distinguishes executable service nodes from ordinary nodes with
similar names. It is structural information, not optional diagnostic metadata.

| Node kind | Meaning and additional fields |
| --- | --- |
| `node` | Ordinary node, created by the resolved concrete type's builder |
| `insert` | `Insert` builder; one child is the inserted payload |
| `delete` | `Delete` builder; one child is the deleted payload |
| `replace` | `Replace` builder; children are before and after, in that order |
| `hole` | Hole builder; `data` is the nonnegative decimal capture slot; optional string `constraint` identifies an abstract or concrete accepted type |
| `pattern` | Independent pattern scaffold retaining its concrete target type and edge layout |
| `diff` | Immutable difference scaffold with the same independent reconstruction |

A hole without `constraint` has type `AstranautHole`; a typed hole has type
`AstranautTypedHole:<constraint-name>`. Hole prototypes are provenance, not captures, and
are not restored. A hole has no children. A mismatch between kind, declared type, data or
builder-produced layout is rejected. Constraints are resolved as `AbstractType`, so a
registered abstract interface can constrain a hole without being instantiable as a node.

Scaffolds use their own builders: a concrete `Addition` builder cannot accept `Hole` or
`Insert` in a required expression slot. The scaffold retains the concrete target descriptor
for later checked substitution or projection. Deserialization never applies an insertion,
replacement or deletion as a side effect. Thus two insertions survive as two ordinary
children of the scaffold, in order, and action payloads may themselves contain actions.
Declared `diff` containers additionally validate both endpoint projections before returning.
Patterns may intentionally have children that only become concrete after substitution.

## Resolution, builders and errors

`TypeResolver` returns `Optional<AbstractType>` for a globally unique name. Applications
should register all concrete builders and any abstract constraints they need. The JSON
reader does not call `Factory.createBuilder`, whose implicit fallback would hide unknown
types, and does not use `Provider` or a `language` field to choose identity.

`UnknownTypeHandler` receives the unresolved name and the ordered occurrence tags. It
returns a concrete descriptor or throws. The supplied policies are:

- `UnknownTypes.DRAFT` (the default): builds immutable `DraftNode` values, retaining tags.
- `UnknownTypes.REJECT`: throws `TreeJsonException`, including for unresolved hole constraints.

Draft fallback preserves structure, but cannot invent missing inheritance information for
an unknown typed-hole constraint. Use a resolver when subtype semantics matter.

Every node is reconstructed through a builder. Ordinary reconstruction uses the same
checked helper as occurrence replacement: explicit named bindings are preferred, mixed
layouts use the child sequence followed by named bindings, and the resulting data, type,
child identities/order and tags are verified. A builder that reorders children or silently
loses a tag is refused. Draft builders accept an explicit complete occurrence layout, so
mixed named/unnamed edges remain unambiguous even when several edges share one object.

`TreeJsonException` reports malformed syntax/shape, absent required fields, invalid
references, duplicate tags, unknown kinds, abstract instantiation, resolver name mismatches
and expected builder/projection refusals. No error becomes `EmptyTree` or `DummyNode`.
Unexpected application-builder programming errors propagate rather than being disguised
as malformed input. Failed decoding never publishes a partial tree.

## Extended metadata and source text

Each extended record contains `metadata` with node `properties`, `typeProperties`, sorted
direct `baseTypes`, and, when provenance exists, `origin.begin` / `origin.end`. A position
contains only `file`, `row`, and `column`; ranges retain the core's half-open convention.
The writer never reflects over `Source`, calls `getCode()` / `getFragmentAsString()`, or
serializes a Source object. Source contents are not accessed or copied.

The reader ignores metadata fields regardless of their shape. Properties, source objects,
fragments and the original sharing layout are not restored. Properties remain ordinary
caller-supplied strings: the writer does not redact text deliberately placed into node data
or arbitrary properties. The promise concerns source-text/provenance serialization, not
content filtering of structural string data or readonly property values.

## Depth and migration

Version 1 deliberately replaces the historical nested, unversioned format. Old JSON files
must be migrated; there is no ambiguous auto-detection or silent legacy fallback. The old
reflection descriptors and deferred ActionList reconstruction were removed. Existing
archived parser fixtures are read by a test-only adapter, not a second production decoder.

The JSON library itself uses recursive container processing. Before parsing, a lexical
guard rejects inputs deeper than 64 JSON containers, including deeply nested extra
metadata. Brackets inside strings and escaped quotes do not count. The version-one table
has constant nesting depth, independently of AST depth. Writing and node reconstruction
are iterative; tests cover 20,000-deep and 20,000-wide trees with Unicode and escaped data.
The table, parsed JSON and reconstructed graph are retained in memory; there is no
streaming or unbounded-input memory guarantee. Serialization also rejects cyclic inputs.

`DraftNode.Constructor` now retains edge tags, supports named insertion/replacement/removal,
and offers atomic `setChildren(nodes, tags)` for complete layouts. The one-list overload
replaces children with unnamed occurrences. Previously published nodes are unaffected.
