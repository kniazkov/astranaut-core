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

import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.DefaultFragment;
import com.kniazkov.astranaut.core.base.DefaultPosition;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.ExtNode;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeOccurrence;
import com.kniazkov.astranaut.core.base.NodeWithStructuralEquality;
import com.kniazkov.astranaut.core.base.Source;
import com.kniazkov.astranaut.core.base.StructuralNodeFixture;
import com.kniazkov.astranaut.core.base.Type;
import com.kniazkov.astranaut.core.example.green.Addition;
import com.kniazkov.astranaut.core.example.green.Variable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Deep, broad and shared trees exercise occurrence-aware algorithms.
 * @since 0.0.1
 */
class OccurrenceAlgorithmsTest {
    @Test
    void preservesPreorderPruningAndSharedOccurrences() {
        final Node branch = DraftNode.create("Branch(Leaf)");
        final Node root = DraftNode.create("Root", "", branch, branch);
        final OccurrenceWalker walker = new OccurrenceWalker(new NodeOccurrence(root));
        Assertions.assertEquals(
            List.of("Root", "Branch", "Leaf", "Branch", "Leaf"),
            walker.collectAll().stream().map(item -> item.getNode().getTypeName()).toList()
        );
        Assertions.assertEquals(2, walker.findAll(item -> item.getDepth() == 1).size());
        Assertions.assertEquals(1, walker.findAll(item -> true).size());
        Assertions.assertTrue(walker.findAll(item -> false).isEmpty());
        final List<NodeOccurrence> visited = new ArrayList<>(0);
        final NodeOccurrence first = walker.findFirst(
            item -> {
                visited.add(item);
                return item.getDepth() == 2;
            }
        ).orElseThrow();
        Assertions.assertEquals(3, visited.size());
        Assertions.assertEquals(new NodeOccurrence(root).getChild(0).getChild(0), first);
        Assertions.assertTrue(walker.findFirst(item -> false).isEmpty());
        Assertions.assertEquals(
            2, new DepthFirstWalker(root).findAll(node -> node.equals(branch)).size()
        );
        Assertions.assertEquals(5, new DepthFirstWalker(root).collectAll().size());
    }

    @Test
    void selectionDoesNotPruneAndRetainedAncestorsAreStable() {
        final Node branch = DraftNode.create("Branch(Leaf)");
        final Node root = DraftNode.create("Root", "", branch, branch);
        final List<Iterable<Node>> parents = new ArrayList<>(0);
        final NodeSelector selector = new NodeSelector(root);
        final List<NodeOccurrence> selected = selector.selectOccurrences(
            (node, ancestors) -> {
                parents.add(ancestors);
                return true;
            }
        );
        Assertions.assertEquals(5, selected.size());
        Assertions.assertEquals(3, selector.select((node, ancestors) -> true).size());
        for (final int position : List.of(2, 4)) {
            final List<Node> retained = new ArrayList<>(0);
            parents.get(position).forEach(retained::add);
            Assertions.assertEquals(List.of(branch, root), retained);
        }
    }

    @Test
    void replacesOnlyOneTaggedOccurrenceAndRejectsInvalidReplacement() {
        final Node shared = new Variable.Constructor().build();
        final Builder builder = Addition.TYPE.createBuilder();
        Assertions.assertTrue(builder.setChildren(List.of(shared, shared)));
        final Node root = builder.build();
        final Node replacement = new Variable.Constructor().build();
        final NodeOccurrence right = new NodeOccurrence(root).getChild(1);
        final OccurrenceReplacer algorithm = new OccurrenceReplacer();
        final Node changed = algorithm.replace(right, replacement).orElseThrow();
        Assertions.assertSame(shared, changed.getChild(0));
        Assertions.assertSame(replacement, changed.getChild(1));
        Assertions.assertEquals(root.getChildTag(1), changed.getChildTag(1));
        Assertions.assertSame(shared, root.getChild(1));
        Assertions.assertTrue(algorithm.replace(right, DraftNode.create("Wrong")).isEmpty());
        Assertions.assertSame(
            replacement, algorithm.replace(new NodeOccurrence(root), replacement).orElseThrow()
        );
    }

