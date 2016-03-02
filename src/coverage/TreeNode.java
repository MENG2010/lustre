package coverage;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    public String data;
    public TreeNode parent;
    public List<TreeNode> children;
 
    public TreeNode() {
    	super();
    }

    public TreeNode(String data) {
        this();
        setData(data);
    }

    public List<TreeNode> getChildren() {
        if (this.children == null) {
            return new ArrayList<TreeNode>();
        }
        return this.children;
    }
 
    public void setChildren(List<TreeNode> children) {
        this.children = children;
    }
 
    public int getNumberOfChildren() {
        if (children == null) {
            return 0;
        }
        return children.size();
    }
     
    public void addChild(TreeNode child) {
        if (children == null) {
            children = new ArrayList<TreeNode>();
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
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{").append(getData().toString()).append(",[");
        int i = 0;
        for (TreeNode e : getChildren()) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(e.getData().toString());
            i++;
        }
        sb.append("]").append("}");
        return sb.toString();
    }
}