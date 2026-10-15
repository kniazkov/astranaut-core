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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Structural operations for valid acyclic Node graphs. Properties, source
 * fragments, implementation classes and sharing topology are not significant.
 * State is invocation-local and keyed by reference identity, never Node.hashCode.
 * Nodes must remain unchanged for the duration of a call.
 * @since 0.0.1
 */
@SuppressWarnings("PMD.ProhibitPublicStaticMethods")
public final class NodeStructure {
    /**
     * Utility class.
     */
    private NodeStructure() {
    }

    /**
     * Compares type and data only, without treating hash equality as evidence.
     * @param left Left node
     * @param right Right node
     * @return Whether local structure matches
     */
    public static boolean locallyEqual(final Node left, final Node right) {
        return left.getType().equals(right.getType()) && left.getData().equals(right.getData());
    }

    /**
     * Compares whole structures iteratively, memoizing pairs of object identities.
     * @param left Left node
     * @param right Right node, possibly null
     * @return Whether structures match
     */
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public static boolean equal(final Node left, final Node right) {
        Objects.requireNonNull(left);
        boolean result = right != null;
        final Deque<NodePair> pending = new ArrayDeque<>();
        final Map<Node, Set<Node>> visited = new IdentityHashMap<>();
        if (result) {
            pending.push(new NodePair(left, right));
        }
        while (result && !pending.isEmpty()) {
            final NodePair pair = pending.pop();
            if (pair.left() != pair.right() && NodeStructure.unvisited(pair, visited)) {
                result = NodeStructure.sameHeader(pair.left(), pair.right());
                for (int index = 0; result && index < pair.left().getChildCount(); index += 1) {
                    pending.push(
                        new NodePair(pair.left().getChild(index), pair.right().getChild(index))
                    );
                }
            }
        }
        return result;
    }

    /**
     * Checks local data, child count and occurrence tags.
     * @param left Left node
     * @param right Right node
     * @return Whether headers match
     */
    public static boolean sameHeader(final Node left, final Node right) {
        boolean result = NodeStructure.locallyEqual(left, right)
            && left.getChildCount() == right.getChildCount();
        for (int index = 0; result && index < left.getChildCount(); index += 1) {
            result = left.getChildTag(index).equals(right.getChildTag(index));
        }
        return result;
    }

    /**
     * Calculates the full structural hash without recursion or a persistent cache.
     * @param node Root
     * @return Structural hash
     */
    public static int hash(final Node node) {
        return NodeStructure.hashes(node).get(node);
    }

    /**
     * Computes an immutable identity-keyed snapshot for one algorithm invocation.
     * Discard it after the operation; changing a legacy mutable node invalidates it.
     * @param node Root
     * @return Hashes for reachable objects, without retaining any global state
     */
    public static Map<Node, Integer> hashes(final Node node) {
        return new StructuralHash().calculate(Objects.requireNonNull(node));
    }

    /**
     * Starts an ordered structural hash.
     * @param node Node
     * @return Header hash
     */
    public static int startHash(final Node node) {
        return 31 * (31 * node.getType().hashCode() + node.getData().hashCode())
            + node.getChildCount();
    }

    /**
     * Hashes a tag including its presence, so absent and empty tags differ.
     * @param node Parent
     * @param index Child index
     * @return Occurrence tag hash
     */
    public static int tagHash(final Node node, final int index) {
        return node.getChildTag(index).map(tag -> 31 + tag.hashCode()).orElse(0);
    }

    /**
     * Memoizes only branching pairs; leaf pairs cannot cause repeated descent.
     * @param pair Pair
     * @param visited Identity-keyed visited pairs
     * @return Whether to compare this pair
     */
    private static boolean unvisited(final NodePair pair, final Map<Node, Set<Node>> visited) {
        boolean result = true;
        if (pair.left().getChildCount() > 0) {
            final Set<Node> matches = visited.computeIfAbsent(
                pair.left(), key -> Collections.newSetFromMap(new IdentityHashMap<>())
            );
            result = matches.add(pair.right());
        }
        return result;
    }

    /**
     * Comparison work item; record equality is never used by the algorithm.
     * @param left Left node
     * @param right Right node
     * @since 0.0.1
     */
    private record NodePair(Node left, Node right) {
    }
}
