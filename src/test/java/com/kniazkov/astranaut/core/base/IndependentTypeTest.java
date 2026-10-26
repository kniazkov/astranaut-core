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

import com.kniazkov.astranaut.core.algorithms.normalization.NormalizedNode;
import com.kniazkov.astranaut.core.algorithms.normalization.Properties;
import com.kniazkov.astranaut.core.algorithms.normalization.Property;
import com.kniazkov.astranaut.core.example.LittleTrees;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contracts for independent descriptors of built-in nodes.
 * @since 0.0.1
 */
final class IndependentTypeTest {
    @Test
    void draftFactoriesDoNotCopyNodeState() {
        final Node original = DraftNode.create("Root<'old'>(Child)");
        final Type type = original.getType();
        final Builder first = type.createBuilder();
        final Builder second = type.createBuilder();
        Assertions.assertNotSame(first, second);
        Assertions.assertTrue(first.setData("new"));
        Assertions.assertTrue(first.setChildren(List.of(DraftNode.create("Other"))));
        final Node created = first.build();
        final Node empty = second.build();
        Assertions.assertEquals(type, created.getType());
        Assertions.assertEquals(type, empty.getType());
        Assertions.assertEquals("", empty.getData());
        Assertions.assertEquals(0, empty.getChildCount());
        Assertions.assertEquals("old", original.getData());
        Assertions.assertEquals("Child", original.getChild(0).getTypeName());
    }

    @Test
    void propertyFactoryKeepsOnlyTheName() {
        final Property original = new Property("label", "before");
        final Type type = original.getType();
        final Builder builder = type.createBuilder();
        Assertions.assertTrue(builder.setData("after"));
        final Node created = builder.build();
        Assertions.assertInstanceOf(Property.class, created);
        Assertions.assertEquals(type, created.getType());
        Assertions.assertEquals("after", created.getData());
        Assertions.assertEquals("before", original.getData());
        Assertions.assertEquals("", type.createBuilder().build().getData());
    }

    @Test
    void propertiesFactoryDoesNotCaptureValues() {
        final Properties original = new Properties(Map.of("key", "value"));
        final Builder builder = original.getType().createBuilder();
        final Node created = builder.build();
        Assertions.assertInstanceOf(Properties.class, created);
        Assertions.assertEquals(0, created.getChildCount());
        Assertions.assertEquals(1, original.getChildCount());
        Assertions.assertEquals(original.getType(), created.getType());
    }

    @Test
    void normalizedFactoryBuildsIndependentNodes() {
        final NormalizedNode original = new NormalizedNode(DraftNode.create("Branch(A,B)"));
        final Type type = original.getType();
        final Builder builder = type.createBuilder();
        Assertions.assertTrue(builder.setChildren(original.getChildrenList()));
        final Node created = builder.build();
        Assertions.assertInstanceOf(NormalizedNode.class, created);
        Assertions.assertEquals(type, created.getType());
        Assertions.assertEquals(4, created.getChildCount());
        Assertions.assertEquals(4, original.getChildCount());
        Assertions.assertFalse(type.createBuilder().isValid());
    }

    @Test
    void wrapperPropertiesDoNotBecomeTypeProperties() {
        final Hole hole = new Hole(DraftNode.create("Expression"), 1);
        final List<Node> nodes = List.of(hole, new PatternNode(hole), new DiffNode(hole));
        for (final Node node : nodes) {
            Assertions.assertEquals("purple", node.getProperties().get("color"));
            Assertions.assertTrue(node.getType().getProperties().isEmpty());
            Assertions.assertEquals(hole.getType(), node.getType());
        }
    }

    @Test
    void wrappersKeepPrototypeTypeProperties() {
        final Node original = LittleTrees.createIntegerLiteral(42);
        final Map<String, String> properties = original.getType().getProperties();
        Assertions.assertFalse(properties.isEmpty());
        final List<Node> nodes = List.of(
            new PatternNode(original), new DiffNode(original)
        );
        for (final Node node : nodes) {
            Assertions.assertEquals(properties, node.getType().getProperties());
            Assertions.assertEquals(original.getType(), node.getType());
        }
    }

    @Test
    void nullDescriptorKeepsItsLeafSchema() {
        final Type type = NullNode.INSTANCE.getType();
        Assertions.assertTrue(type.getBaseTypes().isEmpty());
        Assertions.assertEquals(List.of(), type.getChildDescriptors().orElseThrow());
        Assertions.assertThrows(UnsupportedOperationException.class, type::createBuilder);
    }
}
