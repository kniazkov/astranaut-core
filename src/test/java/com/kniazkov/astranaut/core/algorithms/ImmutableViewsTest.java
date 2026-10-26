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
package com.kniazkov.astranaut.core.algorithms;

import com.kniazkov.astranaut.core.base.DefaultFragment;
import com.kniazkov.astranaut.core.base.DefaultPosition;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.ExtNode;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Source;
import com.kniazkov.astranaut.core.base.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Immutable shared views, completed before publication to concurrent readers.
 * @since 0.0.1
 */
class ImmutableViewsTest {
    /**
     * Presentation property used by the tests.
     */
    private static final String LABEL = "label";

    @Test
    void sharedSubtreeNeverAccumulatesDuplicateChildren() {
        final Node branch = DraftNode.create("Branch(A,B)");
        final Node root = DraftNode.create("Root", "", branch, branch);
        final SubtreeBuilder builder = new SubtreeBuilder(root, SubtreeBuilder.EXCLUDE);
        final Node view = builder.create(Set.of());
        Assertions.assertTrue(root.deepCompare(view));
        Assertions.assertEquals(2, view.getChild(0).getChildCount());
        Assertions.assertSame(view.getChild(0), view.getChild(1));
        Assertions.assertNotSame(view, builder.create(Set.of()));
        final Node labeled = new LabeledTreeBuilder(
            root
        ).build(
            Set.of(
            branch
        ), ImmutableViewsTest.LABEL, "yes"
        )
            .getRoot().orElseThrow();
        Assertions.assertSame(labeled.getChild(0), labeled.getChild(1));
        Assertions.assertEquals(
            "yes", labeled.getChild(
            0
        ).getProperties().get(
            ImmutableViewsTest.LABEL
        )
        );
    }

