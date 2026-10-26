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

import com.kniazkov.astranaut.core.algorithms.DiffTreeBuilder;
import com.kniazkov.astranaut.core.algorithms.mapping.TopDownMapper;
import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.ConstructionException;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Tree;
import com.kniazkov.astranaut.core.utils.JsonSerializer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Normalized metadata and edges remain ordinary editable, serializable tree nodes.
 * @since 0.0.1
 */
class NormalizationIntegrationTest {
    @Test
    void explicitEdgesHaveIndependentBuildersAndUnambiguousTags() {
        final Node leaf = DraftNode.create("Leaf");
        final NormalizedEdge edge = new NormalizedEdge(Optional.of(""), leaf);
        final Builder builder = edge.getType().createBuilder();
        Assertions.assertFalse(builder.isValid());
        Assertions.assertThrows(ConstructionException.class, builder::build);
        Assertions.assertTrue(builder.setData(edge.getData()));
        Assertions.assertFalse(builder.setData("invalid"));
        Assertions.assertFalse(builder.setChildren(List.of()));
        Assertions.assertTrue(builder.setChildren(List.of(leaf)));
        final Node old = builder.build();
        Assertions.assertEquals(edge, old);
        Assertions.assertNotEquals(new NormalizedEdge(Optional.empty(), leaf), old);
        Assertions.assertFalse(builder.setChild(-1, leaf));
        Assertions.assertFalse(builder.removeChild(1));
        Assertions.assertTrue(builder.setChild(0, DraftNode.create("Other")));
        Assertions.assertSame(leaf, old.getChild(0));
        Assertions.assertTrue(builder.removeChild(0));
        Assertions.assertFalse(builder.isValid());
        Assertions.assertThrows(ConstructionException.class, builder::build);
    }

    @Test
    void addingAndRemovingEdgesDoesNotChangeTheNormalizedType() {
        final Node root = new NormalizedNode(DraftNode.create("Root"));
        final Builder builder = root.getType().createBuilder();
        final Node edge = new NormalizedEdge(Optional.of("named"), DraftNode.create("Leaf"));
        Assertions.assertTrue(
            builder.setChildren(List.of(root.getChild(0), root.getChild(1), edge))
        );
        final Node old = builder.build();
        Assertions.assertEquals(root.getType(), old.getType());
        Assertions.assertEquals(Optional.of("named"), old.getChildTag(2));
        Assertions.assertFalse(
            builder.setChildren(List.of(root.getChild(0), root.getChild(1), edge, edge))
        );
        Assertions.assertEquals(old, builder.build());
        Assertions.assertFalse(
            builder.setChild(2, new NormalizedEdge(Optional.of("other"), DraftNode.create("Leaf")))
        );
        Assertions.assertTrue(builder.removeChild("named"));
        Assertions.assertEquals(root, builder.build());
        Assertions.assertEquals(3, old.getChildCount());
    }

    @Test
    void existingDiffAndSerializerAcceptTheStandaloneRepresentation() {
        final Node first = new NormalizedNode(
            new NormalizedNodeTest.Fixture(
                Map.of("color", "red"), List.of(DraftNode.create("A")), List.of(Optional.of("left"))
            )
        );
        final Node second = new NormalizedNode(
            new NormalizedNodeTest.Fixture(
                Map.of("color", "blue"),
                List.of(DraftNode.create("B")), List.of(Optional.of("left"))
            )
        );
        final DiffTreeBuilder diff = new DiffTreeBuilder(first);
        diff.build(second, TopDownMapper.INSTANCE);
        Assertions.assertEquals(first, diff.getDiffTree().getBefore().getRoot().orElseThrow());
        Assertions.assertEquals(second, diff.getDiffTree().getAfter().getRoot().orElseThrow());
        final String json = new JsonSerializer(new Tree(second)).serialize();
        Assertions.assertTrue(json.contains("AstranautNormalizedEdge"));
        Assertions.assertTrue(json.contains("Tleft"));
        Assertions.assertTrue(json.contains("blue"));
    }
}
