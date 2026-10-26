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

import com.kniazkov.astranaut.core.algorithms.conversion.Extracted;
import com.kniazkov.astranaut.core.base.Hole;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.PatternOperator;
import java.util.List;
import java.util.Optional;

/**
 * Stateless transactional matching of immutable patterns. Every attempt has its own
 * captures; a shared matcher can be used concurrently. Repetition is greedy without
 * backtracking, as in the Astranaut generator.
 * @since 0.0.1
 */
public final class PatternMatcher {
    /**
     * Matches one entire node.
     * @param pattern Pattern
     * @param node Candidate
     * @return Isolated captures, or failure
     * @checkstyle NonStaticMethodCheck (12 lines)
     */
    public Optional<Extracted> match(final Node pattern, final Node node) {
        final Cursor cursor = new Cursor(List.of(node), new Extracted());
        final boolean valid = PatternMatcher.consume(pattern, cursor) && cursor.index == 1;
        Optional<Extracted> result = Optional.empty();
        if (valid) {
            result = Optional.of(cursor.captures);
        }
        return result;
    }

    /**
     * Searches candidate starts in the requested direction. Elements inside a candidate
     * always match left-to-right. Empty matches are ignored to guarantee progress.
     * @param pattern Pattern or sequence
     * @param nodes Candidate occurrences
     * @param reverse Whether starts are visited right-to-left
     * @return First nonempty match
     * @checkstyle NonStaticMethodCheck (25 lines)
     */
    public Optional<Match> find(final Node pattern, final List<Node> nodes, final boolean reverse) {
        final List<Node> snapshot = List.copyOf(nodes);
        Optional<Match> result = Optional.empty();
        for (int offset = 0; result.isEmpty() && offset < snapshot.size(); offset += 1) {
            int start = offset;
            if (reverse) {
                start = snapshot.size() - offset - 1;
            }
            final Cursor cursor = new Cursor(snapshot, new Extracted());
            cursor.index = start;
            if (PatternMatcher.consume(pattern, cursor) && cursor.index > start) {
                result = Optional.of(new Match(start, cursor.index, cursor.captures));
            }
        }
        return result;
    }

    /**
     * Transactional expression evaluation.
     * @param pattern Expression
     * @param cursor Attempt state
     * @return Whether matched
     */
    private static boolean consume(final Node pattern, final Cursor cursor) {
        final Cursor trial = cursor.fork();
        final boolean valid;
        if (pattern instanceof PatternOperator operator) {
            valid = switch (operator.getKind()) {
                case SEQUENCE -> PatternMatcher.sequence(operator.getChildrenList(), trial);
                case OPTIONAL, REPEAT -> PatternMatcher.repeat(operator, trial);
                default -> PatternMatcher.single(operator, trial);
            };
        } else {
            valid = PatternMatcher.node(pattern, trial);
        }
        if (valid) {
            cursor.index = trial.index;
            cursor.captures.replaceWith(trial.captures);
        }
        return valid;
    }

    /**
     * Matches a structural node or typed capture.
     * @param pattern Expression
     * @param cursor Attempt
     * @return Whether matched
     */
    private static boolean node(final Node pattern, final Cursor cursor) {
        boolean result = cursor.index < cursor.nodes.size();
        if (result) {
            final Node node = cursor.nodes.get(cursor.index);
            if (pattern instanceof Hole hole) {
                result = cursor.capture(hole, node);
            } else {
                result = node.isSubtypeOf(pattern.getType())
                    && pattern.getData().equals(node.getData())
                    && PatternMatcher.children(pattern, node, cursor.captures);
            }
            cursor.index += 1;
        }
        return result;
    }

    /**
     * Matches the complete ordered child sequence and explicit pattern tags.
     * @param pattern Structural pattern
     * @param node Candidate
     * @param captures Trial captures
     * @return Whether all children matched
     */
    private static boolean children(final Node pattern, final Node node,
        final Extracted captures) {
        final Cursor cursor = new Cursor(node.getChildrenList(), captures);
        boolean valid = true;
        for (int index = 0; valid && index < pattern.getChildCount(); index += 1) {
            final Optional<String> tag = pattern.getChildTag(index);
            valid = tag.isEmpty() || cursor.index < node.getChildCount()
                && tag.equals(node.getChildTag(cursor.index));
            valid = valid && PatternMatcher.consume(pattern.getChild(index), cursor);
        }
        return valid && cursor.index == node.getChildCount();
    }

