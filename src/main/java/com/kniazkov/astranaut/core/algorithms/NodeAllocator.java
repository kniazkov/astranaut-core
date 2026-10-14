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
import com.kniazkov.astranaut.core.base.Builder;
import com.kniazkov.astranaut.core.base.Cardinality;
import com.kniazkov.astranaut.core.base.ChildDescriptor;
import com.kniazkov.astranaut.core.base.DummyNode;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Allocates nodes to positions based on a list of descriptors.
 * This is used when creating nodes with a well-defined order of child nodes,
 *  meaning that a child node of a specific type must be in its "correct" position.
 *  However, the initial set of child nodes may be unordered or incomplete.
 *  The {@link Builder} interface, which is used to create new nodes, allows an arbitrary list
 *  to be set as the list of child nodes. Therefore, techniques such as this algorithm
 *  are needed to allocate these nodes to the correct "slots" if necessary.
 *  This algorithm is well-suited for most cases.
 * @since 0.0.1
 */
public class NodeAllocator {
    /**
     * The list of child descriptors.
     */
    private final List<ChildDescriptor> descriptors;

    /**
     * Required positions (must be filled in).
     */
    private PositionSet required;

    /**
     * All possible positions.
     */
    private PositionSet possible;

    /**
     * Number of unused cells of each type.
     */
    private Map<AbstractType, Integer> unused;

    /**
     * The appropriate types from the descriptors for each node.
     */
    private Map<Node, AbstractType> suitable;

    /**
     * Constructor.
     * @param descriptors Validated list of child descriptors
     * @throws IllegalArgumentException If a descriptor has repeated cardinality
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    public NodeAllocator(final List<ChildDescriptor> descriptors) {
        this.descriptors = List.copyOf(descriptors);
        if (this.descriptors.stream()
            .anyMatch(item -> item.getCardinality() == Cardinality.REPEATED)) {
            throw new IllegalArgumentException("Repeated allocation is not supported yet");
        }
    }

    /**
     * Maps the list of nodes by positions.
     *  The algorithm fills an array in which each node is placed at a suitable position for it.
     * @param destination The array in which each node is placed at a suitable position
     * @param source The source list of nodes
     * @return Mapping result, {@code true} if such a mapping is possible (array was filled)
     * @throws IllegalArgumentException If the destination array is too small
     */
    public boolean allocate(final Node[] destination, final List<Node> source) {
        boolean result = false;
        final int capacity = this.descriptors.size();
        final int count = source.size();
        if (capacity == 0 && count == 0) {
            result = true;
        } else if (destination.length < capacity) {
            throw new IllegalArgumentException();
        } else if (capacity >= count) {
            this.required = new PositionSet(false);
            this.required.init();
            if (count >= this.required.getCount()) {
                result = this.fullMapping(destination, source);
            }
        }
        return result;
    }

    /**
     * Full mapper that performs matching in two passes.
     * @param destination The array in which each node is placed at a suitable position
     * @param source The source list of nodes
     * @return Mapping result, {@code true} if such a mapping is possible (array was filled)
     */
    private boolean fullMapping(final Node[] destination, final List<Node> source) {
        boolean result;
        do {
            result = this.calculate(source);
            if (!result) {
                break;
            }
            this.possible = new PositionSet(true);
            this.possible.init();
            final Node[] array = new Node[source.size()];
            source.toArray(array);
            final int unprocessed = this.bindAllUniqueNodes(destination, array);
            if (unprocessed < 0) {
                result = false;
                break;
            } else if (unprocessed == 0) {
                result = this.required.getCount() == 0;
                break;
            }
            result = this.bindAllNodes(destination, array);
        } while (false);
        return result;
    }

    /**
     * Counts the number of nodes of each type and calculates the suitable type descriptors.
     * @param nodes The list of nodes
     * @return Calculation result, {@code true} if structures have been filled
     */
    private boolean calculate(final List<Node> nodes) {
        boolean result = true;
        this.unused = new HashMap<>();
        this.suitable = new HashMap<>();
        final PositionSet set = new PositionSet(true);
        set.init();
        for (final Node node : nodes) {
            final Optional<AbstractType> match = set.findSuitableBaseType(node);
            if (match.isEmpty()) {
                result = false;
                break;
            }
            final AbstractType type = match.orElseThrow();
            final Integer count = this.unused.computeIfAbsent(type, x -> 0);
            this.unused.put(type, count + 1);
            this.suitable.put(node, type);
        }
        return result;
    }

