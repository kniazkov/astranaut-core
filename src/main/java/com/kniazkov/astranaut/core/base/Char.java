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

import java.util.List;
import java.util.Objects;

/**
 * Node representing one character of the source text.
 * @since 0.0.1
 */
public final class Char extends NodeWithStructuralEquality implements Node {
    /**
     * Name of the type.
     */
    public static final String NAME = "Char";

    /**
     * Type of the node.
     */
    public static final Type TYPE = new CharType();

    /**
     * Fragment of source code that is associated with the node.
     */
    private final Fragment fragment;

    /**
     * Symbol itself.
     */
    private final char symbol;

    /**
     * Takes an immutable snapshot of a validated builder.
     * @param builder Builder state
     */
    private Char(final Constructor builder) {
        this.fragment = builder.fragment;
        this.symbol = builder.symbol;
    }

    @Override
    public Fragment getFragment() {
        return this.fragment;
    }

    @Override
    public Type getType() {
        return Char.TYPE;
    }

    /**
     * Returns the character stored in the node.
     * @return One UTF-16 unit, including NUL
     */
    public char getSymbol() {
        return this.symbol;
    }

    @Override
    public String getData() {
        return String.valueOf(this.symbol);
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public Node getChild(final int index) {
        throw new IndexOutOfBoundsException();
    }

    /**
     * Type implementation describing character nodes.
     * @since 0.0.1
     */
    private static final class CharType extends TypeWithNameBasedEquality implements Type {
        @Override
        public String getName() {
            return Char.NAME;
        }

        @Override
        public Builder createBuilder() {
            return new Char.Constructor();
        }
    }

    /**
     * Builder for character construction.
     * @since 0.0.1
     */
    public static final class Constructor implements Builder {
        /**
         * Fragment of source code that is associated with the node.
         */
        private Fragment fragment = EmptyFragment.INSTANCE;

        /**
         * Symbol itself.
         */
        private char symbol;

        /**
         * Whether a value was explicitly provided, independently of its numeric value.
         */
        private boolean present;

        @Override
        public void setFragment(final Fragment obj) {
            this.fragment = Objects.requireNonNull(obj);
        }

        /**
         * Sets the symbol.
         * @param value Symbol value
         */
        public void setValue(final char value) {
            this.symbol = value;
            this.present = true;
        }

        @Override
        public boolean setData(final String str) {
            final boolean result;
            if (str.length() == 1) {
                this.setValue(str.charAt(0));
                result = true;
            } else {
                result = false;
            }
            return result;
        }

        @Override
        public boolean setChildren(final List<Node> list) {
            return list.isEmpty();
        }

        @Override
        public boolean isValid() {
            return this.present;
        }

        @Override
        public Node build() {
            if (!this.isValid()) {
                throw new ConstructionException("Required builder state is missing");
            }
            return new Char(this);
        }
    }
}
