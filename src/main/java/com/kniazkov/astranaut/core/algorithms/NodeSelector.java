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
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * An algorithm that selects nodes from a tree based on some criteria.
 * @since 0.0.1
 */
public final class NodeSelector {
    /**
     * The root node of the tree from which the nodes will be selected.
     */
    private final Node root;

    /**
     * Constructor.
     * @param root The root node of the tree from which the nodes will be selected
     */
    public NodeSelector(final Node root) {
        this.root = root;
    }

    /**
     * Selects nodes without pruning; this legacy view deduplicates by object identity.
     * @param criteria Node selection criteria
     * @return Set containing selected nodes
     */
    public Set<Node> select(final Criteria criteria) {
        final Set<Node> result = Collections.newSetFromMap(new IdentityHashMap<>());
        for (final NodeOccurrence occurrence : this.selectOccurrences(criteria)) {
            result.add(occurrence.getNode());
        }
        return result;
    }

    /**
     * Selects occurrences in preorder without pruning matching branches.
     * @param criteria Criteria with stable, nearest-first ancestors
     * @return Immutable ordered addresses, including all shared occurrences
     */
    public List<NodeOccurrence> selectOccurrences(final Criteria criteria) {
        final List<NodeOccurrence> result = new ArrayList<>(0);
        new OccurrenceWalker(new NodeOccurrence(this.root)).findFirst(
            occurrence -> {
                if (criteria.isApplicable(occurrence.getNode(), occurrence.getAncestors())) {
                    result.add(occurrence);
                }
                return false;
            }
        );
        return List.copyOf(result);
    }

    /**
     * Node selection criteria.
     *
     * @since 0.0.1
     */
    public interface Criteria {
        /**
         * Checks if the node satisfies the criterion.
         * @param node Node
         * @param parents Parents of the node, starting from the immediate one (in case
         *  the criterion analysis requires information about the parents)
         * @return Checking result
         */
        boolean isApplicable(Node node, Iterable<Node> parents);
    }

}
