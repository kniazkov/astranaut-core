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

/**
 * Immutable difference tree with explicit empty, inserted, deleted or replaced roots.
 * Ordinary payloads are opaque; actions are interpreted only in difference scaffolds.
 * @since 0.0.1
 */
public final class DiffTree extends Tree {
    /**
     * Represents empty to empty.
     */
    public DiffTree() {
        super();
    }

    /**
     * Stores a difference root without changing it.
     * @param root Root action, scaffold or unchanged ordinary node
     */
    public DiffTree(final Node root) {
        super(root);
    }

    /**
     * Restores the earlier structural state, failing explicitly on builder refusal.
     * @return Earlier tree, possibly empty
     */
    public Tree getBefore() {
        return new DiffProjection(false).project(this);
    }

    /**
     * Restores the later structural state, failing explicitly on builder refusal.
     * @return Later tree, possibly empty
     */
    public Tree getAfter() {
        return new DiffProjection(true).project(this);
    }
}
