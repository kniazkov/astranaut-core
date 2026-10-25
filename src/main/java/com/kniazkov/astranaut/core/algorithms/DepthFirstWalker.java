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

import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeOccurrence;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Performs a depth-first traversal of the syntax tree.
 * @since 0.0.1
 */
public class DepthFirstWalker {
    /**
     * The root node of the tree being traversed.
     */
    private final Node root;

    /**
     * Constructor.
     * @param root The root node of the tree being traversed
     */
    public DepthFirstWalker(final Node root) {
        this.root = root;
    }

    /**
     * Processes nodes starting from the root. Processes a node first.
     *  If the stopping criterion is not reached, iteratively processes all children of it,
     *  starting from the first one. Once a node is found that satisfies the criterion,
     *  stops traversal.<br/>
     *  And yes, you can use this algorithm not only to find nodes, but also just to traverse
     *  the tree in the specific order.
     * @param visitor Visitor that processes nodes
     * @return Found node (optional)
     */
    public Optional<Node> findFirst(final Predicate<? super Node> visitor) {
        return new OccurrenceWalker(new NodeOccurrence(this.root))
            .findFirst(occurrence -> visitor.test(occurrence.getNode()))
            .map(NodeOccurrence::getNode);
    }

    /**
     * Processes nodes starting from the root.
     *  If a node matches the criterion, adds it to the set and does not check
     *  the children of this node, otherwise it does.
     * @param visitor Visitor that processes nodes
     * @return List of found nodes (can be empty, but not {@code null})
     */
    public List<Node> findAll(final Predicate<? super Node> visitor) {
        return new OccurrenceWalker(new NodeOccurrence(this.root))
            .findAll(occurrence -> visitor.test(occurrence.getNode()))
            .stream().map(NodeOccurrence::getNode).toList();
    }

    /**
     * Collects all occurrences in preorder, including shared objects more than once.
     * @return Ordered nodes
     */
    public List<Node> collectAll() {
        return new OccurrenceWalker(new NodeOccurrence(this.root)).collectAll()
            .stream().map(NodeOccurrence::getNode).toList();
    }

}
