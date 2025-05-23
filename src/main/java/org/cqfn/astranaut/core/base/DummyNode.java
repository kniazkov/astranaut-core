/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Ivan Kniazkov
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
package org.cqfn.astranaut.core.base;

import java.util.Collections;
import java.util.List;

/**
 * Special placeholder node that represents a problematic or default node instance.
 * @since 1.0
 */
public final class DummyNode implements Node {
    /**
     * The type.
     */
    public static final Type TYPE = new TypeImpl();

    /**
     * The instance.
     */
    public static final Node INSTANCE = new DummyNode();

    /**
     * The builder.
     */
    public static final Builder BUILDER = new BuilderImpl();

    /**
     * Private constructor.
     */
    private DummyNode() {
    }

    @Override
    public Type getType() {
        return DummyNode.TYPE;
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
     * The fake builder that only returns static reference.
     * @since 1.0
     */
    private static final class BuilderImpl implements Builder {
        @SuppressWarnings("PMD.UncommentedEmptyMethodBody")
        @Override
        public void setFragment(final Fragment fragment) {
        }

        @Override
        public boolean setData(final String str) {
            return false;
        }

        @Override
        public boolean setChildrenList(final List<Node> list) {
            return false;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public Node createNode() {
            return DummyNode.INSTANCE;
        }
    }

    /**
     * The type of dummy node.
     * @since 1.0
     */
    private static class TypeImpl implements Type {
        @Override
        public String getName() {
            return "\u2205";
        }

        @Override
        public List<ChildDescriptor> getChildTypes() {
            return Collections.emptyList();
        }

        @Override
        public List<String> getHierarchy() {
            return Collections.emptyList();
        }

        @Override
        public Builder createBuilder() {
            return DummyNode.BUILDER;
        }
    }
}
