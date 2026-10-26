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

import com.kniazkov.astranaut.core.base.ConstructionException;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.EmptyTree;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeOccurrence;
import com.kniazkov.astranaut.core.base.NodeStructure;
import com.kniazkov.astranaut.core.base.Tree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * One deterministic bottom-up pass over occurrence-based sibling sequences.
 * Each accepted replacement is validated up to the root before any state is committed.
 * Rules run in declaration order and their own scan direction. Accepted outputs are
 * not matched again in this pass; stabilization is a separate policy.
 * @since 0.0.1
 */
public final class RuleEngine {
    /**
     * Declared priority order.
     */
    private final List<RewriteRule> rules;

    /**
     * Optional caller-owned bounded execution strategy.
     */
    private final Optional<ParallelOccurrenceWalker> parallel;

    /**
     * Creates a sequential engine.
     * @param rules Ordered rules
     */
    public RuleEngine(final List<RewriteRule> rules) {
        this(rules, Optional.empty());
    }

    /**
     * Enables speculation for explicitly concurrent rules only. Invoke outside the pool.
     * Builders of ancestors are always called sequentially in acceptance order.
     * @param rules Ordered rules
     * @param parallel Caller-owned strategy; the engine never closes its executor
     */
    public RuleEngine(final List<RewriteRule> rules, final ParallelOccurrenceWalker parallel) {
        this(rules, Optional.of(parallel));
    }

    /**
     * Captures engine configuration.
     * @param rules Rules
     * @param parallel Optional strategy
     */
    private RuleEngine(final List<RewriteRule> rules,
        final Optional<ParallelOccurrenceWalker> parallel) {
        this.rules = List.copyOf(rules);
        this.parallel = parallel;
    }

    /**
     * Executes a pass without recursion, preserving independent successful changes.
     * Empty input invokes no rules. No-op proposals are not progress and do not block
     * lower-priority rules. New roots may be empty, but never contain multiple nodes.
     * @param source Source tree
     * @return Result of one pass
     * @throws java.util.concurrent.CancellationException If interrupted
     */
    public Tree apply(final Tree source) {
        Objects.requireNonNull(source);
        Interruption.check();
        Tree result = source;
        if (source.getRoot().isPresent()) {
            final Node wrapper = DraftNode.create(
                "AstranautRuleRoot", "", source.getRoot().orElseThrow()
            );
            final List<Frame> stack = new ArrayList<>(1);
            final Frame root = new Frame(wrapper, 0);
            stack.add(root);
            while (!stack.isEmpty()) {
                Interruption.check();
                final Frame frame = stack.getLast();
                if (frame.next < frame.node.getChildCount()) {
                    final int index = frame.next;
                    frame.next += 1;
                    stack.add(new Frame(frame.node.getChild(index), index));
                } else {
                    this.rewrite(stack);
                    stack.removeLast();
                }
            }
            result = EmptyTree.INSTANCE;
            if (root.node.getChildCount() == 1) {
                result = new Tree(root.node.getChild(0));
            }
        }
        Interruption.check();
        return result;
    }

    /**
     * Applies prioritized rules after all descendant lists have been processed.
     * @param stack Current ancestor chain, including the virtual root
     */
    private void rewrite(final List<Frame> stack) {
        final Frame frame = stack.getLast();
        List<Node> siblings = frame.node.getChildrenList();
        final List<Boolean> settled = new ArrayList<>(
            Collections.nCopies(siblings.size(), false)
        );
        for (final RewriteRule rule : this.rules) {
            Interruption.check();
            final int direction;
            int index;
            if (rule.isRightToLeft()) {
                direction = -1;
                index = settled.size() - 1;
            } else {
                direction = 1;
                index = 0;
            }
            List<Optional<Replacement>> cache = List.of();
            while (index >= 0 && index < settled.size()) {
                Interruption.check();
                if (settled.get(index)) {
                    index += direction;
                    continue;
                }
                if (cache.isEmpty()) {
                    cache = this.proposals(frame.node, rule, settled);
                }
                final Optional<Replacement> proposal = RuleEngine.proposal(
                    rule, new Candidates(siblings, cache), index
                );
                final boolean changed = proposal.isPresent()
                    && RuleEngine.accept(
                        stack, settled, new Attempt(index, proposal.orElseThrow())
                    );
                if (changed) {
                    siblings = frame.node.getChildrenList();
                    cache = List.of();
                    if (direction > 0) {
                        index += proposal.orElseThrow().nodes().size();
                    } else {
                        index -= 1;
                    }
                } else {
                    index += direction;
                }
            }
        }
    }

