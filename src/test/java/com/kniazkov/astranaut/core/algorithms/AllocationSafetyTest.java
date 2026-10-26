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

import com.kniazkov.astranaut.core.base.ChildDescriptor;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.SimpleAbstractType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Atomicity, occurrence identity and independent invocation state.
 * @since 0.0.1
 */
class AllocationSafetyTest {
    @Test
    void rejectionDoesNotPartiallyOverwriteOutput() {
        final Node sentinel = DraftNode.create("sentinel");
        final Node child = DraftNode.create("child");
        final Node[] output = {sentinel, sentinel};
        final NodeAllocator allocator = new NodeAllocator(
            ChildDescriptor.create()
                .required(child.getType())
                .required(new SimpleAbstractType("missing"))
                .build()
        );
        Assertions.assertFalse(allocator.allocate(output, List.of(child, child)));
        Assertions.assertArrayEquals(new Node[] {sentinel, sentinel}, output);
    }

    @Test
    void successfulAllocationClearsAbsentOptionalPositions() {
        final Node child = DraftNode.create("child");
        final Node[] output = {child, child};
        final NodeAllocator allocator = new NodeAllocator(
            ChildDescriptor.create().required(child.getType()).optional(child.getType()).build()
        );
        Assertions.assertTrue(allocator.allocate(output, List.of(child)));
        Assertions.assertSame(child, output[0]);
        Assertions.assertNull(output[1]);
        Assertions.assertTrue(allocator.allocate(output, List.of(child, child)));
        Assertions.assertSame(child, output[1]);
    }

    @Test
    void supportsWideRepeatedInputWithoutRecursion() {
        final Node child = DraftNode.create("child");
        final NodeAllocator allocator = new NodeAllocator(
            ChildDescriptor.create().repeated(child.getType()).build()
        );
        final List<Node> input = Collections.nCopies(30_000, child);
        final List<List<Node>> result = allocator.allocate(input).orElseThrow();
        Assertions.assertEquals(input, result.getFirst());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> result.clear());
        Assertions.assertThrows(
            UnsupportedOperationException.class, () -> result.getFirst().clear()
        );
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> allocator.allocate(new Node[1], input)
        );
    }

    @Test
    void sharedAllocatorHasNoCrossInvocationState() throws Exception {
        final Node child = DraftNode.create("child");
        final NodeAllocator allocator = new NodeAllocator(
            ChildDescriptor.create().required(child.getType()).required(child.getType()).build()
        );
        final List<Callable<Optional<List<List<Node>>>>> tasks = new ArrayList<>(64);
        for (int index = 0; index < 64; index = index + 1) {
            final List<Node> input;
            if (index % 2 == 0) {
                input = List.of(child, child);
            } else {
                input = List.of(child);
            }
            tasks.add(() -> allocator.allocate(input));
        }
        final List<Optional<List<List<Node>>>> sequential = new ArrayList<>(tasks.size());
        for (final var task : tasks) {
            sequential.add(task.call());
        }
        try (var executor = Executors.newFixedThreadPool(4)) {
            final var results = executor.invokeAll(tasks);
            for (int index = 0; index < results.size(); index += 1) {
                Assertions.assertEquals(sequential.get(index), results.get(index).get());
                Assertions.assertEquals(index % 2 == 0, results.get(index).get().isPresent());
            }
        }
    }
}
