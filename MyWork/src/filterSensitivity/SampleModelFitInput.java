package filterSensitivity;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Hashtable;

import methods.WriteToFile;

public class SampleModelFitInput {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		
		/**
		 *  Here simply creating a Distance Matrix based upon on a Fasta File - P - Distance
		 *  
		 *  Then printing the Genetic Distances out to File:
		 *  Genetic
		 *  	-
		 *  	-
		 *  	-
		 *  
		 * 	Note that essentially just printing out each element of the distance matrix 
		 */
		
		if(args[0].matches("-help")){
			System.out.println("JAR file to create genetic distance column.");
			System.out.println("Input information:");
			System.out.println("FASTA\tOUTPUT\tWestCoastOnly?\tRemoveNA?\tIsolates2Remove");
			System.out.println("Note that isolates to remove should be a comma delimited list.");
			
		}else{
			
			//System.out.println("FASTA: " + args[0] + "\nOUTPUT: " + args[1] + "\nWestCoast?: " + args[2] + "\nIngoreNA: " + args[3] + "\nIsolates2Ignore: " + args[4]);
		
			//String file0="C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/ModelFitting/sequenceFastaJava.txt";
			//String file1="C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/ModelFitting/geneticDistances.txt";
		
			Sequence[] sequences = DistanceMatrixMethods.readFastaFile(args[0]);
			//Sequence[] sequences = DistanceMatrixMethods.readFastaFile(file0);
		
			DistanceMatrix distanceMatrix = DistanceMatrixMethods.buildDistanceMatrix(sequences, "pDistance");
		
			//int westCoast = 1;
			//int removeNA = 1;
			//String[] remove = {"AgR314", "AgR315"};
			printIntoColumn(distanceMatrix, args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), args[4].split(","));
			//printIntoColumn(distanceMatrix, file1, westCoast, removeNA, remove);
		}
		
	}
	
	public static Hashtable<String, Integer> noteWbIdsToIgnore(String[] array){
		
		Hashtable<String, Integer> ignore = new Hashtable<String, Integer>();
		
		for(String element : array){
			
			ignore.put(element, 1);
		}
		
		return ignore;		
	}
	
	public static void printIntoColumn(DistanceMatrix distanceMatrix, String fileName, int westCoast, 
			int removeNA, String[] remove) throws IOException{
		/**
		 *  Print the Distance Matrix out to File:
		 *  	Genetic
		 *  	-
		 *  	-
		 *  	-
		 *  
		 *  To Avoid writing to file on multiple occasions a String is built
		 *  
		 */
		
		// Note WBIDs to be ignored
		Hashtable<String, Integer> ignore = noteWbIdsToIgnore(remove);
		
		// Open and Wipe File
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Extract Distance Matrix Information
		double[][] d = distanceMatrix.getDistanceMatrix();
		String[] sampleNames = distanceMatrix.getSampleNames();
		String column = "Genetic" + "\n";
		String wbIdI;
		String wbIdJ;
		
		// Add Distance Matrix Elements
		for(int i = 0; i < sampleNames.length; i++){
				
			wbIdI = sampleNames[i].split("_")[0];
		
			// Which isolates are we ignoring?
			if(sampleNames[i].matches("(.*)WESTCOAST(.*)") == false && westCoast == 1){
				continue;
			}else if(sampleNames[i].matches("(.*)NA_NA(.*)") == true && removeNA == 1){
				continue;
			}else if(ignore.get(wbIdI) != null){
				continue;
			}
			
			for(int j = 0; j < sampleNames.length; j++){
				
				// Avoid making comparisons more than once and avoid the diagonal
				if(i >= j){
					continue;
				}
				
				wbIdJ = sampleNames[j].split("_")[0];
				
				// Which isolates are we ignoring?
				if(sampleNames[j].matches("(.*)WESTCOAST(.*)") == false && westCoast == 1){
					continue;
				}else if(sampleNames[j].matches("(.*)NA_NA(.*)") == true && removeNA == 1){
					continue;
				}else if(ignore.get(wbIdJ) != null){
					continue;
				}
			
				// Record the Genetic Distance
				column = column + d[i][j] + "\n";
			}
		}
		
		// Print out Distance Matrix
		WriteToFile.write(bWriter, column);	
		WriteToFile.close(bWriter);
	}
	
	public static void printIntoColumn(DistanceMatrix distanceMatrixInfo, String fileName, int vntr10, int westCoast, 
			int removeNA) throws IOException{
		/**
		 *  Print the Distance Matrix out to File:
		 *  	Genetic
		 *  	-
		 *  	-
		 *  	-
		 *  
		 *  To Avoid writing to file on multiple occasions a String is built
		 *  
		 */
		
		// Open and Wipe File
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Extract Distance Matrix Information
		double[][] d = distanceMatrixInfo.getDistanceMatrix();
		String[] sampleNames = distanceMatrixInfo.getSampleNames();
		String column = "Genetic" + "\n";
		
		// Create a Matrix to Record what is to be printed - make sure information printed once only
		int[][] record = new int[sampleNames.length][sampleNames.length];
		
		//System.out.println("Examining " + sampleNames.length + " Samples");
		
		//int comparisons = 0;
		
		// Add Distance Matrix Elements
		for(int i = 0; i < sampleNames.length; i++){
			
			/**
			 *  ************* Removal of None VNTR 10 Samples *************
			 *  Sample Name Structure:
			 *  	NSampleNo_AnimalID_HerdID_EpidsodeID_Year_Badger_SampleID
			 *  	0		  1		   2      3			 4	  5		 6
			 *  
			 *  Samples from the following Herds: 29895 & 31121 are not from the VNTR 10 group
			 */
			
			if(sampleNames[i].matches("(.*)29895(.*)") || sampleNames[i].matches("(.*)31121(.*)")){
				if(vntr10 == 1){
					//System.out.println(sampleNames[i]);
					continue;
				}				
			}else if(!sampleNames[i].matches("(.*)WESTCOAST(.*)")){
				if(westCoast == 1){
					//System.out.println(sampleNames[i] + "\tWest Coast Removal");
					continue;
				}
			}else if(sampleNames[i].matches("(.*)NA_NA(.*)")){
				if(removeNA == 1){
					//System.out.println(sampleNames[i] + "\t NA Removal");
					continue;
				}
			}
			
			for(int j = 0; j < sampleNames.length; j++){
				
				// Are we ignoring particular Types?
				if(sampleNames[j].matches("(.*)29895(.*)") || sampleNames[i].matches("(.*)31121(.*)")){
					if(vntr10 == 1){ 
						continue;
					}				
				}else if(!sampleNames[j].matches("(.*)WESTCOAST(.*)")){
					if(westCoast == 1){
						continue;
					}
				}else if(sampleNames[j].matches("(.*)NA_NA(.*)")){
					if(removeNA == 1){
						continue;
					}
				}
								
				// Avoid the Diagonal
				if(i != j && record[i][j] == 0){
					column = column + d[i][j] + "\n";
					
					//comparisons++;
					
					// Record that this Distance has been Printed
					record[i][j] = 1;
					record[j][i] = 1;
				}
				
			}
		}
		
		//System.out.println("Made " + comparisons + " Comparisons");
		
		// Print out Distance Matrix
		WriteToFile.write(bWriter, column);	
		WriteToFile.close(bWriter);
	}
}
