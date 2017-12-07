package newProcessingSequencingData;

public class FilterVariantPositions {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String mergedFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/MarianWork/vcfFiles/merged.txt";
		
		// Set up the necessary filtering set
		int dp = 13;
		int hqdp = 2;
		int mq = 20;
		double support = 0.95;
		double qual = 0; // Not used
		double prop = 0; // Not used
		int fqValue = 0; // Not used
		FilterSet filters = new FilterSet(dp, hqdp, mq, support, qual, prop, fqValue);
		
		// Filter the Variant Positions in the Merged VCF file
		String filteredOutput = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/MarianWork/vcfFiles/filtered.txt";
		String snpSupport = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/MarianWork/vcfFiles/snpSupport.txt";
		
	}

}
