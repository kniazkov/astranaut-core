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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Action that replaces a child element.
 * @since 0.0.1
 */
public final class Replace extends NodeWithStructuralEquality implements Action {
    /**
     * The type.
     */
    public static final Type TYPE = new ReplaceType();

    /**
     * Child element before changes.
     */
    private final Node before;

    /**
     * Child element after changes.
     */
    private final Node after;

    /**
     * Constructor.
     * @param before Child element that will be replaced
     * @param after Child element to be replaced by
     */
    public Replace(final Node before, final Node after) {
        this.before = before;
        this.after = after;
    }

    @Override
    public Node getBefore() {
        return this.before;
    }

    @Override
    public Node getAfter() {
        return this.after;
    }

    @Override
    public Fragment getFragment() {
        return this.before.getFragment();
    }

    @Override
    public Type getType() {
        return Replace.TYPE;
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
        final Node node;
        switch (index) {
            case 0:
                node = this.before;
                break;
            case 1:
                node = this.after;
                break;
            default:
                node = null;
                break;
        }
        return node;
    }

    @Override
    public String toString() {
        return Node.toString(this);
    }

    /**
     * Type of 'Replace' action.
     * @since 0.0.1
     */
    private static final class ReplaceType extends TypeWithNameBasedEquality implements Type {
        /**
         * The 'DELETE' string.
         */
        private static final String REPLACE = "Replace";

        /**
         * Properties.
         */
        private static final Map<String, String> PROPERTIES = Stream.of(
            new String[][] {
                {"color", "blue"},
            }).collect(Collectors.toUnmodifiableMap(data -> data[0], data -> data[1]));

        @Override
        public String getName() {
            return ReplaceType.REPLACE;
        }

        @Override
        public Set<AbstractType> getBaseTypes() {
            return Set.of(Action.TYPE);
        }

        @Override
        public Map<String, String> getProperties() {
            return ReplaceType.PROPERTIES;
        }

        @Override
        public Builder createBuilder() {
            return new Constructor();
        }
    }

    /**
     * Class for 'Delete' action construction.
     * @since 0.0.1
     */
    public static final class Constructor implements Builder {
        /**
         * Children in current insertion order.
         */
        private final List<Node> children = new java.util.ArrayList<>(2);

        @Override
        public void setFragment(final Fragment fragment) {
            // do nothing
        }

        @Override
        public boolean setData(final String str) {
            return str.isEmpty();
        }

        @Override
        public boolean setChildren(final List<Node> list) {
            final List<Node> snapshot = List.copyOf(list);
            final boolean result = snapshot.size() == 2;
            if (result) {
                this.children.clear();
                this.children.addAll(snapshot);
            }
            return result;
        }

        @Override
        public boolean setChild(final int index, final Node child) {
            java.util.Objects.requireNonNull(child);
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
            return this.children.size() == 2;
        }

        @Override
        public Node build() {
            if (!this.isValid()) {
                throw new ConstructionException("Required action children are missing");
            }
            return new Replace(this.children.get(0), this.children.get(1));
        }
    }
}
