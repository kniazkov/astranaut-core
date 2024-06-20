package org.cqfn.astranaut.core.testing;

import java.io.File;
import java.io.IOException;
import org.cqfn.astranaut.core.Node;
import org.cqfn.astranaut.core.exceptions.WrongFileExtension;
import org.cqfn.astranaut.core.utils.TreeVisualizer;

/**
 * Testing class.
 */
public class T01_ReadAndVisualize {
    /**
     * Starting point.
     * @param args Program arguments
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Not enough arguments");
            return;
        }

        final Node root;
        try {
            root = JavaParser.INSTANCE.read(args[0]);
        } catch (IOException ignored) {
            System.err.println("Could not parse " + args[0]);
            return;
        }

        String image = args[0] + ".png";
        TreeVisualizer visualizer = new TreeVisualizer(root);
        try {
            visualizer.visualize(new File(image));
        } catch (IOException | WrongFileExtension ignored) {
            System.err.println("Could not write " + image);
        }


    }
}
