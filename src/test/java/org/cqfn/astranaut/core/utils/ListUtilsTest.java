/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Ivan Kniazkov
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
package org.cqfn.astranaut.core.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ListUtils} class.
 * @since 1.0.2
 */
class ListUtilsTest {
    @Test
    void testAddingItems() {
        final ListUtils<Integer> list = new ListUtils<>();
        list.add(1, null, 2, null);
        final List<Integer> result = list.make();
        final List<Integer> expected = Arrays.asList(1, 2);
        Assertions.assertEquals(expected, result);
        Assertions.assertThrows(UnsupportedOperationException.class, () ->  result.add(3));
    }

    @Test
    void testMergingLists() {
        final ListUtils<Integer> list = new ListUtils<>();
        list.add(1, 2);
        final List<Integer> second = Collections.unmodifiableList(
            Arrays.asList(3, null, 4)
        );
        list.add((List<Integer>) null);
        list.add(second);
        final List<Integer> result = list.make();
        final List<Integer> expected = Arrays.asList(
            1, 2, 3, 4
        );
        Assertions.assertEquals(expected, result);
    }
}
