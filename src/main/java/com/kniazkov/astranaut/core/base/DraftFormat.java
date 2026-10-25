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

/**
 * Iterative description writer with unambiguous quoted data.
 * @since 0.0.1
 */
final class DraftFormat {
    /**
     * No instances.
     */
    private DraftFormat() {
    }

    /**
     * Formats a tree without recursive calls.
     * @param root Root
     * @return Description
     */
    static String format(final Node root) {
        final StringBuilder text = new StringBuilder();
        final Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(root));
        while (!stack.isEmpty()) {
            final Frame frame = stack.peek();
            if (frame.index == -1) {
                text.append(frame.node.getTypeName());
                DraftFormat.appendData(text, frame.node.getData());
                frame.index = 0;
                if (frame.node.getChildCount() > 0) {
                    text.append('(');
                }
            } else if (frame.index < frame.node.getChildCount()) {
                if (frame.index > 0) {
                    text.append(", ");
                }
                stack.push(new Frame(frame.node.getChild(frame.index)));
                frame.index += 1;
            } else {
                if (frame.node.getChildCount() > 0) {
                    text.append(')');
                }
                stack.pop();
            }
        }
        return text.toString();
    }

    /**
     * Appends optional escaped data using double quotes.
     * @param text Destination
     * @param data Exact data
     */
    static void appendData(final StringBuilder text, final String data) {
        if (!data.isEmpty()) {
            text.append("<\"");
            for (int index = 0; index < data.length(); index += 1) {
                DraftFormat.appendUnit(text, data.charAt(index));
            }
            text.append("\">");
        }
    }

    /**
     * Escapes a single unit, retaining supplementary characters as paired units.
     * @param text Destination
     * @param symbol UTF-16 unit
     */
    private static void appendUnit(final StringBuilder text, final char symbol) {
        switch (symbol) {
            case '"', '\\' -> text.append('\\').append(symbol);
            case '\n' -> text.append("\\n");
            case '\r' -> text.append("\\r");
            case '\t' -> text.append("\\t");
            case '\b' -> text.append("\\b");
            case '\f' -> text.append("\\f");
            default -> DraftFormat.appendUnicode(text, symbol);
        }
    }

    /**
     * Writes a plain unit or its exact hexadecimal escape when required.
     * @param text Destination
     * @param symbol UTF-16 unit
     */
    private static void appendUnicode(final StringBuilder text, final char symbol) {
        if (symbol < 32 || Character.isSurrogate(symbol) || symbol == 0xffff) {
            text.append(String.format("\\u%04x", (int) symbol));
        } else {
            text.append(symbol);
        }
    }

    /**
     * One traversal frame, with constant space per depth level.
     * @since 0.0.1
     */
    private static final class Frame {
        /**
         * Current node.
         */
        private final Node node;

        /**
         * Next child, minus one before writing the header.
         */
        private int index;

        /**
         * Creates a frame.
         * @param node Node
         */
        private Frame(final Node node) {
            this.node = node;
            this.index = -1;
        }
    }
}
