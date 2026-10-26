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
package com.kniazkov.astranaut.core.utils.json;

import com.kniazkov.astranaut.core.utils.TreeJsonException;
import com.kniazkov.json.Json;
import com.kniazkov.json.JsonException;
import com.kniazkov.json.JsonObject;
import com.kniazkov.json.JsonParsingMode;

/**
 * Guards the recursive JSON library before parsing; AST depth is independent of JSON depth.
 * @since 0.0.1
 */
@SuppressWarnings("PMD.ProhibitPublicStaticMethods")
public final class JsonInput {
    /**
     * Utility class.
     */
    private JsonInput() { }

    /**
     * Parses strict JSON with a bounded container nesting depth.
     * @param text Input
     * @return Object
     * @throws TreeJsonException If syntax, nesting or top-level shape is invalid
     */
    public static JsonObject parse(final String text) {
        JsonInput.checkDepth(text);
        try {
            return Fields.object(Json.parse(text, JsonParsingMode.STRICT));
        } catch (final JsonException failure) {
            throw new TreeJsonException("Invalid JSON: ".concat(failure.getMessage()), failure);
        }
    }

    /**
     * Counts containers outside strings without interpreting escaped punctuation.
     * The actual parser remains responsible for JSON syntax validation.
     * @param text JSON text
     */
    private static void checkDepth(final String text) {
        final Depth guard = new Depth();
        for (int index = 0; index < text.length(); index += 1) {
            guard.accept(text.charAt(index));
        }
    }

    /**
     * Lexical guard state independent of AST depth.
     * @since 0.0.1
     */
    private static final class Depth {
        /**
         * Container nesting.
         */
        private int level;

        /**
         * Whether inside a quoted string.
         */
        private boolean string;

        /**
         * Whether the next string character is escaped.
         */
        private boolean escaped;

        /**
         * Consumes a character outside or inside a string.
         * @param symbol Character
         * @throws TreeJsonException If nesting exceeds 64
         */
        private void accept(final char symbol) {
            if (this.string) {
                this.quoted(symbol);
            } else if (symbol == '"') {
                this.string = true;
            } else if (symbol == '{' || symbol == '[') {
                this.level += 1;
            } else if (symbol == '}' || symbol == ']') {
                this.level -= 1;
            }
            if (this.level > 64) {
                throw new TreeJsonException("JSON nesting exceeds 64 containers");
            }
        }

        /**
         * Tracks escapes without interpreting their values.
         * @param symbol Character
         */
        private void quoted(final char symbol) {
            if (this.escaped) {
                this.escaped = false;
            } else if (symbol == '\\') {
                this.escaped = true;
            } else if (symbol == '"') {
                this.string = false;
            }
        }
    }
}
