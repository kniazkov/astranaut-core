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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Independent normalized builder with two required metadata groups and any number of edges.
 * @since 0.0.1
 */
final class NormalizedBuilder implements Builder {
    /**
     * Original type identity only, never its construction policy.
     */
    private final NormalizationType type;

    /**
     * Children in current insertion order, retaining metadata roles after removals.
     */
    private List<Entry> entries;

    /**
     * Node data.
     */
    private String data;

    /**
     * Node provenance.
     */
    private Fragment fragment;

    /**
     * Creates an empty independent builder.
     * @param type Normalized type
     */
    NormalizedBuilder(final NormalizationType type) {
        this.type = type;
        this.entries = new ArrayList<>(0);
        this.data = "";
        this.fragment = EmptyFragment.INSTANCE;
    }

    @Override
    public void setFragment(final Fragment value) {
        this.fragment = Objects.requireNonNull(value);
    }

    @Override
    public boolean setData(final String value) {
        this.data = Objects.requireNonNull(value);
        return true;
    }

    @Override
    public boolean setChildren(final List<Node> children) {
        final List<Entry> updated = new ArrayList<>(children.size());
        for (int index = 0; index < children.size(); index += 1) {
            updated.add(new Entry(Math.min(index, 2), Objects.requireNonNull(children.get(index))));
        }
        final boolean accepted = updated.size() >= 2 && NormalizedBuilder.accepts(updated);
        if (accepted) {
            this.entries = updated;
        }
        return accepted;
    }

    @Override
    public boolean setChild(final String tag, final Node child) {
        return this.setChild(this.findTag(tag), child);
    }

    @Override
    public boolean setChild(final int index, final Node child) {
        Objects.requireNonNull(child);
        boolean accepted = false;
        if (index >= 0 && index < this.entries.size()) {
            final List<Entry> updated = new ArrayList<>(this.entries);
            updated.set(index, new Entry(updated.get(index).role(), child));
            accepted = NormalizedBuilder.accepts(updated)
                && NormalizedBuilder.sameTag(this.entries.get(index).node(), child);
            if (accepted) {
                this.entries = updated;
            }
        }
        return accepted;
    }

    @Override
    public boolean removeChild(final String tag) {
        return this.removeChild(this.findTag(tag));
    }

    @Override
    public boolean removeChild(final int index) {
        final boolean accepted = index >= 0 && index < this.entries.size();
        if (accepted) {
            this.entries.remove(index);
        }
        return accepted;
    }

    @Override
    public boolean isValid() {
        return this.entries.stream().anyMatch(entry -> entry.role() == 0)
            && this.entries.stream().anyMatch(entry -> entry.role() == 1);
    }

    @Override
    public Node build() {
        if (!this.isValid()) {
            throw new ConstructionException("Missing normalized metadata group");
        }
        final List<Node> children = this.entries.stream().map(Entry::node).toList();
        return new NormalizedNode(
            this.type, new NormalizedNode.State(this.data, this.fragment, children)
        );
    }

    /**
     * Finds a tagged edge in current insertion order.
     * @param tag Tag
     * @return Index or minus one
     */
    private int findTag(final String tag) {
        Objects.requireNonNull(tag);
        int result = -1;
        for (int index = 0; index < this.entries.size(); index += 1) {
            if (this.entries.get(index).node() instanceof NormalizedEdge edge
                && edge.getTag().filter(tag::equals).isPresent()) {
                result = index;
                break;
            }
        }
        return result;
    }

    /**
     * Checks child kinds and tag uniqueness before publishing an edit.
     * @param entries Candidate entries
     * @return Whether each entry is valid independently of missing required metadata
     */
    private static boolean accepts(final List<Entry> entries) {
        final Set<String> tags = new HashSet<>();
        boolean valid = true;
        for (final Entry entry : entries) {
            if (entry.role() < 2) {
                valid = entry.node() instanceof Properties;
            } else if (entry.node() instanceof NormalizedEdge edge) {
                valid = edge.getTag().isEmpty() || tags.add(edge.getTag().orElseThrow());
            } else {
                valid = false;
            }
            if (!valid) {
                break;
            }
        }
        return valid;
    }

    /**
     * Index replacement retains the edge tag; retagging requires replacing the full list.
     * @param before Existing child
     * @param after Replacement
     * @return Whether tags match
     */
    private static boolean sameTag(final Node before, final Node after) {
        return !(before instanceof NormalizedEdge first)
            || after instanceof NormalizedEdge second && first.getTag().equals(second.getTag());
    }

    /**
     * Metadata role zero/one, or ordinary edge role two.
     * @param role Child role
     * @param node Child
     * @since 0.0.1
     */
    private record Entry(int role, Node node) {
    }
}
