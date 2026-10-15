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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Node that describes the 'Multiplication' type.
 *
 * Multiplication
 * ->
 * left@Expression, right@Expression
 *
 * @since 0.0.1
 */
public final class Multiplication extends NodeWithStructuralEquality implements BinaryExpression {
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
     * The fragment associated with the node.
     */
    private Fragment fragment;

    /**
     * List of child nodes.
     */
    private List<Node> children;

    /**
     * Node with the 'left' tag.
     */
    private Expression left;

    /**
     * Node with the 'right' tag.
     */
    private Expression right;

    /**
     * Private constructor.
     */
    private Multiplication() {
    }

    @Override
    public Fragment getFragment() {
        return this.fragment;
    }

    @Override
    public Type getType() {
        return Multiplication.TYPE;
    }

    @Override
    public String getData() {
        return "";
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    @Override
    public Node getChild(final int index) {
        return this.children.get(index);
    }

    @Override
    public Expression getLeft() {
        return this.left;
    }

    @Override
    public Expression getRight() {
        return this.right;
    }

    @Override
    public Optional<Node> getChild(final String tag) {
        return switch (tag) {
            case Multiplication.LEFT_TAG -> Optional.ofNullable(this.left);
            case Multiplication.RIGHT_TAG -> Optional.ofNullable(this.right);
            default -> Optional.empty();
        };
    }

    @Override
    public Optional<String> getChildTag(final int index) {
        Objects.checkIndex(index, this.getChildCount());
        final List<String> tags = new ArrayList<>(2);
        if (this.left != null) {
            tags.add(Multiplication.LEFT_TAG);
        }
        if (this.right != null) {
            tags.add(Multiplication.RIGHT_TAG);
        }
        return Optional.of(tags.get(index));
    }

    @Override
    public String toString() {
        return String.format("%s * %s", this.left.toString(), this.right.toString());
    }

    /**
     * Type descriptor of the 'Addition' node.
     * @since 0.0.1
     */
    private static final class TypeImpl extends TypeWithNameBasedEquality implements Type {
        /**
         * The name.
         */
        private static final String NAME = "Multiplication";

        /**
         * The list of child types.
         */
        private static final List<ChildDescriptor> CHILDREN = ChildDescriptor.create()
            .required(Expression.TYPE, Multiplication.LEFT_TAG)
            .required(Expression.TYPE, Multiplication.RIGHT_TAG)
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
            return TypeImpl.NAME;
        }

        @Override
        public Optional<List<ChildDescriptor>> getChildDescriptors() {
            return Optional.of(TypeImpl.CHILDREN);
        }

        @Override
        public Set<AbstractType> getBaseTypes() {
            return Set.of(BinaryExpression.TYPE);
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
     * Class for 'Addition' node construction.
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
            super(Multiplication.TYPE.getChildDescriptors().orElseThrow());
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
         * @param node Node
         */
        public void setLeft(final Expression node) {
            this.setChild(Multiplication.LEFT_TAG, node);
        }

        /**
         * Sets the node with the 'right' tag.
         * @param node Node
         */
        public void setRight(final Expression node) {
            this.setChild(Multiplication.RIGHT_TAG, node);
        }

        @Override
        public Multiplication build() {
            if (!this.isValid()) {
                throw new ConstructionException("Required builder state is missing");
            }
            final Expression left = (Expression) this.getBindings()
                .getChild(Multiplication.LEFT_TAG)
                .orElse(null);
            final Expression right = (Expression) this.getBindings()
                .getChild(Multiplication.RIGHT_TAG)
                .orElse(null);
            final Multiplication node = new Multiplication();
            node.fragment = this.fragment;
            node.children = Arrays.asList(left, right);
            node.left = left;
            node.right = right;
            return node;
        }
    }
}
