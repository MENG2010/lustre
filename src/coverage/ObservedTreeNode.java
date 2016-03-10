package coverage;

import java.util.ArrayList;
import java.util.List;

public class ObservedTreeNode {
    public String data;
    public ObservedTreeNode parent;
    public List<ObservedTreeNode> children;
 
    public ObservedTreeNode() {
    	super();
    }

    public ObservedTreeNode(String data) {
        this();
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
        children.add(child);
    }
 
    public String getData() {
        return this.data;
    }
 
    public void setData(String data) {
        this.data = data;
    }
    
    
    //TODO: represent the node in a new way
//    @Override
//    public String toString() {
//        StringBuilder sb = new StringBuilder();
//        sb.append(getData()).append(" -> [");
//        int i = 0;
//        for (TreeNode e : getChildren()) {
//            if (i > 0) {
//                sb.append(" , ");
//            }
//            sb.append(e.getData());
//            i++;
//        }
//        sb.append("] ");
//        return sb.toString();
//    }
    
    @Override
    public String toString() {
    	return this.data;
    }
}