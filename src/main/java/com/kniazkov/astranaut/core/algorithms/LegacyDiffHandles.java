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

import com.kniazkov.astranaut.core.base.DiffNode;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeOccurrence;
import com.kniazkov.astranaut.core.base.PatternItem;
import com.kniazkov.astranaut.core.base.PrototypeBasedNode;
import com.kniazkov.astranaut.core.base.Tree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Identity handles for the legacy object-based difference editor.
 * @since 0.0.1
 */
final class LegacyDiffHandles {
    /**
     * Originally ambiguous source handles.
     */
    private final Set<Node> ambiguous;

    /**
     * Old ancestor identities mapped to their current replacements.
     */
    private final Map<Node, Node> ancestors;

    /**
     * Captures ambiguous source identities before any edit.
     * @param source Original tree
     */
    LegacyDiffHandles(final Tree source) {
        this.ambiguous = LegacyDiffHandles.findAmbiguous(source);
        this.ancestors = new IdentityHashMap<>();
    }

    /**
     * Records rebuilt ancestors after a successful edit.
     * @param next New state
     * @param parent Deepest rebuilt ancestor
     */
    void track(final Tree next, final Optional<NodeOccurrence> parent) {
        if (parent.isPresent()) {
            final List<NodeOccurrence> chain = new ArrayList<>(
                parent.orElseThrow().getDepth() + 1
            );
            Optional<NodeOccurrence> cursor = parent;
            while (cursor.isPresent()) {
                chain.add(cursor.orElseThrow());
                cursor = cursor.orElseThrow().getParent();
            }
            Node node = next.getRoot().orElseThrow();
            for (final NodeOccurrence address : chain.reversed()) {
                if (address.getDepth() > 0) {
                    node = node.getChild(address.getIndex());
                }
                this.remember(address.getNode(), node);
            }
        }
    }

    /**
     * Materializes legacy prototype views whose builders require concrete child classes.
     * Ordinary immutable inputs retain identity. Records aliases for legacy object edits.
     * @param source Source tree
     * @return Valid concrete current state
     */
    Tree prepare(final Tree source) {
        Tree result = source;
        if (source.getRoot().isPresent()
            && LegacyDiffHandles.view(source.getRoot().orElseThrow())) {
            result = new DiffProjection(false).project(source);
            final List<Node> before = new DepthFirstWalker(
                source.getRoot().orElseThrow()
            ).collectAll();
            final List<Node> after = new DepthFirstWalker(
                result.getRoot().orElseThrow()
            ).collectAll();
            for (int index = 0; index < before.size(); index += 1) {
                Node node = before.get(index);
                this.ancestors.put(node, after.get(index));
                while (node instanceof PrototypeBasedNode wrapper) {
                    node = wrapper.getPrototype();
                    this.ancestors.put(node, after.get(index));
                }
            }
        }
        return result;
    }

    /**
     * Finds exactly one occurrence, following legacy rebuilt-ancestor handles.
     * @param current Current tree
     * @param node Requested object
     * @return Unique current address
     */
    Optional<NodeOccurrence> find(final Tree current, final Node node) {
        Objects.requireNonNull(node);
        final Node target = this.ancestors.getOrDefault(node, node);
        Optional<NodeOccurrence> result = Optional.empty();
        if (!this.ambiguous.contains(node) && current.getRoot().isPresent()) {
            final List<NodeOccurrence> found = new OccurrenceWalker(
                new NodeOccurrence(current.getRoot().orElseThrow())
            ).findAll(
                address -> LegacyDiffHandles.matches(address.getNode(), node)
                || LegacyDiffHandles.matches(address.getNode(), target)
            );
            if (found.size() == 1) {
                result = Optional.of(found.getFirst());
            }
        }
        return result;
    }

    /**
     * Resolves a unique direct-child boundary using identity, never structural equality.
     * @param parent Parent
     * @param after Anchor or null for the beginning
     * @return Boundary or minus one if missing/ambiguous
     */
    static int boundary(final Node parent, final Node after) {
        int boundary = 0;
        if (after != null) {
            final int[] matches = java.util.stream.IntStream.range(0, parent.getChildCount())
                .filter(index -> LegacyDiffHandles.matches(parent.getChild(index), after))
                .limit(2).toArray();
            boundary = -1;
            if (matches.length == 1) {
                boundary = matches[0] + 1;
            }
        }
        return boundary;
    }

    /**
     * Updates legacy ancestor handles without creating identity cycles after an undo.
     * @param previous Old ancestor
     * @param next New ancestor
     */
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private void remember(final Node previous, final Node next) {
        this.ancestors.replaceAll(
            (key, value) -> {
                Node result = value;
                if (value == previous) {
                    result = next;
                }
                return result;
            }
        );
        if (previous != next) {
            this.ancestors.put(previous, next);
        }
    }

    /**
     * Identifies ambiguous legacy handles, including prototypes of wrapper occurrences.
     * @param source Input tree
     * @return Identity-based ambiguous handles
     */
    private static Set<Node> findAmbiguous(final Tree source) {
        final Set<Node> result = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Node> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        if (source.getRoot().isPresent()) {
            final List<Node> nodes = new DepthFirstWalker(source.getRoot().orElseThrow())
                .collectAll();
            for (final Node item : nodes) {
                Node node = item;
                while (true) {
                    if (!seen.add(node)) {
                        result.add(node);
                    }
                    if (!(node instanceof PrototypeBasedNode wrapper)) {
                        break;
                    }
                    node = wrapper.getPrototype();
                }
            }
        }
        return result;
    }

    /**
     * Distinguishes legacy ordinary views from literal pattern/difference inputs.
     * @param node Source root
     * @return Whether concrete materialization is needed for typed builders
     */
    private static boolean view(final Node node) {
        return node instanceof PrototypeBasedNode && !(node instanceof PatternItem)
            && !(node instanceof DiffNode);
    }

    /**
     * Matches legacy source identity through immutable mining wrappers.
     * @param candidate Current node
     * @param target Source object
     * @return Whether it represents this exact object
     */
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static boolean matches(final Node candidate, final Node target) {
        Node current = candidate;
        while (current != target && current instanceof PrototypeBasedNode wrapper) {
            current = wrapper.getPrototype();
        }
        return current == target;
    }
}