    /**
     * Binds all nodes, the type of which occurs once.
     * @param destination Array in which each node is placed at a suitable position
     * @param source Array of source nodes
     * @return Number of unprocessed nodes, or -1 if binding failed
     */
    private int bindAllUniqueNodes(final Node[] destination, final Node... source) {
        int count = source.length;
        for (int index = 0; index < source.length; index = index + 1) {
            final Node node = source[index];
            final AbstractType type = this.suitable.get(node);
            if (this.unused.get(type) == 1) {
                count = count - 1;
                List<Integer> positions = this.required.getPositionsByType(type);
                if (positions == null) {
                    positions = this.possible.getPositionsByType(type);
                }
                if (positions.size() > 1) {
                    count = -1;
                    break;
                }
                final Integer position = positions.get(0);
                destination[position] = node;
                source[index] = DummyNode.INSTANCE;
                this.required.removePosition(type, position);
                this.possible.removePosition(type, position);
                this.unused.put(type, 0);
            }
        }
        return count;
    }

    /**
     * Binds all nodes (given their order).
     * @param destination Array in which each node is placed at a suitable position
     * @param source Array of source nodes
     * @return Binding result, {@code true} if all nodes were bind
     */
    private boolean bindAllNodes(final Node[] destination, final Node... source) {
        boolean result = true;
        for (final Node node : source) {
            if (!(node instanceof DummyNode)) {
                final AbstractType type = this.suitable.get(node);
                final List<Integer> allowed = this.possible.getPositionsByType(type);
                if (allowed == null) {
                    result = false;
                    break;
                }
                final int unprocessed = this.unused.get(type);
                final List<Integer> obligatory = this.required.getPositionsByType(type);
                final Integer position;
                if (obligatory == null && allowed.size() > 1) {
                    result = false;
                    break;
                } else if (obligatory == null || unprocessed > obligatory.size()) {
                    position = allowed.get(0);
                } else  {
                    position = obligatory.get(0);
                }
                destination[position] = node;
                this.required.removePosition(type, position);
                this.possible.removePosition(type, position);
                this.unused.put(type, unprocessed - 1);
            }
        }
        return result;
    }

    /**
     * The set of types with a relationship of possible position for a node of each such type.
     * @since 0.0.1
     */
    private final class PositionSet {
        /**
         * Also consider optional items.
         */
        private final boolean optional;

        /**
         * The set of positions arranged by type.
         */
        private Map<AbstractType, List<Integer>> positions;

        /**
         * The number of positions.
         */
        private int count;

        /**
         * Constructor.
         * @param optional Also consider optional items
         */
        private PositionSet(final boolean optional) {
            this.optional = optional;
        }

        /**
         * Returns the number of positions.
         * @return The number of positions
         */
        public int getCount() {
            return this.count;
        }

        /**
         * Returns list of positions by type.
         * @param type The type descriptor
         * @return The list of positions
         */
        public List<Integer> getPositionsByType(final AbstractType type) {
            this.init();
            return this.positions.get(type);
        }

        /**
         * Looks for suitable base type that can be mapped.
         * @param node The node
         * @return Base type, or empty if the node cannot be mapped
         */
        public Optional<AbstractType> findSuitableBaseType(final Node node) {
            Optional<AbstractType> result = Optional.empty();
            final Type type = node.getType();
            if (this.positions.containsKey(type)) {
                result = Optional.of(type);
            } else {
                for (final AbstractType group : this.positions.keySet()) {
                    if (node.isSubtypeOf(group)) {
                        result = Optional.of(group);
                        this.removeFirstPosition(group);
                        break;
                    }
                }
            }
            return result;
        }

        /**
         * Removes position from the set.
         * @param type The type descriptor
         * @param index The position index
         */
        public void removePosition(final AbstractType type, final Integer index) {
            final List<Integer> list = this.positions.get(type);
            if (list != null && list.remove(index)) {
                this.count = this.count - 1;
                if (list.isEmpty()) {
                    this.positions.remove(type);
                }
            }
        }

        /**
         * Initializes the set of positions.
         */
        public void init() {
            if (this.positions == null) {
                this.positions = new TreeMap<>(Comparator.comparing(AbstractType::getName));
                this.count = 0;
                int index = 0;
                for (final ChildDescriptor descriptor : NodeAllocator.this.descriptors) {
                    boolean allowed = true;
                    if (!this.optional && descriptor.getCardinality() == Cardinality.OPTIONAL) {
                        allowed = false;
                    }
                    if (allowed) {
                        final AbstractType type = descriptor.getType();
                        final List<Integer> indexes = this.positions.computeIfAbsent(
                            type, x -> new ArrayList<>(2)
                        );
                        indexes.add(index);
                        this.count = this.count + 1;
                    }
                    index = index + 1;
                }
            }
        }

        /**
         * Removes first position from the set for the specified type.
         * @param type The type descriptor
         */
        private void removeFirstPosition(final AbstractType type) {
            final List<Integer> list = this.positions.get(type);
            list.remove(0);
            if (list.isEmpty()) {
                this.positions.remove(type);
            }
        }
    }
}
