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
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.example.LittleTrees;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Mutation and snapshot contracts of normalization builders.
 * @since 0.0.1
 */
class NormalizationBuilderTest {
    @Test
    void editsPropertyListsWithoutMutatingPublishedNodes() {
        final Builder builder = new Properties(Map.of()).getType().createBuilder();
        final Property first = new Property("first", "one");
        final Property second = new Property("second", "two");
        Assertions.assertTrue(builder.setChildren(List.of(first, second)));
        final Node old = builder.build();
        Assertions.assertTrue(builder.setChild(0, second));
        Assertions.assertFalse(builder.setChild(1, DraftNode.create("wrong")));
        Assertions.assertTrue(builder.removeChild(1));
        Assertions.assertFalse(builder.removeChild(1));
        Assertions.assertEquals(List.of(second), builder.build().getChildrenList());
        Assertions.assertSame(first, old.getChild(0));
        Assertions.assertEquals(2, old.getChildCount());
    }

    @Test
    void removesRequiredNormalizedChildrenWithoutBuildingInvalidNodes() {
        final Node left = LittleTrees.createVariable("x");
        final Node right = LittleTrees.createIntegerLiteral(1);
        final Builder builder = new NormalizedNode(LittleTrees.createAddition(left, right))
            .getType().createBuilder();
        final Properties props = new Properties(Map.of("color", "red"));
        Assertions.assertTrue(builder.setChildren(List.of(props, left, right)));
        final Node old = builder.build();
        Assertions.assertFalse(builder.removeChild(-1));
        Assertions.assertFalse(builder.setChild(-1, props));
        Assertions.assertFalse(builder.setChild(0, right));
        Assertions.assertTrue(builder.setChild(0, props));
        Assertions.assertTrue(builder.setChild(2, new NormalizedNode(left)));
        Assertions.assertTrue(builder.removeChild(1));
        Assertions.assertFalse(builder.isValid());
        Assertions.assertThrows(ConstructionException.class, builder::build);
        Assertions.assertTrue(builder.removeChild(0));
        Assertions.assertFalse(builder.removeChild(4));
        Assertions.assertEquals(3, old.getChildCount());
        Assertions.assertTrue(builder.setChildren(List.of(left, right)));
        Assertions.assertTrue(builder.setChild(0, right));
        Assertions.assertTrue(builder.isValid());
        Assertions.assertEquals(2, builder.build().getChildCount());
    }
}
