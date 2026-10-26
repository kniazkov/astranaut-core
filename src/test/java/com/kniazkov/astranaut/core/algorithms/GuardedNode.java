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
import com.kniazkov.astranaut.core.base.ConstructionException;
import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeWithStructuralEquality;
import com.kniazkov.astranaut.core.base.Type;
import com.kniazkov.astranaut.core.base.TypeWithNameBasedEquality;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Consumer node whose builder validates an entire descendant-dependent condition.
 * @since 0.0.1
 */
final class GuardedNode extends NodeWithStructuralEquality {
    /**
     * Immutable content.
     */
    private final Node content;

    /**
     * Independent validation descriptor.
     */
    private final Definition type;

    /**
     * Wraps valid initial content.
     * @param content Content
     * @param guard Validation condition
     */
    GuardedNode(final Node content, final Predicate<List<Node>> guard) {
        this(content, new Definition(content.getType(), guard));
    }

    /**
     * Publishes validated content.
     * @param content Content
     * @param type Descriptor
     */
    private GuardedNode(final Node content, final Definition type) {
        this.content = content;
        this.type = type;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public String getData() {
        return this.content.getData();
    }

    @Override
    public Fragment getFragment() {
        return this.content.getFragment();
    }

    @Override
    public int getChildCount() {
        return this.content.getChildCount();
    }

    @Override
    public Node getChild(final int index) {
        return this.content.getChild(index);
    }

    @Override
    public Optional<String> getChildTag(final int index) {
        return this.content.getChildTag(index);
    }

    @Override
    public Optional<Node> getChild(final String tag) {
        return this.content.getChild(tag);
    }

    /**
     * Descriptor independent of any node.
     * @since 0.0.1
     */
    private static final class Definition extends TypeWithNameBasedEquality implements Type {
        /**
         * Concrete delegate.
         */
        private final Type original;

        /**
         * Validation.
         */
        private final Predicate<List<Node>> guard;

        /**
         * Creates a descriptor.
         * @param original Delegate
         * @param guard Condition
         */
        private Definition(final Type original, final Predicate<List<Node>> guard) {
            this.original = original;
            this.guard = guard;
        }

        @Override
        public String getName() {
            return this.original.getName();
        }

        @Override
        public Builder createBuilder() {
            return new Constructor(this);
        }
    }

    /**
     * Builder validating beyond direct child types.
     * @since 0.0.1
     */
    private static final class Constructor implements Builder {
        /**
         * Descriptor.
         */
        private final Definition type;

        /**
         * Independent delegate.
         */
        private final Builder delegate;

        /**
         * Creates an empty builder.
         * @param type Descriptor
         */
        private Constructor(final Definition type) {
            this.type = type;
            this.delegate = type.original.createBuilder();
        }

        @Override
        public void setFragment(final Fragment fragment) {
            this.delegate.setFragment(fragment);
        }

        @Override
        public boolean setData(final String data) {
            return this.delegate.setData(data);
        }

        @Override
        public boolean setChildren(final List<Node> children) {
            return this.delegate.setChildren(children);
        }

        @Override
        public boolean setChild(final String tag, final Node child) {
            return this.delegate.setChild(tag, child);
        }

        @Override
        public boolean isValid() {
            return this.delegate.isValid()
                && this.type.guard.test(this.delegate.build().getChildrenList());
        }

        @Override
        public Node build() {
            if (!this.isValid()) {
                throw new ConstructionException("Rejected by consumer guard");
            }
            return new GuardedNode(this.delegate.build(), this.type);
        }
    }
}
