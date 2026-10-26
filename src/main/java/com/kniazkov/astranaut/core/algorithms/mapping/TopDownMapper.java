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
package com.kniazkov.astranaut.core.algorithms.mapping;

import com.kniazkov.astranaut.core.algorithms.DiffStrategy;
import com.kniazkov.astranaut.core.algorithms.PositionalDiff;
import com.kniazkov.astranaut.core.base.DiffTree;
import com.kniazkov.astranaut.core.base.Tree;

/**
 * Compatibility entry point for the ordered occurrence-based difference strategy.
 * Node-identity Mapping tables have been replaced by DiffStrategy and immutable diff trees.
 * @since 0.0.1
 */
public final class TopDownMapper implements DiffStrategy {
    /**
     * Default sequential strategy.
     */
    public static final DiffStrategy INSTANCE = new TopDownMapper();

    /**
     * Shared stateless facade.
     */
    private TopDownMapper() {
    }

    @Override
    public DiffTree compare(final Tree before, final Tree after) {
        return new PositionalDiff().compare(before, after);
    }
}
