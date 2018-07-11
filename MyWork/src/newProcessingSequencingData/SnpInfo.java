package newProcessingSequencingData;

import java.util.Hashtable;

import methods.ArrayMethods;

public class SnpInfo {
	
	public String fileName;
	public String chrom;
	public int pos;
	public char ref;
	public char alt;
	public double qualityScore;
	public Hashtable<String, double[]> infoCol;
	public Hashtable<String, double[]> formatCol;
	
	public SnpInfo(String snpInfoLine, VcfFile vcfFile){
		
		/**
		 * Each SNP Information Line in the VCF file is structured:
		 * 
		 *  #CHROM	POS		ID	REF	ALT	QUAL	FILTER	INFO								FORMAT(fields)	(values)	fileName
		 *  runID	1057	.	A	G	222		.		DP=147;VDB=0.0182;DP4=0,0,67,73;...	GT:PL:GQ		1/1:255,255,0:99
		 *  0		1		2	3	4	5		6		7									8				9
		 *  
		 *  
		 *  
		 * 	"\t"	#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT(fields)	FORMAT(values)	
		 * 			0		1	2	3	4	5		6		7		8				9
		 *													|		|				|
		 *	";"						INFO Column Fields <-----		--> Fields		-->	Values
		 *							DP	AF1	AC1	DP4	MQ	FQ				GT	PL	GQ		"/"	","	"|" ""
		 *							0	1	2	3	4	5				0	1	2
		 *										|					   
		 * 	","			DP4 Column Fields <------					   
		 * 				ref-forward	ref-Reverse	alt-forward	alt-reverse
		 * 				0			1			2			3
		 */
		
		this.fileName = vcfFile.getFileName();
		String[] cols = snpInfoLine.split("\t");
		this.chrom = cols[0];
		this.pos = Integer.parseInt(cols[1]);
		this.ref = cols[3].charAt(0);
		this.qualityScore = Double.parseDouble(cols[5]);
		this.infoCol = getInfoColInfo(cols[7]);
		this.formatCol = getFormatColInfo(cols[8], cols[9]);
		this.alt = getAlternateAllele(cols[4], this.formatCol, this.pos, vcfFile,
				this.infoCol, this.qualityScore, this.ref);
		
	}
	
	// Getting Methods
	public String getFileName(){
		return this.fileName;
	}
	public String getChrom(){
		return this.chrom;
	}
	public int getPos(){
		return this.pos;
	}
	public char getRef(){
		return this.ref;
	}
	public char getAlt(){
		return this.alt;
	}
	public double getQualityScore(){
		return this.qualityScore;
	}
	public Hashtable<String, double[]> getInfoCol(){
		return this.infoCol;
	}
	public Hashtable<String, double[]> getFormatCol(){
		return this.formatCol;
	}
	
	// General Methods
	public static Hashtable<String, double[]> getInfoColInfo(String lineCol){
		
		// Initialise a Hashtable to store the variables and their values
		Hashtable<String, double[]> infoCol = new Hashtable<String, double[]>();
		
		// Split the Info column into its individual columns
		String[] cols = lineCol.split(";");
		
		// Initialise necessary variables
		String[] parts;
		String[] stringValues;
		double[] values;
		
		// Examine each of the columns within the Info column
		for(String col : cols){
			
			// Column Structured: variableName=value(s)
			parts = col.split("=");
			if(parts.length == 1){
				continue;
			}
			
			stringValues = parts[1].split(",");
			
			// Convert each value string into double
			values = new double[stringValues.length];
			
			for(int i = 0; i < stringValues.length; i++){
				values[i] = Double.parseDouble(stringValues[i]);
			}
			
			// Store the variable information
			infoCol.put(parts[0], values);
		}
		
		return infoCol;	
	}

	public static Hashtable<String, double[]> getFormatColInfo(String fieldsCol, String valuesCol){
		
		// Initialise a Hashtable to store the variables and their values
		Hashtable<String, double[]> formatCol = new Hashtable<String, double[]>();
		
		// Split the fields and values columns
		String[] fields = fieldsCol.split(":");
		String[] fieldValues = valuesCol.split(":");
		
		// Initialise the necessary variables
		String[] stringValues;
		double[] values;
		
		// Examine each the fields and their values
		for(int i = 0; i < fields.length; i++){
			
			// Values can separated by either "/", ",", "|", or can be single values
			if(fieldValues[i].matches("(.*)/(.*)")){
				
				stringValues = fieldValues[i].split("/");				
			}else if(fieldValues[i].matches("(.*),(.*)")){
				
				stringValues = fieldValues[i].split(",");
			}else if(fieldValues[i].matches("(.*)|(.*)")){
				
				stringValues = fieldValues[i].split("|");
			}else{
				
				stringValues = new String[1];
				stringValues[0] = fieldValues[i];
			}
			
			// Convert the string values into doubles
			values = new double[stringValues.length];
			
			for(int x = 0; x < stringValues.length; x++){
				values[x] = Double.parseDouble(stringValues[x]);
			}
			
			// Store the field information
			formatCol.put(fields[i], values);
		}
		
		return formatCol;		
	}

	public static char getAlternateAllele(String col, Hashtable<String, double[]> format, int snpPos, 
			VcfFile file, Hashtable<String, double[]> info, double quality, char refAllele){
		
		/**
		 * Rarely more than one alternate allele can be present at a given position. If an alternate allele is called
		 * at a given position then we need to know which of the alternate alleles it was.
		 * The GT (Genotype) variable in the format info tells which allele was called at a given position.
		 * 		0	REF
		 * 		1	1st ALT
		 * 		2	2nd ALT
		 * 		...
		 * 
		 * 	GT	0/0	homozygous REF
		 * 	GT	0/1 heterozygous REF and ALT (diploid)
		 * 	GT 	1/1 homozygous ALT
		 * 
		 * We are not dealing with a diploid organism so only a single allele can be called at a given position.
		 * 
		 * Note that GT can be separated by | if genotype is phased - this is the process by which a haplotype
		 * has been estimated (tried to reconstruct from multiple sequence sources)
		 * 
		 */
		
		// Initialise a HeterozygousSite object
		HeterozygousSite hetSiteInfo;
		char[] alleles;
		
		// Set default allele to unknown (N)
		char allele = 'N';
		
		// Get the alternate Allele(s)
		char[] alternateAlleles = ArrayMethods.toChar(col.split(","));
		allele = alternateAlleles[0];
		
		// Run heterozygosity check - Check if high quality bases supporting both the reference
		// and alternate alleles on both the forward and reverse reads
		double[] highQualityBaseCounts = info.get("DP4");
		if(highQualityBaseCounts[0] > 0 &&
		   highQualityBaseCounts[1] > 0 && 
		   highQualityBaseCounts[2] > 0 &&
		   highQualityBaseCounts[3] > 0){
				
			// Store information about the current Heterozygous site
			hetSiteInfo = new HeterozygousSite(snpPos, refAllele, alternateAlleles, quality, info, format);
			file.addToHeterozygousSiteCount(hetSiteInfo);
		}
				
		// If only one allele present, then don't need to do anything
		if(alternateAlleles.length > 1){
			
			// Get the genotype Information
			int[] genotype = ArrayMethods.convertDouble2Int(format.get("GT"));
			
			// If an alternate allele then it will be in the first column
			if(genotype[0] != 0){ // REF called at current position so don't need to know - choose first (default)
				allele = alternateAlleles[genotype[0] - 1];
			}
		}
		
		return allele;		
	}
}

