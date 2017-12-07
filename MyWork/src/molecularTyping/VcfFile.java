package molecularTyping;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.zip.GZIPInputStream;

import methods.ArrayMethods;

public class VcfFile {

	public String file;
	public String header;
	public int[] positionsPresent;
	public Hashtable<Integer, SnpInfo> infoForEachPosition = new Hashtable<Integer, SnpInfo>();
	public double averageDepth;
	public double regionCoverage;
		
	public VcfFile(String fileName, int[] regionOfInterest) throws IOException{
		
		this.file = fileName;
		readFile(fileName, regionOfInterest);
		
	}
	
	// Setting methods
	public void setAverageDepth(double value){
		this.averageDepth = value;
	}
	public void setRegionCoverage(double value){
		this.regionCoverage = value;
	}
	
	// Getting methods
	public Hashtable<Integer, SnpInfo> getInfoForEachPosition(){
		return this.infoForEachPosition;
	}
	public double getAverageDepth(){
		return this.averageDepth;
	}
	public double getRegionCoverage(){
		return this.regionCoverage;
	}
	
	// Methods for parsing the VCF File
 	public void readFile(String fileName, int[] regionOfInterest) throws IOException{
		
		// Initialise objects for opening and reading files
		InputStream input;
		BufferedReader bfReader = null;
		GZIPInputStream gZipFile;
 		
		// Check if file is zipped
		if(fileName.matches("(.*).gz") == true){
			
			// Open the current vcf file
			input = new FileInputStream(fileName);
			gZipFile = new GZIPInputStream(input);
			bfReader = new BufferedReader(new InputStreamReader(gZipFile));
			
		}else{
			// Open the current vcf file
			input = new FileInputStream(fileName);
			bfReader = new BufferedReader(new InputStreamReader(input));
		}	

		// Initialise the necessary variables for parsing the VCF file
		String line;
		this.header = "";
		String[] cols;
		int genomePos;
		
		// Create large array to store genome positions found
		this.positionsPresent = new int[10];
		int pos = -1;
		
		// Begin reading the VCF file
		while(( line = bfReader.readLine()) != null){
			
			// Store the header lines
			if(line.matches("#(.*)") == true){
				this.header = this.header + "\n" + line;
				continue;
			}
			
			// Split the current line into its columns
			cols = line.split("\t");
			
			
			// Store the sequencing quality information for current position if it is in the region of interest
			genomePos = Integer.parseInt(cols[1]);
			if(regionOfInterest != null && genomePos >= regionOfInterest[0] && genomePos <= regionOfInterest[1]){
				pos++;
				this.positionsPresent = addPosition(this.positionsPresent, genomePos, pos);
				infoForEachPosition.put(genomePos, new SnpInfo(line, this.file));
			}else if(regionOfInterest == null){
				pos++;
				this.positionsPresent = addPosition(this.positionsPresent, genomePos, pos);
				infoForEachPosition.put(genomePos, new SnpInfo(line, this.file));
			}
			
			// Finish is past region of interest
			if(regionOfInterest != null && genomePos > regionOfInterest[1]){
				break;
			}
		}
		
		// Keep only used positions of array
		this.positionsPresent = ArrayMethods.subset(this.positionsPresent, 0, pos);
		
		// Close the VCF file
		input.close();
		bfReader.close();
	}
	
 	public int[] addPosition(int[] array, int value, int pos){
 		
 		// Method to append to an integer array
 		
 		// Check if there is space in the current array
		if(pos < array.length){
			array[pos] = value;
		
		// If not, create a new array that is twice the length and fill it - including the new value
		}else{
			int[] newArray = new int[array.length * 2];
			for(int i = 0; i < array.length; i++){
				newArray[i] = array[i];
			}
			newArray[pos] = value;
			array = newArray;
		}
		
		return array;
	}
}
