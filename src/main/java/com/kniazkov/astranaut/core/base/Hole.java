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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable node-sequence capture. Its constraint is distinct from its service type.
 * @since 0.0.1
 */
public final class Hole extends NodeWithStructuralEquality
    implements PatternItem, PrototypeBasedNode {
    /**
     * Independent service descriptor.
     */
    private final HoleType type;

    /**
     * Prototype used only for mining provenance and legacy serialization.
     */
    private final Node prototype;

    /**
     * Capture slot.
     */
    private final int number;

    /**
     * Immutable presentation properties.
     */
    private final Map<String, String> properties;

    /**
     * Captures any node.
     * @param number Slot
     */
    public Hole(final int number) {
        this(DraftNode.create("Any"), Optional.empty(), number);
    }

    /**
     * Captures a type or any subtype, including abstract constraints.
     * @param constraint Accepted type
     * @param number Slot
     */
    public Hole(final AbstractType constraint, final int number) {
        this(DraftNode.create("Prototype"), Optional.of(constraint), number);
    }

    /**
     * Mines a typed hole from a prototype.
     * @param prototype Original node
     * @param number Slot
     */
    public Hole(final Node prototype, final int number) {
        this(prototype, Optional.of(prototype.getType()), number);
    }

    /**
     * Publishes a validated capture.
     * @param prototype Provenance
     * @param constraint Constraint
     * @param number Slot
     */
    private Hole(final Node prototype, final Optional<AbstractType> constraint, final int number) {
        this.prototype = Objects.requireNonNull(prototype);
        this.type = new HoleType(constraint);
        this.number = Hole.validNumber(number);
        this.properties = Hole.snapshotProperties(prototype);
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public Fragment getFragment() {
        return EmptyFragment.INSTANCE;
    }

    @Override
    public String getData() {
        return Integer.toString(this.number);
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public Node getChild(final int index) {
        return List.<Node>of().get(index);
    }

    @Override
    public Map<String, String> getProperties() {
        return this.properties;
    }

    /**
     * Returns the capture slot.
     * @return Slot
     */
    public int getNumber() {
        return this.number;
    }

    /**
     * Returns the optional subtype constraint.
     * @return Accepted type, or any node
     */
    public Optional<AbstractType> getConstraint() {
        return this.type.constraint;
    }

    /**
     * Tests the constraint without comparing service types.
     * @param node Candidate
     * @return Whether accepted
     */
    public boolean accepts(final Node node) {
        return this.type.constraint.map(node::isSubtypeOf).orElse(true);
    }

    @Override
    public String toString() {
        return Node.toString(this);
    }

    @Override
    public Node getPrototype() {
        return this.prototype;
    }

    /**
     * Validates a capture slot.
     * @param number Slot
     * @return Same slot
     * @throws IllegalArgumentException If negative
     */
    private static int validNumber(final int number) {
        if (number < 0) {
            throw new IllegalArgumentException("Negative capture slot");
        }
        return number;
    }

    /**
     * Copies presentation metadata.
     * @param prototype Source
     * @return Snapshot
     */
    private static Map<String, String> snapshotProperties(final Node prototype) {
        final Map<String, String> result = new HashMap<>(prototype.getProperties());
        result.put("color", "purple");
        return Map.copyOf(result);
    }

    /**
     * Descriptor retains types, never captured nodes.
     * @since 0.0.1
     */
    private static final class HoleType extends TypeWithNameBasedEquality implements Type {
        /**
         * Constraint.
         */
        private final Optional<AbstractType> constraint;

        /**
         * Creates a service descriptor.
         * @param constraint Accepted type
         */
        private HoleType(final Optional<AbstractType> constraint) {
            this.constraint = constraint;
        }

        @Override
        public String getName() {
            return this.constraint.map(value -> "AstranautTypedHole:".concat(value.getName()))
                .orElse("AstranautHole");
        }

        @Override
        public Optional<List<ChildDescriptor>> getChildDescriptors() {
            return Optional.of(List.of());
        }

        @Override
        public Builder createBuilder() {
            return new Constructor(this);
        }
    }

    /**
     * Independent capture builder.
     * @since 0.0.1
     */
    private static final class Constructor implements Builder {
        /**
         * Descriptor.
         */
        private final HoleType type;

        /**
         * Slot, defaulting to zero.
         */
        private int number;

        /**
         * Creates the builder.
         * @param type Descriptor
         */
        private Constructor(final HoleType type) {
            this.type = type;
        }

        @Override
        public void setFragment(final Fragment fragment) {
            Objects.requireNonNull(fragment);
        }

        @Override
        public boolean setData(final String value) {
            boolean valid = false;
            try {
                final int parsed = Integer.parseInt(value);
                valid = parsed >= 0;
                if (valid) {
                    this.number = parsed;
                }
            } catch (final NumberFormatException ignored) {
                valid = false;
            }
            return valid;
        }

        @Override
        public boolean setChildren(final List<Node> nodes) {
            return List.copyOf(nodes).isEmpty();
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Node build() {
            return new Hole(DraftNode.create("Prototype"), this.type.constraint, this.number);
        }
    }
}