    @Test
    void handlesThirtyThousandLevelsWithoutRecursiveCalls() {
        Node root = DraftNode.create("Leaf");
        for (int index = 0; index < 30_000; index += 1) {
            root = DraftNode.create("Link", "data", root);
        }
        final NodeOccurrence address = new NodeOccurrence(root);
        final List<NodeOccurrence> all = new OccurrenceWalker(address).collectAll();
        Assertions.assertEquals(30_001, all.size());
        final NodeOccurrence leaf = all.getLast();
        Assertions.assertEquals(30_000, leaf.getDepth());
        NodeOccurrence other = new NodeOccurrence(root);
        for (int index = 0; index < 30_000; index += 1) {
            other = other.getChild(0);
        }
        Assertions.assertEquals(leaf, other);
        Assertions.assertEquals(leaf.hashCode(), other.hashCode());
        Assertions.assertEquals(30_001, new DepthCalculator().calculate(root));
        Assertions.assertEquals(30_001, new DepthFirstWalker(root).collectAll().size());
        Assertions.assertEquals(
            30_001, new NodeSelector(root).selectOccurrences((node, ancestors) -> true).size()
        );
        final Node replacement = DraftNode.create("NewLeaf");
        final Node rebuilt = new OccurrenceReplacer().replace(leaf, replacement).orElseThrow();
        Assertions.assertEquals("data", rebuilt.getData());
        Assertions.assertSame(replacement, new DepthFirstWalker(rebuilt).collectAll().getLast());
        final ExtNode extended = new ExtNodeCreator().create(root);
        Assertions.assertEquals(30_001, new DepthCalculator().calculate(extended));
    }

    @Test
    void handlesBroadSharingAndKeepsDistinctWrapperParents() {
        final Node shared = DraftNode.create("Branch(Leaf)");
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName("Broad");
        builder.setChildren(Collections.nCopies(30_000, shared));
        final Node root = builder.build();
        Assertions.assertEquals(60_001, new DepthFirstWalker(root).collectAll().size());
        final DepthCalculator calculator = new DepthCalculator();
        Assertions.assertEquals(3, calculator.calculate(root));
        Assertions.assertEquals(1, calculator.calculate(shared.getChild(0)));
        final ExtNode extended = new ExtNodeCreator().create(root);
        final ExtNode first = extended.getExtChild(0);
        final ExtNode second = extended.getExtChild(1);
        Assertions.assertNotSame(first, second);
        Assertions.assertSame(first.getPrototype(), second.getPrototype());
        Assertions.assertSame(first, second.getLeft());
        Assertions.assertSame(second, first.getRight());
        Assertions.assertSame(first, first.getExtChild(0).getParent());
        Assertions.assertSame(second, second.getExtChild(0).getParent());
        Assertions.assertEquals(1, second.getIndex());
        Assertions.assertEquals(root.hashCode(), extended.getAbsoluteHash());
    }

    @Test
    void preservesFragmentsAndDataAndRejectsTagLoss() {
        final Source source = (begin, end) -> "source";
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName("Parent");
        builder.setData("payload");
        builder.setFragment(
            new DefaultFragment(
                new DefaultPosition(source, 1, 1), new DefaultPosition(source, 1, 3)
            )
        );
        final Node shared = DraftNode.create("Shared");
        builder.setChildren(List.of(shared, shared));
        final Node parent = builder.build();
        final Node replacement = DraftNode.create("Replacement");
        final Node changed = new OccurrenceReplacer().replace(
            new NodeOccurrence(parent).getChild(0), replacement
        ).orElseThrow();
        Assertions.assertSame(parent.getFragment(), changed.getFragment());
        Assertions.assertEquals("payload", changed.getData());
        Assertions.assertSame(shared, changed.getChild(1));
        Assertions.assertSame(
            parent.getFragment(), new ExtNodeCreator().create(parent).getFragment()
        );
        final Node legacy = new NodeReplacer().replace(parent, shared, replacement).root();
        Assertions.assertSame(replacement, legacy.getChild(0));
        Assertions.assertSame(replacement, legacy.getChild(1));
        Assertions.assertSame(parent.getFragment(), legacy.getFragment());
        Assertions.assertEquals("payload", legacy.getData());
        final Node tagged = new StructuralNodeFixture(
            parent, List.of(shared), List.of(Optional.of("tag"))
        );
        Assertions.assertTrue(
            new OccurrenceReplacer().replace(
                new NodeOccurrence(tagged).getChild(0), replacement
            ).isEmpty()
        );
    }

    @Test
    void rejectsCyclesWithoutPoisoningLaterDepthCalculations() {
        final DepthCalculator calculator = new DepthCalculator();
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> calculator.calculate(new CyclicNode())
        );
        Assertions.assertEquals(1, calculator.calculate(DraftNode.create("Leaf")));
    }

    /**
     * Deliberately invalid input for cycle detection.
     * @since 0.0.1
     */
    private static final class CyclicNode extends NodeWithStructuralEquality {
        @Override
        public Type getType() {
            return Variable.TYPE;
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
            return this;
        }
    }
}
