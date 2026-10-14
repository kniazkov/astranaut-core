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

import com.kniazkov.astranaut.core.algorithms.NodeAllocator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Optional mutable child-binding helper for builders with a known schema.
 * Keeps insertion order for edits and schema order for construction. Instances
 * belong to one builder; snapshots never expose mutable storage.
 * @since 0.0.1
 */
public final class ChildBindings {
    /**
     * Immutable schema.
     */
    private final List<ChildDescriptor> schema;

    /**
     * Children in insertion order.
     */
    private List<Entry> entries;

    /**
     * Creates empty bindings.
     * @param schema Validated child schema
     */
    public ChildBindings(final List<ChildDescriptor> schema) {
        this.schema = List.copyOf(schema);
        this.entries = new ArrayList<>(schema.size());
    }

    /**
     * Binds all children, discarding previous bindings only on success.
     * @param nodes Source children
     * @return Whether accepted
     */
    public boolean setChildren(final List<Node> nodes) {
        final Optional<List<List<Node>>> result = new NodeAllocator(this.schema).allocate(nodes);
        if (result.isPresent()) {
            final List<Entry> updated = new ArrayList<>(nodes.size());
            final List<List<Node>> slots = result.orElseThrow();
            for (int slot = 0; slot < slots.size(); slot = slot + 1) {
                for (final Node node : slots.get(slot)) {
                    updated.add(new Entry(slot, node));
                }
            }
            this.entries = ChildBindings.inSourceOrder(updated, nodes);
        }
        return result.isPresent();
    }

    /**
     * Sets a tagged position, appending new occurrences to insertion order.
     * @param tag Tag
     * @param node Child
     * @return Whether accepted
     */
    public boolean setChild(final String tag, final Node node) {
        Objects.requireNonNull(node);
        final int slot = this.findSlot(tag);
        boolean result = false;
        if (slot >= 0 && node.isSubtypeOf(this.schema.get(slot).getType())) {
            final int index = this.findEntry(slot);
            final Entry entry = new Entry(slot, node);
            if (index < 0) {
                this.entries.add(entry);
            } else {
                this.entries.set(index, entry);
            }
            result = true;
        }
        return result;
    }

    /**
     * Replaces a child in insertion order, retaining its binding.
     * @param index Current insertion index
     * @param node Child
     * @return Whether accepted
     */
    public boolean setChild(final int index, final Node node) {
        Objects.requireNonNull(node);
        boolean result = false;
        if (index >= 0 && index < this.entries.size()) {
            final int slot = this.entries.get(index).slot();
            if (node.isSubtypeOf(this.schema.get(slot).getType())) {
                this.entries.set(index, new Entry(slot, node));
                result = true;
            }
        }
        return result;
    }

    /**
     * Removes a tagged occurrence, even if required.
     * @param tag Tag
     * @return Whether removed
     */
    public boolean removeChild(final String tag) {
        return this.removeChild(this.findEntry(this.findSlot(tag)));
    }

    /**
     * Removes an occurrence in insertion order.
     * @param index Current index
     * @return Whether removed
     */
    public boolean removeChild(final int index) {
        final boolean result = index >= 0 && index < this.entries.size();
        if (result) {
            this.entries.remove(index);
        }
        return result;
    }

    /**
     * Checks required bindings without changing state.
     * @return Whether all required children are present
     */
    public boolean isValid() {
        boolean result = true;
        for (int slot = 0; slot < this.schema.size(); slot = slot + 1) {
            if (this.schema.get(slot).getCardinality() == Cardinality.REQUIRED
                && this.findEntry(slot) < 0) {
                result = false;
                break;
            }
        }
        return result;
    }

    /**
     * Returns a child by tag, including in incomplete state.
     * @param tag Tag
     * @return Child if present
     */
    public Optional<Node> getChild(final String tag) {
        final int index = this.findEntry(this.findSlot(tag));
        Optional<Node> result = Optional.empty();
        if (index >= 0) {
            result = Optional.of(this.entries.get(index).node());
        }
        return result;
    }

    /**
     * Takes a snapshot in schema order.
     * @return Immutable child list, without missing optional positions
     */
    public List<Node> getChildren() {
        return this.entries.stream()
            .sorted(java.util.Comparator.comparingInt(Entry::slot))
            .map(Entry::node)
            .toList();
    }

    /**
     * Restores source occurrence order without relying on node equality.
     * @param entries Allocated entries in schema order
     * @param nodes Original occurrences
     * @return Entries in source order
     */
    private static List<Entry> inSourceOrder(final List<Entry> entries, final List<Node> nodes) {
        final Map<Node, Deque<Entry>> queues = new IdentityHashMap<>();
        for (final Entry entry : entries) {
            queues.computeIfAbsent(entry.node(), key -> new ArrayDeque<>()).add(entry);
        }
        final List<Entry> result = new ArrayList<>(nodes.size());
        for (final Node node : nodes) {
            result.add(queues.get(node).removeFirst());
        }
        return result;
    }

    /**
     * Locates a tagged slot.
     * @param tag Tag
     * @return Slot or minus one
     */
    private int findSlot(final String tag) {
        Objects.requireNonNull(tag);
        int result = -1;
        for (int slot = 0; slot < this.schema.size(); slot = slot + 1) {
            if (this.schema.get(slot).getTag().filter(tag::equals).isPresent()) {
                result = slot;
                break;
            }
        }
        return result;
    }

    /**
     * Locates the occurrence of a single-valued slot.
     * @param slot Slot
     * @return Insertion index or minus one
     */
    private int findEntry(final int slot) {
        int result = -1;
        for (int index = 0; index < this.entries.size(); index = index + 1) {
            if (this.entries.get(index).slot() == slot) {
                result = index;
                break;
            }
        }
        return result;
    }

    /**
     * A child occurrence and its schema binding.
     * @param slot Schema position
     * @param node Child
     * @since 0.0.1
     */
    private record Entry(int slot, Node node) {
    }
}
