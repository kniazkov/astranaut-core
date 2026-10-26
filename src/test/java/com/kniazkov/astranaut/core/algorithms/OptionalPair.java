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
package com.kniazkov.astranaut.core.algorithms;

import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.ChildDescriptor;
import com.kniazkov.astranaut.core.base.ConstructionException;
import com.kniazkov.astranaut.core.base.EmptyFragment;
import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeWithStructuralEquality;
import com.kniazkov.astranaut.core.base.SchemaBuilder;
import com.kniazkov.astranaut.core.base.SimpleAbstractType;
import com.kniazkov.astranaut.core.base.Type;
import com.kniazkov.astranaut.core.base.TypeWithNameBasedEquality;
import java.util.List;
import java.util.Optional;

/**
 * Consumer with two ambiguous optional positions of the same type.
 * @since 0.0.1
 */
final class OptionalPair extends NodeWithStructuralEquality {
    /**
     * Descriptor.
     */
    static final Type TYPE = new Definition();

    /**
     * Ordered present children.
     */
    private final List<Node> children;

    /**
     * Explicit occurrence names.
     */
    private final List<String> tags;

    /**
     * Snapshotted provenance.
     */
    private final Fragment fragment;

    /**
     * Creates a valid snapshot.
     * @param children Present children
     * @param tags Bindings
     * @param fragment Provenance
     */
    private OptionalPair(
        final List<Node> children, final List<String> tags,
        final Fragment fragment
    ) {
        this.children = List.copyOf(children);
        this.tags = List.copyOf(tags);
        this.fragment = fragment;
    }

    @Override
    public Type getType() {
        return OptionalPair.TYPE;
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
        final int index = this.tags.indexOf(tag);
        Optional<Node> result = Optional.empty();
        if (index >= 0) {
            result = Optional.of(this.children.get(index));
        }
        return result;
    }

    @Override
    public Optional<String> getChildTag(final int index) {
        return Optional.of(this.tags.get(index));
    }

    /**
     * Independent descriptor.
     * @since 0.0.1
     */
    private static final class Definition extends TypeWithNameBasedEquality implements Type {
        @Override
        public String getName() {
            return "OptionalPair";
        }

        @Override
        public Optional<List<ChildDescriptor>> getChildDescriptors() {
            return Optional.of(
                ChildDescriptor.create()
                .optional(new SimpleAbstractType("Item"), "left")
                .optional(new SimpleAbstractType("Item"), "right").build()
            );
        }

        @Override
        public Builder createBuilder() {
            return new Constructor();
        }
    }

    /**
     * Independent builder, including ambiguous reset refusal.
     * @since 0.0.1
     */
    private static final class Constructor extends SchemaBuilder {
        /**
         * Explicit provenance.
         */
        private Fragment fragment;

        /**
         * Creates empty bindings.
         */
        private Constructor() {
            super(OptionalPair.TYPE.getChildDescriptors().orElseThrow());
            this.fragment = EmptyFragment.INSTANCE;
        }

        @Override
        public void setFragment(final Fragment value) {
            this.fragment = value;
        }

        @Override
        public boolean setData(final String data) {
            return data.isEmpty();
        }

        @Override
        public Node build() {
            if (!this.isValid()) {
                throw new ConstructionException("Invalid optional bindings");
            }
            final List<String> names = List.of("left", "right").stream()
                .filter(tag -> this.getBindings().getChild(tag).isPresent()).toList();
            return new OptionalPair(this.getBindings().getChildren(), names, this.fragment);
        }
    }
}
