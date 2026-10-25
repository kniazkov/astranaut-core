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
package com.kniazkov.astranaut.core.algorithms.normalization;

import com.kniazkov.astranaut.core.algorithms.DepthCalculator;
import com.kniazkov.astranaut.core.algorithms.OccurrenceReplacer;
import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeOccurrence;
import com.kniazkov.astranaut.core.base.NodeWithStructuralEquality;
import com.kniazkov.astranaut.core.base.Type;
import com.kniazkov.astranaut.core.base.TypeWithNameBasedEquality;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Independent normalization, metadata, occurrence and stack-safety contracts.
 * @since 0.0.1
 */
class NormalizedNodeTest {
    @Test
    void propertiesBecomeOrderedStructuralChildrenWithoutCallingOriginalBuilder() {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put("z", "last");
        map.put("a", "first");
        final Node left = new Fixture(map, List.of(), List.of());
        final Node right = new Fixture(Map.of("a", "first", "z", "last"), List.of(), List.of());
        final Node different = new Fixture(Map.of("a", "other"), List.of(), List.of());
        Assertions.assertEquals(left, different);
        final NormalizedNode normal = new Normalizer().normalize(left);
        Assertions.assertEquals(normal, new Normalizer().normalize(right));
        Assertions.assertNotEquals(normal, new Normalizer().normalize(different));
        Assertions.assertTrue(normal.getProperties().isEmpty());
        Assertions.assertTrue(normal.getType().getProperties().isEmpty());
        Assertions.assertEquals("Fixture", normal.getOriginalTypeName());
        Assertions.assertNotEquals(left.getType(), normal.getType());
        Assertions.assertEquals("a", ((Property) normal.getChild(0).getChild(0)).getName());
        Assertions.assertEquals(
            Map.of("own", "type"), ((Properties) normal.getChild(1)).collectAllProperties()
        );
        final Builder builder = normal.getType().createBuilder();
        Assertions.assertTrue(builder.setChildren(normal.getChildrenList()));
        Assertions.assertEquals(normal, builder.build());
    }

    @Test
    void keepsSharedChildrenTagsAndRebuildsPropertiesThroughOrdinaryOperations() {
        final Node child = DraftNode.create("Leaf<'value'>");
        final Node root = new Fixture(
            Map.of("color", "red"), List.of(child, child),
            List.of(Optional.of("left"), Optional.of("right"))
        );
        final NormalizedNode normalized = new NormalizedNode(root);
        Assertions.assertSame(
            normalized.getChild(2).getChild(0), normalized.getChild(3).getChild(0)
        );
        Assertions.assertSame(normalized.getChild(2), normalized.getChild("left").orElseThrow());
        Assertions.assertEquals(Optional.of("right"), normalized.getChildTag(3));
        Assertions.assertTrue(normalized.getChildTag(0).isEmpty());
        Assertions.assertTrue(normalized.getChild("missing").isEmpty());
        final Node changed = new OccurrenceReplacer().replace(
            new NodeOccurrence(normalized).getChild(0).getChild(0), new Property("color", "blue")
        ).orElseThrow();
        Assertions.assertEquals("blue", changed.getChild(0).getChild(0).getData());
        Assertions.assertEquals("red", normalized.getChild(0).getChild(0).getData());
        Assertions.assertSame(normalized.getChild(2), changed.getChild(2));
        Assertions.assertEquals(Optional.of("left"), changed.getChildTag(2));
        final Node retagged = new Fixture(
            root.getProperties(), List.of(child, child),
            List.of(Optional.of("other"), Optional.of("right"))
        );
        Assertions.assertNotEquals(normalized, new NormalizedNode(retagged));
    }

    @Test
    void normalizesDeepTreesAndReusesAlreadyNormalizedRoots() {
        Node node = DraftNode.create("Leaf");
        for (int index = 0; index < 30_000; index += 1) {
            node = DraftNode.create("Branch", "", node);
        }
        final Normalizer normalizer = new Normalizer();
        final Node normalized = normalizer.normalize(node);
        Assertions.assertEquals(60_002, new DepthCalculator().calculate(normalized));
        Assertions.assertSame(normalized, normalizer.normalize(normalized));
    }

    @Test
    void distinguishesEmptyPropertyNamesAndServiceLookingTypeNames() {
        final Property empty = new Property("", "");
        final Property unicode = new Property("😀", "");
        Assertions.assertNotEquals(empty.getType(), unicode.getType());
        Assertions.assertNotEquals(new Properties(Map.of()).getType(), empty.getType());
        final Node root = new NormalizedNode(DraftNode.create("AstranautProperties"));
        Assertions.assertNotEquals(root.getType(), new Properties(Map.of()).getType());
        Assertions.assertEquals(2, root.getChildCount());
        Assertions.assertEquals(0, root.getChild(0).getChildCount());
        Assertions.assertEquals(0, root.getChild(1).getChildCount());
    }

    /**
     * Adapter fixture whose type deliberately has no usable source builder.
     * @since 0.0.1
     */
    static final class Fixture extends NodeWithStructuralEquality {
        /**
         * Node metadata.
         */
        private final Map<String, String> properties;

        /**
         * Ordered child occurrences.
         */
        private final List<Node> children;

        /**
         * Tags on those occurrences.
         */
        private final List<Optional<String>> tags;

        /**
         * Creates an immutable adapter.
         * @param properties Metadata
         * @param children Children
         * @param tags Edge tags
         */
        Fixture(final Map<String, String> properties, final List<Node> children,
            final List<Optional<String>> tags) {
            this.properties = Map.copyOf(properties);
            this.children = List.copyOf(children);
            this.tags = List.copyOf(tags);
        }

        @Override
        public Type getType() {
            return new FixtureType();
        }

        @Override
        public String getData() {
            return "";
        }

        @Override
        public Map<String, String> getProperties() {
            return this.properties;
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
        public Optional<String> getChildTag(final int index) {
            return this.tags.get(index);
        }

        @Override
        public Optional<Node> getChild(final String tag) {
            final int index = this.tags.indexOf(Optional.of(tag));
            Optional<Node> result = Optional.empty();
            if (index >= 0) {
                result = Optional.of(this.children.get(index));
            }
            return result;
        }
    }

    /**
     * Source descriptor with its own metadata.
     * @since 0.0.1
     */
    private static final class FixtureType extends TypeWithNameBasedEquality implements Type {
        @Override
        public String getName() {
            return "Fixture";
        }

        @Override
        public Map<String, String> getProperties() {
            return Map.of("own", "type");
        }

        @Override
        public Builder createBuilder() {
            throw new UnsupportedOperationException("Source builder must not be invoked");
        }
    }
}
