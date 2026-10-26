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
import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonObject;

/**
 * Strict structural field access, without coercion or silent defaults.
 * @since 0.0.1
 */
@SuppressWarnings("PMD.ProhibitPublicStaticMethods")
public final class Fields {
    /**
     * Utility class.
     */
    private Fields() { }

    /**
     * Reads a required object member.
     * @param object Object
     * @param name Field
     * @return Value
     */
    public static JsonElement required(final JsonObject object, final String name) {
        return object.element(
            name
        ).orElseThrow(
            () -> new TreeJsonException(
            "Missing ".concat(
            name
        )
        )
        );
    }

    /**
     * Requires an object.
     * @param value Value
     * @return Object
     */
    public static JsonObject object(final JsonElement value) {
        return value.toJsonObject().orElseThrow(() -> new TreeJsonException("Expected object"));
    }

    /**
     * Requires an array field.
     * @param object Object
     * @param name Field
     * @return Array
     */
    public static JsonArray array(final JsonObject object, final String name) {
        return Fields.required(object, name).toJsonArray()
            .orElseThrow(() -> new TreeJsonException("Expected array: ".concat(name)));
    }

    /**
     * Requires a string field.
     * @param object Object
     * @param name Field
     * @return String
     * @throws TreeJsonException If the field is not a string
     */
    public static String string(final JsonObject object, final String name) {
        final JsonElement value = Fields.required(object, name);
        if (!value.isString()) {
            throw new TreeJsonException("Expected string: ".concat(name));
        }
        return value.stringValue();
    }

    /**
     * Requires a nonnegative bounded integer, without truncation.
     * @param value Value
     * @param bound Exclusive upper bound
     * @return Index
     * @throws TreeJsonException If outside the supported integer range
     */
    public static int index(final JsonElement value, final int bound) {
        if (!value.isInteger() || !value.isLongInteger()
            || value.longValue() < 0 || value.longValue() >= bound) {
            throw new TreeJsonException("Invalid integer index");
        }
        return value.intValue();
    }
}
