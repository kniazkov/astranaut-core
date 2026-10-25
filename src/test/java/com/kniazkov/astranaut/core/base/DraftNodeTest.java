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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests covering {@link DraftNode} class.
 * @since 0.0.1
 */
class DraftNodeTest {
    @Test
    void snapshotsBuilderStateAndRejectsNullChildrenAtomically() {
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName("Parent");
        builder.setData("before");
        final Node child = DraftNode.create("Child");
        builder.setChildren(List.of(child, child));
        final DraftNode first = builder.build();
        final List<Node> invalid = new java.util.ArrayList<>(1);
        invalid.add(null);
        Assertions.assertThrows(NullPointerException.class, () -> builder.setChildren(invalid));
        Assertions.assertEquals(2, builder.build().getChildCount());
        builder.setData("after");
        builder.setChildren(List.of());
        Assertions.assertEquals("before", first.getData());
        Assertions.assertEquals(2, first.getChildCount());
        Assertions.assertSame(first.getChild(0), first.getChild(1));
        Assertions.assertEquals(0, builder.build().getChildCount());
    }

    @Test
    void createAndSerialize() {
        final String[] cases = {
            "X",
            "X(A)",
            "X(A, B, C)",
            "X(A(B, C))",
            "Addition(Expression, Expression)",
            "Node<\"data\">",
        };
        for (final String test : cases) {
            Assertions.assertTrue(DraftNodeTest.createAndSerialize(test));
        }
    }

    @Test
    void constructorTest() {
        final DraftNode.Constructor ctor = new DraftNode.Constructor();
        Assertions.assertEquals("", ctor.toString());
        final String name = "Name";
        ctor.setName(name);
        Assertions.assertEquals(name, ctor.toString());
        ctor.setData("data");
        Assertions.assertEquals("Name<\"data\">", ctor.toString());
        final List<Node> children = Arrays.asList(
            DraftNode.create("A"),
            DraftNode.create("B"),
            DraftNode.create("C")
        );
        ctor.setChildren(children);
        final String serialized = "Name<\"data\">(A, B, C)";
        Assertions.assertEquals(serialized, ctor.toString());
        Assertions.assertEquals(serialized, ctor.build().toString());
    }

    @Test
    void addNodeTest() {
        final DraftNode.Constructor ctor = new DraftNode.Constructor();
        ctor.setName("X");
        ctor.addChild(DraftNode.create("A"));
        final String serialized = "X(A)";
        Assertions.assertEquals(serialized, ctor.build().toString());
    }

    @Test
    void testExtendedDescriptorProcessor() {
        final Map<String, Set<Node>> nodes = new TreeMap<>();
        final Node root = DraftNode.create("X(A,A,B(C,D))", nodes);
        Assertions.assertSame(
            root,
            nodes.computeIfAbsent(
                "X",
                s -> Collections.singleton(DummyNode.INSTANCE)
            ).iterator().next()
        );
        Assertions.assertEquals(
            2,
            nodes.computeIfAbsent(
                "A",
                s -> Collections.singleton(DummyNode.INSTANCE)
            ).size()
        );
        Assertions.assertEquals(
            2,
            nodes.computeIfAbsent(
                "B",
                s -> Collections.singleton(DummyNode.INSTANCE)
            ).iterator().next().getChildCount()
        );
    }

    @Test
    void typeTest() {
        final Node node = DraftNode.create("A");
        final Type type = node.getType();
        Assertions.assertEquals("A", type.getName());
        Assertions.assertTrue(type.getChildDescriptors().isEmpty());
        Assertions.assertTrue(type.getBaseTypes().isEmpty());
        Assertions.assertEquals("A", type.getName());
        final Builder builder = type.createBuilder();
        final Node clone = builder.build();
        Assertions.assertTrue(node.deepCompare(clone));
    }

    @Test
    void wrongConstructionTest() {
        final DraftNode.Constructor ctor = new DraftNode.Constructor();
        Assertions.assertThrows(IllegalStateException.class, ctor::build);
    }

    @Test
    void wrongChildrenListFormat() {
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> DraftNode.create("A(B(C #D),E)")
        );
        Assertions.assertThrows(IllegalArgumentException.class, () -> DraftNode.create("A(B"));
    }

    @Test
    void wrongDataFormat() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> DraftNode.create("A<$ >(B)"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> DraftNode.create("A<$ "));
    }

    /**
     * Creates a tree based on the given description and then serializes it to obtain
     *  its description. If the original description matches the generated one, the test
     *  is considered successful; otherwise, the test fails.
     * @param description The description used to create the tree.
     * @return Testing result, {@code true} if the original description matches the serialized
     *  description, {@code false} otherwise.
     */
    private static boolean createAndSerialize(final String description) {
        final Node node = DraftNode.create(description);
        final String serialized = node.toString();
        return description.equals(serialized);
    }
}
