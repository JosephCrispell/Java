package processingSequenceData;

import java.io.BufferedReader;
import java.io.IOException;

import methods.ArrayMethods;

public class MergedFileBufferedReader {

	public BufferedReader bfReader; // Buffered reader that allows access to file lines
	public int shift = 0; // Variable to tell whether or not we want to get the next file line
	public String header; // Store VCF file header
	public String[] sampleNames; // Store sample names 
	public String blankInfo4Samples; // Variable to store a blank insert for when no information amongst the samples is available for a given SNP
	
	public MergedFileBufferedReader(BufferedReader reader, int move) throws IOException {
		
		// Get the file header and sample names
		getHeaderAndSampleNames(reader);
		
		// Default is move to next line
		this.shift = move;
	}
	
	// Setting
	public void setShift(int move){
		this.shift = move;
	}
	
	// Getting
	public BufferedReader getBfReader(){
		return this.bfReader;
	}
	public int getShift(){
		return this.shift;
	}
	public String getHeader(){
		return this.header;
	}
	public String[] getSampleNames(){
		return this.sampleNames;
	}
	public String getBlankInfo4Samples(){
		return this.blankInfo4Samples;
	}
	
	// General Methods
	public void getHeaderAndSampleNames(BufferedReader reader) throws IOException{
		
		// Initialise variables for reading Header and fields
		String line;
		int foundFields = 0;
		String headerString = "";
		
		// Start reading file - stop when reached fields line
		while(foundFields == 0){
			
			// Get next file line
			line = reader.readLine();
			
			// Is the current line a header line?
			if(line.matches("##(.*)")){
				
				// Store header line
				if(headerString.matches("") == false){
					headerString = headerString + "\n" + line;
				}else{
					headerString = line;
				}
			
			// Is the current line the Fields line?
			}else if(line.matches("#(.*)")){
				
				// Store the sample names and create a blank info String for when position is absent in all samples
				getSampleNames(line);
				createBlankSampleInfo();
				
				// Store the header lines
				this.header = headerString;
				
				// Store the Remaining lines in Buffered Reader
				this.bfReader = reader;
				
				// Finished
				foundFields = 1;
			}
		}
	}
	
	public void getSampleNames(String fieldsLine){
		
		// Sample Names: #CHROM	POS	SampleNameA:SampleNameB:SampleNameC:...		
		String[] parts = fieldsLine.split("\t");
		
		this.sampleNames = parts[2].split(":");
	}
	
	public void createBlankSampleInfo(){
		
		// A blank is inserted when no information is available for the current sample
		String singleSampleBlank = "-------------------";
		String blank = "";
		
		// Create a blank sample information for every sample in set
		for( int i = 0; i < this.sampleNames.length; i++){
			if(i == 0){
				blank = singleSampleBlank;
			}else{
				blank = blank + ":" + singleSampleBlank;
			}
		}
		
		// Blank is the correct size - one for each sample in merged.txt file
		this.blankInfo4Samples = blank;
	}
}
