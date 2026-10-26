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
package com.kniazkov.astranaut.core.algorithms.patching;

import com.kniazkov.astranaut.core.algorithms.DepthFirstWalker;
import com.kniazkov.astranaut.core.base.Action;
import com.kniazkov.astranaut.core.base.ActionList;
import com.kniazkov.astranaut.core.base.Delete;
import com.kniazkov.astranaut.core.base.Hole;
import com.kniazkov.astranaut.core.base.Insert;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeOccurrence;
import com.kniazkov.astranaut.core.base.Pattern;
import com.kniazkov.astranaut.core.base.Replace;
import com.kniazkov.astranaut.core.base.Tree;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The matcher matches syntax tree and patterns.
 * @since 0.0.1
 */
class Matcher {
    /**
     * Root node of the tree in which patterns are searched.
     */
    private final Optional<Node> root;

    /**
     * Set of nodes mapped to the root of the pattern.
     */
    private final Set<Node> found;

    /**
     * Constructor.
     * @param tree The syntax tree in which patterns will be searched
     */
    Matcher(final Tree tree) {
        this.root = tree.getRoot();
        this.found = Collections.newSetFromMap(new IdentityHashMap<>());
    }

    /**
     * Matches the tree and the pattern.
     * @param pattern Root node of the pattern
     * @return Actions extracted from the pattern, applicable as a result of matching
     *  to the nodes of the tree
     */
    ActionList match(final Pattern pattern) {
        final Node head = pattern.getRoot().orElseThrow();
        final List<Node> preset = this.root.map(
            node -> new DepthFirstWalker(node).findAll(
                candidate -> head instanceof Hole hole && hole.accepts(candidate)
                    || candidate.getType().equals(head.getType())
                    && candidate.getData().equals(head.getData())
            )
        ).orElseGet(List::of);
        final ActionList list = new ActionList();
        for (final Node node : preset) {
            final Matched applicants = new Matched();
            final boolean matches = Matcher.checkNode(new NodeOccurrence(node), head, applicants);
            if (matches) {
                this.found.add(node);
                list.merge(applicants);
            }
        }
        return list;
    }

    /**
     * Matches only the current occurrence; traversal and atomic acceptance belong to RuleEngine.
     * @param pattern Pattern
     * @return Local actions or non-applicability
     */
    Optional<Matched> matchRoot(final Pattern pattern) {
        final Matched actions = new Matched();
        return this.root.filter(
            node -> Matcher.checkNode(
                new NodeOccurrence(node), pattern.getRoot().orElseThrow(), actions
            )
        ).map(node -> actions);
    }

    /**
     * Returns the set of nodes mapped to the root of the pattern.
     * @return The set of nodes found.
     *  The size of this set essentially means how many times the pattern has been applied
     */
    Set<Node> getFoundNodes() {
        return Collections.unmodifiableSet(this.found);
    }

    /**
     * Checks if the node of the original tree matches the pattern node.
     * @param address Occurrence in the original tree
     * @param pattern Node of the difference tree (i.e. pattern)
     * @param matched Intermediate data obtained by matching subtrees
     * @return Matching result ({@code true} if matches)
     */
    private static boolean checkNode(
        final NodeOccurrence address, final Node pattern, final Matched matched) {
        final Node node = address.getNode();
        final Node sample;
        final Action action = Action.toAction(pattern);
        if (action instanceof Replace || action instanceof Delete) {
            sample = action.getBefore();
        } else {
            sample = pattern;
        }
        boolean result = Matcher.accepts(sample, node);
        do {
            if (!result) {
                break;
            }
            if (sample instanceof Hole hole) {
                result = matched.checkHole(hole.getNumber(), node);
            } else {
                result = node.getData().equals(sample.getData())
                    && Matcher.matchNodeAndSample(address, sample, matched);
            }
            if (!result) {
                break;
            }
            if (action instanceof Replace) {
                matched.replaceAt(address, action.getAfter());
            } else if (action instanceof Delete) {
                matched.deleteAt(address);
            }
        } while (false);
        return result;
    }

    /**
     * Checks ordinary types or a hole constraint.
     * @param sample Pattern
     * @param node Candidate
     * @return Whether accepted
     */
    private static boolean accepts(final Node sample, final Node node) {
        return sample instanceof Hole hole && hole.accepts(node)
            || node.getType().equals(sample.getType());
    }

    /**
     * Matches a node and a sample provided the types are equal, the data is equal,
     *  and the sample is not an action.
     * @param address Occurrence in the original tree
     * @param sample Sample to match
     * @param matched Intermediate data obtained by matching subtrees
     * @return Matching result ({@code true} if matches)
     */
    private static boolean matchNodeAndSample(final NodeOccurrence address, final Node sample,
        final Matched matched) {
        final Node node = address.getNode();
        final boolean result;
        if (node.getChildCount() == 0) {
            result = Matcher.mathEmptyNodeAndSample(address, sample, matched);
        } else {
            result = Matcher.checkChildren(address, sample, matched);
        }
        return result;
    }

    /**
     * Matches a node without children and a sample.
     * @param address Occurrence in the original tree
     * @param sample Sample to match
     * @param matched Intermediate data obtained by matching subtrees
     * @return Matching result ({@code true} if matches)
     */
    private static boolean mathEmptyNodeAndSample(final NodeOccurrence address, final Node sample,
        final Matched matched) {
        final Node node = address.getNode();
        boolean result = true;
        final int count = sample.getChildCount();
        Node previous = null;
        for (int index = 0; index < count; index = index + 1) {
            final Action action = Action.toAction(sample.getChild(index));
            if (action instanceof Insert) {
                final Node insertion = action.getAfter();
                matched.insertNodeAfter(insertion, node, previous);
                matched.insertAt(address, 0, insertion);
                previous = insertion;
            } else {
                result = false;
                break;
            }
        }
        return result;
    }

    /**
     * Checks if the children of the node of the original tree
     *  matches the children of the pattern node.
     * @param address Occurrence in the original tree
     * @param sample Node of the difference tree (i.e. pattern)
     * @param matched Intermediate data obtained by matching subtrees
     * @return Matching result ({@code true} if matches)
     */
    private static boolean checkChildren(
        final NodeOccurrence address, final Node sample, final Matched matched) {
        final Node node = address.getNode();
        final int left = node.getChildCount();
        final int right = sample.getChildCount();
        boolean result = false;
        Matched applicants = matched.fork();
        for (int index = 0; !result && index < left; index = index + 1) {
            applicants = matched.fork();
            result = true;
            final Iterator<Node> iterator = sample.getIteratorOverChildren();
            int offset = 0;
            Node previous = null;
            while (result && offset < right && iterator.hasNext()) {
                final Node child = iterator.next();
                final Action action = Action.toAction(child);
                if (action instanceof Insert) {
                    final Node insertion = action.getAfter();
                    applicants.insertNodeAfter(insertion, node, previous);
                    applicants.insertAt(address, index + offset, insertion);
                    previous = insertion;
                } else if (index + offset >= left) {
                    result = false;
                } else {
                    final Node current = node.getChild(index + offset);
                    result = Matcher.checkNode(
                        address.getChild(index + offset),
                        child,
                        applicants
                    );
                    previous = current;
                    offset = offset + 1;
                }
            }
        }
        if (result) {
            matched.merge(applicants);
        }
        return result;
    }
}
