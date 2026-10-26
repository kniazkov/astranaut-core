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

import com.kniazkov.astranaut.core.base.Tree;
import java.math.BigInteger;
import java.util.Objects;

/**
 * Repeats complete deterministic passes until structural stability.
 * Brent's algorithm retains one checkpoint, never the complete history. Rules and
 * builders must depend only on structural input, not invocation count or metadata.
 * Interrupt the calling thread to cancel; callbacks must cooperate with interruption.
 * @since 0.0.1
 */
public final class RuleStabilizer {
    /**
     * Configured one-pass engine.
     */
    private final RuleEngine engine;

    /**
     * Selects the same engine for every pass, including its parallel strategy.
     * @param engine Engine
     */
    public RuleStabilizer(final RuleEngine engine) {
        this.engine = Objects.requireNonNull(engine);
    }

    /**
     * Checks states only at complete pass boundaries with traversal and priority reset.
     * Adjacent equality is stability; a nonadjacent checkpoint repeat is a cycle.
     * Hashes alone are never used. Arbitrary nonrepeating growth is not detectable.
     * @param source Initial tree
     * @return Stable tree, possibly empty
     * @throws RewriteCycleException If a structural state repeats without stabilizing
     * @throws java.util.concurrent.CancellationException If the caller is interrupted
     */
    public Tree apply(final Tree source) {
        Tree current = Objects.requireNonNull(source);
        Tree checkpoint = current;
        BigInteger power = BigInteger.ONE;
        BigInteger distance = BigInteger.ZERO;
        BigInteger start = BigInteger.ZERO;
        while (true) {
            Interruption.check();
            final Tree next = this.engine.apply(current);
            Interruption.check();
            final boolean stable = current.deepCompare(next);
            Interruption.check();
            if (stable) {
                return next;
            }
            distance = distance.add(BigInteger.ONE);
            final boolean repeated = checkpoint.deepCompare(next);
            Interruption.check();
            if (repeated) {
                throw new RewriteCycleException(start, distance);
            }
            current = next;
            if (distance.equals(power)) {
                checkpoint = current;
                start = start.add(distance);
                power = power.shiftLeft(1);
                distance = BigInteger.ZERO;
            }
        }
    }
}
