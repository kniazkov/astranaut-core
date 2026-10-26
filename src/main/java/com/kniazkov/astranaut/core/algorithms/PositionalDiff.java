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

import com.kniazkov.astranaut.core.base.Action;
import com.kniazkov.astranaut.core.base.Delete;
import com.kniazkov.astranaut.core.base.DiffNode;
import com.kniazkov.astranaut.core.base.DiffTree;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Insert;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeOccurrence;
import com.kniazkov.astranaut.core.base.NodeStructure;
import com.kniazkov.astranaut.core.base.PatternNode;
import com.kniazkov.astranaut.core.base.Replace;
import com.kniazkov.astranaut.core.base.Tree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Iterative ordered diff: retain equal prefixes/suffixes and pair the middle by position.
 * Tags changing layout or local type/data changes replace the whole affected subtree.
 * Shared objects at different positions are compared independently. No minimality claim.
 * @since 0.0.1
 */
public final class PositionalDiff implements DiffStrategy {
    /**
     * Explicit caller-owned strategy, used only at the top sibling frontier.
     */
    private final Optional<ParallelOccurrenceWalker> parallel;

    /**
     * Creates a sequential strategy.
     */
    public PositionalDiff() {
        this(Optional.empty());
    }

    /**
     * Uses bounded independent top-level branches; invoke outside the supplied pool.
     * Input accessors must be safe for concurrent reading. Builders run during projection.
     * @param parallel Caller-owned strategy
     */
    public PositionalDiff(final ParallelOccurrenceWalker parallel) {
        this(Optional.of(parallel));
    }

    /**
     * Stores the optional execution strategy.
     * @param parallel Execution strategy
     */
    private PositionalDiff(final Optional<ParallelOccurrenceWalker> parallel) {
        this.parallel = parallel;
    }

    @Override
    public DiffTree compare(final Tree before, final Tree after) {
        Interruption.check();
        final DiffTree result;
        if (before.getRoot().isEmpty() || after.getRoot().isEmpty()) {
            result = new WholeTreeDiff().compare(before, after);
        } else {
            final Node left = before.getRoot().orElseThrow();
            final Node right = after.getRoot().orElseThrow();
            final Context context = new Context(
                NodeStructure.hashes(left), NodeStructure.hashes(right)
            );
            final Pair pair = new Pair(Optional.of(left), Optional.of(right));
            final Optional<Node> terminal = context.terminal(pair);
            if (terminal.isPresent()) {
                result = new DiffTree(terminal.orElseThrow());
            } else if (this.parallel.isPresent()) {
                final Frame frame = new Frame(pair, context);
                final Node frontier = DraftNode.create(
                    "AstranautDiffFrontier", "", frame.pairs.stream()
                        .map(item -> item.before().orElseGet(item.after()::orElseThrow))
                        .toArray(Node[]::new)
                );
                final List<Node> children = this.parallel.orElseThrow().mapChildren(
                    new NodeOccurrence(frontier),
                    address -> PositionalDiff.build(frame.pairs.get(address.getIndex()), context)
                );
                result = new DiffTree(frame.finish(children));
            } else {
                result = new DiffTree(PositionalDiff.build(pair, context));
            }
        }
        return result;
    }

    /**
     * Evaluates one paired branch without recursion or nested parallel submissions.
     * @param pair Endpoints
     * @param context Immutable hash accelerators
     * @return Reversible branch
     */
    private static Node build(final Pair pair, final Context context) {
        final Optional<Node> terminal = context.terminal(pair);
        return terminal.orElseGet(() -> PositionalDiff.expand(pair, context));
    }

    /**
     * Constructs a nonterminal branch iteratively.
     * @param pair Endpoints
     * @param context Hash accelerators
     * @return Completed branch
     */
    private static Node expand(final Pair pair, final Context context) {
        final List<Frame> stack = new ArrayList<>(1);
        stack.add(new Frame(pair, context));
        Node result = pair.before().orElseThrow();
        while (!stack.isEmpty()) {
            Interruption.check();
            final Frame frame = stack.getLast();
            if (frame.children.size() < frame.pairs.size()) {
                PositionalDiff.descend(stack, context);
            } else {
                result = frame.finish(frame.children);
                stack.removeLast();
                if (!stack.isEmpty()) {
                    stack.getLast().children.add(result);
                }
            }
        }
        return result;
    }

    /**
     * Resolves or schedules the next child occurrence.
     * @param stack Frames
     * @param context Hash accelerators
     */
    private static void descend(final List<Frame> stack, final Context context) {
        final Frame frame = stack.getLast();
        final Pair child = frame.pairs.get(frame.children.size());
        final Optional<Node> simple = context.terminal(child);
        if (simple.isPresent()) {
            frame.children.add(simple.orElseThrow());
        } else {
            stack.add(new Frame(child, context));
        }
    }

    /**
     * Endpoints of one occurrence pair, with absence representing insertion/deletion.
     * @param before Earlier occurrence
     * @param after Later occurrence
     * @since 0.0.1
     */
    private record Pair(Optional<Node> before, Optional<Node> after) { }

    /**
     * Read-only identity-indexed structural hashes; equality still verifies every candidate.
     * @param before Earlier hashes
     * @param after Later hashes
     * @since 0.0.1
     */
    private record Context(Map<Node, Integer> before, Map<Node, Integer> after) {
        /**
         * Tests exact structure, never treating a hash collision as equality.
         * @param left Earlier node
         * @param right Later node
         * @return Equality
         */
        private boolean equal(final Node left, final Node right) {
            return this.before.get(left).equals(this.after.get(right))
                && NodeStructure.equal(left, right);
        }

