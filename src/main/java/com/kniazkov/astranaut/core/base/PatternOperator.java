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
import java.util.Objects;

/**
 * Immutable matching and substitution expressions, themselves ordinary tree nodes.
 * Data holes use decimal slot numbers; ranges contain two inclusive UTF-16 bounds.
 * WITH_DATA contains a node template followed by DATA/DATA_HOLE expressions.
 * @since 0.0.1
 */
public final class PatternOperator extends NodeWithStructuralEquality implements PatternItem {
    /**
     * Expression kind.
     * @since 0.0.1
     */
    public enum Kind {
        /**
         * Any single node.
         */
        ANY(0, 0),
        /**
         * Exact data, also a substitution string literal.
         */
        DATA(0, 0),
        /**
         * Captures data or reads a captured string during substitution.
         */
        DATA_HOLE(0, 0),
        /**
         * Inclusive single-character range.
         */
        RANGE(0, 0),
        /**
         * First successful alternative.
         */
        OR(1, Integer.MAX_VALUE),
        /**
         * All conditions on the same node, in order.
         */
        AND(1, Integer.MAX_VALUE),
        /**
         * Negation, discarding speculative captures.
         */
        NOT(1, 1),
        /**
         * Greedy zero-or-one sequence element.
         */
        OPTIONAL(1, 1),
        /**
         * Greedy zero-or-more sequence element.
         */
        REPEAT(1, 1),
        /**
         * Ordered sequence of expressions.
         */
        SEQUENCE(0, Integer.MAX_VALUE),
        /**
         * Exact complete child sequence.
         */
        CHILDREN(0, Integer.MAX_VALUE),
        /**
         * Output template with computed data; not a matching expression.
         */
        WITH_DATA(1, Integer.MAX_VALUE);

        /**
         * Minimum operand count.
         */
        private final int minimum;

        /**
         * Maximum operand count.
         */
        private final int maximum;

        /**
         * Defines an operator arity.
         * @param minimum Minimum
         * @param maximum Maximum
         */
        Kind(final int minimum, final int maximum) {
            this.minimum = minimum;
            this.maximum = maximum;
        }
    }

    /**
     * Service descriptor.
     */
    private final OperatorType type;

    /**
     * Immutable data and operands.
     */
    private final Node content;

    /**
     * Creates a validated expression.
     * @param kind Operator
     * @param data Literal, range or slot
     * @param children Operands
     */
    public PatternOperator(final Kind kind, final String data, final List<Node> children) {
        this(new OperatorType(kind), PatternOperator.createContent(kind, data, children));
    }

    /**
     * Publishes validated content.
     * @param type Descriptor
     * @param content Data and operands
     */
    private PatternOperator(final OperatorType type, final Node content) {
        this.type = type;
        this.content = content;
    }

    /**
     * Returns the operator.
     * @return Kind
     */
    public Kind getKind() {
        return this.type.kind;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public Fragment getFragment() {
        return this.content.getFragment();
    }

    @Override
    public String getData() {
        return this.content.getData();
    }

    @Override
    public int getChildCount() {
        return this.content.getChildCount();
    }

    @Override
    public Node getChild(final int index) {
        return this.content.getChild(index);
    }

    /**
     * Constructs through the same validation as subsequent reconstruction.
     * @param kind Operator
     * @param data Data
     * @param children Operands
     * @return Valid immutable content
     * @throws ConstructionException If malformed
     */
    private static Node createContent(final Kind kind, final String data,
        final List<Node> children) {
        final Builder builder = new OperatorType(kind).createBuilder();
        if (!builder.setData(data) || !builder.setChildren(children)) {
            throw new ConstructionException("Invalid pattern expression");
        }
        return ((PatternOperator) builder.build()).content;
    }

    /**
     * Independent descriptor with stable service identity.
     * @since 0.0.1
     */
    private static final class OperatorType extends TypeWithNameBasedEquality implements Type {
        /**
         * Operator.
         */
        private final Kind kind;

        /**
         * Creates the descriptor.
         * @param kind Operator
         */
        private OperatorType(final Kind kind) {
            this.kind = Objects.requireNonNull(kind);
        }

        @Override
        public String getName() {
            return "AstranautPattern:".concat(this.kind.name());
        }

        @Override
        public Builder createBuilder() {
            return new Constructor(this);
        }
    }

    /**
     * Validates expression arity and payload without retaining a source node.
     * @since 0.0.1
     */
    private static final class Constructor implements Builder {
        /**
         * Descriptor.
         */
        private final OperatorType type;

        /**
         * Independent mutable content.
         */
        private final DraftNode.Constructor content;

        /**
         * Creates an empty expression builder.
         * @param type Descriptor
         */
        private Constructor(final OperatorType type) {
            this.type = type;
            this.content = Constructor.createContent(type);
        }

        @Override
        public void setFragment(final Fragment fragment) {
            this.content.setFragment(fragment);
        }

        @Override
        public boolean setData(final String data) {
            Objects.requireNonNull(data);
            final boolean valid = switch (this.type.kind) {
                case DATA_HOLE -> Constructor.validNumber(data);
                case RANGE -> data.length() == 2 && data.charAt(0) <= data.charAt(1);
                case DATA -> true;
                default -> data.isEmpty();
            };
            if (valid) {
                this.content.setData(data);
            }
            return valid;
        }

        @Override
        public boolean setChildren(final List<Node> children) {
            final List<Node> snapshot = List.copyOf(children);
            final boolean valid = this.validCount(snapshot.size());
            if (valid) {
                this.content.setChildren(snapshot);
            }
            return valid;
        }

        @Override
        public boolean setChild(final int index, final Node child) {
            return this.content.setChild(index, child);
        }

        @Override
        public boolean removeChild(final int index) {
            return this.content.removeChild(index);
        }

        @Override
        public boolean isValid() {
            final Node node = this.content.build();
            final boolean data = this.type.kind != Kind.DATA_HOLE || !node.getData().isEmpty();
            return data && (this.type.kind != Kind.RANGE || node.getData().length() == 2)
                && this.validCount(node.getChildCount());
        }

        @Override
        public Node build() {
            if (!this.isValid()) {
                throw new ConstructionException("Incomplete pattern expression");
            }
            return new PatternOperator(this.type, this.content.build());
        }

        /**
         * Checks operator arity.
         * @param count Operand count
         * @return Whether valid
         */
        private boolean validCount(final int count) {
            return count >= this.type.kind.minimum && count <= this.type.kind.maximum;
        }

        /**
         * Initializes independent metadata.
         * @param type Descriptor
         * @return Builder
         */
        private static DraftNode.Constructor createContent(final Type type) {
            final DraftNode.Constructor result = new DraftNode.Constructor();
            result.setName(type.getName());
            return result;
        }

        /**
         * Validates the complete nonnegative integer slot range.
         * @param data Canonical decimal representation
         * @return Whether valid
         */
        private static boolean validNumber(final String data) {
            boolean valid = false;
            try {
                final int number = Integer.parseInt(data);
                valid = number >= 0 && Integer.toString(number).equals(data);
            } catch (final NumberFormatException ignored) {
                valid = false;
            }
            return valid;
        }
    }
}
