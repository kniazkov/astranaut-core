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
package com.kniazkov.astranaut.core.algorithms.conversion;

import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.Char;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.DummyNode;
import com.kniazkov.astranaut.core.base.NamedType;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.TestBuilder;
import com.kniazkov.astranaut.core.base.TestBuilderCase;
import com.kniazkov.astranaut.core.base.Tree;
import com.kniazkov.astranaut.core.base.Type;
import com.kniazkov.astranaut.core.example.LittleTrees;
import com.kniazkov.astranaut.core.example.converters.Converter0;
import com.kniazkov.astranaut.core.example.converters.Converter1;
import com.kniazkov.astranaut.core.example.converters.Converter2;
import com.kniazkov.astranaut.core.example.converters.Converter3;
import com.kniazkov.astranaut.core.example.converters.WhitespaceConverter;
import com.kniazkov.astranaut.core.example.green.GreenFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests covering conversion algorithms.
 * @since 0.0.1
 */
class ConversionTest {
    /**
     * Root node name (for test purposes).
     */
    private static final String ROOT_NODE_NAME = "Root";

    /**
     * Node representing the '+' operator.
     */
    private static final Node OPERATOR_PLUS = DraftNode.create("Operator<'+'>");

    /**
     * Node representing the '*' operator.
     */
    private static final Node OPERATOR_MUL = DraftNode.create("Operator<'*'>");

    /**
     * Node representing the '=' operator.
     */
    private static final Node OPERATOR_ASSIGN = DraftNode.create("Operator<'='>");

    @Test
    void collectingConverters() {
        final List<Converter> list = new LinkedList<>();
        Converter.collectConverters("com.kniazkov.astranaut.core.example.converters", list);
        Assertions.assertEquals(4, list.size());
    }