    /**
     * Evaluates a single-node condition.
     * @param pattern Condition
     * @param cursor Attempt
     * @return Whether matched
     */
    private static boolean single(final PatternOperator pattern, final Cursor cursor) {
        boolean valid = false;
        if (cursor.index < cursor.nodes.size()) {
            final Node node = cursor.nodes.get(cursor.index);
            valid = switch (pattern.getKind()) {
                case ANY -> true;
                case DATA -> pattern.getData().equals(node.getData());
                case DATA_HOLE -> cursor.capture(pattern, node);
                case RANGE -> PatternMatcher.inRange(pattern.getData(), node.getData());
                case NOT -> !PatternMatcher.consume(pattern.getChild(0), cursor.fork());
                case OR, AND -> PatternMatcher.combine(pattern, cursor);
                case CHILDREN -> PatternMatcher.children(pattern, node, cursor.captures);
                default -> false;
            };
            cursor.index += 1;
        }
        return valid;
    }

    /**
     * Tests inclusive character bounds.
     * @param bounds Two bounds
     * @param data Candidate string
     * @return Whether within the range
     */
    private static boolean inRange(final String bounds, final String data) {
        return data.length() == 1 && data.charAt(0) >= bounds.charAt(0)
            && data.charAt(0) <= bounds.charAt(1);
    }

    /**
     * Combines conditions on the same node. Failed alternatives never commit.
     * @param pattern Combination
     * @param cursor Attempt
     * @return Whether matched
     */
    private static boolean combine(final PatternOperator pattern, final Cursor cursor) {
        final boolean conjunction = pattern.getKind() == PatternOperator.Kind.AND;
        boolean valid = conjunction;
        for (final Node child : pattern.getChildrenList()) {
            final Cursor trial = cursor.fork();
            valid = PatternMatcher.consume(child, trial) && trial.index == cursor.index + 1;
            if (valid) {
                cursor.captures.replaceWith(trial.captures);
            }
            if (valid != conjunction) {
                break;
            }
        }
        return valid;
    }

    /**
     * Matches an ordered sequence atomically through the enclosing consume call.
     * @param patterns Elements
     * @param cursor Attempt
     * @return Whether matched
     */
    private static boolean sequence(final List<Node> patterns, final Cursor cursor) {
        boolean valid = true;
        for (final Node pattern : patterns) {
            valid = PatternMatcher.consume(pattern, cursor);
            if (!valid) {
                break;
            }
        }
        return valid;
    }

    /**
     * Greedy repetition without backtracking or zero-width loops.
     * @param pattern Optional or repeated element
     * @param cursor Attempt
     * @return Always true, including zero occurrences
     */
    private static boolean repeat(final PatternOperator pattern, final Cursor cursor) {
        boolean again = true;
        while (again) {
            final Cursor trial = cursor.fork();
            again = PatternMatcher.consume(pattern.getChild(0), trial)
                && trial.index > cursor.index;
            if (again) {
                cursor.index = trial.index;
                cursor.captures.replaceWith(trial.captures);
            }
            again = again && pattern.getKind() == PatternOperator.Kind.REPEAT;
        }
        return true;
    }

    /**
     * Successful sequence occurrence. Captures belong exclusively to this result.
     * @param start Inclusive start
     * @param end Exclusive end
     * @param captures Captures in left-to-right matching order
     * @since 0.0.1
     */
    public record Match(int start, int end, Extracted captures) { }

    /**
     * Mutable state confined to one attempt.
     * @since 0.0.1
     */
    private static final class Cursor {
        /**
         * Immutable candidate sequence.
         */
        private final List<Node> nodes;

        /**
         * Attempt-local captures.
         */
        private final Extracted captures;

        /**
         * Current occurrence.
         */
        private int index;

        /**
         * Starts a sequence attempt.
         * @param nodes Candidates
         * @param captures Captures
         */
        private Cursor(final List<Node> nodes, final Extracted captures) {
            this.nodes = nodes;
            this.captures = captures;
        }

        /**
         * Captures a typed node or its data in this attempt only.
         * @param pattern Hole expression
         * @param node Candidate
         * @return Whether the constraint accepts the node
         */
        private boolean capture(final Node pattern, final Node node) {
            boolean valid = true;
            if (pattern instanceof Hole hole) {
                valid = hole.accepts(node);
                if (valid) {
                    this.captures.addNode(hole.getNumber(), node);
                }
            } else {
                this.captures.addData(Integer.parseInt(pattern.getData()), node.getData());
            }
            return valid;
        }

        /**
         * Isolates a speculative attempt.
         * @return Independent cursor
         */
        private Cursor fork() {
            final Cursor trial = new Cursor(this.nodes, this.captures.fork());
            trial.index = this.index;
            return trial;
        }
    }
}
