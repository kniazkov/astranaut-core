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

import com.kniazkov.astranaut.core.base.AbstractType;
import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.ChildDescriptor;
import com.kniazkov.astranaut.core.base.ConstructionException;
import com.kniazkov.astranaut.core.base.EmptyFragment;
import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeWithStructuralEquality;
import com.kniazkov.astranaut.core.base.SchemaBuilder;
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
public final class SimpleAssignment extends NodeWithStructuralEquality implements Assignment {
    /**
     * The type.
     */
    public static final Type TYPE = new TypeImpl();

    /**
     * The left child tag.
     */
    private static final String LEFT_TAG = "left";

    /**
     * The right child tag.
     */
    private static final String RIGHT_TAG = "right";

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
            case SimpleAssignment.LEFT_TAG -> Optional.ofNullable(this.left);
            case SimpleAssignment.RIGHT_TAG -> Optional.ofNullable(this.right);
            default -> Optional.empty();
        };
    }

    @Override
    public Optional<String> getChildTag(final int index) {
        Objects.checkIndex(index, this.getChildCount());
        final List<String> tags = new ArrayList<>(2);
        if (this.left != null) {
            tags.add(SimpleAssignment.LEFT_TAG);
        }
        if (this.right != null) {
            tags.add(SimpleAssignment.RIGHT_TAG);
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
            .required(AssignableExpression.TYPE, SimpleAssignment.LEFT_TAG)
            .required(Expression.TYPE, SimpleAssignment.RIGHT_TAG)
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
    public static final class Constructor extends SchemaBuilder {
        /**
         * The fragment associated with the node.
         */
        private Fragment fragment;

        /**
         * Creates an empty builder.
         */
        public Constructor() {
            super(SimpleAssignment.TYPE.getChildDescriptors().orElseThrow());
            this.fragment = EmptyFragment.INSTANCE;
        }

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
            this.setChild(SimpleAssignment.LEFT_TAG, node);
        }

        /**
         * Sets the node with the 'right' tag.
         * @param node The node
         */
        public void setRight(final Expression node) {
            this.setChild(SimpleAssignment.RIGHT_TAG, node);
        }

        @Override
        public SimpleAssignment build() {
            if (!this.isValid()) {
                throw new ConstructionException("Required builder state is missing");
            }
            final AssignableExpression left = (AssignableExpression) this.getBindings()
                .getChild(SimpleAssignment.LEFT_TAG).orElse(null);
            final Expression right = (Expression) this.getBindings()
                .getChild(SimpleAssignment.RIGHT_TAG)
                .orElse(null);
            final SimpleAssignment node = new SimpleAssignment();
            node.fragment = this.fragment;
            node.children = new ListUtils<Node>()
                .add(
                    left,
                    right
                )
                .make();
            node.left = left;
            node.right = right;
            return node;
        }
    }
}
