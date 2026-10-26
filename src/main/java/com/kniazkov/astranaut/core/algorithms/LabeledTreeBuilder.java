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

import com.kniazkov.astranaut.core.base.EmptyTree;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeWithStructuralEquality;
import com.kniazkov.astranaut.core.base.PrototypeBasedNode;
import com.kniazkov.astranaut.core.base.Tree;
import com.kniazkov.astranaut.core.base.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * This algorithm labels the selected nodes of the tree with some property.
 * @since 0.0.1
 */
public class LabeledTreeBuilder {
    /**
     * The root node of the original tree.
     */
    private final Optional<Node> root;

    /**
     * Constructor.
     * @param root Root node of the original not labeled tree
     */
    public LabeledTreeBuilder(final Node root) {
        this(new Tree(root));
    }

    /**
     * Constructor.
     * @param tree Original not labeled tree
     */
    public LabeledTreeBuilder(final Tree tree) {
        this.root = tree.getRoot();
    }

    /**
     * Constructs a new tree in which the specified nodes have one added property.
     * @param selected Set of selected nodes in which the property will be added
     * @param name New property name
     * @param value New property value
     * @return Labeled tree
     */
    public Tree build(final Set<Node> selected, final String name, final String value) {
        return this.root.map(
            node -> new Tree(
                NodeViewBuilder.build(
                node,
                    source -> java.util.stream.IntStream.range(
                    0, source.getChildCount()
                ).boxed().toList(),
                    (source, indexes, children) -> new LabeledNode(
                    source, children, LabeledNode.initProperties(
                        source, selected, Map.entry(
                        name, value
                    )
                    )
                )
            )
            )
        ).orElse(EmptyTree.INSTANCE);
    }

    /**
     * A node created from the original node, however, with one modified property.
     * @since 0.0.1
     */
    private static final class LabeledNode extends NodeWithStructuralEquality
        implements PrototypeBasedNode {
        /**
         * Original node.
         */
        private final Node original;

        /**
         * Child labeled nodes.
         */
        private final List<Node> children;

        /**
         * Collection of properties that contains a specific label.
         */
        private final Map<String, String> properties;

        /**
         * Constructor.
         * @param original Original node.
         * @param children Completed child snapshots
         * @param properties Snapshot properties
         */
        private LabeledNode(
            final Node original, final List<Node> children,
            final Map<String, String> properties
        ) {
            this.original = original;
            this.children = List.copyOf(children);
            this.properties = Map.copyOf(properties);
        }

        @Override
        public com.kniazkov.astranaut.core.base.Fragment getFragment() {
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
            return this.properties;
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
        public Optional<String> getChildTag(final int index) {
            java.util.Objects.checkIndex(index, this.getChildCount());
            return this.original.getChildTag(index);
        }

        @Override
        public Optional<Node> getChild(final String tag) {
            java.util.Objects.requireNonNull(tag);
            Optional<Node> result = Optional.empty();
            for (int index = 0; index < this.getChildCount(); index += 1) {
                if (this.getChildTag(index).filter(tag::equals).isPresent()) {
                    result = Optional.of(this.getChild(index));
                    break;
                }
            }
            return result;
        }

        @Override
        public String toString() {
            return this.original.toString();
        }

        @Override
        public Node getPrototype() {
            return this.original;
        }

        /**
         * Initiates a property collection with the added property (if necessary).
         * @param original Original node.
         * @param selected Set of selected nodes in which the property will be added
         * @param property Property to be added
         * @return Property collection with the added property
         */
        private static Map<String, String> initProperties(final Node original,
            final Set<Node> selected, final Map.Entry<String, String> property) {
            final Map<String, String> result;
            if (selected.contains(original)) {
                result = new TreeMap<>(original.getProperties());
                result.put(property.getKey(), property.getValue());
            } else {
                result = original.getProperties();
            }
            return Map.copyOf(result);
        }
    }
}
