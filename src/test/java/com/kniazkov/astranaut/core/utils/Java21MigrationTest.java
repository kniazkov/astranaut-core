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
package com.kniazkov.astranaut.core.utils;

import com.kniazkov.astranaut.core.algorithms.DepthFirstWalker;
import com.kniazkov.astranaut.core.algorithms.conversion.ConversionResult;
import com.kniazkov.astranaut.core.algorithms.conversion.Converter;
import com.kniazkov.astranaut.core.algorithms.conversion.DefaultTransformer;
import com.kniazkov.astranaut.core.base.ActionList;
import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Factory;
import com.kniazkov.astranaut.core.base.Hole;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeWithStructuralEquality;
import com.kniazkov.astranaut.core.base.Tree;
import com.kniazkov.astranaut.core.base.Type;
import com.kniazkov.astranaut.core.example.LittleTrees;
import com.kniazkov.astranaut.core.example.converters.Converter0;
import com.kniazkov.astranaut.core.example.green.GreenFactory;
import com.kniazkov.astranaut.core.example.green.Return;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Behavioral contracts at migrated Java 21 call sites.
 * @since 0.0.1
 */
class Java21MigrationTest {
    @Test
    void optionalChildrenAreFilteredAndBuiltListsRemainSnapshots() {
        final Builder builder = Return.TYPE.createBuilder();
        final Node empty = builder.build();
        Assertions.assertTrue(empty.getChildrenList().isEmpty());
        final Node first = LittleTrees.createIntegerLiteral(1);
        Assertions.assertTrue(builder.setChild("expression", first));
        final Node populated = builder.build();
        Assertions.assertTrue(builder.setChild("expression", LittleTrees.createIntegerLiteral(2)));
        Assertions.assertTrue(empty.getChildrenList().isEmpty());
        Assertions.assertEquals(List.of(first), populated.getChildrenList());
        Assertions.assertSame(first, populated.getChild(0));
        Assertions.assertThrows(
            UnsupportedOperationException.class, () -> populated.getChildrenList().clear()
        );
    }

    @Test
    void propertiesOverrideDuplicateKeysWithoutAliasingTheirSource() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("color", "red");
        properties.put("language", "Java");
        final Node hole = new Hole(new PropertyNode(properties), 1);
        properties.put("language", "Python");
        Assertions.assertEquals(
            Map.of("color", "purple", "language", "Java"), hole.getProperties()
        );
        Assertions.assertThrows(
            UnsupportedOperationException.class, () -> hole.getProperties().put("extra", "value")
        );
    }

    @Test
    void explicitConverterListIsSnapshotted() {
        final List<Converter> converters = new ArrayList<>(List.of(Converter0.INSTANCE));
        final DefaultTransformer transformer = new DefaultTransformer(
            converters, GreenFactory.INSTANCE
        );
        converters.clear();
        final Node operator = DraftNode.create("Operator<'+'>");
        final Node expression = DraftNode.create(
            "Root", "", LittleTrees.createIntegerLiteral(2), operator, LittleTrees.createIntegerLiteral(3)
        );
        Assertions.assertEquals(
            "Addition", transformer.transform(expression).getChild(0).getType().getName()
        );
    }

    @Test
    void explicitRegistrationRetainsPriorityAndRejectsNullEntries() {
        final Converter first = new Rename("First");
        final Converter second = new Rename("Second");
        final Node root = DraftNode.create("Root(Source)");
        for (final List<Converter> order : List.of(
            List.of(first, second), List.of(second, first)
        )) {
            final Node converted = new DefaultTransformer(order, GreenFactory.INSTANCE)
                .transform(root).getChild(0);
            final Node expected = order.getFirst().convert(
                root.getChildrenList(), 0, GreenFactory.INSTANCE
            ).orElseThrow().getNode();
            Assertions.assertEquals(expected, converted);
        }
        Assertions.assertThrows(
            NullPointerException.class,
            () -> new DefaultTransformer(Arrays.asList(first, null), GreenFactory.INSTANCE)
        );
    }

    @Test
    void consumerDefersInsertionUntilTheParentIsKnown() {
        final Node root = DraftNode.create("Root(First)");
        final Node inserted = DraftNode.create("Inserted");
        final ActionList actions = new ActionList();
        final Consumer<Node> callback = actions.insertNodeAfter(inserted, root.getChild(0));
        Assertions.assertFalse(actions.hasActions());
        callback.accept(root);
        final Node result = actions.convertTreeToDiffTree(new Tree(root))
            .getAfter().getRoot().orElseThrow();
        Assertions.assertEquals(List.of(root.getChild(0), inserted), result.getChildrenList());
    }

    @Test
    void standardPredicatesKeepTraversalOrderAndPruning() {
        final Node root = DraftNode.create("Root(Branch(Leaf), Leaf)");
        final List<String> visited = new ArrayList<>(3);
        final Predicate<Node> stop = node -> {
            visited.add(node.getType().getName());
            return node.getType().getName().equals("Branch");
        };
        final DepthFirstWalker walker = new DepthFirstWalker(root);
        Assertions.assertSame(root.getChild(0), walker.findFirst(stop).orElseThrow());
        Assertions.assertEquals(List.of("Root", "Branch"), visited);
        visited.clear();
        Assertions.assertEquals(List.of(root.getChild(0)), walker.findAll(stop));
        Assertions.assertEquals(List.of("Root", "Branch", "Leaf"), visited);
    }

    @Test
    void serializationCreatesParentsAndWritesExactUnicode(@TempDir final Path directory)
        throws IOException {
        final Path target = directory.resolve("nested/output/tree.json");
        final JsonSerializer serializer = new JsonSerializer(
            new Tree(DraftNode.create("Text", "Кириллица 😀\r\n"))
        );
        Assertions.assertTrue(serializer.serializeToFile(target.toString()));
        Assertions.assertEquals(serializer.serialize(), Files.readString(target));
        Files.writeString(target, "longer previous file content".repeat(100));
        Assertions.assertTrue(serializer.serializeToFile(target.toString()));
        Assertions.assertEquals(serializer.serialize(), Files.readString(target));
        Assertions.assertFalse(serializer.serializeToFile(target.resolve("child.json").toString()));
    }

    /**
     * Competing rule which consumes the same source node as its peers.
     * @param name Replacement type name
     * @since 0.0.1
     */
    private record Rename(String name) implements Converter {
        @Override
        public Optional<ConversionResult> convert(final List<Node> nodes, final int index,
            final Factory factory) {
            return Optional.of(nodes.get(index))
                .filter(node -> node.getTypeName().equals("Source"))
                .map(node -> new ConversionResult(DraftNode.create(this.name), 1));
        }

        @Override
        public int getMinConsumed() {
            return 1;
        }
    }

    /**
     * Adapter fixture exposing a mutable external property map to test defensive copying.
     * @since 0.0.1
     */
    private static final class PropertyNode extends NodeWithStructuralEquality {
        /**
         * External map.
         */
        private final Map<String, String> properties;

        /**
         * Constructor.
         * @param properties External properties
         */
        private PropertyNode(final Map<String, String> properties) {
            this.properties = properties;
        }

        @Override
        public String getData() {
            return "";
        }

        @Override
        public int getChildCount() {
            return 0;
        }

        @Override
        public Node getChild(final int index) {
            throw new IndexOutOfBoundsException(index);
        }

        @Override
        public Type getType() {
            return Return.TYPE;
        }

        @Override
        public Map<String, String> getProperties() {
            return this.properties;
        }
    }
}
