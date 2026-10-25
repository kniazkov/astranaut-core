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
package com.kniazkov.astranaut.core.utils.parsing;

import com.kniazkov.astranaut.core.base.Char;
import com.kniazkov.astranaut.core.base.DefaultPosition;
import com.kniazkov.astranaut.core.base.Position;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

/**
 * Immutable exact text with one-based UTF-16 coordinates. LF starts a new row;
 * CR, tabs, NUL and surrogate halves each occupy one column and are preserved.
 * EOF is a valid position, but is not a character. CRLF is not normalized.
 * @since 0.0.1
 */
public class StringSource implements CharParser {
    /**
     * Exact immutable text.
     */
    private final String code;

    /**
     * UTF-16 offsets of line starts, including the empty line after a final LF.
     */
    private final int[] starts;

    /**
     * Takes an immutable text snapshot.
     * @param code Exact source text
     */
    public StringSource(final String code) {
        this.code = Objects.requireNonNull(code);
        this.starts = StringSource.lineStarts(code);
    }

    @Override
    public final String getFragmentAsString(final Position start, final Position end) {
        final int first = this.offsetOf(start);
        final int last = this.offsetOf(end);
        if (first > last) {
            throw new IllegalArgumentException("Reversed source range");
        }
        return this.code.substring(first, last);
    }

    @Override
    public final Iterator<Char> iterator() {
        return new StringSourceIterator(this);
    }

    /**
     * Gets the exact text length in UTF-16 units.
     * @return Length, excluding EOF
     */
    public final int length() {
        return this.code.length();
    }

    /**
     * Converts a zero-based UTF-16 offset, including EOF, to canonical coordinates.
     * A boundary after LF belongs to the next row, column one.
     * @param offset Offset from zero through length
     * @return Immutable position
     * @throws IndexOutOfBoundsException If the offset is outside the text
     */
    public final Position positionAt(final int offset) {
        Objects.checkFromToIndex(offset, offset, this.code.length());
        int row = Arrays.binarySearch(this.starts, offset);
        if (row < 0) {
            row = -row - 2;
        }
        return new DefaultPosition(this, row + 1, offset - this.starts[row] + 1);
    }

    /**
     * Converts adapter coordinates to a zero-based UTF-16 offset, including EOF.
     * No clamping or cross-line aliases are accepted.
     * @param position Position in this source instance
     * @return UTF-16 offset
     * @throws IllegalArgumentException If the position belongs to a different source
     * @throws IndexOutOfBoundsException If coordinates are outside this source
     */
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public final int offsetOf(final Position position) {
        if (Objects.requireNonNull(position).getSource() != this) {
            throw new IllegalArgumentException("Position belongs to a different source");
        }
        final int row = position.getRow();
        Objects.checkIndex(row - 1, this.starts.length);
        final int begin = this.starts[row - 1];
        int limit = this.code.length() - begin;
        if (row < this.starts.length) {
            limit = this.starts[row] - begin - 1;
        }
        final int column = position.getColumn();
        if (column < 1 || column - 1 > limit) {
            throw new IndexOutOfBoundsException("Column outside source row");
        }
        return begin + column - 1;
    }

    /**
     * Reads one UTF-16 unit. Invalid coordinates and EOF are not sentinel characters.
     * @param row One-based row
     * @param column One-based column
     * @return Character, including NUL when present in the source
     * @throws IndexOutOfBoundsException If coordinates are invalid or point to EOF
     * @throws IllegalArgumentException If row or column is not positive
     */
    public final char getSymbol(final int row, final int column) {
        return this.code.charAt(this.offsetOf(new DefaultPosition(this, row, column)));
    }

    /**
     * Creates the immutable index without splitting or losing line terminators.
     * @param code Exact text
     * @return Line starts
     */
    private static int[] lineStarts(final String code) {
        int count = 1;
        for (int index = 0; index < code.length(); index += 1) {
            if (code.charAt(index) == '\n') {
                count += 1;
            }
        }
        final int[] offsets = new int[count];
        int row = 1;
        for (int index = 0; index < code.length(); index += 1) {
            if (code.charAt(index) == '\n') {
                offsets[row] = index + 1;
                row += 1;
            }
        }
        return offsets;
    }
}
