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
package com.kniazkov.astranaut.core.base;

import com.kniazkov.astranaut.core.algorithms.DepthFirstWalker;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable structural pattern containing ordinary nodes, actions and holes.
 * Editing produces a new pattern; a published node never changes its children.
 * @since 0.0.1
 */
public final class PatternNode extends NodeWithStructuralEquality
    implements PatternItem, PrototypeBasedNode {
    /**
     * Source retained for mining identity only.
     */
    private final Node prototype;

    /**
     * Independent pattern builder descriptor.
     */
    private final TypeImpl type;

    /**
     * Immutable children.
     */
    private final List<Node> children;

    /**
     * Snapshotted source data.
     */
    private final String data;

    /**
     * Snapshotted provenance.
     */
    private final Fragment fragment;

    /**
     * Snapshotted display metadata.
     */
    private final Map<String, String> properties;

    /**
     * Wraps a tree iteratively, preserving actions and existing holes.
     * @param original Source
     */
    public PatternNode(final Node original) {
        this(original, PatternNode.wrapChildren(original));
    }

    /**
     * Creates a structural snapshot with replacement children.
     * @param original Source metadata
     * @param children Pattern children
     */
    private PatternNode(final Node original, final List<Node> children) {
        this(original, new TypeImpl(original), children);
    }

    /**
     * Publishes a node using an independent descriptor.
     * @param original Metadata source
     * @param type Descriptor
     * @param children Children
     */
    private PatternNode(final Node original, final TypeImpl type, final List<Node> children) {
        this.prototype = original;
        this.type = type;
        this.children = List.copyOf(children);
        this.data = original.getData();
        this.fragment = original.getFragment();
        this.properties = Map.copyOf(original.getProperties());
    }

    @Override
    public Type getType() {
        return this.type;
    }

    /**
     * Returns the concrete output descriptor, independent of mining prototypes.
     * @return Target type whose builder validates substituted children
     */
    public Type getTargetType() {
        return this.type.original;
    }

    @Override
    public Node getPrototype() {
        return this.prototype;
    }

    @Override
    public Fragment getFragment() {
        return this.fragment;
    }

    @Override
    public Map<String, String> getProperties() {
        return this.properties;
    }

    @Override
    public String getData() {
        return this.data;
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
        return this.type.tags.get(index);
    }

    @Override
    public Optional<Node> getChild(final String tag) {
        final int index = this.type.tags.indexOf(Optional.of(tag));
        Optional<Node> result = Optional.empty();
        if (index >= 0) {
            result = Optional.of(this.getChild(index));
        }
        return result;
    }

    @Override
    public String toString() {
        return Node.toString(this);
    }

    /**
     * Replaces the ordered child occurrences without changing this node or its tags.
     * @param nodes Replacement children
     * @return New immutable pattern node
     * @throws IllegalArgumentException If the occurrence count changes
     */
    public PatternNode withChildren(final List<Node> nodes) {
        if (nodes.size() != this.children.size()) {
            throw new IllegalArgumentException("Pattern occurrence layout must be retained");
        }
        return new PatternNode(this, this.type, nodes);
    }

    /**
     * Converts one occurrence into a hole, retaining its parent tag.
     * @param index Child index
     * @param number Hole slot
     * @return New immutable pattern
     */
    public PatternNode withHole(final int index, final int number) {
        final List<Node> nodes = new ArrayList<>(this.children);
        nodes.set(index, new Hole(this.children.get(index), number));
        return this.withChildren(nodes);
    }

    /**
     * Resolves the original output descriptor through mining wrappers.
     * @param node Source
     * @return Concrete descriptor
     */
    private static Type targetType(final Node node) {
        Node current = node;
        while (current instanceof PrototypeBasedNode prototype
            && !(current instanceof PatternNode) && !(current instanceof Hole)) {
            current = prototype.getPrototype();
        }
        final Type result;
        if (current instanceof PatternNode pattern) {
            result = pattern.getTargetType();
        } else {
            result = current.getType();
        }
        return result;
    }

    /**
     * Copies ordinary descendants bottom-up; operation payloads remain ordinary nodes.
     * @param root Source root
     * @return Immutable direct children
     */
    private static List<Node> wrapChildren(final Node root) {
        final Map<Node, Node> wrapped = new IdentityHashMap<>();
        final List<Node> nodes = new ArrayList<>(0);
        new DepthFirstWalker(root).findAll(
            node -> {
                nodes.add(node);
                return Action.toAction(node) != null || node instanceof Hole;
            }
        );
        for (final Node node : nodes.reversed()) {
            if (wrapped.containsKey(node)) {
                continue;
            }
            final Action action = Action.toAction(node);
            if (action != null) {
                wrapped.put(node, action);
            } else if (node instanceof Hole) {
                wrapped.put(node, node);
            } else {
                final List<Node> children = node.getChildrenList().stream()
                    .map(wrapped::get).toList();
                wrapped.put(node, new PatternNode(node, children));
            }
        }
        return root.getChildrenList().stream().map(child -> wrapped.getOrDefault(child, child))
            .toList();
    }

    /**
     * Transparent source type identity with a pattern-aware independent builder.
     * @since 0.0.1
     */
    private static final class TypeImpl extends TypeWithNameBasedEquality implements Type {
        /**
         * Immutable source descriptor.
         */
        private final Type original;

        /**
         * Immutable occurrence layout, without captured child objects.
         */
        private final List<Optional<String>> tags;

        /**
         * Captures metadata without retaining a source node.
         * @param original Source node
         */
        private TypeImpl(final Node original) {
            this.original = PatternNode.targetType(original);
            this.tags = java.util.stream.IntStream.range(0, original.getChildCount())
                .mapToObj(original::getChildTag).toList();
        }

        @Override
        public String getName() {
            return this.original.getName();
        }

        @Override
        public java.util.Set<AbstractType> getBaseTypes() {
            return this.original.getBaseTypes();
        }

        @Override
        public Map<String, String> getProperties() {
            return this.original.getProperties();
        }

        @Override
        public Builder createBuilder() {
            return new Constructor(this);
        }
    }

    /**
     * Builds patterns independently of concrete source child constraints.
     * @since 0.0.1
     */
    private static final class Constructor implements Builder {
        /**
         * Descriptor.
         */
        private final TypeImpl type;

        /**
         * Mutable source-like metadata builder.
         */
        private final DraftNode.Constructor metadata;

        /**
         * Children in insertion order, indexed by layout slot.
         */
        private final Map<Integer, Node> children;

        /**
         * Creates an empty builder.
         * @param type Pattern descriptor
         */
        private Constructor(final TypeImpl type) {
            this.type = type;
            this.metadata = Constructor.createMetadata(type);
            this.children = new java.util.LinkedHashMap<>();
        }

        @Override
        public void setFragment(final Fragment value) {
            this.metadata.setFragment(value);
        }

        @Override
        public boolean setData(final String value) {
            return this.metadata.setData(value);
        }

        @Override
        public boolean setChildren(final List<Node> nodes) {
            final List<Node> snapshot = List.copyOf(nodes);
            final boolean valid = snapshot.size() == this.type.tags.size();
            if (valid) {
                this.children.clear();
                for (int index = 0; index < snapshot.size(); index += 1) {
                    this.children.put(index, snapshot.get(index));
                }
            }
            return valid;
        }

        @Override
        public boolean setChild(final String tag, final Node node) {
            Objects.requireNonNull(node);
            final int index = this.type.tags.indexOf(Optional.of(tag));
            if (index >= 0) {
                this.children.put(index, node);
            }
            return index >= 0;
        }

        @Override
        public boolean setChild(final int index, final Node node) {
            Objects.requireNonNull(node);
            final boolean valid = index >= 0 && index < this.children.size();
            if (valid) {
                this.children.put(new ArrayList<>(this.children.keySet()).get(index), node);
            }
            return valid;
        }

        @Override
        public boolean removeChild(final String tag) {
            return this.children.remove(this.type.tags.indexOf(Optional.of(tag))) != null;
        }

        @Override
        public boolean removeChild(final int index) {
            final boolean valid = index >= 0 && index < this.children.size();
            if (valid) {
                this.children.remove(new ArrayList<>(this.children.keySet()).get(index));
            }
            return valid;
        }

        @Override
        public boolean isValid() {
            return this.children.size() == this.type.tags.size();
        }

        @Override
        public Node build() {
            if (!this.isValid()) {
                throw new ConstructionException("Incomplete pattern children");
            }
            final List<Node> nodes = java.util.stream.IntStream.range(0, this.children.size())
                .mapToObj(this.children::get).toList();
            return new PatternNode(this.metadata.build(), this.type, nodes);
        }

        /**
         * Initializes independent metadata once, before building.
         * @param type Descriptor
         * @return Metadata builder
         */
        private static DraftNode.Constructor createMetadata(final Type type) {
            final DraftNode.Constructor result = new DraftNode.Constructor();
            result.setName(type.getName());
            return result;
        }
    }
}
