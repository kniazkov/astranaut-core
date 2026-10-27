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

import com.kniazkov.astranaut.core.algorithms.DiffTreeBuilder;
import com.kniazkov.astranaut.core.algorithms.PatternBuilder;
import com.kniazkov.astranaut.core.algorithms.SubtreeBuilder;
import com.kniazkov.astranaut.core.algorithms.mapping.TopDownMapper;
import com.kniazkov.astranaut.core.algorithms.patching.DefaultPatcher;
import com.kniazkov.astranaut.core.utils.JsonDeserializer;
import com.kniazkov.astranaut.core.utils.JsonSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Empty trees at boundaries of existing consumers.
 * @since 0.0.1
 */
class EmptyTreeIntegrationTest {
    @Test
    void emptyJsonNeedsNoFactory() {
        final String json = new JsonSerializer(new Tree()).serialize();
        Assertions.assertTrue(json.contains("\"root\": null"));
        final Tree restored = new JsonDeserializer(
            json, name -> {
                throw new AssertionError("An empty tree needs no resolver");
            }
        ).convert();
        Assertions.assertTrue(restored.getRoot().isEmpty());
    }

    @Test
    void emptyTreeHasNoPatternMatches() {
        final Tree tree = new Tree();
        final Pattern pattern = new Pattern(new PatternNode(DraftNode.create("Root")));
        Assertions.assertSame(tree, DefaultPatcher.INSTANCE.patch(tree, pattern));
    }

    @Test
    void rootBasedBuildersRejectEmptyInputExplicitly() {
        final Tree tree = new Tree();
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PatternBuilder(tree));
        Assertions.assertTrue(
            new DiffTreeBuilder(
            tree
        ).getDiffTree().getAfter().getRoot().isEmpty()
        );
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> new SubtreeBuilder(tree, SubtreeBuilder.INCLUDE)
        );
        final DiffTreeBuilder builder = new DiffTreeBuilder(Tree.createDraft("Root"));
        Assertions.assertTrue(builder.build(tree, TopDownMapper.INSTANCE));
        Assertions.assertTrue(
            builder.getDiffTree().getBefore().deepCompare(Tree.createDraft("Root"))
        );
    }
}
