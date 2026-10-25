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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Strict iterative parser of draft descriptions. EOF is an offset, never a character.
 * @since 0.0.1
 */
final class DraftParser {
    /**
     * Complete immutable input.
     */
    private final String text;

    /**
     * Current UTF-16 offset.
     */
    private int offset;

    /**
     * Creates a parser.
     * @param text Description
     */
    DraftParser(final String text) {
        this.text = Objects.requireNonNull(text);
        this.offset = 0;
    }

    /**
     * Parses exactly one complete tree without recursive calls.
     * @return Root
     * @throws IllegalArgumentException If the description is malformed
     */
    Node parse() {
        final Deque<DraftNode.Constructor> parents = new ArrayDeque<>();
        DraftNode.Constructor builder = this.header();
        while (true) {
            if (this.take('(') && !this.take(')')) {
                parents.push(builder);
                builder = this.header();
                continue;
            }
            Node result = builder.build();
            while (!parents.isEmpty()) {
                parents.peek().addChild(result);
                if (this.take(',')) {
                    builder = this.header();
                    break;
                }
                this.expect(')');
                result = parents.pop().build();
            }
            if (parents.isEmpty()) {
                this.space();
                if (this.offset != this.text.length()) {
                    throw this.error("End of description expected");
                }
                return result;
            }
        }
    }

    /**
     * Reads the name and optional quoted data of one node.
     * @return Fresh builder
     * @throws IllegalArgumentException If a name or data delimiter is missing
     */
    private DraftNode.Constructor header() {
        this.space();
        final int begin = this.offset;
        if (this.offset >= this.text.length()
            || !Character.isLetter(this.text.codePointAt(this.offset))) {
            throw this.error("Type name expected");
        }
        this.offset += Character.charCount(this.text.codePointAt(this.offset));
        while (this.offset < this.text.length()
            && Character.isLetterOrDigit(this.text.codePointAt(this.offset))) {
            this.offset += Character.charCount(this.text.codePointAt(this.offset));
        }
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName(this.text.substring(begin, this.offset));
        if (this.take('<')) {
            builder.setData(this.data());
            this.expect('>');
        }
        return builder;
    }

    /**
     * Reads quoted data, preserving literal UTF-16 units and decoding explicit escapes.
     * @return Decoded data
     * @throws IllegalArgumentException If a quote or escape is incomplete or invalid
     */
    private String data() {
        this.space();
        final char quote = this.next();
        if (quote != '\'' && quote != '"') {
            throw this.error("Quoted data expected");
        }
        final StringBuilder data = new StringBuilder();
        boolean closed = false;
        while (!closed) {
            final char symbol = this.next();
            if (symbol == quote) {
                closed = true;
            } else if (symbol == '\\') {
                data.append(this.escape());
            } else {
                data.append(symbol);
            }
        }
        return data.toString();
    }

    /**
     * Reads one escaped UTF-16 unit.
     * @return Decoded unit
     * @throws IllegalArgumentException If the escape is unknown or truncated
     */
    private char escape() {
        final char symbol = this.next();
        return switch (symbol) {
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'u' -> this.unicode();
            default -> {
                if ("\\'\"".indexOf(symbol) < 0) {
                    throw this.error("Unknown escape");
                }
                yield symbol;
            }
        };
    }

    /**
     * Decodes exactly four hexadecimal digits as one UTF-16 unit.
     * @return Decoded unit
     * @throws IllegalArgumentException If any digit is missing or invalid
     */
    private char unicode() {
        int value = 0;
        for (int index = 0; index < 4; index += 1) {
            final char symbol = this.next();
            final int digit = Character.digit(symbol, 16);
            if (digit < 0 || symbol > 127) {
                throw this.error("Four ASCII hexadecimal digits expected");
            }
            value = value * 16 + digit;
        }
        return (char) value;
    }

    /**
     * Reads a unit without conflating real NUL or U+FFFF with EOF.
     * @return Next unit
     * @throws IllegalArgumentException If input ends unexpectedly
     */
    private char next() {
        if (this.offset == this.text.length()) {
            throw this.error("Unexpected end of description");
        }
        final char symbol = this.text.charAt(this.offset);
        this.offset += 1;
        return symbol;
    }

    /**
     * Consumes an optional token after whitespace.
     * @param symbol Token
     * @return Whether consumed
     */
    private boolean take(final char symbol) {
        this.space();
        final boolean found = this.offset < this.text.length()
            && this.text.charAt(this.offset) == symbol;
        if (found) {
            this.offset += 1;
        }
        return found;
    }

    /**
     * Consumes a required token.
     * @param symbol Token
     * @throws IllegalArgumentException If the token is absent
     */
    private void expect(final char symbol) {
        if (!this.take(symbol)) {
            throw this.error(String.format("Expected '%s'", symbol));
        }
    }

    /**
     * Skips whitespace outside names and quoted data.
     */
    private void space() {
        while (this.offset < this.text.length()
            && Character.isWhitespace(this.text.charAt(this.offset))) {
            this.offset += 1;
        }
    }

    /**
     * Creates a bounded diagnostic without copying the input.
     * @param message Expected token or failure reason
     * @return Controlled syntax error with a zero-based UTF-16 offset
     */
    private IllegalArgumentException error(final String message) {
        return new IllegalArgumentException(
            String.format("%s at UTF-16 offset %d", message, this.offset)
        );
    }
}
