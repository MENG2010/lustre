package coverage;

import java.util.ArrayList;
import java.util.List;

public class ObservedTree {
 
    private ObservedTreeNode root;
    
    public ObservedTree(ObservedTreeNode root) {
    	setroot(root);
    }
 
    public ObservedTreeNode getroot() {
        return this.root;
    }
 
    public void setroot(ObservedTreeNode root) {
    	if (root == null) {
    		throw new IllegalArgumentException("null root!");
    	}
        this.root = root;
    }
    
    /*
     * convert the tree to a list
     */
    public List<ObservedTreeNode> toList() {
        List<ObservedTreeNode> list = new ArrayList<ObservedTreeNode>();
        traverse(root, list);
        return list;
    }
    
    /*
     * convert a subtree rooted with given sub-root to a list
     */
    public List<ObservedTreeNode> subtreeToList(ObservedTreeNode subroot) {
    	List<ObservedTreeNode> list = new ArrayList<ObservedTreeNode>();
    	traverse(subroot, list);
    	return list;
    }
    
    @Override
    public String toString() {
        return toList().toString();
//    	List<String> list = new ArrayList<String>();
//    	print(this.root, list);
//    	return list.toString();
    }
    
    /*
     * Traverse the sub-tree of given node
     */
    private void traverse(ObservedTreeNode parent, List<ObservedTreeNode> list) {
        list.add(parent);
        for (ObservedTreeNode child : parent.getChildren()) {
            traverse(child, list);
        }
    }
    
    /*
     * print subtree (DFS) given root
     */
    public void print(ObservedTreeNode node) {
    	if (node == null) {
    		return;
    	}
    	System.out.println("Children of [" + node.getData() + "]: " + node.getChildren().toString());
    	for (ObservedTreeNode child : node.getChildren()) {
    		print(child);
    	}
    }
        
}