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

import java.math.BigInteger;

/**
 * A repeated structural state at complete pass boundaries, distinct from refusal.
 * Carries counters only so diagnostics do not retain potentially large trees.
 * @since 0.0.1
 */
public final class RewriteCycleException extends IllegalStateException {
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Zero-based checkpoint pass, not necessarily the first cycle entry.
     */
    private final BigInteger checkpoint;

    /**
     * Cycle length in complete passes.
     */
    private final BigInteger period;

    /**
     * Records the detected repeat.
     * @param checkpoint Earlier equal state's pass
     * @param period Distance to the repeated state
     */
    RewriteCycleException(final BigInteger checkpoint, final BigInteger period) {
        super(String.format("Rewrite cycle: checkpoint pass %s, period %s", checkpoint, period));
        this.checkpoint = checkpoint;
        this.period = period;
    }

    /**
     * Returns the earlier of the two detected equal states.
     * @return Zero-based pass number
     */
    public BigInteger getCheckpointPass() {
        return this.checkpoint;
    }

    /**
     * Returns the detected cycle length.
     * @return Positive pass count
     */
    public BigInteger getPeriod() {
        return this.period;
    }
}
