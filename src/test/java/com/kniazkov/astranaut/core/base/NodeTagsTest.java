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
package com.kniazkov.astranaut.core.base;

import com.kniazkov.astranaut.core.example.green.Addition;
import com.kniazkov.astranaut.core.example.green.Expression;
import com.kniazkov.astranaut.core.example.green.IntegerLiteral;
import com.kniazkov.astranaut.core.example.green.ProgramItem;
import com.kniazkov.astranaut.core.example.green.Return;
import com.kniazkov.astranaut.core.example.green.Statement;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tagged access for generated-style nodes and schema-free adapters.
 * @since 0.0.1
 */
class NodeTagsTest {
    /**
     * Left child tag.
     */
    private static final String LEFT = "left";

    /**
     * Right child tag.
     */
    private static final String RIGHT = "right";

    @Test
    void preservesTransitiveBasesOfExistingGeneratedStyleTypes() {
        Assertions.assertEquals(Set.of(Statement.TYPE), Return.TYPE.getBaseTypes());
        Assertions.assertTrue(Return.TYPE.isSubtypeOf(ProgramItem.TYPE));
        Assertions.assertEquals(
            Optional.of(Statement.TYPE), Return.TYPE.findCommonBaseType(Statement.TYPE)
        );
    }

    @Test
    void defaultsToNoTagsAndChecksIndexBounds() {
        final Node node = DraftNode.create("Parent(Child)");
        Assertions.assertTrue(node.getChild("missing").isEmpty());
        Assertions.assertTrue(node.getChildTag(0).isEmpty());
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> node.getChildTag(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> node.getChildTag(1));
        Assertions.assertThrows(NullPointerException.class, () -> node.getChild((String) null));
    }

    @Test
    void generatedStyleNodeDistinguishesOccurrencesOfSharedChild() {
        final IntegerLiteral.Constructor literal = new IntegerLiteral.Constructor();
        literal.setData("42");
        final Node child = literal.build();
        final Addition.Constructor builder = new Addition.Constructor();
        Assertions.assertTrue(builder.setChildren(List.of(child, child)));
        final Addition node = (Addition) builder.build();
        Assertions.assertSame(child, node.getLeft());
        Assertions.assertSame(child, node.getRight());
        Assertions.assertSame(child, node.getChild(NodeTagsTest.LEFT).orElseThrow());
        Assertions.assertSame(child, node.getChild(NodeTagsTest.RIGHT).orElseThrow());
        Assertions.assertEquals(Optional.of(NodeTagsTest.LEFT), node.getChildTag(0));
        Assertions.assertEquals(Optional.of(NodeTagsTest.RIGHT), node.getChildTag(1));
        Assertions.assertTrue(node.getChild("unknown").isEmpty());
        final List<ChildDescriptor> schema = node.getType().getChildDescriptors().orElseThrow();
        Assertions.assertSame(Expression.TYPE, schema.get(0).getType());
        Assertions.assertEquals(node.getChildTag(1), schema.get(1).getTag());
    }

    @Test
    void schemaFreeAdapterPreservesTagAfterOptionalGap() {
        final IntegerLiteral.Constructor literal = new IntegerLiteral.Constructor();
        literal.setData("1");
        final IntegerLiteral right = (IntegerLiteral) literal.build();
        final OptionalPair node = new OptionalPair(Optional.empty(), Optional.of(right));
        Assertions.assertTrue(node.getType().getChildDescriptors().isEmpty());
        Assertions.assertTrue(node.getChild(NodeTagsTest.LEFT).isEmpty());
        Assertions.assertEquals(Optional.of(NodeTagsTest.RIGHT), node.getChildTag(0));
        Assertions.assertSame(right, node.getChild(0));
        Assertions.assertSame(right, node.getChild(NodeTagsTest.RIGHT).orElseThrow());
        final OptionalRight base = node;
        Assertions.assertSame(right, base.getRight().orElseThrow());
    }

    @Test
    void equalLookingChildrenRemainSeparateOccurrences() {
        final IntegerLiteral.Constructor literal = new IntegerLiteral.Constructor();
        literal.setData("1");
        final IntegerLiteral left = (IntegerLiteral) literal.build();
        final IntegerLiteral right = (IntegerLiteral) literal.build();
        final OptionalPair pair = new OptionalPair(Optional.of(left), Optional.of(right));
        Assertions.assertTrue(left.deepCompare(right));
        Assertions.assertNotSame(left, right);
        Assertions.assertSame(left, pair.getChild(NodeTagsTest.LEFT).orElseThrow());
        Assertions.assertSame(right, pair.getChild(NodeTagsTest.RIGHT).orElseThrow());
        Assertions.assertEquals(Optional.of(NodeTagsTest.RIGHT), pair.getChildTag(1));
    }

    @Test
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    void supportsConcurrentReads() throws Exception {
        final IntegerLiteral.Constructor literal = new IntegerLiteral.Constructor();
        literal.setData("7");
        final IntegerLiteral child = (IntegerLiteral) literal.build();
        final OptionalPair pair = new OptionalPair(Optional.of(child), Optional.of(child));
        final Callable<Boolean> read = () ->
            pair.getChild(NodeTagsTest.RIGHT).orElseThrow() == pair.getChild(1)
            && pair.getChildTag(0).equals(Optional.of(NodeTagsTest.LEFT));
        try (var executor = Executors.newFixedThreadPool(2)) {
            for (final var result : executor.invokeAll(List.of(read, read, read, read))) {
                Assertions.assertTrue(result.get());
            }
        }
    }

    /**
     * Covariant optional getter used by an external base interface.
     * @since 0.0.1
     */
    private interface OptionalRight extends Node {
        /**
         * Returns the optional right child.
         * @return Child
         */
        Optional<? extends Expression> getRight();
    }

    /**
     * Independent schema-free adapter with two optional positions.
     * @since 0.0.1
     */
    private static final class OptionalPair implements OptionalRight {
        /**
         * Left child.
         */
        private final Optional<IntegerLiteral> left;

        /**
         * Right child.
         */
        private final Optional<IntegerLiteral> right;

        /**
         * Present children in order.
         */
        private final List<Node> children;

        /**
         * Tags of present occurrences.
         */
        private final List<String> tags;

        /**
         * Creates an immutable adapter.
         * @param left Left child
         * @param right Right child
         */
        OptionalPair(final Optional<IntegerLiteral> left, final Optional<IntegerLiteral> right) {
            this.left = left;
            this.right = right;
            this.children = java.util.stream.Stream.of(left, right)
                .flatMap(Optional::stream)
                .<Node>map(node -> node)
                .toList();
            this.tags = java.util.stream.Stream.of(
                left.map(node -> NodeTagsTest.LEFT), right.map(node -> NodeTagsTest.RIGHT)
            ).flatMap(Optional::stream).toList();
        }

        @Override
        public Type getType() {
            return DraftNode.create("OptionalPair").getType();
        }

        @Override
        public String getData() {
            return "";
        }

        @Override
        public int getChildCount() {
            return this.children.size();
        }

        @Override
        public Node getChild(final int index) {
            return this.children.get(index);
        }

        @Override
        public Optional<Node> getChild(final String tag) {
            return switch (tag) {
                case NodeTagsTest.LEFT -> this.left.map(node -> node);
                case NodeTagsTest.RIGHT -> this.right.map(node -> node);
                default -> Optional.empty();
            };
        }

        @Override
        public Optional<String> getChildTag(final int index) {
            return Optional.of(this.tags.get(index));
        }

        @Override
        public Optional<IntegerLiteral> getRight() {
            return this.right;
        }
    }
}
