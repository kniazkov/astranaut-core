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

/**
 * An immutable source snapshot. Implementations must not reread mutable external content.
 * Coordinates use one-based rows and UTF-16 columns; tabs occupy one unit.
 * LF starts the next row; CRLF remains two units of original text.
 * @since 0.0.1
 */
public interface Source {
    /**
     * Returns exact text in the half-open range [start, end), without normalization.
     * Both positions must belong to this source instance. Invalid or reversed ranges
     * must be rejected instead of clamped. Equal positions denote empty text.
     * @param start Start position
     * @param end End position
     * @return Text of the fragment
     */
    String getFragmentAsString(Position start, Position end);

    /**
     * Returns the full name of the source file if available.
     *  If the source is not associated with a file, this method returns an empty string.
     * @return The full name of the source file, or an empty string if unavailable.
     */
    default String getFileName() {
        return "";
    }
}
