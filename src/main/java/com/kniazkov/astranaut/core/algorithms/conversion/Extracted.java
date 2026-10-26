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
package com.kniazkov.astranaut.core.algorithms.conversion;

import com.kniazkov.astranaut.core.base.Node;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Captures belonging to one matching attempt. Node and data slots are independent.
 * An immutable append log makes appending, forking and committing constant-time;
 * speculative attempts never copy all previously captured subtrees or strings.
 * @since 0.0.1
 */
public final class Extracted {
    /**
     * Immutable log head; only this attempt's reference is mutable.
     */
    private Optional<Entry> head = Optional.empty();

    /**
     * Appends a node in matching order.
     * @param number Slot
     * @param node Captured node
     */
    public void addNode(final int number, final Node node) {
        this.append(number, Objects.requireNonNull(node));
    }

    /**
     * Appends data in matching order, including empty strings.
     * @param number Slot
     * @param value Captured data
     */
    public void addData(final int number, final String value) {
        this.append(number, Objects.requireNonNull(value));
    }

    /**
     * Reads an immutable snapshot, preserving slot and within-slot order.
     * @param numbers Slots, possibly repeated
     * @return Captured nodes, retaining identity and provenance
     */
    public List<Node> getNodes(final int... numbers) {
        final List<Node> result = new ArrayList<>(numbers.length);
        for (final int number : numbers) {
            result.addAll(
                this.values(number).stream().filter(Node.class::isInstance)
                    .map(Node.class::cast).toList()
            );
        }
        return List.copyOf(result);
    }

    /**
     * Reads concatenated data, or an empty string for an unfilled slot.
     * @param number Slot
     * @return Data
     */
    public String getData(final int number) {
        final StringBuilder result = new StringBuilder();
        this.values(number).stream().filter(String.class::isInstance)
            .map(String.class::cast).forEach(result::append);
        return result.toString();
    }

    /**
     * Creates an independently mutable speculative snapshot in constant time.
     * @return Detached attempt sharing only immutable log entries
     */
    public Extracted fork() {
        final Extracted result = new Extracted();
        result.replaceWith(this);
        return result;
    }

    /**
     * Commits a successful attempt without sharing mutable state.
     * @param other Successful attempt
     */
    public void replaceWith(final Extracted other) {
        this.head = other.head;
    }

    /**
     * Appends one immutable value.
     * @param number Slot
     * @param value Nonnull node or string from the typed public methods
     */
    private void append(final int number, final Object value) {
        Extracted.checkNumber(number);
        this.head = Optional.of(new Entry(new Capture(number, value), this.head));
    }

    /**
     * Reads one slot in chronological order, without recursive log traversal.
     * @param number Slot
     * @return Values
     */
    private List<Object> values(final int number) {
        Extracted.checkNumber(number);
        final List<Object> result = new ArrayList<>(0);
        Optional<Entry> current = this.head;
        while (current.isPresent()) {
            final Entry entry = current.orElseThrow();
            if (entry.capture().number() == number) {
                result.add(entry.capture().value());
            }
            current = entry.previous();
        }
        return result.reversed();
    }

    /**
     * Validates a capture slot.
     * @param number Slot
     * @throws IllegalArgumentException If negative
     */
    private static void checkNumber(final int number) {
        if (number < 0) {
            throw new IllegalArgumentException("Negative capture slot");
        }
    }

    /**
     * Immutable capture value. The public API accepts only nodes and strings.
     * @param number Independent slot
     * @param value Node or string
     * @since 0.0.1
     */
    private record Capture(int number, Object value) { }

    /**
     * Persistent log entry.
     * @param capture Value
     * @param previous Earlier entries
     * @since 0.0.1
     */
    private record Entry(Capture capture, Optional<Entry> previous) { }
}