        /**
         * Resolves simple cases or requests traversal of children.
         * @param pair Occurrence pair
         * @return Terminal branch, or empty if a scaffold is needed
         */
        private Optional<Node> terminal(final Pair pair) {
            final Optional<Node> result;
            if (pair.before().isEmpty()) {
                result = Optional.of(new Insert(pair.after().orElseThrow()));
            } else if (pair.after().isEmpty()) {
                result = Optional.of(new Delete(pair.before().orElseThrow()));
            } else {
                final Node left = pair.before().orElseThrow();
                final Node right = pair.after().orElseThrow();
                if (Context.opaque(left) || Context.opaque(right)) {
                    result = Optional.of(new Replace(left, right));
                } else if (this.equal(left, right)) {
                    result = Optional.of(left);
                } else if (NodeStructure.locallyEqual(left, right)
                    && Context.compatibleTags(left, right)) {
                    result = Optional.empty();
                } else {
                    result = Optional.of(new Replace(left, right));
                }
            }
            return result;
        }

        /**
         * Detects service nodes which must be quoted as literal input payloads.
         * @param node Input node
         * @return Whether automatic projection would interpret it
         */
        private static boolean opaque(final Node node) {
            return node instanceof Action || node instanceof PatternNode
                || node instanceof DiffNode;
        }

        /**
         * Requires identical named layouts, otherwise a whole-parent replacement is used.
         * @param left Earlier node
         * @param right Later node
         * @return Whether positional reconstruction preserves both layouts
         */
        private static boolean compatibleTags(final Node left, final Node right) {
            final List<Optional<String>> first = IntStream.range(0, left.getChildCount())
                .mapToObj(left::getChildTag).toList();
            final List<Optional<String>> second = IntStream.range(0, right.getChildCount())
                .mapToObj(right::getChildTag).toList();
            return first.equals(second)
                || first.stream().allMatch(Optional::isEmpty)
                && second.stream().allMatch(Optional::isEmpty);
        }
    }

    /**
     * One occurrence-scoped construction frame.
     * @since 0.0.1
     */
    private static final class Frame {
        /**
         * Source parent metadata.
         */
        private final Node source;

        /**
         * Ordered paired slots, including explicit absent sides.
         */
        private final List<Pair> pairs;

        /**
         * Constructed operation children.
         */
        private final List<Node> children;

        /**
         * Plans one sibling sequence.
         * @param pair Parents
         * @param context Hash accelerators
         */
        private Frame(final Pair pair, final Context context) {
            this.source = pair.before().orElseThrow();
            this.pairs = Frame.align(pair, context);
            this.children = new ArrayList<>(this.pairs.size());
        }

        /**
         * Publishes an immutable scaffold.
         * @param nodes Constructed children
         * @return Difference parent
         */
        private Node finish(final List<Node> nodes) {
            List<Optional<String>> tags = IntStream.range(0, this.source.getChildCount())
                .mapToObj(this.source::getChildTag).toList();
            if (tags.stream().allMatch(Optional::isEmpty)) {
                tags = Collections.nCopies(nodes.size(), Optional.empty());
            }
            return new DiffNode(PatternNode.snapshot(this.source, nodes, tags));
        }

        /**
         * Retains exact prefix/suffix anchors and pairs remaining slots in order.
         * @param pair Parents
         * @param context Hash accelerators
         * @return Immutable plan
         */
        private static List<Pair> align(final Pair pair, final Context context) {
            final Node left = pair.before().orElseThrow();
            final Node right = pair.after().orElseThrow();
            final int first = left.getChildCount();
            final int second = right.getChildCount();
            int prefix = 0;
            int suffix = 0;
            final boolean tagged = IntStream.range(0, first)
                .anyMatch(index -> left.getChildTag(index).isPresent());
            if (!tagged) {
                while (prefix < Math.min(first, second)
                    && context.equal(left.getChild(prefix), right.getChild(prefix))) {
                    prefix += 1;
                }
                suffix = Frame.suffix(pair, context, prefix);
            }
            final List<Pair> result = new ArrayList<>(Math.max(first, second));
            for (int index = 0; index < prefix; index += 1) {
                result.add(
                    new Pair(Optional.of(left.getChild(index)), Optional.of(right.getChild(index)))
                );
            }
            final int count = Math.max(first, second) - prefix - suffix;
            for (int offset = 0; offset < count; offset += 1) {
                final int index = prefix + offset;
                Optional<Node> before = Optional.empty();
                Optional<Node> after = Optional.empty();
                if (index < first - suffix) {
                    before = Optional.of(left.getChild(index));
                }
                if (index < second - suffix) {
                    after = Optional.of(right.getChild(index));
                }
                result.add(new Pair(before, after));
            }
            for (int offset = suffix; offset > 0; offset -= 1) {
                result.add(
                    new Pair(
                    Optional.of(left.getChild(first - offset)),
                    Optional.of(right.getChild(second - offset))
                )
                );
            }
            return List.copyOf(result);
        }

        /**
         * Finds exact trailing anchors without overlapping the prefix.
         * @param pair Parents
         * @param context Hash accelerators
         * @param prefix Already matched prefix
         * @return Suffix length
         */
        private static int suffix(final Pair pair, final Context context, final int prefix) {
            final Node left = pair.before().orElseThrow();
            final Node right = pair.after().orElseThrow();
            final int first = left.getChildCount();
            final int second = right.getChildCount();
            int count = 0;
            while (count < Math.min(first, second) - prefix
                && context.equal(
                    left.getChild(first - count - 1), right.getChild(second - count - 1)
                )) {
                count += 1;
            }
            return count;
        }

    }
}
