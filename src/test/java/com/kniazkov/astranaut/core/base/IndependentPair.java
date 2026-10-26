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

import com.kniazkov.astranaut.core.example.green.Expression;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Schema-free consumer fixture with its own builder, without allocation helpers.
 * @since 0.0.1
 */
final class IndependentPair extends NodeWithStructuralEquality {
    /**
     * Stable descriptor without a child schema.
     */
    static final Type TYPE = new Definition();

    /**
     * Canonical child tags.
     */
    private static final List<String> TAGS = List.of("left", "right");

    /**
     * Completed children in canonical order.
     */
    private final List<Node> children;

    /**
     * Provenance.
     */
    private final Fragment fragment;

    /**
     * Publishes a completed pair.
     * @param children Canonical children
     * @param fragment Provenance
     */
    private IndependentPair(final List<Node> children, final Fragment fragment) {
        this.children = List.copyOf(children);
        this.fragment = fragment;
    }

    @Override
    public Type getType() {
        return IndependentPair.TYPE;
    }

    @Override
    public String getData() {
        return "";
    }

    @Override
    public Fragment getFragment() {
        return this.fragment;
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
        final int index = IndependentPair.TAGS.indexOf(Objects.requireNonNull(tag));
        Optional<Node> result = Optional.empty();
        if (index >= 0) {
            result = Optional.of(this.children.get(index));
        }
        return result;
    }

    @Override
    public Optional<String> getChildTag(final int index) {
        return Optional.of(IndependentPair.TAGS.get(index));
    }

    /**
     * Independent type descriptor.
     * @since 0.0.1
     */
    private static final class Definition extends TypeWithNameBasedEquality implements Type {
        @Override
        public String getName() {
            return "IndependentPair";
        }

        @Override
        public Builder createBuilder() {
            return new Constructor();
        }
    }

    /**
     * Hand-written consumer builder; insertion order is distinct from node order.
     * @since 0.0.1
     */
    private static final class Constructor implements Builder {
        /**
         * Present bindings in insertion order.
         */
        private final Map<String, Node> children = new LinkedHashMap<>();

        /**
         * Current provenance.
         */
        private Fragment fragment = EmptyFragment.INSTANCE;

        @Override
        public void setFragment(final Fragment value) {
            this.fragment = Objects.requireNonNull(value);
        }

        @Override
        public boolean setData(final String value) {
            return value.isEmpty();
        }

        @Override
        public boolean setChildren(final List<Node> values) {
            final boolean accepted = values.size() == 2
                && values.stream().allMatch(node -> node.isSubtypeOf(Expression.TYPE));
            if (accepted) {
                this.children.clear();
                this.children.put(IndependentPair.TAGS.get(0), values.get(0));
                this.children.put(IndependentPair.TAGS.get(1), values.get(1));
            }
            return accepted;
        }

        @Override
        public boolean setChild(final String tag, final Node child) {
            final boolean accepted = IndependentPair.TAGS.contains(Objects.requireNonNull(tag))
                && child.isSubtypeOf(Expression.TYPE);
            if (accepted) {
                this.children.put(tag, child);
            }
            return accepted;
        }

        @Override
        public boolean setChild(final int index, final Node child) {
            boolean accepted = false;
            if (index >= 0 && index < this.children.size()) {
                accepted = this.setChild(List.copyOf(this.children.keySet()).get(index), child);
            }
            return accepted;
        }

        @Override
        public boolean removeChild(final String tag) {
            return this.children.remove(Objects.requireNonNull(tag)) != null;
        }

        @Override
        public boolean removeChild(final int index) {
            boolean accepted = false;
            if (index >= 0 && index < this.children.size()) {
                accepted = this.removeChild(List.copyOf(this.children.keySet()).get(index));
            }
            return accepted;
        }

        @Override
        public boolean isValid() {
            return this.children.size() == 2;
        }

        @Override
        public Node build() {
            if (!this.isValid()) {
                throw new ConstructionException("Both pair children are required");
            }
            return new IndependentPair(
                IndependentPair.TAGS.stream().map(this.children::get).toList(), this.fragment
            );
        }
    }
}
