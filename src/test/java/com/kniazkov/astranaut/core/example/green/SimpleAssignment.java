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

package com.kniazkov.astranaut.core.example.green;

import com.kniazkov.astranaut.core.algorithms.NodeAllocator;
import com.kniazkov.astranaut.core.base.AbstractType;
import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.ChildDescriptor;
import com.kniazkov.astranaut.core.base.EmptyFragment;
import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Type;
import com.kniazkov.astranaut.core.base.TypeWithNameBasedEquality;
import com.kniazkov.astranaut.core.utils.ListUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Node that describes the 'SimpleAssignment' type.
 * @since 0.0.1
 */
public final class SimpleAssignment implements Assignment {
    /**
     * The type.
     */
    public static final Type TYPE = new TypeImpl();

    /**
     * The number of children.
     */
    private static final int CHILD_COUNT = 2;

    /**
     * The fragment associated with the node.
     */
    private Fragment fragment;

    /**
     * List of child nodes.
     */
    private List<Node> children;

    /**
     * Child with the 'left' tag.
     */
    private AssignableExpression left;

    /**
     * Child with the 'right' tag.
     */
    private Expression right;

    /**
     * Constructor.
     */
    private SimpleAssignment() {
    }

    @Override
    public Type getType() {
        return SimpleAssignment.TYPE;
    }

    @Override
    public Fragment getFragment() {
        return this.fragment;
    }

    @Override
    public String getData() {
        return "";
    }

    @Override
    public int getChildCount() {
        return SimpleAssignment.CHILD_COUNT;
    }

    @Override
    public Node getChild(final int index) {
        return this.children.get(index);
    }

    @Override
    public AssignableExpression getLeft() {
        return this.left;
    }

    @Override
    public Expression getRight() {
        return this.right;
    }

    @Override
    public Optional<Node> getChild(final String tag) {
        return switch (tag) {
            case "left" -> Optional.ofNullable(this.left);
            case "right" -> Optional.ofNullable(this.right);
            default -> Optional.empty();
        };
    }

    @Override
    public Optional<String> getChildTag(final int index) {
        Objects.checkIndex(index, this.getChildCount());
        final List<String> tags = new ArrayList<>(2);
        if (this.left != null) {
            tags.add("left");
        }
        if (this.right != null) {
            tags.add("right");
        }
        return Optional.of(tags.get(index));
    }

    @Override
    public String toString() {
        return String.format("%s = %s", this.left.toString(), this.right.toString());
    }

    /**
     * Type descriptor of the 'SimpleAssignment' node.
     * @since 0.0.1
     */
    private static final class TypeImpl extends TypeWithNameBasedEquality implements Type {
        /**
         * The 'SimpleAssignment' string.
         */
        private static final String SIMPLE_ASSIGNMEN = "SimpleAssignment";

        /**
         * The list of child types.
         */
        private static final List<ChildDescriptor> CHILDREN = ChildDescriptor.create()
            .required(AssignableExpression.TYPE, "left")
            .required(Expression.TYPE, "right")
            .build();

        /**
         * Properties.
         */
        private static final Map<String, String> PROPERTIES = Stream.of(
            new String[][] {
                {"color", "green"},
                {"language", "common"},
            }).collect(Collectors.toUnmodifiableMap(data -> data[0], data -> data[1]));

        @Override
        public String getName() {
            return TypeImpl.SIMPLE_ASSIGNMEN;
        }

        @Override
        public Optional<List<ChildDescriptor>> getChildDescriptors() {
            return Optional.of(TypeImpl.CHILDREN);
        }

        @Override
        public Set<AbstractType> getBaseTypes() {
            return Set.of(Assignment.TYPE);
        }

        @Override
        public Map<String, String> getProperties() {
            return TypeImpl.PROPERTIES;
        }

        @Override
        public Builder createBuilder() {
            return new Constructor();
        }
    }

    /**
     * Class for 'SimpleAssignment' node construction.
     * @since 0.0.1
     */
    public static final class Constructor implements Builder {
        /**
         * The maximum number of nodes.
         */
        private static final int MAX_NODE_COUNT = 2;

        /**
         * The position of the 'left' field.
         */
        private static final int LEFT_POS = 0;

        /**
         * The position of the 'right' field.
         */
        private static final int RIGHT_POS = 1;

        /**
         * The fragment associated with the node.
         */
        private Fragment fragment = EmptyFragment.INSTANCE;

        /**
         * Node with the 'left' tag.
         */
        private AssignableExpression left;

        /**
         * Node with the 'right' tag.
         */
        private Expression right;

        @Override
        public void setFragment(final Fragment obj) {
            this.fragment = obj;
        }

        @Override
        public boolean setData(final String str) {
            return str.isEmpty();
        }

        /**
         * Sets the node with the 'left' tag.
         * @param node The node
         */
        public void setLeft(final AssignableExpression node) {
            this.left = node;
        }

        /**
         * Sets the node with the 'right' tag.
         * @param node The node
         */
        public void setRight(final Expression node) {
            this.right = node;
        }

        @Override
        public boolean setChildrenList(final List<Node> list) {
            final Node[] mapping = new Node[Constructor.MAX_NODE_COUNT];
            final NodeAllocator allocator =
                new NodeAllocator(SimpleAssignment.TYPE.getChildDescriptors().orElseThrow());
            final boolean result = allocator.allocate(mapping, list);
            if (result) {
                this.left = (AssignableExpression) mapping[Constructor.LEFT_POS];
                this.right = (Expression) mapping[Constructor.RIGHT_POS];
            }
            return result;
        }

        @Override
        public boolean isValid() {
            return this.left != null
                && this.right != null;
        }

        @Override
        public SimpleAssignment createNode() {
            if (!this.isValid()) {
                throw new IllegalStateException();
            }
            final SimpleAssignment node = new SimpleAssignment();
            node.fragment = this.fragment;
            node.children = new ListUtils<Node>()
                .add(
                    this.left,
                    this.right
                )
                .make();
            node.left = this.left;
            node.right = this.right;
            return node;
        }
    }
}