    /**
     * Speculates on immutable siblings only. Any accepted edit invalidates this entire cache.
     * @param parent Current parent
     * @param rule Rule
     * @param settled Consumed occurrences
     * @return Indexed proposals, or empty to evaluate sequentially on demand
     */
    private List<Optional<Replacement>> proposals(final Node parent, final RewriteRule rule,
        final List<Boolean> settled) {
        List<Optional<Replacement>> result = List.of();
        if (rule.isConcurrent() && this.parallel.isPresent()) {
            final List<Node> nodes = parent.getChildrenList();
            result = this.parallel.orElseThrow().mapChildren(
                new NodeOccurrence(parent),
                child -> {
                    Optional<Replacement> proposal = Optional.empty();
                    if (!settled.get(child.getIndex())) {
                        proposal = RuleEngine.invoke(rule, nodes, child.getIndex());
                    }
                    return proposal;
                }
            );
        }
        return result;
    }

    /**
     * Reads a speculative proposal or invokes a sequential rule.
     * @param rule Rule
     * @param input Current siblings and precomputed proposals
     * @param index Candidate start
     * @return Proposal
     */
    private static Optional<Replacement> proposal(final RewriteRule rule, final Candidates input,
        final int index) {
        Optional<Replacement> result = Optional.empty();
        if (input.cache().isEmpty()) {
            result = RuleEngine.invoke(rule, input.nodes(), index);
        } else {
            result = input.cache().get(index);
        }
        return result;
    }

    /**
     * Converts only expected construction refusal to non-applicability.
     * @param rule Rule
     * @param nodes Current siblings
     * @param index Start
     * @return Proposal
     */
    private static Optional<Replacement> invoke(final RewriteRule rule, final List<Node> nodes,
        final int index) {
        Optional<Replacement> result;
        try {
            Interruption.check();
            result = Objects.requireNonNull(rule.apply(nodes, index));
            Interruption.check();
        } catch (final ConstructionException ignored) {
            result = Optional.empty();
        }
        return result;
    }

    /**
     * Commits one nonoverlapping proposal only after validation of the entire chain.
     * @param stack Ancestors
     * @param settled Already consumed occurrences
     * @param attempt Proposal and start
     * @return Whether committed
     * @throws IllegalArgumentException If the rule consumes beyond the input
     */
    private static boolean accept(final List<Frame> stack, final List<Boolean> settled,
        final Attempt attempt) {
        if (attempt.replacement().consumed() > settled.size() - attempt.index()) {
            throw new IllegalArgumentException("Rule consumed beyond the input sequence");
        }
        final int end = attempt.index() + attempt.replacement().consumed();
        boolean result = false;
        if (!settled.subList(attempt.index(), end).contains(true)) {
            final Optional<List<Node>> rebuilt = RuleEngine.rebuild(stack, attempt);
            if (rebuilt.isPresent()) {
                final List<Node> nodes = rebuilt.orElseThrow();
                for (int index = 0; index < stack.size(); index += 1) {
                    stack.get(index).node = nodes.get(index);
                }
                settled.subList(attempt.index(), end).clear();
                settled.addAll(
                    attempt.index(), Collections.nCopies(attempt.replacement().nodes().size(), true)
                );
                result = true;
            }
        }
        return result;
    }

