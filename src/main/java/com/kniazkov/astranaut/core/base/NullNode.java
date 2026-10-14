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

import java.util.List;
import java.util.Optional;

/**
 * Singleton node representing intentional absence of a meaningful node value.
 * @since 0.0.1
 */
public final class NullNode implements Node {
    /**
     * The instance.
     */
    public static final Node INSTANCE = new NullNode();

    /**
     * Shared type descriptor.
     */
    private static final Type TYPE = new TypeImpl();

    /**
     * Private constructor.
     */
    private NullNode() {
    }

    @Override
    public Type getType() {
        return NullNode.TYPE;
    }

    @Override
    public String getData() {
        return "";
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public Node getChild(final int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public String toString() {
        return Node.toString(this);
    }

    /**
     * Type descriptor independent of node instances.
     * @since 0.0.1
     */
    private static final class TypeImpl extends NamedType implements Type {
        @Override
        public String getName() {
            return "\u2400";
        }

        @Override
        public Optional<List<ChildDescriptor>> getChildDescriptors() {
            return Optional.of(List.of());
        }

        @Override
        public Builder createBuilder() {
            throw new UnsupportedOperationException();
        }
    }
}
