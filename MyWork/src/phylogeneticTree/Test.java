package phylogeneticTree;

import java.io.IOException;

public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		// TODO Auto-generated method stub

		String file = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/AnalysesForPaper/EastCoast/run_27-07-15_2_HKY_Exponential_Skyline_Cattle/NZ_27-07-15_2_HKY_Exponential_Skyline_Cattle.trees.txt";
		
		BeastNewick trees = BeastNewickTreeMethods.readBeastFormattedNexus(file, 10001);
		
	}

}
