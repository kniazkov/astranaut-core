/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Ivan Kniazkov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package consumer;

import com.kniazkov.astranaut.core.base.AbstractType;
import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.ChildDescriptor;
import com.kniazkov.astranaut.core.base.ConstructionException;
import com.kniazkov.astranaut.core.base.DefaultFragment;
import com.kniazkov.astranaut.core.base.DefaultPosition;
import com.kniazkov.astranaut.core.base.DiffTree;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.EmptyFragment;
import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeOccurrence;
import com.kniazkov.astranaut.core.base.Position;
import com.kniazkov.astranaut.core.base.SimpleAbstractType;
import com.kniazkov.astranaut.core.base.Source;
import com.kniazkov.astranaut.core.base.Tree;
import com.kniazkov.astranaut.core.base.Type;
import com.kniazkov.astranaut.core.algorithms.NodeAllocator;
import com.kniazkov.astranaut.core.algorithms.OccurrenceReplacer;
import com.kniazkov.astranaut.core.algorithms.PositionalDiff;
import com.kniazkov.astranaut.core.algorithms.Replacement;
import com.kniazkov.astranaut.core.algorithms.RewriteRule;
import com.kniazkov.astranaut.core.algorithms.RuleEngine;
import com.kniazkov.astranaut.core.utils.JsonDeserializer;
import com.kniazkov.astranaut.core.utils.JsonSerializer;
import com.kniazkov.astranaut.core.utils.TypeResolver;
import com.kniazkov.astranaut.core.utils.UnknownTypes;
import com.kniazkov.astranaut.core.utils.parsing.FileSource;
import com.kniazkov.astranaut.core.utils.parsing.StringSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Contracts exercised only through the installed public JAR.
 * @since 0.0.1
 */
class PublicContractsTest {
    @Test void hasNoVisualizationOrLoggingBackendOnClasspath() {
        for (String name : List.of("guru.nidi.graphviz.engine.Graphviz",
            "com.eclipsesource.v8.V8", "ch.qos.logback.classic.Logger")) {
            assertThrows(ClassNotFoundException.class, () -> Class.forName(name));
        }
    }

