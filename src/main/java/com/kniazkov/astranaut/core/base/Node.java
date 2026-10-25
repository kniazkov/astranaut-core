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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The {@code Node} interface represents a node in an abstract syntax tree (AST).
 *  This is our foundational interface on which the entire platform is built.
 *  The interface contains a minimal number of non-default methods, making it
 *  easy to create custom nodes by requiring the implementation of only a few methods.
 *  Additionally, the interface is designed to allow for automatic generation
 *  of classes based on it.
 *  The interface is designed to be immutable, which ensures thread safety and
 *  consistency, as well as simplifying the debugging process.
 * Implementations must provide structural equals/hashCode: type, data, ordered
 * children and their tags are significant; properties and fragments are not.
 * Extend {@link NodeWithStructuralEquality} or delegate to {@link NodeStructure}.
 * Mixing implementations that retain Object equality violates this contract.
 * @since 0.0.1
 */
@SuppressWarnings("PMD.ExcessivePublicCount")
public interface Node {
    /**
     * Returns the fragment of source code that is associated with the node.
     * @return The fragment
     */
    default Fragment getFragment() {
        return EmptyFragment.INSTANCE;
    }

    /**
     * Returns the type of the node.
     * @return The type
     */
    Type getType();

    /**
     * Returns data associated with the node (in a textual format).
     * @return Node data or empty string
     */
    String getData();

    /**
     * Retrieves the properties associated with the node.
     *  The presence of specific properties depends on the specific node implementation
     *  and may vary. By default, returns the properties of the node type.
     * @return Immutable map of properties where keys are property names
     *  and values are property values
     */
    default Map<String, String> getProperties() {
        return this.getType().getProperties();
    }

    /**
     * Returns the number of children.
     * @return Child node count
     */
    int getChildCount();

    /**
     * Returns a child by its index.
     * @param index Child index
     * @return A node
     */
    Node getChild(int index);

    /**
     * Looks up an explicitly named child independently of the type schema.
     * @param tag Child tag
     * @return Child, or empty for an unknown tag or absent optional child
     */
    default Optional<Node> getChild(final String tag) {
        Objects.requireNonNull(tag);
        return Optional.empty();
    }

    /**
     * Returns the tag of a child occurrence, independently of the type schema.
     * The same child object may occur under different tags. A returned tag must
     * resolve through getChild(tag) to the child at this index.
     * @param index Index in the compact ordered child list
     * @return Tag, or empty for an untagged occurrence
     * @throws IndexOutOfBoundsException If the index is outside the child list
     */
    default Optional<String> getChildTag(final int index) {
        Objects.checkIndex(index, this.getChildCount());
        return Optional.empty();
    }

    /**
     * Returns the name of the type.
     * @return The name
     */
    default String getTypeName() {
        return this.getType().getName();
    }

    /**
     * Calculates the local hash of the node.
     *  Nodes with the same type and the same data (but possibly different children)
     *  will have the same local hash.
     * @return Local hash of the node
     */
    default int getLocalHash() {
        return this.getType().hashCode() * 31 + this.getData().hashCode();
    }

    /**
     * Checks whether this node has the requested type or one of its subtypes.
     * @param type Requested abstract or concrete type
     * @return Whether the node type is a subtype of the requested type
     */
    default boolean isSubtypeOf(final AbstractType type) {
        return this.getType().isSubtypeOf(type);
    }

    /**
     * Returns an immutable, non-null snapshot of the ordered child nodes.
     * @return The node list
     */
    default List<Node> getChildrenList() {
        final List<Node> children = new ArrayList<>(this.getChildCount());
        this.forEachChild(children::add);
        return List.copyOf(children);
    }

    /**
     * Returns an iterator over the child nodes of this node.
     * @return An {@link Iterator} over the child nodes
     */
    default Iterator<Node> getIteratorOverChildren() {
        return this.getChildrenList().iterator();
    }

    /**
     * Performs some action for each child node.
     * @param action An action
     */
    default void forEachChild(final Consumer<Node> action) {
        final int count = this.getChildCount();
        for (int index = 0; index < count; index = index + 1) {
            action.accept(this.getChild(index));
        }
    }

    /**
     * Performs a deep comparison of a node with another node,
     *  i.e., compares type, data, tags and children without recursive Java calls.
     * @param other Other node
     * @return Comparison result, {@code true} if the nodes are equal
     */
    default boolean deepCompare(final Node other) {
        return NodeStructure.equal(this, other);
    }

    /**
     * Creates a deep clone of the node, including all its children.
     * This method performs a deep cloning operation on the node. It first clones
     *  the node itself and then recursively clones all its children, ensuring that
     *  the entire subtree rooted at this node is cloned.
     * @return A deep clone of the node, including all its children
     */
    default Node deepClone() {
        final Builder builder = this.getType().createBuilder();
        builder.setFragment(this.getFragment());
        builder.setData(this.getData());
        final int count = this.getChildCount();
        final List<Node> list = new ArrayList<>(count);
        for (int index = 0; index < count; index = index + 1) {
            list.add(this.getChild(index).deepClone());
        }
        builder.setChildren(list);
        return builder.build();
    }

    /**
     * Converts the given {@link Node} to its string representation.
     * @param node Node to be converted to a string
     * @return String representation of the given node
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    static String toString(final Node node) {
        return DraftFormat.format(node);
    }

}
