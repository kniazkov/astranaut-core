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

import com.kniazkov.astranaut.core.base.ConstructionException;
import com.kniazkov.astranaut.core.base.DiffTree;
import com.kniazkov.astranaut.core.base.EmptyTree;
import com.kniazkov.astranaut.core.base.Insertion;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.NodeOccurrence;
import com.kniazkov.astranaut.core.base.Tree;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Mutable editor publishing immutable reversible snapshots. Occurrence edits address the
 * current after-state; stale addresses refuse. Legacy object edits require a unique occurrence.
 * Every edit validates all ancestors before publication; false leaves state unchanged.
 * The overloads retain legacy object edits alongside explicit occurrence edits.
 * @since 0.0.1
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class DiffTreeBuilder {
    /**
     * Original immutable tree.
     */
    private final Tree original;

    /**
     * Identity bookkeeping for legacy object-based edits.
     */
    private final LegacyDiffHandles handles;

    /**
     * Current valid after-state.
     */
    private Tree current;

    /**
     * Last published immutable difference.
     */
    private DiffTree difference;

    /**
     * Legacy insertion tails, preserving repeated insertion order at the same anchor.
     */
    private final Map<Node, Map<Node, Node>> tails;

    /**
     * Starts editing a nonempty tree.
     * @param before Original root
     */
    public DiffTreeBuilder(final Node before) {
        this(new Tree(before));
    }

    /**
     * Starts editing either root state.
     * @param before Original tree
     */
    public DiffTreeBuilder(final Tree before) {
        this.handles = new LegacyDiffHandles(Objects.requireNonNull(before));
        this.current = this.handles.prepare(before);
        this.original = this.current;
        this.difference = new PositionalDiff().compare(this.current, this.current);
        this.tails = new IdentityHashMap<>();
    }

    /**
     * Computes and validates a strategy result before replacing builder state.
     * @param after Target root
     * @param strategy Difference strategy
     * @return Whether both projections match their endpoints
     */
    public boolean build(final Node after, final DiffStrategy strategy) {
        return this.build(new Tree(after), strategy);
    }

    /**
     * Computes a reversible diff, including empty states. Unexpected failures propagate.
     * @param after Target state
     * @param strategy Difference strategy
     * @return False on construction refusal or an incorrect strategy result
     */
    public boolean build(final Tree after, final DiffStrategy strategy) {
        Objects.requireNonNull(after);
        boolean accepted = false;
        try {
            final DiffTree next = strategy.compare(this.original, after);
            if (this.original.deepCompare(
                next.getBefore()
            ) && after.deepCompare(
                next.getAfter()
            )) {
                this.current = after;
                this.difference = next;
                accepted = true;
            }
        } catch (final ConstructionException ignored) {
            accepted = false;
        }
        return accepted;
    }

    /**
     * Returns the current immutable state for obtaining occurrence addresses.
     * @return Current after-state
     */
    public Tree getCurrentTree() {
        return this.current;
    }

    /**
     * Returns a stable snapshot which later edits cannot change.
     * @return Difference tree
     */
    public DiffTree getDiffTree() {
        return this.difference;
    }

    /**
     * Replaces one current occurrence, including the root.
     * @param address Current occurrence
     * @param replacement New valid subtree
     * @return Whether accepted
     */
    public boolean replaceNode(final NodeOccurrence address, final Node replacement) {
        boolean accepted = false;
        if (this.belongs(address)) {
            final Optional<Node> root = new OccurrenceReplacer().replace(address, replacement);
            if (root.isPresent()) {
                accepted = this.commit(new Tree(root.orElseThrow()), address.getParent());
            }
        }
        return accepted;
    }

    /**
     * Deletes one current occurrence, with deletion of the root yielding an empty tree.
     * @param address Current occurrence
     * @return Whether accepted by every ancestor
     */
    public boolean deleteNode(final NodeOccurrence address) {
        boolean accepted = false;
        if (this.belongs(address)) {
            if (address.getParent().isEmpty()) {
                accepted = this.commit(EmptyTree.INSTANCE, Optional.empty());
            } else {
                final NodeOccurrence parent = address.getParent().orElseThrow();
                final List<Node> children = new ArrayList<>(parent.getNode().getChildrenList());
                final List<Optional<String>> tags = DiffTreeBuilder.tags(parent.getNode());
                children.remove(address.getIndex());
                tags.remove(address.getIndex());
                accepted = this.change(
                    parent, new OccurrenceReplacer.Content(
                    parent.getNode().getData(), children, tags
                )
                );
            }
        }
        return accepted;
    }

    /**
     * Inserts an untagged child at a current boundary, before index or at the end.
     * Named layout changes can instead replace the whole parent with a valid new node.
     * @param parent Current parent occurrence
     * @param index Current child boundary
     * @param node Inserted valid subtree
     * @return Whether accepted
     */
    public boolean insertNode(final NodeOccurrence parent, final int index, final Node node) {
        Objects.requireNonNull(node);
        boolean accepted = false;
        if (this.belongs(parent) && index >= 0 && index <= parent.getNode().getChildCount()) {
            final List<Node> children = new ArrayList<>(parent.getNode().getChildrenList());
            final List<Optional<String>> tags = DiffTreeBuilder.tags(parent.getNode());
            children.add(index, node);
            tags.add(index, Optional.empty());
            accepted = this.change(
                parent, new OccurrenceReplacer.Content(parent.getNode().getData(), children, tags)
            );
        }
        return accepted;
    }

    /**
     * Legacy object-based replacement; shared/ambiguous references refuse.
     * @param node Unique source object
     * @param replacement New subtree
     * @return Whether accepted
     */
    public boolean replaceNode(final Node node, final Node replacement) {
        return this.handles.find(
            this.current, node
        ).map(
            address -> this.replaceNode(address, replacement)
        ).orElse(
            false
        );
    }

    /**
     * Legacy object-based deletion; shared/ambiguous references refuse.
     * @param node Unique source object
     * @return Whether accepted
     */
    public boolean deleteNode(final Node node) {
        return this.handles.find(this.current, node).map(this::deleteNode).orElse(false);
    }

    /**
     * Legacy ordered insertion. Repeated calls at one anchor retain declaration order.
     * @param insertion Insertion command
     * @return Whether a unique valid target exists and accepts the child
     */
    public boolean insertNode(final Insertion insertion) {
        final Map<Node, Node> anchors = this.tails.getOrDefault(
            insertion.getInto(), new IdentityHashMap<>()
        );
        final Node after = anchors.getOrDefault(insertion.getAfter(), insertion.getAfter());
        Optional<NodeOccurrence> parent = Optional.empty();
        if (insertion.getInto() != null) {
            parent = this.handles.find(this.current, insertion.getInto());
        } else if (after != null) {
            parent = this.handles.find(this.current, after).flatMap(NodeOccurrence::getParent);
        }
        boolean accepted = false;
        if (parent.isPresent()) {
            final NodeOccurrence address = parent.orElseThrow();
            final int index = LegacyDiffHandles.boundary(address.getNode(), after);
            if (index >= 0) {
                accepted = this.insertNode(address, index, insertion.getNode());
            }
        }
        if (accepted) {
            this.tails.computeIfAbsent(insertion.getInto(), key -> new IdentityHashMap<>())
                .put(insertion.getAfter(), insertion.getNode());
        }
        return accepted;
    }

    /**
     * Applies one changed child sequence and validates all ancestors.
     * @param parent Current parent
     * @param content Desired children and tags
     * @return Whether committed
     */
    private boolean change(final NodeOccurrence parent, final OccurrenceReplacer.Content content) {
        final Optional<Node> changed = OccurrenceReplacer.rebuild(parent.getNode(), content);
        boolean accepted = false;
        if (changed.isPresent()) {
            final Optional<Node> root = new OccurrenceReplacer().replace(
                parent, changed.orElseThrow()
            );
            if (root.isPresent()) {
                accepted = this.commit(new Tree(root.orElseThrow()), Optional.of(parent));
            }
        }
        return accepted;
    }

    /**
     * Commits valid endpoints and remembers rebuilt ancestor handles for legacy callers.
     * @param next Candidate state
     * @param parent Deepest rebuilt ancestor
     * @return Whether the whole diff was accepted
     */
    private boolean commit(final Tree next, final Optional<NodeOccurrence> parent) {
        final boolean accepted = this.build(next, new PositionalDiff());
        if (accepted) {
            this.handles.track(next, parent);
        }
        return accepted;
    }

    /**
     * Rejects foreign or stale root identities.
     * @param address Address
     * @return Whether it belongs to the current snapshot
     */
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private boolean belongs(final NodeOccurrence address) {
        NodeOccurrence root = Objects.requireNonNull(address);
        while (root.getParent().isPresent()) {
            root = root.getParent().orElseThrow();
        }
        return this.current.getRoot().isPresent()
            && root.getNode() == this.current.getRoot().orElseThrow();
    }

    /**
     * Captures mutable working tags for one parent.
     * @param node Parent
     * @return Tags
     */
    private static List<Optional<String>> tags(final Node node) {
        return new ArrayList<>(
            java.util.stream.IntStream.range(0, node.getChildCount())
            .mapToObj(node::getChildTag).toList()
        );
    }

}