    @Test
    void extractingChildren() {
        final Node root = DraftNode.create(
            ConversionTest.ROOT_NODE_NAME,
            "",
            LittleTrees.createIntegerLiteral(2),
            ConversionTest.OPERATOR_PLUS,
            LittleTrees.createIntegerLiteral(3)
        );
        final Converter converter = Converter0.INSTANCE;
        final Optional<ConversionResult> result =
            converter.convert(root.getChildrenList(), 0, GreenFactory.INSTANCE);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get().getConsumed());
        Assertions.assertEquals("2 + 3", result.get().getNode().toString());
    }

    @Test
    void extractingData() {
        final Node before = DraftNode.create("int<'7'>");
        final Node root = DraftNode.create(ConversionTest.ROOT_NODE_NAME, "", before);
        final Converter converter = Converter2.INSTANCE;
        final Optional<ConversionResult> result =
            converter.convert(root.getChildrenList(), 0, GreenFactory.INSTANCE);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("7", result.get().getNode().toString());
    }

    @Test
    void leftToRight() {
        final Node root = DraftNode.create(
            ConversionTest.ROOT_NODE_NAME,
            "",
            LittleTrees.createIntegerLiteral(2),
            ConversionTest.OPERATOR_PLUS,
            LittleTrees.createIntegerLiteral(3),
            ConversionTest.OPERATOR_PLUS,
            LittleTrees.createIntegerLiteral(4)
        );
        final DefaultTransformer transformer = new DefaultTransformer(
            Collections.singletonList(Converter0.INSTANCE),
            GreenFactory.INSTANCE
        );
        final Tree result = transformer.transform(new Tree(root));
        Assertions.assertEquals(1, result.getRoot().getChildCount());
        Assertions.assertEquals("2 + 3 + 4", result.getRoot().getChild(0).toString());
    }

    @Test
    void leftToRightTwoRules() {
        final Node root = DraftNode.create(
            ConversionTest.ROOT_NODE_NAME,
            "",
            LittleTrees.createIntegerLiteral(2),
            ConversionTest.OPERATOR_PLUS,
            LittleTrees.createIntegerLiteral(3),
            ConversionTest.OPERATOR_MUL,
            LittleTrees.createIntegerLiteral(4)
        );
        final DefaultTransformer transformer = new DefaultTransformer(
            Arrays.asList(Converter3.INSTANCE, Converter0.INSTANCE),
            GreenFactory.INSTANCE
        );
        final Tree result = transformer.transform(new Tree(root));
        Assertions.assertEquals(1, result.getRoot().getChildCount());
        Assertions.assertEquals("2 + 3 * 4", result.getRoot().getChild(0).toString());
    }

    @Test
    void rightToLeft() {
        final Node root = DraftNode.create(
            ConversionTest.ROOT_NODE_NAME,
            "",
            LittleTrees.createVariable("x"),
            ConversionTest.OPERATOR_ASSIGN,
            LittleTrees.createVariable("y"),
            ConversionTest.OPERATOR_ASSIGN,
            LittleTrees.createIntegerLiteral(0)
        );
        final DefaultTransformer transformer = new DefaultTransformer(
            Collections.singletonList(Converter1.INSTANCE),
            GreenFactory.INSTANCE
        );
        final Tree result = transformer.transform(new Tree(root));
        Assertions.assertEquals(1, result.getRoot().getChildCount());
        Assertions.assertEquals("x = y = 0", result.getRoot().getChild(0).toString());
    }

    @Test
    void partialTransformation() {
        final Node root = DraftNode.create(
            ConversionTest.ROOT_NODE_NAME,
            "",
            LittleTrees.createVariable("a"),
            LittleTrees.createVariable("x"),
            ConversionTest.OPERATOR_ASSIGN,
            LittleTrees.createVariable("y"),
            LittleTrees.createVariable("b"),
            LittleTrees.createVariable("q"),
            ConversionTest.OPERATOR_ASSIGN,
            LittleTrees.createVariable("r"),
            LittleTrees.createVariable("c"),
            LittleTrees.createVariable("d"),
            LittleTrees.createVariable("e"),
            LittleTrees.createVariable("f")
        );
        final DefaultTransformer transformer = new DefaultTransformer(
            Collections.singletonList(Converter1.INSTANCE),
            GreenFactory.INSTANCE
        );
        final Tree result = transformer.transform(new Tree(root));
        Assertions.assertEquals(8, result.getRoot().getChildCount());
        Assertions.assertEquals("x = y", result.getRoot().getChild(1).toString());
        Assertions.assertEquals("q = r", result.getRoot().getChild(3).toString());
    }

    @Test
    void differentDirections() {
        final Node root = DraftNode.create(
            ConversionTest.ROOT_NODE_NAME,
            "",
            LittleTrees.createVariable("x"),
            ConversionTest.OPERATOR_ASSIGN,
            LittleTrees.createVariable("y"),
            ConversionTest.OPERATOR_PLUS,
            DraftNode.create("int<'0'>")
        );
        final DefaultTransformer transformer = new DefaultTransformer(
            Arrays.asList(
                Converter2.INSTANCE,
                Converter0.INSTANCE,
                Converter1.INSTANCE
            ),
            GreenFactory.INSTANCE
        );
        final Tree result = transformer.transform(new Tree(root));
        Assertions.assertEquals(1, result.getRoot().getChildCount());
        Assertions.assertEquals("x = y + 0", result.getRoot().getChild(0).toString());
    }

    @Test
    void badBuilder() {
        final DefaultTransformer transformer = new DefaultTransformer(
            Collections.singletonList(Converter0.INSTANCE),
            GreenFactory.INSTANCE
        );
        Tree result = transformer.transform(new Tree(new TestNode(TestBuilderCase.BAD_DATA)));
        Assertions.assertSame(DummyNode.INSTANCE, result.getRoot());
        result = transformer.transform(new Tree(new TestNode(TestBuilderCase.BAD_CHILDREN)));
        Assertions.assertSame(DummyNode.INSTANCE, result.getRoot());
        result = transformer.transform(new Tree(new TestNode(TestBuilderCase.INVALID_BUILDER)));
        Assertions.assertSame(DummyNode.INSTANCE, result.getRoot());
    }

    @Test
    void nullNode() {
        final Char.Constructor ctor = new Char.Constructor();
        ctor.setValue('a');
        final Node first = ctor.createNode();
        ctor.setValue('b');
        final Node second = ctor.createNode();
        ctor.setValue('c');
        final Node third = ctor.createNode();
        ctor.setValue(' ');
        final Node space = ctor.createNode();
        final Node root = DraftNode.create(
            ConversionTest.ROOT_NODE_NAME,
            "",
            first,
            space,
            second,
            space,
            space,
            third
        );
        final DefaultTransformer transformer = new DefaultTransformer(
            Collections.singletonList(WhitespaceConverter.INSTANCE),
            GreenFactory.INSTANCE
        );
        final Tree result = transformer.transform(new Tree(root));
        final List<Node> list = result.getRoot().getChildrenList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("a", list.get(0).getData());
        Assertions.assertEquals("b", list.get(1).getData());
        Assertions.assertEquals("c", list.get(2).getData());
    }

    /**
     * Node that returns "bad" builder for testing purposes.
     * @since 0.0.1
     */
    @SuppressWarnings("PMD.TestClassWithoutTestCases")
    private static final class TestNode implements Node {
        /**
         * Predefined list of child nodes.
         */
        private static final Node[] CHILDREN = {
            LittleTrees.createIntegerLiteral(2),
            ConversionTest.OPERATOR_PLUS,
            LittleTrees.createIntegerLiteral(3),
        };

        /**
         * Descriptor with configurable builder behavior.
         */
        private final Type type;

        /**
         * Constructor.
         * @param test Test case
         */
        private TestNode(final TestBuilderCase test) {
            this.type = new TypeImpl(test);
        }

        @Override
        public Type getType() {
            return this.type;
        }

        @Override
        public String getData() {
            return "";
        }

        @Override
        public int getChildCount() {
            return TestNode.CHILDREN.length;
        }

        @Override
        public Node getChild(final int index) {
            return TestNode.CHILDREN[index];
        }

        /**
         * Type descriptor independent of node instances.
         * @since 0.0.1
         */
        private static final class TypeImpl extends NamedType implements Type {
            /**
             * Immutable type definition.
             */
            private final TestBuilderCase test;

            /**
             * Creates the descriptor.
             * @param test Builder behavior
             */
            private TypeImpl(final TestBuilderCase test) {
                this.test = test;
            }

            @Override
            public String getName() {
                return "Root";
            }

            @Override
            public Builder createBuilder() {
                return new TestBuilder(this.test);
            }
        }
    }
}
