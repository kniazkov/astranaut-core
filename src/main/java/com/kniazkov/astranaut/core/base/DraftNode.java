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
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The DraftNode class implements the {@link Node} interface. This class serves as a "draft node",
 *  where no data consistency checks are performed. The constructor of this class allows
 *  the creation of any tree structure without validation, making it suitable for testing
 *  purposes and for displaying trees obtained from third-party parsers in the ASTranaut
 *  format.<br/>
 *  DraftNode is designed to be immutable.
 * @since 0.0.1
 */
public final class DraftNode extends NodeWithStructuralEquality implements Node {
    /**
     * The fragment associated with the node.
     */
    private final Fragment fragment;

    /**
     * The node type.
     */
    private final Type type;

    /**
     * The node data.
     */
    private final String data;

    /**
     * The list of children nodes.
     */
    private final List<Node> children;

    /**
     * Takes an immutable snapshot of a builder.
     * @param builder Builder to copy
     */
    private DraftNode(final Constructor builder) {
        this.fragment = Objects.requireNonNull(builder.fragment);
        this.type = new TypeImpl(Objects.requireNonNull(builder.name));
        this.data = Objects.requireNonNull(builder.data);
        this.children = List.copyOf(builder.children);
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public Fragment getFragment() {
        return this.fragment;
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
    public String toString() {
        return Node.toString(this);
    }

    /**
     * Creates a tree from draft nodes based on description.
     *  Description format: A(B&lt;"data"&gt;,C(...),...) where 'A' is the type name
     *  (it consists only of letters and digits, first symbol is letter only) followed by
     *  child nodes (in the same format) in parentheses separated by commas.
     * @param description Description
     * @return Root node of the tree created by description
     * @throws IllegalArgumentException If the description is incomplete or malformed
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static Node create(final String description) {
        return DraftNode.create(description, null);
    }

    /**
     * Creates a tree from draft nodes based on description.
     *  Description format: A(B&lt;"data"&gt;,C(...),...) where 'A' is the type name
     *  (it consists only of letters and digits, first symbol is letter only) followed by
     *  child nodes (in the same format) in parentheses separated by commas.
     * @param description Description
     * @param nodes Collection in which to place the nodes to be created, sorted by type name
     * @return Root node of the tree created by description
     * @throws IllegalArgumentException If the description is incomplete or malformed
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static Node create(final String description, final Map<String, Set<Node>> nodes) {
        final Node root = new DraftParser(description).parse();
        if (nodes != null) {
            DraftNode.register(root, nodes);
        }
        return root;
    }

    /**
     * Creates a node from the type name, data, and existing child nodes..
     * @param type The type name
     * @param data The data (in a textual format)
     * @param children The list of children
     * @return A new node
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static Node create(final String type, final String data, final Node... children) {
        final DraftNode.Constructor ctor = new DraftNode.Constructor();
        ctor.setName(type);
        ctor.setData(data);
        ctor.setChildren(Arrays.asList(children));
        return ctor.build();
    }

    /**
     * Registers completed nodes only after the entire input has been validated.
     * @param root Parsed tree
     * @param nodes Caller-owned index, retaining separate equal objects
     */
    private static void register(final Node root, final Map<String, Set<Node>> nodes) {
        new DepthFirstWalker(root).findFirst(
            node -> {
                nodes.computeIfAbsent(
                    node.getTypeName(), key -> Collections.newSetFromMap(new IdentityHashMap<>())
                ).add(node);
                return false;
            }
        );
    }

    /**
     * The constructor class for draft node.
     * @since 0.0.1
     */
    public static final class Constructor implements Builder {
        /**
         * The fragment associated with the node.
         */
        private Fragment fragment;

        /**
         * The type name.
         */
        private String name;

        /**
         * The node data.
         */
        private String data;

        /**
         * The list of children nodes.
         */
        private final List<Node> children;

        /**
         * Constructor.
         */
        public Constructor() {
            this.fragment = EmptyFragment.INSTANCE;
            this.name = "";
            this.data = "";
            this.children = new LinkedList<>();
        }

        @Override
        public void setFragment(final Fragment obj) {
            this.fragment = Objects.requireNonNull(obj);
        }

        /**
         * Sets the type name.
         * @param str Type mane
         */
        public void setName(final String str) {
            this.name = Objects.requireNonNull(str);
        }

        @Override
        public boolean setData(final String str) {
            this.data = Objects.requireNonNull(str);
            return true;
        }

        /**
         * Adds a child node.
         * @param node Node
         */
        public void addChild(final Node node) {
            this.children.add(Objects.requireNonNull(node));
        }

        @Override
        public boolean setChildren(final List<Node> list) {
            final List<Node> snapshot = List.copyOf(list);
            this.children.clear();
            this.children.addAll(snapshot);
            return true;
        }

        @Override
        public boolean setChild(final int index, final Node child) {
            Objects.requireNonNull(child);
            final boolean result = index >= 0 && index < this.children.size();
            if (result) {
                this.children.set(index, child);
            }
            return result;
        }

        @Override
        public boolean removeChild(final int index) {
            final boolean result = index >= 0 && index < this.children.size();
            if (result) {
                this.children.remove(index);
            }
            return result;
        }

        @Override
        public boolean isValid() {
            return !this.name.isEmpty();
        }

        @Override
        public DraftNode build() {
            if (!this.isValid()) {
                throw new ConstructionException("Required builder state is missing");
            }
            return new DraftNode(this);
        }

        @Override
        public String toString() {
            return DraftFormat.format(this.name, this.data, this.children);
        }
    }

    /**
     * Type descriptor independent of node instances.
     * @since 0.0.1
     */
    private static final class TypeImpl extends TypeWithNameBasedEquality implements Type {
        /**
         * Immutable type definition.
         */
        private final String name;

        /**
         * Creates the descriptor.
         * @param name Type name
         */
        private TypeImpl(final String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Builder createBuilder() {
            final Constructor ctor = new Constructor();
            ctor.setName(this.name);
            return ctor;
        }
    }
}
