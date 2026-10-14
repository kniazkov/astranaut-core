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
package com.kniazkov.astranaut.core.example.converters;

import com.kniazkov.astranaut.core.algorithms.conversion.Extracted;
import com.kniazkov.astranaut.core.algorithms.conversion.Matcher;
import com.kniazkov.astranaut.core.base.AbstractType;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.SimpleAbstractType;

/**
 * Matcher that matches nodes representing integer values and extracts the data.
 * @since 0.0.1
 */
public final class IntegerMatcher implements Matcher {
    /**
     * The instance.
     */
    public static final Matcher INSTANCE = new IntegerMatcher();

    /**
     * Expected type.
     */
    private static final AbstractType TYPE = new SimpleAbstractType("int");

    /**
     * Hole number where to save.
     */
    private static final int HOLE_NUMBER = 1;

    /**
     * Private constructor.
     */
    private IntegerMatcher() {
    }

    @Override
    public boolean match(final Node node, final Extracted extracted) {
        final boolean result = node.isSubtypeOf(IntegerMatcher.TYPE);
        if (result) {
            extracted.addData(IntegerMatcher.HOLE_NUMBER, node.getData());
        }
        return result;
    }
}
