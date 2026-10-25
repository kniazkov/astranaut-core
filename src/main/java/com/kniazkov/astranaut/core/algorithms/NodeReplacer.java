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
import com.kniazkov.astranaut.core.base.DummyNode;
import com.kniazkov.astranaut.core.base.Node;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Legacy replacement of all reference-identical subtrees.
 * Use {@link OccurrenceReplacer} to change only one address.
 * @since 0.0.1
 */
public class NodeReplacer {
    /**
     * Replaces a subtree of the initial tree with the newly created subtree.
     * Replaces every occurrence of the source object, retaining this legacy contract.
     * @param tree The initial tree
     * @param source The subtree to be replaced
     * @param target The new subtree
     * @return Modified tree or an empty tree if it was not modified,
     *  and the index:
     *  - {@code -1} if the full tree was replaced or was not replaced at all;
     *  - index of the last modified root child if a subtree was replaced
     * @throws ConstructionException If an ancestor refuses reconstruction
     * @checkstyle NonStaticMethodCheck (40 lines)
     */
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public Result replace(final Node tree, final Node source, final Node target) {
        final Map<Node, Node> changed = new IdentityHashMap<>();
        final List<Node> nodes = new ArrayList<>(0);
        new DepthFirstWalker(tree).findAll(
            node -> {
                nodes.add(node);
                return node == source;
            }
        );
        for (final Node node : nodes.reversed()) {
            if (node == source) {
                changed.put(node, target);
            } else if (!changed.containsKey(node)) {
                NodeReplacer.rebuild(node, changed);
            }
        }
        Result result = new Result(DummyNode.INSTANCE, -1);
        if (changed.containsKey(tree)) {
            int position = -1;
            for (int index = 0; tree != source && index < tree.getChildCount(); index += 1) {
                if (changed.containsKey(tree.getChild(index))) {
                    position = index;
                }
            }
            result = new Result(changed.get(tree), position);
        }
        return result;
    }

    /**
     * Rebuilds a changed ancestor once by identity, preserving metadata and tags.
     * @param node Ancestor
     * @param changed Rebuilt nodes keyed by reference identity, not structural equality
     * @throws ConstructionException If the builder refuses the reconstructed ancestor
     */
    private static void rebuild(final Node node, final Map<Node, Node> changed) {
        final List<Node> children = new ArrayList<>(node.getChildCount());
        boolean modified = false;
        for (int index = 0; index < node.getChildCount(); index += 1) {
            final Node child = node.getChild(index);
            children.add(changed.getOrDefault(child, child));
            modified = modified || changed.containsKey(child);
        }
        if (modified) {
            changed.put(
                node, OccurrenceReplacer.rebuild(node, children).orElseThrow(
                    () -> new ConstructionException("Builder refused subtree replacement")
                )
            );
        }
    }

    /**
     * Result of the legacy all-occurrences replacement operation.
     * @param root Rebuilt root, or DummyNode when no occurrence was replaced
     * @param lastChangedChild Last changed root child, or -1 for root/no replacement
     * @since 0.0.1
     */
    public record Result(Node root, int lastChangedChild) {
    }
}
