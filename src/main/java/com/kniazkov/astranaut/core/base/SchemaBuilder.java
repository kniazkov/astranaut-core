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

/**
 * Optional base for builders that delegate child binding to a schema.
 * Subclasses validate data and construct their own immutable node snapshots.
 * @since 0.0.1
 */
public abstract class SchemaBuilder implements Builder {
    /**
     * Child state owned exclusively by this builder.
     */
    private final ChildBindings bindings;

    /**
     * Creates an empty builder.
     * @param schema Complete child schema
     */
    protected SchemaBuilder(final List<ChildDescriptor> schema) {
        this.bindings = new ChildBindings(schema);
    }

    @Override
    public final boolean setChildren(final List<Node> nodes) {
        return this.bindings.setChildren(nodes);
    }

    @Override
    public final boolean setChild(final String tag, final Node node) {
        return this.bindings.setChild(tag, node);
    }

    @Override
    public final boolean setChild(final int index, final Node node) {
        return this.bindings.setChild(index, node);
    }

    @Override
    public final boolean removeChild(final String tag) {
        return this.bindings.removeChild(tag);
    }

    @Override
    public final boolean removeChild(final int index) {
        return this.bindings.removeChild(index);
    }

    @Override
    public boolean isValid() {
        return this.bindings.isValid();
    }

    /**
     * Accesses builder-owned child state for constructing a node snapshot.
     * @return Child bindings
     */
    protected final ChildBindings getBindings() {
        return this.bindings;
    }
}
