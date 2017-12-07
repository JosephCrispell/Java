package phylogeneticTree;

import java.util.Hashtable;

public class Global {

	public static int nodeNo = -1;
	public static Hashtable<String, Integer> variables = new Hashtable();
	public static int lastIndexUsed = 0;
	public static int branchIndex = -1;
	
	public static int[][] counts;
	public static double[][] time;
	
	public static Node[] terminalNodes = new Node[0];

}
