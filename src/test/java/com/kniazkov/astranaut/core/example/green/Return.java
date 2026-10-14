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
 * Node that describes the 'Return' type.
 * @since 0.0.1
 */
public final class Return implements Statement {
    /**
     * The type.
     */
    public static final Type TYPE = new TypeImpl();

    /**
     * The expression child tag.
     */
    private static final String EXPRESSION_TAG = "expression";

    /**
     * The fragment associated with the node.
     */
    private Fragment fragment;

    /**
     * List of child nodes.
     */
    private List<Node> children;

    /**
     * Child with the 'expression' tag.
     */
    private Expression expression;

    /**
     * Constructor.
     */
    private Return() {
    }

    @Override
    public Type getType() {
        return Return.TYPE;
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
        return this.children.size();
    }

    @Override
    public Node getChild(final int index) {
        return this.children.get(index);
    }

    @Override
    public Optional<Node> getChild(final String tag) {
        final Optional<Node> result;
        if (Objects.requireNonNull(tag).equals(Return.EXPRESSION_TAG)) {
            result = Optional.ofNullable(this.expression);
        } else {
            result = Optional.empty();
        }
        return result;
    }

    @Override
    public Optional<String> getChildTag(final int index) {
        Objects.checkIndex(index, this.getChildCount());
        final List<String> tags = new ArrayList<>(1);
        if (this.expression != null) {
            tags.add(Return.EXPRESSION_TAG);
        }
        return Optional.of(tags.get(index));
    }

    @Override
    public String toString() {
        return "return;";
    }

    /**
     * Returns the child with the 'expression' tag.
     * @return The node
     */
    public Expression getExpression() {
        return this.expression;
    }

    /**
     * Type descriptor of the 'Return' node.
     * @since 0.0.1
     */
    private static final class TypeImpl extends TypeWithNameBasedEquality implements Type {
        /**
         * The 'Return' string.
         */
        private static final String RETURN = "Return";

        /**
         * The list of child types.
         */
        private static final List<ChildDescriptor> CHILDREN = ChildDescriptor.create()
            .optional(Expression.TYPE, Return.EXPRESSION_TAG)
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
            return TypeImpl.RETURN;
        }

        @Override
        public Optional<List<ChildDescriptor>> getChildDescriptors() {
            return Optional.of(TypeImpl.CHILDREN);
        }

        @Override
        public Set<AbstractType> getBaseTypes() {
            return Set.of(Statement.TYPE);
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
     * Class for 'Return' node construction.
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
            super(Return.TYPE.getChildDescriptors().orElseThrow());
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
         * Sets the node with the 'expression' tag.
         * @param node The node
         */
        public void setExpression(final Expression node) {
            this.setChild(Return.EXPRESSION_TAG, node);
        }

        @Override
        public Return build() {
            if (!this.isValid()) {
                throw new ConstructionException("Required builder state is missing");
            }
            final Expression expression = (Expression) this.getBindings()
                .getChild(Return.EXPRESSION_TAG)
                .orElse(null);
            final Return node = new Return();
            node.fragment = this.fragment;
            node.children = new ListUtils<Node>()
                .add(
                    expression
                )
                .make();
            node.expression = expression;
            return node;
        }
    }
}
