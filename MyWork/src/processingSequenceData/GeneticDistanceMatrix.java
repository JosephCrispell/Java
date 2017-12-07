package processingSequenceData;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Hashtable;

import methods.WriteToFile;
import filterSensitivity.DistanceMatrix;
import filterSensitivity.DistanceMatrixMethods;
import filterSensitivity.Sequence;

public class GeneticDistanceMatrix {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		// TODO Auto-generated method stub

		String fasta="C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/FitEpiVarsToGeneticDists/isolateSequencesJava.fasta";
		String outFile="C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/FitEpiVarsToGeneticDists/geneticDistances_Removed-314-315_WC.txt";
		
		Sequence[] sequences = DistanceMatrixMethods.readFastaFile(fasta);
		
		DistanceMatrix distanceMatrix = DistanceMatrixMethods.buildDistanceMatrix(sequences, "pDistance");
		
		int westCoastOnly = 1;
		int removeNA = 1;
		String[] remove = {"AgR314", "AgR315"};
		printIntoColumn(distanceMatrix, outFile, westCoastOnly, removeNA, remove);
	}
	
	public static Hashtable<String, Integer> isolates2Ignore(String[] isolates){
		
		Hashtable<String, Integer> ignore = new Hashtable<String, Integer>();
		
		for(String isolate : isolates){
			ignore.put(isolate, 1);
		}
		
		return ignore;
	}
	
	public static void printIntoColumn(DistanceMatrix distanceMatrixInfo, String fileName, int westCoastOnly, 
			int removeNA, String[] isolates2Remove) throws IOException{
		/**
		 *  Print the Distance Matrix out to File:
		 *  	Genetic
		 *  	-
		 *  	-
		 *  	-
		 *  
		 *  To Avoid writing to file on multiple occasions a String is built
		 *  
		 *  Sample Name Structure:
		 *  AgR111		S8		STATIONBF		-44.419		170.256		MACKENZIECOUNTRY	1987	POSSUM	11	1
		 *  SampleID	runID	Region			Latitude	Longitude	Location			Year	Species	REA	SampleNo
		 *  0			1		2				3			4			5					6		7		8	9
		 */
		
		// Open and Wipe File
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Note the isolates to ignore
		Hashtable<String, Integer> ignore = isolates2Ignore(isolates2Remove);
		String sampleI;
		String sampleJ;
		
		// Extract Distance Matrix Information
		double[][] d = distanceMatrixInfo.getDistanceMatrix();
		String[] sampleNames = distanceMatrixInfo.getSampleNames();
		String column = "Genetic" + "\n";
		
		// Add Distance Matrix Elements
		for(int i = 0; i < sampleNames.length; i++){
			
			// Get Sample ID
			sampleI = sampleNames[i].split("_")[0];
			
			// What isolates are we interested in?
			if(sampleNames[i].matches("(.*)WESTCOAST(.*)") == false && westCoastOnly == 1){
				continue;
			}else if(sampleNames[i].matches("(.*)NA_NA(.*)") == true && removeNA == 1){
				continue;
			}if(ignore.get(sampleI) != null){
				continue;
			}
			
			for(int j = 0; j < sampleNames.length; j++){
				
				// Get Sample ID
				sampleJ = sampleNames[j].split("_")[0];
				
				// What isolates are we interested in?
				if(sampleNames[j].matches("(.*)WESTCOAST(.*)") == false && westCoastOnly == 1){
					continue;
				}else if(sampleNames[j].matches("(.*)NA_NA(.*)") == true && removeNA == 1){
					continue;
				}if(ignore.get(sampleJ) != null){
					continue;
				}
								
				// Only compare isolates once and avoid self-comparisons
				if(i < j){
					column = column + d[i][j] + "\n";
				}
				
			}
		}
		
		// Print out Distance Matrix
		WriteToFile.write(bWriter, column);	
		WriteToFile.close(bWriter);
	}

}
