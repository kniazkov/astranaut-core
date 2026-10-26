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

import com.kniazkov.astranaut.core.base.Delete;
import com.kniazkov.astranaut.core.base.DiffTree;
import com.kniazkov.astranaut.core.base.Insert;
import com.kniazkov.astranaut.core.base.Replace;
import com.kniazkov.astranaut.core.base.Tree;

/**
 * Coarse strategy retaining complete immutable endpoints as opaque operation payloads.
 * @since 0.0.1
 */
public final class WholeTreeDiff implements DiffStrategy {
    @Override
    public DiffTree compare(final Tree before, final Tree after) {
        final DiffTree result;
        if (before.getRoot().isEmpty() && after.getRoot().isEmpty()) {
            result = new DiffTree();
        } else if (before.getRoot().isEmpty()) {
            result = new DiffTree(new Insert(after.getRoot().orElseThrow()));
        } else if (after.getRoot().isEmpty()) {
            result = new DiffTree(new Delete(before.getRoot().orElseThrow()));
        } else {
            result = new DiffTree(
                new Replace(before.getRoot().orElseThrow(), after.getRoot().orElseThrow())
            );
        }
        return result;
    }
}
