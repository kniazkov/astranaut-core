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
package com.kniazkov.astranaut.core.algorithms;

import com.kniazkov.astranaut.core.base.AbstractType;
import com.kniazkov.astranaut.core.base.ChildDescriptor;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.example.green.AssignableExpression;
import com.kniazkov.astranaut.core.example.green.Expression;
import com.kniazkov.astranaut.core.example.green.IntegerLiteral;
import com.kniazkov.astranaut.core.example.green.Variable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Compares allocation against exhaustive enumeration, without search pruning.
 * @since 0.0.1
 */
class AllocationOracleTest {
    @Test
    void agreesWithEverySmallAllocation() {
        for (int length = 0; length <= 3; length = length + 1) {
            for (int code = 0; code < (int) Math.pow(6, length); code = code + 1) {
                this.checkSchema(this.schema(length, code));
            }
        }
    }

    /**
     * Checks every input sequence of up to three occurrences.
     * @param schema Schema
     */
    private void checkSchema(final List<ChildDescriptor> schema) {
        for (int length = 0; length <= 3; length = length + 1) {
            for (int code = 0; code < 1 << length; code = code + 1) {
                final List<Node> input = this.input(length, code);
                Assertions.assertEquals(
                    this.expected(schema, input), new NodeAllocator(schema).allocate(input),
                    () -> String.format("schema=%s, input=%s", schema, input)
                );
            }
        }
    }

    /**
     * Builds all cardinalities for two overlapping accepted types.
     * @param length Schema length
     * @param code Base-six encoding
     * @return Schema
     */
    private List<ChildDescriptor> schema(final int length, final int code) {
        final ChildDescriptor.Constructor builder = ChildDescriptor.create();
        int value = code;
        for (int index = 0; index < length; index = index + 1) {
            final AbstractType type;
            if (value % 2 == 0) {
                type = Expression.TYPE;
            } else {
                type = AssignableExpression.TYPE;
            }
            switch (value % 6 / 2) {
                case 0 -> builder.required(type);
                case 1 -> builder.optional(type);
                default -> builder.repeated(type);
            }
            value = value / 6;
        }
        return builder.build();
    }

    /**
     * Builds an occurrence-distinct input sequence.
     * @param length Sequence length
     * @param code Binary encoding
     * @return Source nodes
     */
    private List<Node> input(final int length, final int code) {
        final List<Node> result = new ArrayList<>(length);
        for (int index = 0; index < length; index = index + 1) {
            if ((code & 1 << index) == 0) {
                result.add(new Variable.Constructor().build());
            } else {
                result.add(new IntegerLiteral.Constructor().build());
            }
        }
        return result;
    }

    /**
     * Enumerates every function from source indexes to schema indexes.
     * @param schema Schema
     * @param nodes Source occurrences
     * @return The unique valid function's allocation, or empty
     */
    private Optional<List<List<Node>>> expected(final List<ChildDescriptor> schema,
        final List<Node> nodes) {
        final List<List<List<Node>>> valid = new ArrayList<>(2);
        final int limit = (int) Math.pow(schema.size(), nodes.size());
        for (int code = 0; code < limit; code = code + 1) {
            final List<List<Integer>> slots = this.assignment(schema.size(), nodes.size(), code);
            if (this.valid(schema, nodes, slots)) {
                valid.add(
                    slots.stream().map(slot -> slot.stream().map(nodes::get).toList()).toList()
                );
            }
        }
        Optional<List<List<Node>>> result = Optional.empty();
        if (valid.size() == 1) {
            result = Optional.of(valid.getFirst());
        }
        return result;
    }

    /**
     * Decodes an allocation, independently of type and cardinality constraints.
     * @param width Slot count
     * @param count Source count
     * @param code Encoded function
     * @return Source indexes grouped by slot
     */
    private List<List<Integer>> assignment(final int width, final int count, final int code) {
        final List<List<Integer>> slots = new ArrayList<>(width);
        for (int index = 0; index < width; index = index + 1) {
            slots.add(new ArrayList<>(count));
        }
        int value = code;
        for (int index = 0; index < count; index = index + 1) {
            slots.get(value % width).add(index);
            value = value / width;
        }
        return slots;
    }

    /**
     * Validates a whole allocation after enumeration.
     * @param schema Schema
     * @param nodes Source nodes
     * @param slots Candidate allocation
     * @return Whether compatible, complete and order-preserving
     */
    private boolean valid(final List<ChildDescriptor> schema, final List<Node> nodes,
        final List<List<Integer>> slots) {
        boolean result = true;
        for (int index = 0; index < schema.size(); index = index + 1) {
            final ChildDescriptor desc = schema.get(index);
            final List<Integer> slot = slots.get(index);
            final boolean quantity = switch (desc.getCardinality()) {
                case REQUIRED -> slot.size() == 1;
                case OPTIONAL -> slot.size() <= 1;
                case REPEATED -> true;
            };
            result = result && quantity
                && slot.stream().allMatch(item -> nodes.get(item).isSubtypeOf(desc.getType()));
        }
        return result && this.ordered(schema, nodes, slots);
    }

    /**
     * Verifies relative order of every mutually interchangeable pair.
     * @param schema Schema
     * @param nodes Source nodes
     * @param slots Candidate allocation
     * @return Whether all pairs preserve input order
     */
    private boolean ordered(final List<ChildDescriptor> schema, final List<Node> nodes,
        final List<List<Integer>> slots) {
        boolean result = true;
        for (int left = 0; left < nodes.size(); left = left + 1) {
            for (int right = left + 1; right < nodes.size(); right = right + 1) {
                final int first = this.position(slots, left);
                final int second = this.position(slots, right);
                result = result && !(first > second
                    && nodes.get(left).isSubtypeOf(schema.get(second).getType())
                    && nodes.get(right).isSubtypeOf(schema.get(first).getType()));
            }
        }
        return result;
    }

    /**
     * Looks up an occurrence in a complete candidate function.
     * @param slots Candidate allocation
     * @param index Source index
     * @return Schema position
     */
    private int position(final List<List<Integer>> slots, final int index) {
        int result = -1;
        for (int slot = 0; slot < slots.size(); slot = slot + 1) {
            if (slots.get(slot).contains(index)) {
                result = slot;
                break;
            }
        }
        return result;
    }
}
