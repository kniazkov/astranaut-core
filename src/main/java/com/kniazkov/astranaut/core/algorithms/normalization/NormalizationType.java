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
package com.kniazkov.astranaut.core.algorithms.normalization;

import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.Type;
import com.kniazkov.astranaut.core.base.TypeWithNameBasedEquality;
import java.util.HexFormat;

/**
 * Independent normalized type identified by the original type name.
 * Normalized layouts do not inherit construction constraints from source types.
 * @since 0.0.1
 */
final class NormalizationType extends TypeWithNameBasedEquality implements Type {
    /**
     * Original type name.
     */
    private final String original;

    /**
     * Globally distinguishable structural layout name.
     */
    private final String name;

    /**
     * Creates an immutable layout.
     * @param original Source type name
     */
    NormalizationType(final String original) {
        this.original = original;
        this.name = "AstranautNormalized".concat(NormalizationType.encode(original));
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Builder createBuilder() {
        return new NormalizedBuilder(this);
    }

    /**
     * Returns the original type name, without retaining its descriptor or factory.
     * @return Original name
     */
    String originalName() {
        return this.original;
    }

    /**
     * Encodes UTF-16 units injectively, including unpaired surrogates and empty strings.
     * @param text Original text
     * @return Identifier-safe hexadecimal text
     */
    static String encode(final String text) {
        final StringBuilder result = new StringBuilder(text.length() * 4);
        for (int index = 0; index < text.length(); index += 1) {
            result.append(HexFormat.of().toHexDigits(text.charAt(index)));
        }
        return result.toString();
    }

}