    @Test
    void selectionAndBuilderReuseCannotChangePublishedSnapshots() {
        final Node root = DraftNode.create("Root(A,B)");
        final Set<Node> selected = new HashSet<>(List.of(root.getChild(0)));
        final SubtreeBuilder builder = new SubtreeBuilder(root, SubtreeBuilder.INCLUDE);
        final Node view = builder.create(selected);
        final Node labeled = new LabeledTreeBuilder(
            root
        ).build(
            selected, ImmutableViewsTest.LABEL, "old"
        )
            .getRoot().orElseThrow();
        selected.clear();
        selected.add(root.getChild(1));
        Assertions.assertEquals("A", view.getChild(0).getTypeName());
        Assertions.assertEquals("B", builder.create(selected).getChild(0).getTypeName());
        Assertions.assertEquals(
            "old", labeled.getChild(
            0
        ).getProperties().get(
            ImmutableViewsTest.LABEL
        )
        );
        Assertions.assertTrue(labeled.getChild(1).getProperties().isEmpty());
        Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> view.getChildrenList().clear()
        );
        Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> labeled.getChild(0).getProperties().clear()
        );
    }

    @Test
    @Timeout(30)
    void deepAndWideViewsUseExplicitConstructionStacks() {
        Node chain = DraftNode.create("Leaf");
        for (int index = 0; index < 20_000; index += 1) {
            chain = DraftNode.create("Branch", "", chain);
        }
        final Node subtree = new SubtreeBuilder(chain, SubtreeBuilder.EXCLUDE).create(Set.of());
        final Node labeled = new LabeledTreeBuilder(
            chain
        ).build(
            Set.of(), ImmutableViewsTest.LABEL, "value"
        )
            .getRoot().orElseThrow();
        Assertions.assertTrue(chain.deepCompare(subtree));
        Assertions.assertTrue(chain.deepCompare(labeled));
        final Node[] leaves = new Node[20_000];
        java.util.Arrays.fill(leaves, DraftNode.create("Shared"));
        final Node wide = DraftNode.create("Wide", "", leaves);
        Assertions.assertTrue(
            wide.deepCompare(
                new SubtreeBuilder(wide, SubtreeBuilder.EXCLUDE).create(Set.of())
        )
        );
        Assertions.assertTrue(
            wide.deepCompare(
                new LabeledTreeBuilder(
                wide
            ).build(
                Set.of(), ImmutableViewsTest.LABEL, "value"
            ).getRoot().orElseThrow()
        )
        );
    }

    @Test
    @Timeout(30)
    void concurrentReadersSeeStableChildrenTagsMetadataAndOccurrenceLinks() throws Exception {
        final Source source = (begin, end) -> "text";
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName("Root");
        builder.setFragment(
            new DefaultFragment(
                new DefaultPosition(source, 1, 1), new DefaultPosition(source, 1, 3)
        )
        );
        final Node shared = DraftNode.create("Branch(Leaf)");
        builder.setChildren(
            List.of(
            shared, shared
        ), List.of(
            Optional.of(
            "left"
        ), Optional.of(
            "right"
        )
        )
        );
        final Node original = builder.build();
        final Node subtree = new SubtreeBuilder(original, SubtreeBuilder.EXCLUDE).create(Set.of());
        final Node labeled = new LabeledTreeBuilder(
            original
        ).build(
            Set.of(
            original
        ), ImmutableViewsTest.LABEL, "yes"
        )
            .getRoot().orElseThrow();
        final ExtNode extended = new ExtNodeCreator().create(original);
        final List<Node> views = List.of(subtree, labeled, extended);
        final List<Callable<Void>> tasks = new ArrayList<>(100);
        for (int index = 0; index < 100; index += 1) {
            tasks.add(
                () -> {
                    for (final Node view : views) {
                        final Node child = view.getChild(1);
                        Assertions.assertSame(child, view.getChild("right").orElseThrow());
                        Assertions.assertSame(child, view.getChild(1));
                        Assertions.assertSame(original.getFragment(), view.getFragment());
                        Assertions.assertSame(original.getType(), view.getType());
                        Assertions.assertEquals(original.getData(), view.getData());
                        Assertions.assertEquals(Optional.of("left"), view.getChildTag(0));
                    }
                    Assertions.assertEquals(
                        "yes", labeled.getProperties().get(
                        ImmutableViewsTest.LABEL
                    )
                    );
                    Assertions.assertSame(extended, extended.getExtChild(0).getParent());
                    Assertions.assertSame(
                        extended.getExtChild(1), extended.getExtChild(0).getRight()
                    );
                    Assertions.assertNotSame(extended.getChild(0), extended.getChild(1));
                    return null;
                }
            );
        }
        try (var pool = Executors.newFixedThreadPool(4)) {
            for (final var result : pool.invokeAll(tasks)) {
                result.get();
            }
        }
    }

    @Test
    void cyclesFailWithoutPublishingOrPoisoningLaterBuilds() {
        final Node cycle = new Cycle();
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new SubtreeBuilder(cycle, SubtreeBuilder.EXCLUDE).create(Set.of())
        );
        final LabeledTreeBuilder labeling = new LabeledTreeBuilder(cycle);
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> labeling.build(Set.of(), ImmutableViewsTest.LABEL, "value")
        );
        final Node valid = DraftNode.create("Root(Leaf)");
        Assertions.assertTrue(
            valid.deepCompare(
                new SubtreeBuilder(valid, SubtreeBuilder.EXCLUDE).create(Set.of())
        )
        );
    }

    /**
     * Invalid producer fixture for selected-edge cycle rejection.
     * @since 0.0.1
     */
    private static final class Cycle implements Node {
        @Override
        public Type getType() {
            return DraftNode.create("Cycle").getType();
        }

        @Override
        public String getData() {
            return "";
        }

        @Override
        public int getChildCount() {
            return 1;
        }

        @Override
        public Node getChild(final int index) {
            java.util.Objects.checkIndex(index, 1);
            return this;
        }
    }
}
