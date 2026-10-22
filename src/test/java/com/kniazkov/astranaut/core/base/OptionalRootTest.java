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

import com.kniazkov.astranaut.core.algorithms.LabeledTreeBuilder;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Empty roots and shared immutable tree contents.
 * @since 0.0.1
 */
class OptionalRootTest {
    /**
     * Shared test type name.
     */
    private static final String ROOT_NAME = "Root";

    @Test
    void emptyTreeHasNoPlaceholder() {
        final Tree tree = new Tree();
        Assertions.assertTrue(tree.getRoot().isEmpty());
        Assertions.assertTrue(EmptyTree.INSTANCE.getRoot().isEmpty());
        Assertions.assertTrue(tree.deepCompare(EmptyTree.INSTANCE));
        Assertions.assertTrue(EmptyTree.INSTANCE.deepCompare(tree));
        Assertions.assertEquals("", tree.toString());
        Assertions.assertEquals("", tree.getLanguage());
        Assertions.assertFalse(tree.deepCompare(Tree.createDraft(OptionalRootTest.ROOT_NAME)));
        Assertions.assertFalse(Tree.createDraft(OptionalRootTest.ROOT_NAME).deepCompare(tree));
    }

    @Test
    void nullIsNotAnEmptyTree() {
        Assertions.assertThrows(NullPointerException.class, () -> new Tree(null));
        final Tree tree = new Tree();
        Assertions.assertThrows(NullPointerException.class, () -> tree.deepCompare(null));
    }

    @Test
    void explicitPlaceholderIsStillARealRoot() {
        final Tree tree = new Tree(DummyNode.INSTANCE);
        Assertions.assertSame(DummyNode.INSTANCE, tree.getRoot().orElseThrow());
        Assertions.assertFalse(tree.deepCompare(EmptyTree.INSTANCE));
    }

    @Test
    void emptyTransformationDoesNotInvokeCallback() {
        final Transformer transform = node -> {
            throw new AssertionError("An empty tree has no node to transform");
        };
        Assertions.assertTrue(transform.transform(new Tree()).getRoot().isEmpty());
        final Transformer invalid = node -> null;
        Assertions.assertThrows(
            NullPointerException.class,
            () -> invalid.transform(Tree.createDraft(OptionalRootTest.ROOT_NAME))
        );
    }

    @Test
    void nonEmptyTransformationUsesItsRoot() {
        final Node first = DraftNode.create("First");
        final Node second = DraftNode.create("Second");
        final Transformer transform = node -> {
            Assertions.assertSame(first, node);
            return second;
        };
        final Tree original = new Tree(first);
        Assertions.assertSame(second, transform.transform(original).getRoot().orElseThrow());
        Assertions.assertSame(first, original.getRoot().orElseThrow());
    }

    @Test
    void labelingAnEmptyTreeKeepsItEmpty() {
        final Tree result = new LabeledTreeBuilder(new Tree()).build(Set.of(), "key", "value");
        Assertions.assertTrue(result.getRoot().isEmpty());
    }

    @Test
    void builderReuseCannotChangePublishedSharedChildren() throws Exception {
        final Node child = DraftNode.create("Child");
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName(OptionalRootTest.ROOT_NAME);
        builder.setChildren(List.of(child, child));
        final Tree tree = new Tree(builder.build());
        builder.setName("Changed");
        builder.setChildren(List.of());
        final Node root = tree.getRoot().orElseThrow();
        Assertions.assertThrows(
            UnsupportedOperationException.class, () -> root.getChildrenList().clear()
        );
        try (var executor = Executors.newFixedThreadPool(4)) {
            final var tasks = new java.util.ArrayList<java.util.concurrent.Future<?>>(16);
            for (int index = 0; index < 16; index += 1) {
                tasks.add(
                    executor.submit(
                        () -> {
                            Assertions.assertSame(root, tree.getRoot().orElseThrow());
                            Assertions.assertEquals(OptionalRootTest.ROOT_NAME, root.getTypeName());
                            Assertions.assertSame(child, root.getChild(0));
                            Assertions.assertSame(child, root.getChild(1));
                            Assertions.assertEquals(root.hashCode(), NodeStructure.hash(root));
                        }
                    )
                );
            }
            for (final var task : tasks) {
                task.get();
            }
        }
    }
}
