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
import java.util.Optional;

/**
 * Independent adapter implementing structural equality without the optional base.
 * @since 0.0.1
 */
public final class StructuralNodeFixture implements Node {
    /**
     * Local metadata source.
     */
    private final Node original;

    /**
     * Immutable child occurrences.
     */
    private final List<Node> children;

    /**
     * Occurrence tags.
     */
    private final List<Optional<String>> tags;

    /**
     * Creates a consumer implementation.
     * @param original Local metadata
     * @param children Children
     * @param tags Tags parallel to children
     */
    public StructuralNodeFixture(final Node original, final List<Node> children,
        final List<Optional<String>> tags) {
        this.original = original;
        this.children = List.copyOf(children);
        this.tags = List.copyOf(tags);
    }

    @Override
    public Type getType() {
        return this.original.getType();
    }

    @Override
    public String getData() {
        return this.original.getData();
    }

    @Override
    public Fragment getFragment() {
        return this.original.getFragment();
    }

    @Override
    public Map<String, String> getProperties() {
        return this.original.getProperties();
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
    public Optional<String> getChildTag(final int index) {
        return this.tags.get(index);
    }

    @Override
    public Optional<Node> getChild(final String tag) {
        Optional<Node> result = Optional.empty();
        for (int index = 0; index < this.tags.size(); index += 1) {
            if (this.tags.get(index).filter(tag::equals).isPresent()) {
                result = Optional.of(this.children.get(index));
                break;
            }
        }
        return result;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Node node && NodeStructure.equal(this, node);
    }

    @Override
    public int hashCode() {
        return NodeStructure.hash(this);
    }
}
