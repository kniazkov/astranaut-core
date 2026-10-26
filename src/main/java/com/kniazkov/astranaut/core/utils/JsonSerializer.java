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

import com.kniazkov.astranaut.core.base.DiffTree;
import com.kniazkov.astranaut.core.base.Pattern;
import com.kniazkov.astranaut.core.base.Tree;
import com.kniazkov.astranaut.core.utils.json.NodeWriter;
import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Version-one flat node-table JSON. Structure mode is the default.
 * @since 0.0.1
 */
public final class JsonSerializer {
    /**
     * Immutable input.
     */
    private final Tree tree;

    /**
     * Selected metadata mode.
     */
    private final Mode mode;

    /**
     * Selects structure-only output.
     * @param tree Input
     */
    public JsonSerializer(final Tree tree) {
        this(tree, Mode.STRUCTURE);
    }

    /**
     * Selects either structural or extended output.
     * @param tree Input
     * @param mode Output mode
     */
    public JsonSerializer(final Tree tree, final Mode mode) {
        this.tree = Objects.requireNonNull(tree);
        this.mode = Objects.requireNonNull(mode);
    }

    /**
     * Writes a dependency-ordered node table, including explicit empty-root state.
     * @return JSON text
     */
    public String serialize() {
        final JsonObject result = new JsonObject();
        result.addNumber("version", 1);
        String kind = "tree";
        if (this.tree instanceof DiffTree) {
            kind = "diff";
        } else if (this.tree instanceof Pattern) {
            kind = "pattern";
        }
        result.addString("kind", kind);
        final JsonArray nodes = result.createArray("nodes");
        if (this.tree.getRoot().isPresent()) {
            result.addNumber(
                "root", new NodeWriter(this.mode == Mode.EXTENDED)
                .write(this.tree.getRoot().orElseThrow(), nodes)
            );
        } else {
            result.addNull("root");
        }
        return result.toText("  ");
    }

    /**
     * Converts the tree to a string that contains a JSON object and
     *  writes the result to a file.
     * @param filename The file name
     * @return The result, {@code true} if the file was successful written
     */
    public boolean serializeToFile(final String filename) {
        final String json = this.serialize();
        boolean success = true;
        try {
            final Path path = Path.of(filename);
            final Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, json);
        } catch (final IOException | InvalidPathException ignored) {
            success = false;
        }
        return success;
    }

    /**
     * Structural content is always present; metadata never includes source text.
     * @since 0.0.1
     */
    public enum Mode {
        /**
         * Types, data, operation/hole parameters and ordered tagged edges.
         */
        STRUCTURE,
        /**
         * Additionally properties, direct base names and source coordinates/filenames.
         */
        EXTENDED
    }
}
