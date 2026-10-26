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

import com.kniazkov.astranaut.core.base.Hole;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Pattern;
import com.kniazkov.astranaut.core.base.PatternNode;
import com.kniazkov.astranaut.core.base.PrototypeBasedNode;
import com.kniazkov.astranaut.core.base.Tree;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable pattern mining builder. Every published Pattern is an immutable snapshot.
 * @since 0.0.1
 */
public final class PatternBuilder {
    /**
     * Immutable baseline.
     */
    private final PatternNode root;

    /**
     * Selected hole slots by baseline node identity.
     */
    private final Map<Node, Integer> holes;

    /**
     * Snapshots the source tree.
     * @param tree Source tree
     * @throws IllegalArgumentException If empty
     */
    public PatternBuilder(final Tree tree) {
        this.root = new PatternNode(
            tree.getRoot().orElseThrow(
                () -> new IllegalArgumentException("Pattern mining requires a root")
            )
        );
        this.holes = new IdentityHashMap<>();
    }

    /**
     * Publishes an independent pattern, preserving actions and untouched subtrees.
     * @return Pattern snapshot
     */
    public Pattern getPattern() {
        final Map<Node, Node> done = new IdentityHashMap<>();
        final List<Node> nodes = new DepthFirstWalker(this.root).collectAll();
        for (final Node node : nodes.reversed()) {
            if (done.containsKey(node)) {
                continue;
            }
            if (this.holes.containsKey(node)) {
                done.put(node, new Hole(node, this.holes.get(node)));
            } else if (node instanceof PatternNode pattern) {
                done.put(
                    node, pattern.withChildren(
                        node.getChildrenList().stream().map(done::get).toList()
                    )
                );
            } else {
                done.put(node, node);
            }
        }
        return new Pattern(done.get(this.root));
    }

    /**
     * Selects every baseline occurrence of a source object, not structurally equal objects.
     * @param node Source object or one of its mining prototypes
     * @param number Non-negative slot
     * @return Whether any occurrence was selected
     * @throws IllegalArgumentException If the slot is negative
     */
    public boolean makeHole(final Node node, final int number) {
        if (number < 0) {
            throw new IllegalArgumentException("Negative hole slot");
        }
        final List<Node> found = new DepthFirstWalker(this.root).findAll(
            candidate -> candidate instanceof PatternNode
                && PatternBuilder.isPrototype(candidate, node)
        );
        found.forEach(candidate -> this.holes.put(candidate, number));
        return !found.isEmpty();
    }

    /**
     * Follows mining wrappers by reference identity.
     * @param candidate Baseline node
     * @param target Requested source object
     * @return Whether the prototype chain contains the target
     */
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static boolean isPrototype(final Node candidate, final Node target) {
        Node current = candidate;
        while (current != target && current instanceof PrototypeBasedNode prototype) {
            current = prototype.getPrototype();
        }
        return current == target;
    }
}