    /**
     * Tentatively rebuilds ancestors in local storage, publishing nothing on refusal.
     * @param stack Ancestors
     * @param attempt Local proposal
     * @return Rebuilt chain in root-to-leaf order
     */
    private static Optional<List<Node>> rebuild(final List<Frame> stack, final Attempt attempt) {
        Optional<List<Node>> result = Optional.empty();
        try {
            final Node parent = stack.getLast().node;
            final OccurrenceReplacer.Content content = attempt.splice(parent);
            if (!RuleEngine.unchanged(parent, content)) {
                final List<Node> rebuilt = new ArrayList<>(stack.size());
                Optional<Node> current = RuleEngine.build(parent, content, stack.size() == 1);
                for (int depth = stack.size() - 1; current.isPresent() && depth >= 0; depth -= 1) {
                    Interruption.check();
                    rebuilt.add(current.orElseThrow());
                    if (depth > 0) {
                        final Node outer = stack.get(depth - 1).node;
                        final List<Node> children = new ArrayList<>(outer.getChildrenList());
                        children.set(stack.get(depth).index, current.orElseThrow());
                        current = OccurrenceReplacer.rebuild(outer, children);
                    }
                }
                if (current.isPresent()) {
                    result = Optional.of(rebuilt.reversed());
                }
            }
        } catch (final ConstructionException ignored) {
            result = Optional.empty();
        }
        return result;
    }

    /**
     * Enforces the single-root boundary before calling a parent builder.
     * @param parent Parent
     * @param content New content
     * @param virtual Whether this is the virtual root
     * @return Accepted parent
     */
    private static Optional<Node> build(final Node parent, final OccurrenceReplacer.Content content,
        final boolean virtual) {
        Optional<Node> result = Optional.empty();
        if (!virtual || content.children().size() <= 1
            && content.tags().stream().allMatch(Optional::isEmpty)) {
            result = OccurrenceReplacer.rebuild(parent, content);
        }
        return result;
    }

    /**
     * Detects no-op structure including edge tags, without comparing hashes alone.
     * @param parent Old parent
     * @param content Proposed content
     * @return Whether unchanged
     */
    private static boolean unchanged(final Node parent, final OccurrenceReplacer.Content content) {
        boolean result = parent.getChildCount() == content.children().size();
        for (int index = 0; result && index < parent.getChildCount(); index += 1) {
            result = parent.getChildTag(index).equals(content.tags().get(index))
                && NodeStructure.equal(parent.getChild(index), content.children().get(index));
        }
        return result;
    }

    /**
     * Current immutable candidate snapshot.
     * @param nodes Siblings
     * @param cache Optional speculative proposals
     * @since 0.0.1
     */
    private record Candidates(List<Node> nodes, List<Optional<Replacement>> cache) { }

    /**
     * One traversal frame. Mutated only by the coordinator after complete validation.
     * @since 0.0.1
     */
    private static final class Frame {
        /**
         * Current accepted node.
         */
        private Node node;

        /**
         * Position in the parent frame.
         */
        private final int index;

        /**
         * Next original child to descend into.
         */
        private int next;

        /**
         * Creates a frame.
         * @param node Node
         * @param index Position
         */
        private Frame(final Node node, final int index) {
            this.node = node;
            this.index = index;
        }
    }

    /**
     * An occurrence-scoped proposal.
     * @param index Start
     * @param replacement Replacement
     * @since 0.0.1
     */
    private record Attempt(int index, Replacement replacement) {
        /**
         * Splices nodes and tags without inspecting node identity for tag inference.
         * @param parent Source parent
         * @return Proposed complete content
         * @throws ConstructionException If tagged arity changes ambiguously
         */
        private OccurrenceReplacer.Content splice(final Node parent) {
            final int end = this.index + this.replacement.consumed();
            final List<Node> children = new ArrayList<>(parent.getChildrenList());
            final List<Optional<String>> tags = new ArrayList<>(
                IntStream.range(0, parent.getChildCount()).mapToObj(parent::getChildTag).toList()
            );
            List<Optional<String>> bindings = this.replacement.tags().orElseGet(
                () -> List.copyOf(tags.subList(this.index, end))
            );
            if (this.replacement.tags().isEmpty()
                && bindings.size() != this.replacement.nodes().size()) {
                if (!this.replacement.nodes().isEmpty()
                    && bindings.stream().anyMatch(Optional::isPresent)) {
                    throw new ConstructionException(
                        "Explicit tags required for changed tagged arity"
                    );
                }
                bindings = Collections.nCopies(this.replacement.nodes().size(), Optional.empty());
            }
            children.subList(this.index, end).clear();
            children.addAll(this.index, this.replacement.nodes());
            tags.subList(this.index, end).clear();
            tags.addAll(this.index, bindings);
            return new OccurrenceReplacer.Content(parent.getData(), children, tags);
        }
    }
}
