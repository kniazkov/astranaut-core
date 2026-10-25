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

import com.kniazkov.astranaut.core.base.Fragment;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeWithStructuralEquality;
import com.kniazkov.astranaut.core.base.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Independent immutable normalized representation, without a denormalization contract.
 * Child zero contains node properties, child one contains the type's own properties;
 * Remaining children are explicit edges retaining source order and tags.
 * Both metadata groups always exist.
 * @since 0.0.1
 */
public final class NormalizedNode extends NodeWithStructuralEquality {
    /**
     * Independent layout descriptor.
     */
    private final NormalizationType type;

    /**
     * Immutable node state.
     */
    private final State state;

    /**
     * Normalizes an acyclic tree without recursive Java calls.
     * @param original Original root
     */
    public NormalizedNode(final Node original) {
        this(new Normalizer().normalize(original));
    }

    /**
     * Copies only references to immutable state.
     * @param normalized Normalized root
     */
    private NormalizedNode(final NormalizedNode normalized) {
        this(normalized.type, normalized.state);
    }

    /**
     * Publishes validated state owned by a normalizer or builder.
     * @param type Layout
     * @param state State
     */
    NormalizedNode(final NormalizationType type, final State state) {
        this.type = type;
        this.state = state;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public Fragment getFragment() {
        return this.state.fragment();
    }

    @Override
    public String getData() {
        return this.state.data();
    }

    @Override
    public int getChildCount() {
        return this.state.children().size();
    }

    @Override
    public Node getChild(final int index) {
        return this.state.children().get(index);
    }

    @Override
    public Optional<String> getChildTag(final int index) {
        Objects.checkIndex(index, this.getChildCount());
        Optional<String> result = Optional.empty();
        if (index >= 2) {
            result = ((NormalizedEdge) this.getChild(index)).getTag();
        }
        return result;
    }

    @Override
    public Optional<Node> getChild(final String tag) {
        Objects.requireNonNull(tag);
        Optional<Node> result = Optional.empty();
        for (int index = 2; index < this.getChildCount(); index += 1) {
            if (this.getChildTag(index).filter(tag::equals).isPresent()) {
                result = Optional.of(this.getChild(index));
                break;
            }
        }
        return result;
    }

    /**
     * Returns the source type name for diagnostics, without restoring its concrete class.
     * @return Original name
     */
    public String getOriginalTypeName() {
        return this.type.originalName();
    }

    @Override
    public String toString() {
        return Node.toString(this);
    }

    /**
     * Snapshot detached from all mutable builder state.
     * @param data Node data
     * @param fragment Original provenance
     * @param children Ordered children including metadata
     * @since 0.0.1
     */
    record State(String data, Fragment fragment, List<Node> children) {
        /**
         * Takes a defensive snapshot.
         * @param data Node data
         * @param fragment Provenance
         * @param children Children
         */
        State {
            data = Objects.requireNonNull(data);
            fragment = Objects.requireNonNull(fragment);
            children = List.copyOf(children);
        }
    }
}
