package newickTree;

import java.util.ArrayList;

public class Node {

	public int index;
	public boolean internal;
	public int parentIndex;
	public int[] subNodeIndices;
	public double branchLength;
	
	public Node(int id, boolean isInternal) {
		
		this.index = id;
		this.internal = isInternal;
	}
}
