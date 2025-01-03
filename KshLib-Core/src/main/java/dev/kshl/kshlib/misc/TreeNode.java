package dev.kshl.kshlib.misc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class TreeNode<E> {

    @Nullable
    private final E node;
    @Nullable
    private TreeNode<E> parent;

    private final Set<TreeNode<E>> children = new HashSet<>();

    public TreeNode(@Nullable E node, @Nullable TreeNode<E> parent) {
        this.node = node;
        this.parent = parent;
    }

    @Nullable
    public E getNode() {
        return node;
    }

    @Nullable
    public TreeNode<E> getParent() {
        return parent;
    }

    @Nonnull
    public TreeNode<E> getUltimateParentOrThis() {
        if (parent == null) return this;
        return parent.getUltimateParentOrThis();
    }

    private void checkNotParent(TreeNode<E> other) {
        if (Objects.equals(parent, other)) {
            throw new ParentOfNodeException();
        }
        if (parent != null) parent.checkNotParent(other);
    }

    public Set<TreeNode<E>> getChildren() {
        return children;
    }

    public void addChild(TreeNode<E> nodeMap) {
        if (nodeMap.parent != null) {
            throw new AlreadyHasParentException();
        }
        if (equals(nodeMap)) {
            throw new AddingNodeToItselfException();
        }
        checkNotParent(nodeMap);
        nodeMap.parent = this;
        nodeMap.setCachedHashDirty();
        this.setCachedHashDirty();
        this.children.add(nodeMap);
    }

    private final Object hashLock = new Object();
    private int cachedHash;
    private boolean cachedHashDirty = true;

    private void setCachedHashDirty() {
        synchronized (hashLock) {
            this.cachedHashDirty = true;
        }
    }

    @Override
    public int hashCode() {
        synchronized (hashLock) {
            if (cachedHashDirty) {
                cachedHash = Objects.hash(node, parent, children);
                cachedHashDirty = false;
            }
            return cachedHash;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TreeNode<?> treeNode)) return false;
        if (!Objects.equals(node, treeNode.node)) return false;
        if (!Objects.equals(parent, treeNode.parent)) return false;

        return Objects.equals(children, treeNode.children);
    }

    public Set<E> flatten() {
        return flatten(new HashSet<>());
    }

    private Set<E> flatten(Set<E> set) {
        if (node != null) set.add(node);
        for (TreeNode<E> child : children) {
            child.flatten(set);
        }
        return set;
    }

    public static class ParentOfNodeException extends IllegalArgumentException {
        ParentOfNodeException() {
            super("Node is a parent of this node");
        }
    }

    public static class AlreadyHasParentException extends IllegalArgumentException {
        AlreadyHasParentException() {
            super("nodeMap already has a parent. Can't add to multiple nodes");
        }
    }

    public static class AddingNodeToItselfException extends IllegalArgumentException {
        AddingNodeToItselfException() {
            super("Can't add nodeMap to itself");
        }
    }
}
