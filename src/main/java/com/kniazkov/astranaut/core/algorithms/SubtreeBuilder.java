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

import com.kniazkov.astranaut.core.base.DummyNode;
import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeWithStructuralEquality;
import com.kniazkov.astranaut.core.base.PrototypeBasedNode;
import com.kniazkov.astranaut.core.base.Tree;
import com.kniazkov.astranaut.core.base.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Algorithm that produces a subtree from the original tree.
 * @since 0.0.1
 */
public class SubtreeBuilder {
    /**
     * Algorithm that composes a subtree only from the nodes that are specified in the set.
     */
    public static final Algorithm INCLUDE = SubtreeBuilder::isPresentInSet;

    /**
     * Algorithm that composes a subtree from all nodes in the original tree,
     *  but excludes nodes specified in the set.
     */
    public static final Algorithm EXCLUDE =
        (node, set) -> !SubtreeBuilder.isPresentInSet(node, set);

    /**
     * The root node of the original tree.
     */
    private final Node root;

    /**
     * Algorithm that selects nodes based on some criteria.
     */
    private final Algorithm algorithm;

    /**
     * Constructor.
     * @param root The root node of the original tree
     * @param algorithm Algorithm that selects nodes based on some criteria
     */
    public SubtreeBuilder(final Node root, final Algorithm algorithm) {
        this.root = root;
        this.algorithm = algorithm;
    }

    /**
     * Constructor.
     * @param tree Original tree
     * @param algorithm Algorithm that selects nodes based on some criteria
     * @throws IllegalArgumentException If the tree has no root
     */
    public SubtreeBuilder(final Tree tree, final Algorithm algorithm) {
        this(tree.getRoot().orElseThrow(
            () -> new IllegalArgumentException("This builder requires a non-empty tree")
        ), algorithm);
    }

    /**
     * Creates a subtree from the original tree, with the result depends on the nodes that are
     *  specified in the set.
     * Selection callbacks must be pure and source nodes immutable during construction.
     * @param nodes The set of nodes
     * @return Root node of created subtree.
     */
    public Node create(final Set<Node> nodes) {
        final Node view = NodeViewBuilder.build(
            this.root, node -> this.select(node, nodes), SubNode::new
        );
        Node result = view;
        if (view.getChildCount() == 0) {
            result = DummyNode.INSTANCE;
        }
        return result;
    }

    /**
     * Selects each source edge once; shared subtrees reuse their completed immutable view.
     * @param node Source parent
     * @param selected Selected source nodes
     * @return Ordered original indexes
     */
    private List<Integer> select(final Node node, final Set<Node> selected) {
        final List<Integer> result = new ArrayList<>(node.getChildCount());
        for (int index = 0; index < node.getChildCount(); index += 1) {
            if (this.algorithm.isApplicable(node.getChild(index), selected)) {
                result.add(index);
            }
        }
        return result;
    }

    /**
     * Checks whether a node or its prototype is present in the node set.
     * @param node Node to be checked
     * @param set Set of nodes
     * @return Checking result.
     */
    private static boolean isPresentInSet(final Node node, final Set<Node> set) {
        boolean result = set.contains(node);
        Node current = node;
        while (!result && current instanceof PrototypeBasedNode wrapper) {
            current = wrapper.getPrototype();
            result = set.contains(current);
        }
        return result;
    }

    /**
     * An algorithm that selects nodes based on some criteria.
     * @since 0.0.1
     */
    public interface Algorithm {
        /**
         * Checks if the node is applicable to the set.
         * @param node Node to be checked
         * @param set Set of nodes
         * @return Checking result.
         */
        boolean isApplicable(Node node, Set<Node> set);
    }

    /**
     * A node created from the original node, but which has only children from the specified set.
     * @since 0.0.1
     */
    private static final class SubNode extends NodeWithStructuralEquality
        implements PrototypeBasedNode {
        /**
         * Original node.
         */
        private final Node original;

        /**
         * Immutable list containing the indexes of the nodes that should be included
         *  to the resulting tree.
         */
        private final List<Integer> indexes;

        /**
         * Immutable list of children (also truncated nodes).
         */
        private final List<Node> children;

        /**
         * Constructor.
         * @param original Original node
         * @param indexes Original edge positions
         * @param children Completed child snapshots
         */
        private SubNode(final Node original, final List<Integer> indexes,
            final List<Node> children) {
            this.original = original;
            this.indexes = List.copyOf(indexes);
            this.children = List.copyOf(children);
        }

        @Override
        public Fragment getFragment() {
            return this.original.getFragment();
        }

        @Override
        public Type getType() {
            return this.original.getType();
        }

        @Override
        public String getData() {
            return this.original.getData();
        }

        @Override
        public Map<String, String> getProperties() {
            return this.original.getProperties();
        }

        @Override
        public int getChildCount() {
            return this.children.size();
        }

        @Override
        public Node getChild(final int index) {
            return this.children.get(index);
        }

        @Override
        public Node getPrototype() {
            return this.original;
        }

        @Override
        public java.util.Optional<String> getChildTag(final int index) {
            java.util.Objects.checkIndex(index, this.getChildCount());
            return this.original.getChildTag(this.indexes.get(index));
        }

        @Override
        public java.util.Optional<Node> getChild(final String tag) {
            java.util.Objects.requireNonNull(tag);
            java.util.Optional<Node> result = java.util.Optional.empty();
            for (int index = 0; index < this.getChildCount(); index += 1) {
                if (this.getChildTag(index).filter(tag::equals).isPresent()) {
                    result = java.util.Optional.of(this.getChild(index));
                    break;
                }
            }
            return result;
        }

        @Override
        public String toString() {
            return Node.toString(this);
        }
    }
}
