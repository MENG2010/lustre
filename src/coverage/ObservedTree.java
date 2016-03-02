package coverage;

import java.util.ArrayList;
import java.util.List;

public class ObservedTree {
 
    private TreeNode root;
    
    public ObservedTree(TreeNode root) {
    	setroot(root);
    }
 
    public TreeNode getroot() {
        return this.root;
    }
 
    public void setroot(TreeNode root) {
    	if (root == null) {
    		throw new IllegalArgumentException("null root!");
    	}
        this.root = root;
    }
    
    /*
     * convert the tree to a list
     */
    public List<TreeNode> toList() {
        List<TreeNode> list = new ArrayList<TreeNode>();
        traverse(root, list);
        return list;
    }
    
    @Override
    public String toString() {
        return toList().toString();
    }
    
    /*
     * Traverse the sub-tree of given node
     */
    private void traverse(TreeNode node, List<TreeNode> list) {
        list.add(node);
        for (TreeNode data : node.getChildren()) {
            traverse(data, list);
        }
    }
    
    //TODO:traverse the sub-tree BFS
    
}