    @Test void loadsExactlyTheBuiltJar() throws Exception {
        Path loaded = Path.of(Node.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        assertTrue(Files.isRegularFile(loaded), "core must come from a JAR, not classes");
        Path expected = Path.of(System.getProperty("expected.core.jar"));
        assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(expected)),
            MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(loaded)));
        assertEquals("jar", Node.class.getResource("Node.class").getProtocol());
    }

    @TestFactory Stream<DynamicTest> commonNodeAndBuilderContracts() {
        Map<String, Supplier<Node>> implementations = Map.of(
            "built-in", () -> DraftNode.create("Leaf"),
            "independent", () -> Adapter.leaf("leaf"));
        return implementations.entrySet().stream().map(entry -> DynamicTest.dynamicTest(
            entry.getKey(), () -> checkSnapshot(entry.getValue().get())));
    }

    private void checkSnapshot(Node leaf) {
        Builder builder = leaf.getType().createBuilder();
        assertTrue(builder.setData(leaf.getData()));
        assertTrue(builder.setChildren(List.of()));
        assertTrue(builder.isValid());
        Node before = builder.build();
        assertEquals(leaf, before);
        assertEquals(leaf.hashCode(), before.hashCode());
        List<Node> input = new ArrayList<>(List.of(leaf));
        assertTrue(builder.setChildren(input));
        input.clear();
        Node parent = builder.build();
        assertSame(leaf, parent.getChild(0));
        assertEquals(parent, builder.build());
        assertFalse(builder.setChild(-1, leaf));
        assertFalse(builder.removeChild(1));
        assertEquals(parent, builder.build());
        assertTrue(builder.removeChild(0));
        assertEquals(before, builder.build());
        assertEquals(1, parent.getChildCount());
        assertThrows(UnsupportedOperationException.class, () -> parent.getChildrenList().clear());
    }

    @Test void adapterHasIndependentTypesOptionalGetterAndStrictBuilder() {
        Type type = Adapter.TYPE;
        assertTrue(type.getChildDescriptors().isEmpty());
        assertEquals(new SimpleAbstractType(type.getName()), type);
        assertEquals(1, Set.of(type).stream().filter(new SimpleAbstractType(type.getName())::equals).count());
        assertTrue(type.isSubtypeOf(Adapter.EXPRESSION));
        assertTrue(type.isSubtypeOf(Adapter.LOCATED));
        assertEquals(Optional.of(Adapter.EXPRESSION), type.findCommonBaseType(Adapter.EXPRESSION));
        assertTrue(Adapter.EXPRESSION.findCommonBaseType(Adapter.LOCATED).isEmpty());
        Builder builder = type.createBuilder();
        assertFalse(builder.isValid());
        assertThrows(ConstructionException.class, builder::build);
        assertTrue(builder.setData("parent"));
        assertFalse(builder.setData("forbidden"));
        Adapter child = Adapter.leaf("child");
        assertTrue(builder.setChild("right", child));
        Adapter parent = (Adapter) builder.build();
        OptionalChild base = parent;
        assertSame(child, base.getRight().orElseThrow());
        assertSame(child, parent.getChild("right").orElseThrow());
        assertEquals(Optional.of("right"), parent.getChildTag(0));
        assertFalse(builder.setChildren(List.of(child, child)));
        assertFalse(builder.setChild("right", DraftNode.create("Wrong")));
        assertEquals(parent, builder.build());
        assertTrue(builder.removeChild("right"));
        assertTrue(((Adapter) builder.build()).getRight().isEmpty());
        assertSame(child, parent.getChild(0));
    }

    @Test void preservesTagsAndBothDiffStatesThroughBothJsonModes() {
        Builder builder = Adapter.TYPE.createBuilder();
        assertTrue(builder.setData("parent"));
        assertTrue(builder.setChild("right", Adapter.leaf("before")));
        Node before = builder.build();
        assertTrue(builder.setChild("right", Adapter.leaf("after")));
        Node after = builder.build();
        TypeResolver resolver = name -> name.equals(Adapter.TYPE.getName())
            ? Optional.of(Adapter.TYPE) : Optional.empty();
        for (JsonSerializer.Mode mode : JsonSerializer.Mode.values()) {
            Tree tree = new JsonDeserializer(new JsonSerializer(new Tree(before), mode).serialize(),
                resolver, UnknownTypes.REJECT).convert();
            assertEquals(before, tree.getRoot().orElseThrow());
            assertInstanceOf(Adapter.class, tree.getRoot().orElseThrow());
            DiffTree diff = new PositionalDiff().compare(new Tree(before), new Tree(after));
            Tree restored = new JsonDeserializer(new JsonSerializer(diff, mode).serialize(),
                resolver, UnknownTypes.REJECT).convert();
            DiffTree copy = assertInstanceOf(DiffTree.class, restored);
            assertEquals(before, copy.getBefore().getRoot().orElseThrow());
            assertEquals(after, copy.getAfter().getRoot().orElseThrow());
            assertTrue(new JsonDeserializer(new JsonSerializer(new Tree(), mode).serialize(),
                resolver).convert().getRoot().isEmpty());
        }
    }

    @Test void refusingAncestorRollsBackRuleAndUnexpectedErrorsEscape() {
        Builder builder = Adapter.TYPE.createBuilder();
        assertTrue(builder.setData("parent"));
        Adapter child = Adapter.leaf("child");
        assertTrue(builder.setChild("right", child));
        Node root = builder.build();
        RewriteRule rule = (nodes, index) -> nodes.get(index).getData().equals("child")
            ? Optional.of(new Replacement(1, List.of(DraftNode.create("Wrong")))) : Optional.empty();
        Tree result = new RuleEngine(List.of(rule)).apply(new Tree(root));
        assertSame(root, result.getRoot().orElseThrow());
        assertSame(child, root.getChild(0));
        assertTrue(new OccurrenceReplacer().replace(new NodeOccurrence(root).getChild(0),
            DraftNode.create("Wrong")).isEmpty());
        IllegalStateException error = new IllegalStateException("consumer defect");
        RewriteRule broken = (nodes, index) -> { throw error; };
        assertSame(error, assertThrows(IllegalStateException.class,
            () -> new RuleEngine(List.of(broken)).apply(new Tree(root))));
    }

    @Test void ambiguousAllocationAndHashCollisionsRemainDistinct() {
        Node first = Adapter.leaf("Aa");
        Node second = Adapter.leaf("BB");
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, second);
        List<ChildDescriptor> schema = ChildDescriptor.create()
            .optional(Adapter.EXPRESSION, "left").optional(Adapter.EXPRESSION, "right").build();
        NodeAllocator allocator = new NodeAllocator(schema);
        assertTrue(allocator.allocate(List.of(first)).isEmpty());
        var allocation = allocator.allocate(List.of(first, second)).orElseThrow();
        assertSame(first, allocation.get(0).getFirst());
        assertSame(second, allocation.get(1).getFirst());
        assertTrue(new NodeAllocator(ChildDescriptor.create().repeated(Adapter.EXPRESSION).build())
            .allocate(List.of(first, first)).isPresent());
        AbstractType other = new SimpleAbstractType("ConsumerOther",
            Set.of(Adapter.EXPRESSION, Adapter.LOCATED), Map.of());
        assertTrue(Adapter.TYPE.findCommonBaseType(other).isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> Adapter.TYPE.getBaseTypes().clear());
    }

    @Test void sameSourceContractAcceptsAnIndependentSnapshot() {
        String text = "a😀z";
        for (Source source : List.of(new StringSource(text), new LineSource(text))) {
            Position begin = new DefaultPosition(source, 1, 2);
            Position end = new DefaultPosition(source, 1, 4);
            assertEquals("😀", new DefaultFragment(begin, end).getCode());
            assertEquals("", new DefaultFragment(end, end).getCode());
            assertThrows(IllegalArgumentException.class,
                () -> source.getFragmentAsString(end, begin));
            assertThrows(IllegalArgumentException.class,
                () -> source.getFragmentAsString(new DefaultPosition(new LineSource(text), 1, 2), end));
        }
    }

    /** Independent single-line UTF-16 snapshot.
     * @since 0.0.1
     */
    private record LineSource(String text) implements Source {
        @Override public String getFragmentAsString(Position begin, Position end) {
            if (begin.getSource() != this || end.getSource() != this
                || begin.getRow() != 1 || end.getRow() != 1) {
                throw new IllegalArgumentException("Foreign source or invalid row");
            }
            int first = begin.getColumn() - 1;
            int last = end.getColumn() - 1;
            if (first > last) { throw new IllegalArgumentException("Reversed range"); }
            return this.text.substring(first, last);
        }
    }

    @Test void sourceSnapshotsAndProvenanceWorkFromExternalCode(@TempDir Path directory) throws Exception {
        String text = "а😀\t\r\nend";
        Path file = directory.resolve("source.txt");
        Files.writeString(file, text);
        FileSource snapshot = new FileSource(file.toString());
        Files.delete(file);
        for (StringSource source : List.of(new StringSource(text), snapshot)) {
            for (int offset = 0; offset <= text.length(); offset++) {
                assertEquals(offset, source.offsetOf(source.positionAt(offset)));
            }
            Fragment fragment = new DefaultFragment(source.positionAt(1), source.positionAt(3));
            assertEquals("😀", fragment.getCode());
            Builder builder = Adapter.TYPE.createBuilder();
            assertTrue(builder.setData("located"));
            builder.setFragment(fragment);
            Node node = builder.build();
            builder.setFragment(EmptyFragment.INSTANCE);
            assertSame(fragment, node.getFragment());
            assertEquals("😀", node.getFragment().getCode());
        }
    }
}
