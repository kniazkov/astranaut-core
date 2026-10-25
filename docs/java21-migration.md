# Java 21 utility migration

Core uses standard collections, functional interfaces and text I/O. This is a
source-breaking cleanup for 0.0.1. The generator repository is not changed here;
the checked-in examples in core demonstrate the replacement code.

## Collections: filter, then snapshot

`ListUtils` and `MapUtils` have been removed. Their `make()` methods returned live
unmodifiable views of mutable builders. New node state must be a snapshot.

For individual optional children, retain order and omit absent values:

```java
List<Node> children = Stream.<Node>of(left, optionalRight)
    .filter(Objects::nonNull).toList();
```

For mixtures of individual children and captured sequences, accumulate into an
`ArrayList<Node>`, append in DSL order, skip null optional children, then use
`List.copyOf(children)`. A nullable sequence must be checked before streaming;
`ListUtils.add((List<Node>) null)` previously meant an empty sequence. For example:

```java
List<Node> children = new ArrayList<>();
Stream.of(left, optionalRight).filter(Objects::nonNull).forEach(children::add);
if (captured != null) {
    captured.stream().filter(Objects::nonNull).forEach(children::add);
}
List<Node> snapshot = List.copyOf(children);
```

`List.of` is appropriate only when all children are present. Both `List.of` and
`List.copyOf` reject null; they do not perform the old utility's filtering.
The `Return`, `ExpressionStatement` and `SimpleAssignment` example builders use
filtered immutable lists. Reusing a builder does not change an earlier node.

Use `Map.of("language", "Java")` for fixed unique keys. For overlays or repeated
keys, preserve the old **last write wins** behavior before taking a snapshot:

```java
Map<String, String> properties = new HashMap<>(prototype.getProperties());
properties.put("color", "purple");
Map<String, String> snapshot = Map.copyOf(properties);
```

This is the implementation used by `Hole`. `Map.of`/`Map.ofEntries` reject duplicate
keys, so they are not substitutes for an overlay. Standard immutable maps reject
null keys and values; do not emit them in properties. Neither old `HashMap` nor
`Map.copyOf` promises iteration order. If an API requires insertion order, use
`Collections.unmodifiableMap(new LinkedHashMap<>(properties))`: copying is essential.
Do not derive rule priority or child order from map iteration order.

## Explicit converter registration

`ObjectsLoader` and `Converter.collectConverters(String, List)` have been removed.
A missing numbered class, missing `INSTANCE` field or wrong singleton type used to
silently truncate the rule list. Emit references to every converter instead:

```java
List<Converter> converters = List.of(
    common.rules.Converter0.INSTANCE,
    language.rules.Converter0.INSTANCE,
    language.rules.Converter1.INSTANCE
);
DefaultTransformer transformer = new DefaultTransformer(converters, factory);
```

Names above illustrate generated packages. Preserve existing priority: common
rules first, then language-specific rules, each in DSL declaration order. There is
no numbered-class probing and no requirement that names form a contiguous range.
Missing classes/fields and incorrect types become compilation errors; broken
singleton initialization propagates normally. `DefaultTransformer` snapshots the
provided list with `List.copyOf`, rejecting null entries immediately. Mutable
registration lists may be edited afterwards without changing an existing transformer.
No new reflection or registry framework is needed.

## Functional interfaces

`DepthFirstWalker.Visitor` is replaced by `Predicate<? super Node>`:

```java
Predicate<Node> wanted = node -> node.isSubtypeOf(statementType);
Optional<Node> first = new DepthFirstWalker(root).findFirst(wanted);
List<Node> matches = new DepthFirstWalker(root).findAll(wanted);
```

The boolean still means stop globally for `findFirst`, or select and prune this
subtree for `findAll`. Preorder and occurrence semantics are unchanged. Replace
explicit `visitor.process(node)` calls with `visitor.test(node)`.

`Promise` and `Promise.Callback` were only synchronous callback wrappers. The
replacement is `Consumer<T>`, not `CompletableFuture` (the old API was neither
asynchronous nor one-shot):

```java
Consumer<Node> setParent = actions.insertNodeAfter(inserted, preceding);
setParent.accept(parent);
```

Replace `new Promise<>(callback)` with the consumer itself and `set`/`process` with
`accept`. Each invocation performs an action, as before. The private numeric
callback in `NodePairFinder` is now `ToIntFunction<ExtNode>`.

## Named carriers instead of Pair

`Pair` has been removed. Public results now name their components:

```java
Position.Bounds bounds = Position.bounds(first, second, third);
Fragment fragment = new DefaultFragment(bounds);
Position begin = bounds.begin();
Position end = bounds.end();

NodeReplacer.Result replacement = new NodeReplacer().replace(root, source, target);
Node rebuilt = replacement.root();
int lastChild = replacement.lastChangedChild();
```

The legacy all-occurrences replacer's `DummyNode`/`-1` protocol is unchanged;
`OccurrenceReplacer` remains the API for replacing one address. Internal section
splits and selected mapping occurrences use private/package records; labels use
`Map.entry(name, value)`. Records compare their named components by value, so they
are result carriers, not identity keys for matching occurrences. No Node becomes
a record and no identity-based algorithm switches to record equality.

## Exact UTF-8 I/O

`FilesReader`, its `CustomExceptionCreator`, and `FilesWriter` are removed:

```java
Path file = Path.of(filename);
String text = Files.readString(file); // UTF-8, no newline normalization
Path directory = file.getParent();
if (directory != null) {
    Files.createDirectories(directory);
}
Files.writeString(file, text); // UTF-8, create or truncate
```

Catch `IOException` at the caller's boundary. For an unchecked API, wrap it in
`UncheckedIOException` with the path and original cause, as `FileSource` does.
For a domain exception, catch and wrap the same cause in that exception; a custom
factory interface is unnecessary. An empty file is valid, a failed read is not
an empty file. Java's text methods reject malformed input instead of silently
replacing it. They preserve CR, LF, tabs, NUL and Unicode text exactly.

`JsonSerializer.serializeToFile` retains its boolean success contract and creation
of missing parent directories. Direct users of the old `writeStringNoExcept`
should catch `IOException` themselves if they need that boolean contract.

## Generator follow-up (outside this repository)

- `RegularNodeGenerator`, `NonAbstractNodeGenerator`: emit immutable children with
  explicit optional filtering; remove `ListUtils` imports/flags.
- `ResultingSubtreeBuilderGenerator`, `TransformationGenerator` and
  `TransformationGeneratorFlags`: concatenate captured sequences in order using
  standard lists; preserve omission of null optional entries; snapshot afterwards.
- `FactoryGenerator`, `ProviderGenerator`, `NonAbstractNodeDescriptor` and
  `LiteralDescriptor`: fixed maps use `Map.ofEntries`; overlays use a mutable map
  plus `Map.copyOf`; preserve duplicate-key precedence where applicable.
- `TransformerGenerator`: generate explicit converter references with common rules
  preceding language rules. Its own private collector method may remain, but it
  must no longer call the removed core method.
- `RegularBuilder`: filter optional children and take a snapshot instead of calling
  `ListUtils`.
- CLI `Transform`, `GeneratorArguments`, `BaseAction`: use `Files.readString` and
  `Files.writeString`; preserve domain errors, causes and directory creation.
- Update generated-code documentation and fixtures to these replacements and the
  current core package. `NodeAllocator`, `ChildDescriptor` and `Char` remain.

The remaining mutable pattern, normalization and diff models belong to their
respective architecture tasks. This cleanup does not change their algorithms or
seal the public Node interface.
