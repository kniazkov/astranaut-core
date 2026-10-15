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

import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeStructure;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds distinct node objects with matching type and nonempty data.
 * Children are not part of this local grouping; hashes only select candidates.
 * Returned sets use reference identity to retain structurally equal objects.
 * @since 0.0.1
 */
public final class IdenticalNodeFinder {
    /**
     * Search root.
     */
    private final Node root;

    /**
     * Creates a finder.
     * @param root Root
     */
    public IdenticalNodeFinder(final Node root) {
        this.root = root;
    }

    /**
     * Groups locally equal nodes, without merging hash collisions.
     * @return Identity sets of matching objects
     */
    public Set<Set<Node>> find() {
        final Map<Integer, List<Set<Node>>> buckets = new HashMap<>();
        final Set<Node> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        final Deque<Node> pending = new ArrayDeque<>();
        pending.push(this.root);
        while (!pending.isEmpty()) {
            final Node node = pending.pop();
            if (visited.add(node)) {
                IdenticalNodeFinder.group(node, buckets);
                node.forEachChild(pending::push);
            }
        }
        final Set<Set<Node>> result = Collections.newSetFromMap(new IdentityHashMap<>());
        for (final List<Set<Node>> bucket : buckets.values()) {
            for (final Set<Node> group : bucket) {
                if (group.size() > 1) {
                    result.add(Collections.unmodifiableSet(group));
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Confirms candidates and adds the object to its local structural group.
     * @param node Node
     * @param buckets Hash buckets
     */
    private static void group(final Node node, final Map<Integer, List<Set<Node>>> buckets) {
        if (!node.getData().isEmpty()) {
            final List<Set<Node>> groups = buckets.computeIfAbsent(
                node.getLocalHash(), key -> new ArrayList<>(1)
            );
            Set<Node> found = Collections.newSetFromMap(new IdentityHashMap<>());
            for (final Set<Node> group : groups) {
                if (NodeStructure.locallyEqual(node, group.iterator().next())) {
                    found = group;
                    break;
                }
            }
            if (found.isEmpty()) {
                groups.add(found);
            }
            found.add(node);
        }
    }
}
