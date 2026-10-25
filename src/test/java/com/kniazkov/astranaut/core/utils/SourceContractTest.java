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
package com.kniazkov.astranaut.core.utils;

import com.kniazkov.astranaut.core.base.Char;
import com.kniazkov.astranaut.core.base.DefaultFragment;
import com.kniazkov.astranaut.core.base.Position;
import com.kniazkov.astranaut.core.utils.parsing.FileSource;
import com.kniazkov.astranaut.core.utils.parsing.StringSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exact immutable source snapshots and canonical UTF-16 boundaries.
 * @since 0.0.1
 */
class SourceContractTest {
    @Test
    void preservesEveryUnitAndEveryHalfOpenCharacterRange() {
        for (final String text : List.of(
            "", "x", "\n", "a\n\n", "а😀\t\r\n\nz\n", "\r", "\r\n", "a\0b\uffffc"
        )) {
            final StringSource source = new StringSource(text);
            final List<Char> chars = source.parseIntoList();
            Assertions.assertEquals(text.length(), chars.size());
            Assertions.assertEquals(
                text, source.getFragmentAsString(
                    source.positionAt(0), source.positionAt(text.length())
                )
            );
            for (int index = 0; index < text.length(); index += 1) {
                final Char node = chars.get(index);
                Assertions.assertEquals(text.charAt(index), node.getSymbol());
                Assertions.assertEquals(
                    text.substring(index, index + 1), node.getFragment().getCode()
                );
                Assertions.assertEquals(source.positionAt(index), node.getFragment().getBegin());
                Assertions.assertEquals(source.positionAt(index + 1), node.getFragment().getEnd());
            }
            for (int offset = 0; offset <= text.length(); offset += 1) {
                final Position position = source.positionAt(offset);
                Assertions.assertEquals(offset, source.offsetOf(position));
                Assertions.assertEquals("", new DefaultFragment(position, position).getCode());
            }
            final Iterator<Char> iterator = source.iterator();
            iterator.forEachRemaining(node -> Assertions.assertNotNull(node));
            Assertions.assertFalse(iterator.hasNext());
            Assertions.assertThrows(NoSuchElementException.class, iterator::next);
        }
    }

    @Test
    void exposesCanonicalCoordinatesForAdaptersIncludingSurrogateBoundaries() {
        final StringSource source = new StringSource("а😀\t\r\n\nz\n");
        final int[][] coordinates = {
            {1, 1}, {1, 2}, {1, 3}, {1, 4}, {1, 5}, {1, 6}, {2, 1}, {3, 1}, {3, 2}, {4, 1},
        };
        for (int offset = 0; offset < coordinates.length; offset += 1) {
            final Position position = source.positionAt(offset);
            Assertions.assertEquals(coordinates[offset][0], position.getRow());
            Assertions.assertEquals(coordinates[offset][1], position.getColumn());
        }
        Assertions.assertEquals(
            "😀", source.getFragmentAsString(source.positionAt(1), source.positionAt(3))
        );
        Assertions.assertEquals(
            1, source.getFragmentAsString(source.positionAt(1), source.positionAt(2)).length()
        );
        Assertions.assertEquals(
            "\r\n\n", source.getFragmentAsString(source.positionAt(4), source.positionAt(7))
        );
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> source.positionAt(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> source.positionAt(10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> source.getSymbol(1, 7));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> source.getSymbol(4, 1));
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> source.getFragmentAsString(source.positionAt(2), source.positionAt(1))
        );
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> source.offsetOf(new StringSource("а😀\t\r\n\nz\n").positionAt(0))
        );
    }

    @Test
    void fileReadsAreExactStrictUnicodeAndRemainSnapshots(@TempDir final Path directory)
        throws IOException {
        final Path file = directory.resolve("source.txt");
        final String text = "Кириллица 😀\t\r\n\n\0end\n";
        Files.writeString(file, text, StandardCharsets.UTF_8);
        final FileSource source = new FileSource(file.toString());
        final FilesReader reader = new FilesReader(file.toString());
        Assertions.assertEquals(text, reader.readAsString());
        Assertions.assertEquals(text, reader.readAsStringUnchecked());
        Files.writeString(file, "changed", StandardCharsets.UTF_8);
        Files.delete(file);
        Assertions.assertEquals(
            text, source.getFragmentAsString(
                source.positionAt(0), source.positionAt(source.length())
            )
        );
        final UncheckedIOException missing = Assertions.assertThrows(
            UncheckedIOException.class, () -> new FileSource(file.toString())
        );
        Assertions.assertNotNull(missing.getCause());
        Assertions.assertTrue(missing.getMessage().contains(file.toString()));
        Files.write(file, new byte[] {(byte) 0xc3, 0x28});
        Assertions.assertThrows(IOException.class, reader::readAsString);
        Assertions.assertThrows(UncheckedIOException.class, () -> new FileSource(file.toString()));
        Files.writeString(file, "", StandardCharsets.UTF_8);
        Assertions.assertEquals(0, new FileSource(file.toString()).length());
    }
}
