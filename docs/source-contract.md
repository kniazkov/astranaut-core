# Sources, coordinates and draft descriptions

`Source` is an immutable text snapshot. Implementations must not reread a mutable file
when a fragment is accessed. `FileSource` reads the complete file once, using strict
UTF-8 decoding. I/O failures and malformed UTF-8 throw `UncheckedIOException` with the
original cause and file path. An empty file is a successful, empty snapshot.
`FilesReader.readAsString()` exposes the checked `IOException` instead.

No text is silently removed or normalized: CR, LF, trailing line terminators, blank
lines, tabs, NUL and supplementary Unicode characters all survive reading and parsing.
`Char` represents one UTF-16 unit; a supplementary character produces two Char nodes.
NUL is a real character, distinct from EOF and an unset character builder.

## Coordinates and ranges

Rows and columns start at one. Columns count UTF-16 units, including tabs and CR.
LF alone advances the row and resets the column. Therefore CRLF advances one row while
retaining both original units; a standalone CR is an ordinary unit, not a row separator.

Every range is **[begin, end)**. The end is exclusive, including for single-character
fragments. An empty range has a real position and is distinct from `EmptyFragment`,
which represents missing provenance. Only EmptyFragment has private zero-coordinate
sentinels; ordinary positions must have positive coordinates.

| Text | EOF position |
| --- | --- |
| Empty string | Row 1, column 1 |
| `x` | Row 1, column 2 |
| `x\n` | Row 2, column 1 |
| `x\r\n` | Row 2, column 1 |

`StringSource.positionAt(offset)` converts a zero-based UTF-16 offset, including EOF,
to canonical coordinates. `offsetOf(position)` converts back. After LF, only the next
row's column one is valid: coordinates past the preceding row do not alias it.
Positions between UTF-16 surrogate halves are valid; a code-point-based adapter must
convert its offsets before using these APIs.

```java
StringSource source = new StringSource("a😀\r\n");
Position begin = source.positionAt(1);
Position end = source.positionAt(3);
Fragment emoji = new DefaultFragment(begin, end);
assert emoji.getCode().equals("😀");
```

Wrong-source, reversed and out-of-range accesses are rejected. They are never swapped,
clamped or expanded with imaginary newlines. `getSymbol(row, column)` reads a real unit;
EOF and invalid coordinates throw instead of returning a sentinel character.
`DefaultFragment` snapshots coordinate values from the supplied Position objects.
Sources themselves are identified by object identity: equal text or equal file names
do not imply the same origin. Custom Source implementations must obey immutability.

## Provenance of synthesized nodes

`Fragment.fromNodes(matched)` calculates the smallest span covering the available
fragments. Pass the **entire matched sequence**, including discarded delimiters, not
just the retained children. For a match `(x)`, a synthesized grouping node receives
`(x)`, while the reused child retains its original `x` fragment.

```java
Builder builder = type.createBuilder();
builder.setFragment(Fragment.fromNodes(matched));
// Fill the builder with the node's validated data and retained children.
```

Missing fragments are ignored. If no real provenance is available, or real fragments
belong to different source instances, the result is `EmptyFragment.INSTANCE`.
Explicit `Fragment.fromPositions` forms bounds from same-source boundary positions;
one supplied position produces an empty range, not a single-character range.

This establishes the source/span primitive for the common rule engine (#17). Rule
execution and rollback remain part of that separate task; captured nodes must be
reused with their own provenance, while newly synthesized nodes receive the match span.

## Strict draft description parser

`DraftNode.create` accepts one complete description:

```text
Node := Name [ '<' QuotedData '>' ] [ '(' [ Node (',' Node)* ] ')' ]
Name := UnicodeLetter (UnicodeLetter | UnicodeDigit)*
```

Whitespace is allowed between tokens. Data can use single or double quotes. Supported
escapes are `\\`, `\'`, `\"`, `\n`, `\r`, `\t`, `\b`, `\f` and `\uXXXX` (four ASCII
hexadecimal digits, one UTF-16 unit). Literal data units are preserved, including NUL
and U+FFFF. Unknown escapes, incomplete quotes/delimiters, missing commas and trailing
input throw `IllegalArgumentException` with a zero-based UTF-16 offset.

Parsing and description writing are iterative and support deep trees without Java
stack recursion. `Node.toString(node)` and DraftNode descriptions escape data so quotes,
backslashes and control characters round-trip. This debug format records names, data
and ordered children; it does not encode tags, properties or source provenance. Use
the dedicated serialization API for that richer structure.

The draft builder uses the same formatter as built nodes, including for children with
no custom `toString`. Formatting an incomplete builder does not call `build`, validate
or mutate its state.

The optional type-to-nodes index is populated only after successful parsing; a malformed
description does not leave partially parsed nodes in the caller's map. Equal but distinct
node objects remain separate index entries.

## Migration

- Character range ends now point **after** the character; old inclusive ends must be converted.
- Use `positionAt` for offsets and EOF rather than guessing line/column boundaries.
- `readAsStringNoExcept` was removed. Use `readAsStringUnchecked` or the checked reader;
  unreadable files are no longer silently treated as empty.
- Custom read-error factories now receive the original `IOException` as an argument.
- Invalid draft descriptions previously accepted by error recovery must be corrected.
- The obsolete single-point `CharFragment` implementation was removed.
