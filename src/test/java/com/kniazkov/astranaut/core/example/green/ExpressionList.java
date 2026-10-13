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
import com.kniazkov.astranaut.core.base.Cardinality;
import com.kniazkov.astranaut.core.base.ChildDescriptor;
import com.kniazkov.astranaut.core.base.EmptyFragment;
import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.NamedType;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Node that describes the 'ExpressionList' type.
 * @since 0.0.1
 */
public final class ExpressionList implements Node {
    /**
     * The type.
     */
    public static final Type TYPE = new TypeImpl();

    /**
     * The fragment associated with the node.
     */
    private Fragment fragment;

    /**
     * List of child nodes.
     */
    private List<Expression> children;

    /**
     * Private constructor.
     */
    private ExpressionList() {
    }

    @Override
    public Fragment getFragment() {
        return this.fragment;
    }

    @Override
    public Type getType() {
        return ExpressionList.TYPE;
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
        return this.getExpression(index);
    }

    /**
     * Return a child with 'Expression' type by its index.
     * @param index Child index
     * @return A node
     */
    public Expression getExpression(final int index) {
        return this.children.get(index);
    }

    /**
     * Type descriptor of the 'ExpressionList' node.
     *
     * @since 0.0.1
     */
    private static final class TypeImpl extends NamedType implements Type {
        /**
         * The 'ExpressionList' string.
         */
        private static final String EXPRESSION_LIST = "ExpressionList";

        /**
         * The list of child types.
         */
        private static final List<ChildDescriptor> CHILDREN =
            Collections.singletonList(
                new ChildDescriptor(Expression.TYPE, Optional.empty(), Cardinality.REPEATED)
            );

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
            return TypeImpl.EXPRESSION_LIST;
        }

        @Override
        public Optional<List<ChildDescriptor>> getChildDescriptors() {
            return Optional.of(ChildDescriptor.copySchema(TypeImpl.CHILDREN));
        }

        @Override
        public Set<AbstractType> getBaseTypes() {
            return Set.of();
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
     *
     * @since 0.0.1
     */
    public static final class Constructor implements Builder {
        /**
         * The fragment associated with the node.
         */
        private Fragment fragment = EmptyFragment.INSTANCE;

        /**
         * List of child nodes.
         */
        private List<Expression> children = Collections.emptyList();

        @Override
        public void setFragment(final Fragment obj) {
            this.fragment = obj;
        }

        @Override
        public boolean setData(final String str) {
            return str.isEmpty();
        }

        @Override
        public boolean setChildrenList(final List<Node> list) {
            boolean result = true;
            final List<Expression> clarified = new ArrayList<>(list.size());
            for (final Node node : list) {
                if (node instanceof Expression) {
                    clarified.add((Expression) node);
                } else {
                    result = false;
                    break;
                }
            }
            if (result) {
                this.children = Collections.unmodifiableList(clarified);
            }
            return result;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public ExpressionList createNode() {
            if (!this.isValid()) {
                throw new IllegalStateException();
            }
            final ExpressionList node = new ExpressionList();
            node.fragment = this.fragment;
            node.children = this.children;
            return node;
        }
    }
}
