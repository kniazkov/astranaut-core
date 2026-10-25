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
package com.kniazkov.astranaut.core.utils.parsing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A {@link CharParser} implementation that reads characters from a file.
 * @since 0.0.1
 */
public final class FileSource extends StringSource {
    /**
     * The path to the file containing source code.
     */
    private final String path;

    /**
     * Constructs a {@code FileSource} by reading the content of the specified file.
     * @param path The path to the UTF-8 file to snapshot
     * @throws java.io.UncheckedIOException If reading or UTF-8 decoding fails
     */
    public FileSource(final String path) {
        super(FileSource.read(path));
        this.path = path;
    }

    @Override
    public String getFileName() {
        return this.path;
    }

    /**
     * Reads exact UTF-8 text once, preserving the file path and cause on failure.
     * @param path File path
     * @return Source snapshot
     * @throws UncheckedIOException If reading or UTF-8 decoding fails
     */
    private static String read(final String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (final IOException exception) {
            throw new UncheckedIOException(String.format("Cannot read %s", path), exception);
        }
    }
}
