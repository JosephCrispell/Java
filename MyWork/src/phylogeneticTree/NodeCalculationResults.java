package phylogeneticTree;

public class NodeCalculationResults {

	/**
	 * Haven't written this as a results object because as you traverse the tree and return the results from traversing each subNode the process
	 * of returning and writing to object is lost from the traversals back through the return statements.
	 */
	public static double[] rateEstimates = new double[0];
	public static double[] diffusionCoefficients = new double[0];
	public static double totalTime = 0;
	public static double totalDistance = 0;
	public static double totalDiffusionCoefficient = 0;
	public static double[] clockRates = new double[0];
	public static double[] latLongRates = new double[0];
	
	public static void restoreDefaults(){
		rateEstimates = new double[0];
		diffusionCoefficients = new double[0];
		totalTime = 0;
		totalDistance = 0;
		totalDiffusionCoefficient = 0;
		clockRates = new double[0];
		latLongRates = new double[0];
	}

}
