package newProcessingSequencingData;

import java.io.BufferedReader;
import java.io.IOException;

public class VcfFile {

	public BufferedReader bfReader; // Buffered reader that allows access to file lines
	public int shift = 0; // Variable to tell whether or not we want to get the next file line
	public String header; // Store VCF file header
	public String fileName;
	public String id;
	public HeterozygousSite[] heterozygousSites = new HeterozygousSite[999];
	public int hetSitePos = -1;
	
	public VcfFile(String file, String sampleId, BufferedReader reader, int move) throws IOException {
		
		this.fileName = file;
		this.id = sampleId;
		
		// Get the file header and set Buffered Reader
		getHeader(reader);
		
		// Default is move to next line
		this.shift = move;
	}
	
	// Setting
	public void setShift(int move){
		this.shift = move;
	}
	public void addToHeterozygousSiteCount(HeterozygousSite siteInfo){
		
		this.hetSitePos++;
		if(this.hetSitePos < this.heterozygousSites.length){
			this.heterozygousSites[this.hetSitePos] = siteInfo;
		}else{
			
			HeterozygousSite[] newArray = new HeterozygousSite[this.heterozygousSites.length + 999];
			for(int i = 0; i < this.heterozygousSites.length; i++){
				newArray[i] = this.heterozygousSites[i];
			}
			newArray[this.hetSitePos] = siteInfo;
			this.heterozygousSites = newArray;
		}
		
	}
	
	// Getting
	public int getHeterozygousSiteCount(){
		return this.hetSitePos + 1;
	}
	public String getFileName(){
		return this.fileName;
	}
	public String getId(){
		return this.id;
	}
	public BufferedReader getBfReader(){
		return this.bfReader;
	}
	public int getShift(){
		return this.shift;
	}
	public String getHeader(){
		return this.header;
	}

	
	// General Methods
	public void getHeader(BufferedReader reader) throws IOException{
		
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
				
				// Store the Header
				this.header = headerString;
				
				// Store the Remaining lines in Buffered Reader
				this.bfReader = reader;
				
				// Finished
				foundFields = 1;
			}
		}
	}

}
