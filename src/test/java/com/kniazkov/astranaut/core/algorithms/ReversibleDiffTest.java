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

import com.kniazkov.astranaut.core.base.ActionList;
import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.ConstructionException;
import com.kniazkov.astranaut.core.base.Delete;
import com.kniazkov.astranaut.core.base.DiffNode;
import com.kniazkov.astranaut.core.base.DiffTree;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.EmptyTree;
import com.kniazkov.astranaut.core.base.Hole;
import com.kniazkov.astranaut.core.base.Insert;
import com.kniazkov.astranaut.core.base.Insertion;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeOccurrence;
import com.kniazkov.astranaut.core.base.PatternNode;
import com.kniazkov.astranaut.core.base.Replace;
import com.kniazkov.astranaut.core.base.Tree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Reversibility and immutable occurrence editing, independent of diff optimality.
 * @since 0.0.1
 */
@SuppressWarnings("PMD.TooManyMethods")
final class ReversibleDiffTest {
    /**
     * Common test root type.
     */
    private static final String ROOT = "Root";

    @Test
    void emptyTransitionsAndRootReplacementWorkForEveryStrategy() {
        final List<Tree> states = List.of(
            EmptyTree.INSTANCE, Tree.createDraft("A"), Tree.createDraft("B")
        );
        for (final DiffStrategy strategy : List.of(new PositionalDiff(), new WholeTreeDiff())) {
            for (final Tree before : states) {
                states.forEach(
                    after -> {
                        this.check(strategy, before, after);
                        final DiffTreeBuilder builder = new DiffTreeBuilder(before);
                        Assertions.assertTrue(builder.build(after, strategy));
                        Assertions.assertTrue(after.deepCompare(builder.getCurrentTree()));
                    }
                );
            }
        }
    }

    @Test
    void insertionAndDeletionOperationsRetainSequenceOrder() {
        final Tree before = Tree.createDraft("Root(A,Z)");
        final Tree after = Tree.createDraft("Root(A,B,C,D,Z)");
        final DiffTree diff = new PositionalDiff().compare(before, after);
        final Node root = diff.getRoot().orElseThrow();
        for (int index = 1; index <= 3; index += 1) {
            final Insert insert = Assertions.assertInstanceOf(Insert.class, root.getChild(index));
            Assertions.assertEquals(
                after.getRoot().orElseThrow().getChild(index), insert.getAfter()
            );
        }
        final DiffTree reverse = new PositionalDiff().compare(after, before);
        Assertions.assertInstanceOf(Delete.class, reverse.getRoot().orElseThrow().getChild(2));
        Assertions.assertTrue(after.deepCompare(diff.getAfter()));
        Assertions.assertTrue(before.deepCompare(reverse.getAfter()));
    }

    @Test
    void collidingHashesStillRestoreDifferentData() {
        final Tree before = Tree.createDraft("Root(Literal<'Aa'>)");
        final Tree after = Tree.createDraft("Root(Literal<'BB'>)");
        Assertions.assertEquals(
            before.getRoot().orElseThrow().hashCode(), after.getRoot().orElseThrow().hashCode()
        );
        this.check(new PositionalDiff(), before, after);
    }

    @Test
    void sharedOccurrencesAreEditedIndependentlyAndSnapshotsStayImmutable() {
        final Node shared = DraftNode.create("A");
        final Node source = DraftNode.create(ReversibleDiffTest.ROOT, "", shared, shared);
        final DiffTreeBuilder builder = new DiffTreeBuilder(source);
        Assertions.assertFalse(builder.replaceNode(shared, DraftNode.create("B")));
        Assertions.assertTrue(
            builder.replaceNode(new NodeOccurrence(source).getChild(0), DraftNode.create("B"))
        );
        final DiffTree snapshot = builder.getDiffTree();
        Assertions.assertFalse(builder.deleteNode(new NodeOccurrence(source).getChild(1)));
        final Node current = builder.getCurrentTree().getRoot().orElseThrow();
        Assertions.assertTrue(builder.deleteNode(new NodeOccurrence(current).getChild(1)));
        Assertions.assertTrue(Tree.createDraft("Root(B,A)").deepCompare(snapshot.getAfter()));
        Assertions.assertTrue(
            Tree.createDraft("Root(B)").deepCompare(builder.getDiffTree().getAfter())
        );
        Assertions.assertTrue(new Tree(source).deepCompare(builder.getDiffTree().getBefore()));
        Assertions.assertSame(shared, source.getChild(0));
        Assertions.assertSame(shared, source.getChild(1));
    }

