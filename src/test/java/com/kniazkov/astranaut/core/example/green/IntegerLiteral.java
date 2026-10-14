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
import com.kniazkov.astranaut.core.base.EmptyFragment;
import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Type;
import com.kniazkov.astranaut.core.base.TypeWithNameBasedEquality;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Node that describes the 'IntegerLiteral' type.
 * @since 0.0.1
 */
public final class IntegerLiteral implements Expression {
    /**
     * The type.
     */
    public static final Type TYPE = new TypeImpl();

    /**
     * The fragment associated with the node.
     */
    private Fragment fragment;

    /**
     * The data.
     */
    private int data;

    /**
     * Private constructor.
     */
    private IntegerLiteral() {
    }

    @Override
    public Fragment getFragment() {
        return this.fragment;
    }

    @Override
    public Type getType() {
        return IntegerLiteral.TYPE;
    }

    @Override
    public String getData() {
        return String.valueOf(this.data);
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public Node getChild(final int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public String toString() {
        return this.getData();
    }

    /**
     * Type descriptor of the 'Addition' node.
     *
     * @since 0.0.1
     */
    private static final class TypeImpl extends TypeWithNameBasedEquality implements Type {
        /**
         * The name.
         */
        private static final String NAME = "IntegerLiteral";

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
            return Optional.of(ChildDescriptor.create().build());
        }

        @Override
        public Set<AbstractType> getBaseTypes() {
            return Set.of(Expression.TYPE);
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
     * Class for 'IntegerLiteral' node construction.
     *
     * @since 0.0.1
     */
    public static final class Constructor implements Builder {
        /**
         * The fragment associated with the node.
         */
        private Fragment fragment = EmptyFragment.INSTANCE;

        /**
         * The data.
         */
        private int data;

        @Override
        public void setFragment(final Fragment obj) {
            this.fragment = obj;
        }

        @Override
        public boolean setData(final String str) {
            boolean success = true;
            try {
                this.data = Integer.parseInt(str);
            } catch (final NumberFormatException ignored) {
                success = false;
            }
            return success;
        }

        @Override
        public boolean setChildrenList(final List<Node> list) {
            return list.isEmpty();
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public IntegerLiteral createNode() {
            final IntegerLiteral node = new IntegerLiteral();
            node.fragment = this.fragment;
            node.data = this.data;
            return node;
        }
    }
}
