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
package consumer;

import com.kniazkov.astranaut.core.base.AbstractType;
import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.ConstructionException;
import com.kniazkov.astranaut.core.base.EmptyFragment;
import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeWithStructuralEquality;
import com.kniazkov.astranaut.core.base.SimpleAbstractType;
import com.kniazkov.astranaut.core.base.Type;
import com.kniazkov.astranaut.core.base.TypeWithNameBasedEquality;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;

/**
 * Independent parser-style immutable node with a schema-free builder.
 * @since 0.0.1
 */
final class Adapter extends NodeWithStructuralEquality implements OptionalChild {
    static final AbstractType EXPRESSION = new SimpleAbstractType("ConsumerExpression");
    static final AbstractType LOCATED = new SimpleAbstractType("ConsumerLocated");
    static final Type TYPE = new Definition();
    private final String data;
    private final Optional<Adapter> right;
    private final Fragment fragment;

    private Adapter(String data, Optional<Adapter> right, Fragment fragment) {
        this.data = data;
        this.right = right;
        this.fragment = fragment;
    }

    static Adapter leaf(String data) {
        Builder builder = TYPE.createBuilder();
        if (!builder.setData(data)) {
            throw new IllegalArgumentException(data);
        }
        return (Adapter) builder.build();
    }

    @Override public Type getType() { return TYPE; }
    @Override public String getData() { return this.data; }
    @Override public Fragment getFragment() { return this.fragment; }
    @Override public int getChildCount() { return this.right.isPresent() ? 1 : 0; }
    @Override public Node getChild(int index) {
        java.util.Objects.checkIndex(index, this.getChildCount());
        return this.right.orElseThrow();
    }
    @Override public Optional<Adapter> getRight() { return this.right; }
    @Override public Optional<Node> getChild(String tag) {
        return "right".equals(Objects.requireNonNull(tag)) ? this.right.map(child -> child) : Optional.empty();
    }
    @Override public Optional<String> getChildTag(int index) {
        java.util.Objects.checkIndex(index, this.getChildCount());
        return Optional.of("right");
    }

    /** Type descriptor.
     * @since 0.0.1
     */
    private static final class Definition extends TypeWithNameBasedEquality implements Type {
        @Override public String getName() { return "ConsumerAdapter"; }
        @Override public Set<AbstractType> getBaseTypes() { return Set.of(EXPRESSION, LOCATED); }
        @Override public Builder createBuilder() { return new Constructor(); }
    }

    /** Independent mutable construction state.
     * @since 0.0.1
     */
    private static final class Constructor implements Builder {
        private String data = "";
        private Optional<Adapter> right = Optional.empty();
        private Fragment fragment = EmptyFragment.INSTANCE;

        @Override public void setFragment(Fragment value) {
            this.fragment = java.util.Objects.requireNonNull(value);
        }
        @Override public boolean setData(String value) {
            boolean accepted = !value.isEmpty() && !value.equals("forbidden");
            if (accepted) { this.data = value; }
            return accepted;
        }
        @Override public boolean setChildren(List<Node> children) {
            boolean accepted = children.size() <= 1
                && children.stream().allMatch(Adapter.class::isInstance);
            if (accepted) { this.right = children.stream().map(Adapter.class::cast).findFirst(); }
            return accepted;
        }
        @Override public boolean setChild(String tag, Node child) {
            boolean accepted = "right".equals(Objects.requireNonNull(tag))
                && Objects.requireNonNull(child) instanceof Adapter;
            if (accepted) { this.right = Optional.of((Adapter) child); }
            return accepted;
        }
        @Override public boolean setChild(int index, Node child) {
            return index == 0 && this.right.isPresent() && this.setChild("right", child);
        }
        @Override public boolean removeChild(String tag) {
            return "right".equals(tag) && this.removeChild(0);
        }
        @Override public boolean removeChild(int index) {
            boolean accepted = index == 0 && this.right.isPresent();
            if (accepted) { this.right = Optional.empty(); }
            return accepted;
        }
        @Override public boolean isValid() { return !this.data.isEmpty(); }
        @Override public Node build() {
            if (!this.isValid()) { throw new ConstructionException("Data is required"); }
            return new Adapter(this.data, this.right, this.fragment);
        }
    }
}

/**
 * Covariance at an external consumer boundary.
 * @since 0.0.1
 */
interface OptionalChild extends Node {
    Optional<? extends Node> getRight();
}