    @Test
    void legacyInsertionsAtOneBoundaryKeepDeclarationOrder() {
        final Node source = DraftNode.create("Root(Z)");
        final DiffTreeBuilder builder = new DiffTreeBuilder(source);
        Assertions.assertTrue(
            builder.insertNode(new Insertion(DraftNode.create("A"), source, null))
        );
        final DiffTree snapshot = builder.getDiffTree();
        Assertions.assertTrue(
            builder.insertNode(new Insertion(DraftNode.create("B"), source, null))
        );
        Assertions.assertTrue(
            builder.insertNode(new Insertion(DraftNode.create("C"), source, null))
        );
        Assertions.assertTrue(Tree.createDraft("Root(A,Z)").deepCompare(snapshot.getAfter()));
        Assertions.assertTrue(
            Tree.createDraft("Root(A,B,C,Z)").deepCompare(builder.getDiffTree().getAfter())
        );
    }

    @Test
    void explicitBoundaryEditsAndRootDeletionWork() {
        final Node source = DraftNode.create("Root(A)");
        final DiffTreeBuilder builder = new DiffTreeBuilder(source);
        Assertions.assertFalse(
            builder.insertNode(new NodeOccurrence(source), 2, DraftNode.create("B"))
        );
        Assertions.assertTrue(
            builder.insertNode(new NodeOccurrence(source), 1, DraftNode.create("B"))
        );
        final Node current = builder.getCurrentTree().getRoot().orElseThrow();
        Assertions.assertTrue(builder.deleteNode(new NodeOccurrence(current)));
        Assertions.assertTrue(builder.getDiffTree().getAfter().getRoot().isEmpty());
        Assertions.assertTrue(new Tree(source).deepCompare(builder.getDiffTree().getBefore()));
    }

    @Test
    void expectedRefusalIsAtomicAndActionListCannotHideIt() {
        final Node source = new GuardedNode(
            DraftNode.create("Root(A)"),
            nodes -> nodes.size() == 1 && nodes.getFirst().getTypeName().equals("A")
        );
        final DiffTreeBuilder builder = new DiffTreeBuilder(source);
        final DiffTree snapshot = builder.getDiffTree();
        Assertions.assertFalse(
            builder.replaceNode(new NodeOccurrence(source).getChild(0), DraftNode.create("B"))
        );
        Assertions.assertFalse(builder.deleteNode(new NodeOccurrence(source).getChild(0)));
        Assertions.assertSame(snapshot, builder.getDiffTree());
        final ActionList actions = new ActionList();
        actions.replaceNode(source.getChild(0), DraftNode.create("B"));
        Assertions.assertThrows(
            ConstructionException.class, () -> actions.convertTreeToDiffTree(new Tree(source))
        );
    }

