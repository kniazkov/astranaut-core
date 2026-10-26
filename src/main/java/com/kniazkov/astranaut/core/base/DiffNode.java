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

import com.kniazkov.astranaut.core.algorithms.DiffProjection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable difference scaffold. Editing belongs to DiffTreeBuilder, never to nodes.
 * The shared pattern builder also allows ordinary tree algorithms to rebuild scaffolds.
 * @since 0.0.1
 */
public final class DiffNode extends NodeWithStructuralEquality
    implements DiffTreeItem, PrototypeBasedNode {
    /**
     * Immutable target metadata and operation children.
     */
    private final PatternNode content;

    /**
     * Wraps an existing tree iteratively.
     * @param original Source tree
     */
    public DiffNode(final Node original) {
        this(new PatternNode(original));
    }

    /**
     * Creates an immutable explicit scaffold.
     * @param content Pattern-compatible content
     */
    public DiffNode(final PatternNode content) {
        this.content = java.util.Objects.requireNonNull(content);
    }

    @Override
    public Type getType() {
        return this.content.getType();
    }

    /**
     * Returns the descriptor used to reconstruct ordinary states.
     * @return Target descriptor
     */
    public Type getTargetType() {
        return this.content.getTargetType();
    }

    @Override
    public Node getPrototype() {
        return this.content.getPrototype();
    }

    @Override
    public Fragment getFragment() {
        return this.content.getFragment();
    }

    @Override
    public String getData() {
        return this.content.getData();
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
    public Optional<Node> getChild(final String tag) {
        return this.content.getChild(tag);
    }

    @Override
    public Optional<String> getChildTag(final int index) {
        return this.content.getChildTag(index);
    }

    @Override
    public List<Node> getChildrenList() {
        return this.content.getChildrenList();
    }

    @Override
    public Map<String, String> getProperties() {
        return this.content.getProperties();
    }

    @Override
    public Node getBefore() {
        return new DiffProjection(false).project(new Tree(this)).getRoot().orElseThrow();
    }

    @Override
    public Node getAfter() {
        return new DiffProjection(true).project(new Tree(this)).getRoot().orElseThrow();
    }

    @Override
    public String toString() {
        return Node.toString(this);
    }
}
