package observability.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import jkind.lustre.Type;

public class TreeNode {
    public String rawId;
    public Type type;
    public TreeNode parent;
    public List<TreeNode> children;
    public boolean isArithExpr;
    /* <renamedId, timeSeen> */
    public Map<String, Integer> renamedIds;
    
    public TreeNode(String rawId, Type type) {
    	setId(rawId);
    	setType(type);
    	setRenamedIds(new HashMap<String, Integer>());
    	this.parent = null;
    	this.children = new ArrayList<>();
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
        this.children.add(child);
    }

    public void setId(String id) {
        this.rawId = id;
    }
    
    public void setType(Type type) {
    	this.type = type;
    }
    
    public void setRenamedIds(Map<String, Integer> renamedIds) {
    	this.renamedIds = renamedIds;
    }
    
    // return all leaf nodes under specific node
    public List<TreeNode> getAllLeafNodes() {
    	List<TreeNode> leaves = new ArrayList<>();
    	if (this.children == null || this.children.isEmpty()) {
    		leaves.add(this);
    	} else {
    		for (TreeNode child : this.children) {
    			leaves.addAll(child.getAllLeafNodes());
    		}
    	}

    	return leaves;
    }
    
    public boolean containsChild(String id) {
    	for (TreeNode child : this.children) {
    		if (id.equals(child.rawId)
    				|| child.renamedIds.containsKey(id)) {
    			return true;
    		}
    	}
    	
    	return false;
    }
    
    public TreeNode getChild(String id) {
		for (TreeNode child : this.children) {
			if (id.equals(child.rawId)) {
				return child;
			}
		}
		
		return null;
    }
    
    public List<TreeNode> convertToList() {
    	List<TreeNode> list = new ArrayList<>();
    	convertToList(this, list);
    	return list;
    }
    
    private void convertToList(TreeNode root, List<TreeNode> list) {
    	if (root == null) {
    		return;
    	}
    	
    	list.add(root);
    	
    	if (root.children != null) {
	    	for (TreeNode child : root.children) {
	    		convertToList(child, list);
	    	}
    	}
    }
    
    public boolean containsNode(String id) {
    	List<TreeNode> list = new ArrayList<>();
    	convertToList(this, list);
    	
    	for (TreeNode node : list) {
    		if ((node.rawId.equals(id))
    				|| (node.renamedIds.containsKey(id))) {
    			return true;
    		}
    	}
    	
    	return false;
    }
    
    // return paths from current node to leaf nodes
    public void getPaths(List<List<TreeNode>> paths) {
    	this.getPaths(this, paths, new ArrayList<TreeNode>());
    }
    
    // return paths from given node to each leaf node in the subtree
    private void getPaths(TreeNode root, List<List<TreeNode>> paths,
    							List<TreeNode> path) {
    	if (root == null) {
    		return;
    	}
		while (root.parent != null 
				&& path.lastIndexOf(root.parent) != path.size() - 1) {
			// remove siblings of parent node
			path.remove(path.size() - 1);
		}
		// append the node to the list next to its parent
		path.add(root);
    	
    	if (root.children == null || root.children.isEmpty()) {
    		// add one path (root -> node list -> leaf)
    		paths.add(new ArrayList<TreeNode>(path));
    	} else {
    		// explore current path
    		for (TreeNode child : root.children) {
    			child.getPaths(child, paths, path);
    		}
    	}
    }
    
    // for testing
    public void print() {
    	System.out.println(bfs(this));
    }
    
    private List<List<String>> bfs(TreeNode root) {
    	List<List<String>> result = new ArrayList<>();
    	
    	if (root == null) {
    		return result;
    	}
    	
    	Queue<TreeNode> queue1 = new LinkedList<>();
    	Queue<TreeNode> queue2 = new LinkedList<>();
    	
    	queue1.offer(root);
    	
    	while (! queue1.isEmpty()) {
    		List<String> level = new ArrayList<>();
    		
    		queue2.clear();
    		
    		for (TreeNode node : queue1) {
    			level.add(node.rawId);
    			
    			for (TreeNode child : node.children) {
    				queue2.offer(child);
    			}
    		}
    		
    		Queue<TreeNode> temp = queue1;
    		queue1 = queue2;
    		queue2 = temp;
    		
    		result.add(level);
    	}    	
    	
    	return result;
    }
    
    @Override
    public boolean equals(Object node) {
    	if (node == null) {
    		return false;
    	} else if (! (node instanceof TreeNode)) {
    		throw new IllegalArgumentException("Wrong type of node!");
    	} else {
    		// node data is the only identifier
    		return this.rawId.equals(((TreeNode)node).rawId);
    	}
    }
        
    @Override
    public String toString() {
    	StringBuilder node = new StringBuilder();
    	
    	node.append("(");
    	node.append("data=").append(this.rawId).append(", ");
    	node.append("type=").append(this.type.toString()).append(", ");
    	node.append("isArithExpr=").append(this.isArithExpr).append(", ");
		node.append("arithIds=").append(this.renamedIds);
    	node.append(")");
    	
    	return node.toString();
    }
}