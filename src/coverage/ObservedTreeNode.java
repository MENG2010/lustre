package coverage;

import java.util.ArrayList;
import java.util.List;

public class ObservedTreeNode {
    public String data;
    public ObservedTreeNode parent;
    public List<ObservedTreeNode> children;
 
    public ObservedTreeNode(String data) {
        setData(data);
    }

    public List<ObservedTreeNode> getChildren() {
        if (this.children == null) {
            return new ArrayList<ObservedTreeNode>();
        }
        return this.children;
    }
 
    public void setChildren(List<ObservedTreeNode> children) {
        this.children = children;
    }
 
    public int getNumberOfChildren() {
        if (children == null) {
            return 0;
        }
        return children.size();
    }
     
    public void addChild(ObservedTreeNode child) {
        if (children == null) {
            children = new ArrayList<ObservedTreeNode>();
        }
        child.parent = this;
        this.children.add(child);
    }
 
    public String getData() {
        return this.data;
    }
 
    public void setData(String data) {
        this.data = data;
    }
    
    // return all leaf nodes of specific node
    public List<String> getAllLeaves() {
    	List<String> leaves = new ArrayList<String>();
    	if (this.children == null) {
            leaves.add(this.data);
        } else {
            for (ObservedTreeNode child : this.children) {
                leaves.addAll(child.getAllLeaves());
            }
        }
    	
    	return leaves;
    }
    
    @Override
    public String toString() {
    	return this.data;
    }
}