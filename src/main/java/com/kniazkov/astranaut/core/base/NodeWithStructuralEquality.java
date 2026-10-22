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
 * Optional base implementing the structural Node equality contract.
 * Implementations with another superclass can delegate to {@link NodeStructure}.
 * No hash is retained: legacy mutable wrappers must not be mutated while used
 * as keys in ordinary hash collections or during a structural operation.
 * @since 0.0.1
 */
public abstract class NodeWithStructuralEquality implements Node {
    /**
     * Creates a structural node.
     */
    @SuppressWarnings("PMD.UncommentedEmptyConstructor")
    protected NodeWithStructuralEquality() {
    }

    @Override
    public final boolean equals(final Object other) {
        return other instanceof Node node && NodeStructure.equal(this, node);
    }

    @Override
    public final int hashCode() {
        return NodeStructure.hash(this);
    }
}
