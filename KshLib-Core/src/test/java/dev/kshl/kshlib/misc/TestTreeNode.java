package dev.kshl.kshlib.misc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestTreeNode {
    private TreeNode<Integer>[] treeNodes;

    @BeforeEach
    public void initTestNodes() {
        treeNodes = new TreeNode[10];
        for (int i = 0; i < treeNodes.length; i++) {
            treeNodes[i] = new TreeNode<>(i);
        }
    }

    @Test
    public void testBasicFunctionality() {
        treeNodes[0].addChild(treeNodes[1]);
        treeNodes[1].addChild(treeNodes[2]);

        Set<Integer> expected = Set.of(0, 1, 2);

        assertEquals(expected, treeNodes[0].getUltimateParentOrThis().flatten());
        assertEquals(expected, treeNodes[1].getUltimateParentOrThis().flatten());
        assertEquals(expected, treeNodes[2].getUltimateParentOrThis().flatten());
    }

    @Test
    public void testCyclicParents() {
        treeNodes[0].addChild(treeNodes[1]);
        assertThrows(TreeNode.ParentOfNodeException.class, () -> treeNodes[1].addChild(treeNodes[0])); // Can't add node to a parent that it is the parent of
        assertThrows(TreeNode.AlreadyHasParentException.class, () -> treeNodes[2].addChild(treeNodes[1])); // Can't add node to multiple parents
        assertThrows(TreeNode.AddingNodeToItselfException.class, () -> treeNodes[0].addChild(treeNodes[0])); // Can't add node to itself
    }

    @Test
    public void testParentHierarchy() {
        for (int i = 0; i < treeNodes.length - 1; i++) {
            treeNodes[i].addChild(treeNodes[i + 1]);
        }
        for (int i = 0; i < treeNodes.length - 1; i++) {
            assertEquals(treeNodes[i].getUltimateParentOrThis(), treeNodes[i + 1].getUltimateParentOrThis());
        }
    }
}
