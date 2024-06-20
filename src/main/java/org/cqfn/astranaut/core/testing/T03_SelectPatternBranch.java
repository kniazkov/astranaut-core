package org.cqfn.astranaut.core.testing;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.cqfn.astranaut.core.Action;
import org.cqfn.astranaut.core.DifferenceNode;
import org.cqfn.astranaut.core.Node;
import org.cqfn.astranaut.core.algorithms.DeepTraversal;
import org.cqfn.astranaut.core.algorithms.DifferenceTreeBuilder;
import org.cqfn.astranaut.core.algorithms.NodeSelector;
import org.cqfn.astranaut.core.algorithms.mapping.TopDownMapper;
import org.cqfn.astranaut.core.exceptions.WrongFileExtension;
import org.cqfn.astranaut.core.utils.TreeVisualizer;

/**
 * Testing class.
 */
public class T03_SelectPatternBranch {
    /**
     * Starting point.
     * @param args Program arguments
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Not enough arguments");
            return;
        }

        final Node before;
        try {
            before = JavaParser.INSTANCE.read(args[0]);
        } catch (IOException ignored) {
            System.err.println("Could not parse " + args[0]);
            return;
        }

        final Node after;
        try {
            after = JavaParser.INSTANCE.read(args[1]);
        } catch (IOException ignored) {
            System.err.println("Could not parse " + args[1]);
            return;
        }

        final DifferenceTreeBuilder diffBuilder = new DifferenceTreeBuilder(before);
        diffBuilder.build(after, TopDownMapper.INSTANCE);

        final DifferenceNode diff = diffBuilder.getRoot();

        Set<Node> branches = new NodeSelector(diff)
                .select((node, parents) -> node.belongsToGroup("MethodDeclaration"));
        Node selectedBranch = null;
        for (Node branch : branches) {
            Optional<Node> action = new DeepTraversal(branch)
                    .findFirst(node -> node instanceof Action);
            if (action.isPresent()) {
                selectedBranch = branch;
                break;
            }
        }

        if (selectedBranch != null) {
            String image = args[0] + ".branch.png";
            TreeVisualizer visualizer = new TreeVisualizer(selectedBranch);
            try {
                visualizer.visualize(new File(image));
            } catch (IOException | WrongFileExtension ignored) {
                System.err.println("Could not write " + image);
            }
        }
    }
}