    @Test
    void invalidCustomStrategyCannotReplacePublishedState() {
        final DiffTreeBuilder builder = new DiffTreeBuilder(Tree.createDraft("A"));
        final DiffTree snapshot = builder.getDiffTree();
        Assertions.assertFalse(
            builder.build(
            Tree.createDraft("B"), (before, after) -> new DiffTree(DraftNode.create("Wrong"))
        )
        );
        Assertions.assertSame(snapshot, builder.getDiffTree());
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> builder.build(
                Tree.createDraft("B"), (before, after) -> {
                    throw new IllegalArgumentException("Unexpected strategy bug");
                }
            )
        );
    }

    @Test
    void projectedBuilderRefusalIsExplicit() {
        final Node source = new GuardedNode(
            DraftNode.create("Root(A)"),
            nodes -> nodes.getFirst().getTypeName().equals("A")
        );
        final Node action = new Replace(source.getChild(0), DraftNode.create("B"));
        final DiffTree diff = new DiffTree(
            new DiffNode(PatternNode.snapshot(source, List.of(action), List.of(Optional.empty())))
        );
        Assertions.assertTrue(new Tree(source).deepCompare(diff.getBefore()));
        Assertions.assertThrows(ConstructionException.class, diff::getAfter);
    }

    @Test
    void operationsAndHolesInInputAreOpaquePayloads() {
        final Node first = new Insert(new Hole(DraftNode.create("A"), 3));
        final Node second = new Delete(new Replace(DraftNode.create("B"), DraftNode.create("C")));
        for (final DiffStrategy strategy : List.of(new PositionalDiff(), new WholeTreeDiff())) {
            this.check(strategy, new Tree(first), new Tree(second));
            this.check(strategy, new Tree(first), new Tree(first));
            this.check(
                strategy, new Tree(DraftNode.create(ReversibleDiffTest.ROOT, "", first)),
                new Tree(DraftNode.create(ReversibleDiffTest.ROOT, "", second))
            );
        }
    }

    @Test
    void builderTreatsPatternInputAsDataInsteadOfExecutingItsActions() {
        final PatternNode pattern = new PatternNode(
            DraftNode.create(ReversibleDiffTest.ROOT, "", new Insert(DraftNode.create("A")))
        );
        final DiffTreeBuilder builder = new DiffTreeBuilder(pattern);
        Assertions.assertTrue(new Tree(pattern).deepCompare(builder.getCurrentTree()));
        Assertions.assertTrue(
            builder.replaceNode(
                new NodeOccurrence(pattern).getChild(0), new Insert(DraftNode.create("B"))
            )
        );
        Assertions.assertTrue(new Tree(pattern).deepCompare(builder.getDiffTree().getBefore()));
        final Node after = builder.getDiffTree().getAfter().getRoot().orElseThrow();
        Assertions.assertInstanceOf(Insert.class, after.getChild(0));
        Assertions.assertEquals("B", after.getChild(0).getChild(0).getTypeName());
    }

    @Test
    void sharedAncestorHandlesStayAmbiguousAfterBothOccurrencesAreRebuilt() {
        final Node shared = DraftNode.create("Inner(A)");
        final Node root = DraftNode.create(ReversibleDiffTest.ROOT, "", shared, shared);
        final DiffTreeBuilder builder = new DiffTreeBuilder(root);
        Assertions.assertTrue(
            builder.replaceNode(
                new NodeOccurrence(root).getChild(0).getChild(0), DraftNode.create("B")
            )
        );
        final Node current = builder.getCurrentTree().getRoot().orElseThrow();
        Assertions.assertTrue(
            builder.replaceNode(
                new NodeOccurrence(current).getChild(1).getChild(0), DraftNode.create("C")
            )
        );
        final DiffTree snapshot = builder.getDiffTree();
        Assertions.assertFalse(builder.replaceNode(shared, DraftNode.create("Wrong")));
        Assertions.assertSame(snapshot, builder.getDiffTree());
        Assertions.assertTrue(
            Tree.createDraft("Root(Inner(B),Inner(C))").deepCompare(snapshot.getAfter())
        );
    }

    @Test
    void ordinaryOccurrenceReplacementCanEditTheDifferenceItself() {
        final DiffTree diff = new PositionalDiff().compare(
            Tree.createDraft(ReversibleDiffTest.ROOT), Tree.createDraft("Root(A,B)")
        );
        final Node root = diff.getRoot().orElseThrow();
        final NodeOccurrence payload = new NodeOccurrence(root).getChild(0).getChild(0);
        final Node edited = new OccurrenceReplacer().replace(
            payload, DraftNode.create("C")
        ).orElseThrow();
        final DiffTree next = new DiffTree(edited);
        Assertions.assertTrue(
            Tree.createDraft(ReversibleDiffTest.ROOT).deepCompare(next.getBefore())
        );
        Assertions.assertTrue(Tree.createDraft("Root(C,B)").deepCompare(next.getAfter()));
        Assertions.assertTrue(Tree.createDraft("Root(A,B)").deepCompare(diff.getAfter()));
    }

    @Test
    void tagsSurviveNamedOptionalAndSchemaFreeProjection() {
        final Node child = DraftNode.create("Item");
        final Builder first = OptionalPair.TYPE.createBuilder();
        Assertions.assertTrue(first.setChild("left", child));
        final Builder second = OptionalPair.TYPE.createBuilder();
        Assertions.assertTrue(second.setChild("right", child));
        final Node left = first.build();
        final Node right = second.build();
        this.check(new PositionalDiff(), new Tree(left), new Tree(right));
        final Node adapter = new GuardedNode(right, nodes -> true);
        Assertions.assertTrue(adapter.getType().getChildDescriptors().isEmpty());
        Assertions.assertTrue(second.setChild("right", DraftNode.create("Item", "changed")));
        final Node changed = new GuardedNode(second.build(), nodes -> true);
        final DiffTree diff = new PositionalDiff().compare(new Tree(adapter), new Tree(changed));
        Assertions.assertInstanceOf(DiffNode.class, diff.getRoot().orElseThrow());
        Assertions.assertTrue(new Tree(adapter).deepCompare(diff.getBefore()));
        Assertions.assertTrue(new Tree(changed).deepCompare(diff.getAfter()));
        Assertions.assertTrue(diff.getAfter().getRoot().orElseThrow().getChild("left").isEmpty());
    }

    @Test
    @Timeout(30)
    void deepAndWideDifferencesAvoidRecursiveStacks() {
        Node before = DraftNode.create("A");
        Node after = DraftNode.create("B");
        for (int depth = 0; depth < 20_000; depth += 1) {
            before = DraftNode.create("Branch", "", before);
            after = DraftNode.create("Branch", "", after);
        }
        this.check(new PositionalDiff(), new Tree(before), new Tree(after));
        final Node shared = DraftNode.create("Leaf");
        final Node[] children = Collections.nCopies(20_000, shared).toArray(Node[]::new);
        final Node wide = DraftNode.create(ReversibleDiffTest.ROOT, "", children);
        children[10_000] = DraftNode.create("Other");
        this.check(
            new PositionalDiff(), new Tree(
            wide
        ), new Tree(
            DraftNode.create(ReversibleDiffTest.ROOT, "", children)
        )
        );
    }

    @Test
    void generatedGraphsRestoreBothStatesAndParallelResultsAreDeterministic() {
        final Random random = new Random(723_981L);
        final List<Node> nodes = new ArrayList<>(100);
        nodes.add(DraftNode.create("Leaf"));
        for (int index = 0; index < 99; index += 1) {
            final Node[] children = new Node[random.nextInt(4)];
            for (int child = 0; child < children.length; child += 1) {
                children[child] = nodes.get(random.nextInt(nodes.size()));
            }
            nodes.add(DraftNode.create("Branch", Integer.toString(random.nextInt(4)), children));
        }
        for (final int size : List.of(1, 2, 4)) {
            try (ExecutorService pool = Executors.newFixedThreadPool(size)) {
                final DiffStrategy parallel = new PositionalDiff(
                    new ParallelOccurrenceWalker(pool, size, 1)
                );
                for (int attempt = 0; attempt < 100; attempt += 1) {
                    final Node first = nodes.get(random.nextInt(nodes.size()));
                    final Node second = nodes.get(random.nextInt(nodes.size()));
                    final Tree before = new Tree(
                        DraftNode.create(ReversibleDiffTest.ROOT, "", first, first, second)
                    );
                    final Tree after = new Tree(
                        DraftNode.create(ReversibleDiffTest.ROOT, "", second, first, first)
                    );
                    final DiffTree expected = new PositionalDiff().compare(before, after);
                    final DiffTree actual = parallel.compare(before, after);
                    Assertions.assertTrue(expected.deepCompare(actual));
                    Assertions.assertTrue(before.deepCompare(actual.getBefore()));
                    Assertions.assertTrue(after.deepCompare(actual.getAfter()));
                }
                Assertions.assertFalse(pool.isShutdown());
            }
        }
    }

    /**
     * Checks the common reversible contract.
     * @param strategy Strategy
     * @param before Earlier state
     * @param after Later state
     */
    private void check(final DiffStrategy strategy, final Tree before, final Tree after) {
        final DiffTree difference = strategy.compare(before, after);
        Assertions.assertTrue(before.deepCompare(difference.getBefore()));
        Assertions.assertTrue(after.deepCompare(difference.getAfter()));
    }
}
