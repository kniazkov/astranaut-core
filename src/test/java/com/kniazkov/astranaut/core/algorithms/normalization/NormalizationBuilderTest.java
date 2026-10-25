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

import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.ConstructionException;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.EmptyFragment;
import com.kniazkov.astranaut.core.base.Node;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Atomic editing and snapshot contracts for normalized builders.
 * @since 0.0.1
 */
class NormalizationBuilderTest {
    @Test
    void propertyListsAreSortedUniqueSnapshots() {
        final Builder builder = new Properties(Map.of()).getType().createBuilder();
        final Property first = new Property("first", "one");
        final Property second = new Property("second", "two");
        final List<Node> input = new ArrayList<>(List.of(second, first));
        Assertions.assertTrue(builder.setChildren(input));
        input.clear();
        final Node old = builder.build();
        Assertions.assertEquals(List.of(first, second), old.getChildrenList());
        Assertions.assertFalse(builder.setChild(0, first));
        Assertions.assertFalse(builder.setChildren(List.of(first, first)));
        Assertions.assertFalse(builder.setChild(1, DraftNode.create("wrong")));
        Assertions.assertEquals(old, builder.build());
        Assertions.assertTrue(builder.removeChild(0));
        Assertions.assertFalse(builder.removeChild(4));
        Assertions.assertEquals(List.of(first), builder.build().getChildrenList());
        Assertions.assertEquals(2, old.getChildCount());
    }

    @Test
    void normalizedBuildersPreserveLayoutsWithoutOriginalConstraints() {
        final Node child = DraftNode.create("Child");
        final Node root = new NormalizedNode(
            new NormalizedNodeTest.Fixture(
                Map.of(), List.of(child, child), List.of(Optional.of("a"), Optional.of("b"))
            )
        );
        final Builder builder = root.getType().createBuilder();
        Assertions.assertFalse(builder.isValid());
        Assertions.assertThrows(ConstructionException.class, builder::build);
        Assertions.assertTrue(builder.setChildren(root.getChildrenList()));
        final Node old = builder.build();
        Assertions.assertTrue(builder.setData("data allowed by normalized model"));
        builder.setFragment(EmptyFragment.INSTANCE);
        Assertions.assertFalse(builder.setChildren(List.of(child)));
        Assertions.assertFalse(builder.setChild(0, child));
        Assertions.assertFalse(builder.setChild(-1, child));
        Assertions.assertFalse(builder.setChild("unknown", child));
        Assertions.assertTrue(builder.setChild("a", new NormalizedEdge(Optional.of("a"), child)));
        Assertions.assertSame(child, builder.build().getChild("a").orElseThrow().getChild(0));
        Assertions.assertTrue(builder.removeChild("a"));
        Assertions.assertTrue(builder.isValid());
        Assertions.assertFalse(
            builder.setChild("a", new NormalizedEdge(Optional.of("a"), child))
        );
        Assertions.assertTrue(builder.setChildren(root.getChildrenList()));
        Assertions.assertTrue(
            builder.setChild(
                2, new NormalizedEdge(Optional.of("a"), DraftNode.create("Replacement"))
            )
        );
        Assertions.assertEquals(
            "Replacement", builder.build().getChild("a").orElseThrow().getChild(0).getTypeName()
        );
        Assertions.assertTrue(builder.removeChild(0));
        Assertions.assertFalse(builder.isValid());
        Assertions.assertEquals(root, old);
    }
}
