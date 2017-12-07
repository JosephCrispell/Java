package phylogeneticTree;

public class StateSpecificMutationRateResults {

	/**
	 * Haven't written this as a results object because as you traverse the tree and return the results from traversing each subNode the process
	 * of returning and writing to object is lost from the traversals back through the return statements.
	 */
	public static double[][] stateRates = null;
	public static int noStateSwitches = 0;
	public static int[] noStateBranches = null;
	
	public static void restoreDefaults(){

		stateRates = null;
		noStateSwitches = 0;
		noStateBranches = null;
	}
}
