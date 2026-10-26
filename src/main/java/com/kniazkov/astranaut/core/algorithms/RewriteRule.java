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
import java.util.List;
import java.util.Optional;

/**
 * A single-attempt rule over an immutable sibling sequence. Expected construction
 * refusal may be thrown; other failures are programming errors and propagate.
 * New output nodes must be built validly and carry the matched span's provenance;
 * captured nodes retain their own provenance. The engine validates ancestors.
 * @since 0.0.1
 */
@FunctionalInterface
public interface RewriteRule {
    /**
     * Attempts exactly this start, without searching another position.
     * @param nodes Current immutable siblings
     * @param index Start
     * @return Replacement or normal non-applicability
     */
    Optional<Replacement> apply(List<Node> nodes, int index);

    /**
     * Selects descending candidate starts; matching within a candidate stays ordered.
     * @return Whether to search right-to-left
     */
    default boolean isRightToLeft() {
        return false;
    }

    /**
     * Explicitly permits speculative concurrent calls, including factories used by the rule.
     * Opt-in requires deterministic, side-effect-free behavior independent of call order.
     * @return Whether concurrent speculative invocation is safe
     */
    default boolean isConcurrent() {
        return false;
    }
}
