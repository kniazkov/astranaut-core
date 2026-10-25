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
package com.kniazkov.astranaut.core.algorithms.normalization;

import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.ConstructionException;
import com.kniazkov.astranaut.core.base.EmptyFragment;
import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeWithStructuralEquality;
import com.kniazkov.astranaut.core.base.Type;
import com.kniazkov.astranaut.core.base.TypeWithNameBasedEquality;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable child occurrence made explicit in the normalized representation.
 * Its data is N for an absent tag, or T followed by the exact tag. Its only child
 * is the normalized target. Shared targets can have distinct edges and tags.
 * @since 0.0.1
 */
public final class NormalizedEdge extends NodeWithStructuralEquality {
    /**
     * Shared independent type.
     */
    private static final Type TYPE = new EdgeType();

    /**
     * Tag on the source occurrence.
     */
    private final Optional<String> tag;

    /**
     * Target subtree.
     */
    private final Node child;

    /**
     * Occurrence provenance.
     */
    private final Fragment fragment;

    /**
     * Creates an occurrence without copying its immutable target.
     * @param tag Original edge tag, or empty
     * @param child Target subtree
     */
    public NormalizedEdge(final Optional<String> tag, final Node child) {
        this(tag, child, child.getFragment());
    }

    /**
     * Creates an occurrence with explicitly supplied provenance.
     * @param tag Tag
     * @param child Target
     * @param fragment Provenance
     */
    private NormalizedEdge(final Optional<String> tag, final Node child, final Fragment fragment) {
        this.tag = Objects.requireNonNull(tag);
        this.child = Objects.requireNonNull(child);
        this.fragment = Objects.requireNonNull(fragment);
    }

    @Override
    public Type getType() {
        return NormalizedEdge.TYPE;
    }

    @Override
    public Fragment getFragment() {
        return this.fragment;
    }

    @Override
    public String getData() {
        return this.tag.map(value -> "T".concat(value)).orElse("N");
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public Node getChild(final int index) {
        Objects.checkIndex(index, 1);
        return this.child;
    }

    /**
     * Returns the original edge tag.
     * @return Tag, including an explicitly empty string when present
     */
    public Optional<String> getTag() {
        return this.tag;
    }

    @Override
    public String toString() {
        return Node.toString(this);
    }

    /**
     * Descriptor with no captured target node.
     * @since 0.0.1
     */
    private static final class EdgeType extends TypeWithNameBasedEquality implements Type {
        @Override
        public String getName() {
            return "AstranautNormalizedEdge";
        }

        @Override
        public Builder createBuilder() {
            return new EdgeBuilder();
        }
    }

    /**
     * Mutable occurrence construction state.
     * @since 0.0.1
     */
    private static final class EdgeBuilder implements Builder {
        /**
         * Encoded tag.
         */
        private String data = "N";

        /**
         * Optional construction target, required before building.
         */
        private Optional<Node> child = Optional.empty();

        /**
         * Provenance.
         */
        private Fragment fragment = EmptyFragment.INSTANCE;

        @Override
        public void setFragment(final Fragment value) {
            this.fragment = Objects.requireNonNull(value);
        }

        @Override
        public boolean setData(final String value) {
            final boolean accepted = value.equals("N") || value.startsWith("T");
            if (accepted) {
                this.data = value;
            }
            return accepted;
        }

        @Override
        public boolean setChildren(final List<Node> nodes) {
            final List<Node> snapshot = List.copyOf(nodes);
            if (snapshot.size() == 1) {
                this.child = Optional.of(snapshot.getFirst());
            }
            return snapshot.size() == 1;
        }

        @Override
        public boolean setChild(final int index, final Node node) {
            Objects.requireNonNull(node);
            final boolean accepted = index == 0 && this.child.isPresent();
            if (accepted) {
                this.child = Optional.of(node);
            }
            return accepted;
        }

        @Override
        public boolean removeChild(final int index) {
            final boolean accepted = index == 0 && this.child.isPresent();
            if (accepted) {
                this.child = Optional.empty();
            }
            return accepted;
        }

        @Override
        public boolean isValid() {
            return this.child.isPresent();
        }

        @Override
        public Node build() {
            final Node target = this.child.orElseThrow(
                () -> new ConstructionException("Missing normalized edge target")
            );
            Optional<String> tag = Optional.empty();
            if (this.data.startsWith("T")) {
                tag = Optional.of(this.data.substring(1));
            }
            return new NormalizedEdge(tag, target, this.fragment);
        }
    }
}
