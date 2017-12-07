package woodchesterCattle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.HashtableMethods;
import methods.WriteToFile;

public class ExamineOutbreakHistory {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		// Get the Herd Test Data
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/";
		String herdTestDataFile = path + "CattleHerdTestData/20160217_joe_vetnet_allherdtests_1990-2012.csv";
		//String herdTestDataFile = path + "CattleHerdTestData/test.txt";
		
		// Get the Cattle Isolate data
		String isolateInfo = path + "NewAnalyses_02-06-16/IsolateData/CattleIsolateInfo_LatLongs_plusID.csv";
		
		// Are we using CPH or CPHHs?
		int useCPH = 1;
		
		// Get a list of the CPHHs of interest, along with the sampling dates associated with them
		Hashtable<String, IsolateData[]> cphhs = getCPHForEachIsolate(isolateInfo, useCPH);
		
		// Search for the CPHHs in the herd test history
		Hashtable<String, Integer> outbreakSizes = searchForCPHHsInTestHistory(herdTestDataFile, cphhs, 180, useCPH);
				
		System.out.println("Number of isolates found = " + outbreakSizes.size() + "\n");
		
		// Print the outbreak size found for each of the isolates
		for(String key : HashtableMethods.getKeysString(outbreakSizes)){
			System.out.println(key + "\t" + outbreakSizes.get(key));
		}		
		System.out.println();
		
		// Print the outbreak size info out to file
		String output = path + "NewAnalyses_02-06-16/IsolateData/CattleIsolateInfo_LatLongs_plusID_outbreakSize.csv";
		addOutbreakSizeToIsolateInfoFile(isolateInfo, outbreakSizes, output);
	}
	
	public static void addOutbreakSizeToIsolateInfoFile(String isolateInfoFile, Hashtable<String, Integer> outbreakSizes,
			String outputFile) throws IOException{
		
		// Open the isolate information file
		InputStream input = new FileInputStream(isolateInfoFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
		
		// Initialise a variable to store the outbreak size
		String size;
		
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		int lineNo = 0;
										
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			lineNo++;
			
			// Print the header out with an extra field
			if(lineNo == 1){
				WriteToFile.writeLn(bWriter, line + ",OutbreakSize");
				continue;
			}

			// Do we have an outbreak size for the current isolate?
			cols = line.split(",");
			size = "NA";
			if(outbreakSizes.get(cols[35]) != null){
				size = Integer.toString(outbreakSizes.get(cols[35]));
			}
			
//			if(size.matches("NA")){
//				System.out.println(cols[12] + "\t"+ cols[16] + "\t" + cols[5]);
//			}
			
			// Print the isolate info with the outbreak size, if its available
			WriteToFile.writeLn(bWriter, line + "," + size);
		}
		
		// Close the input and output files
		input.close();
		reader.close();
		WriteToFile.close(bWriter);
	}
	
	public static Hashtable<String, Integer> searchForCPHHsInTestHistory(String herdTestDataFile, Hashtable<String, IsolateData[]> cphhs,
			int nDays, int useCPH) throws IOException{
		
		/**
		 * Structure of the herd test history table:
		 * 	cphh	test_date	test_type	number	reactors	number_not_tested
		 * 	0		1			2			3		4			5
		 */
		
		// Open the animals table file
		InputStream input = new FileInputStream(herdTestDataFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise a variable to store the test history date
		Calendar date;
		int[] dateFormat = {2, 1, 0};
		int[] size = {0, 0};
		
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		int lineNo = 0;
		
		// Initialise a hashtable to store the outbreak size associated with each cultured isolate
		Hashtable<String, Integer> outbreakSizes = new Hashtable<String, Integer>();
		
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			lineNo++;
					
			// Skip the header and last line
			if(lineNo == 1 || line.matches("(.*)rows(.*)")){
				continue;
			}
			
			// Split the line into its columns. NOTE - split function chops out empty columns!!!!!!
			cols = line.split(",");
			
			// Check a CPHH is present
			if(cols[0].matches("")){
				continue;
			}
			
			// Convert the CPHH to a CPH
			if(useCPH == 1){
				cols[0] = cols[0].substring(0, cols[0].length() - 2);
			}
			
			// Check if this is a CPHH we are interested in?
			if(cphhs.get(cols[0]) != null){
				
				// Get the date for the current test history
				date = CalendarMethods.parseDate(cols[1], "-", dateFormat);
				
				// Examine each of the isolates associated with the current CPHH
				for(IsolateData isolate : cphhs.get(cols[0])){
					
					// Does the current isolate's date fall close to the breakdown date?
					if(CalendarMethods.checkIfDatesAreWithinRange(isolate.getCultureDate(), date, nDays)){
						
						// Get the outbreak size, check if this a slaughter house observation
						size[0] = 0;
						size[1] = 0;
						if(cols[2].matches("VE-SLH")){
							size[0] = 1;
							size[1] = 1;
						}else{
							size[0] = Integer.parseInt(cols[3]);
							
							// Were there any reactors?
							if(cols.length >= 5 && cols[4].matches("") == false){
								size[1] = Integer.parseInt(cols[4]);
							}
						}
						
						// Were no reactors found?
						if(size[1] == 0){
							continue;
						}
						
						// Has outbreak data already been found for the current isolate?
						if(outbreakSizes.get(isolate.getEartag()) != null){
							
							outbreakSizes.put(isolate.getEartag(), outbreakSizes.get(isolate.getEartag()) + size[1]);						
						}else{
							outbreakSizes.put(isolate.getEartag(), size[1]);
						}
					}
				}
			}
			
			// Print information about progress
			if(lineNo % 500000 == 0){
				System.out.println("Finished Examining line: " + lineNo);
			}
		}
				
		// Close the cattle isolate information table
		input.close();
		reader.close();
		
		return outbreakSizes;
	}

	public static Hashtable<String, IsolateData[]> getCPHForEachIsolate(String isolateInfoFileName, int useCPH) throws IOException{
		
		/**
		 * Structure of the cattle isolate information table:
		 * 	CPH_10km	SampleRef	CultureResult	DateCultured	ReasonForSlaughter	SkinTestType
		 * 	0			1			2				3				4					5
		 * 	
		 * 	Species	DeerType	Mapx	Mapy	MapRef	LesionsFound	Eartag	Database	CPH	CPHH	
		 * 	6		7			8		9		10		11				12		13			14	15
		 * 
		 * 	BreakdownID	County	BadgerInvestigationNo	Year	Spoligotype	Genotype	ComplVNTR	VNTR
		 * 	16			17		18						19		20			21			22			23
		 * 
		 * 	Profile2	VNStatus	VNPtA	VNPtB	VNPtC	VNPtD	VNPtE	VNPtF	VNPtG	VNPtH	VNPtI	
		 * 	24			25			26		27		28		29		30		31		32		33		34
		 * 
		 * 	Rawtag	BreakYr	VLA.Genotype	GenotypeFirst	GenotypeSecond	GenotypeThird	TypingYear	Name
		 * 	35		36		37				38				39				40				41			42
		 * 
		 * 	Location	Expected_Genotype1	Expected_Genotype2	Expected_Genotype3	Expected_Genotype4	
		 * 	43			44					45					46					47
		 * 
		 * 	Expected_Genotype5	Latitude	Longitude	StrainId	MovementsAnimalID
		 * 	48					49			50			51			52
		 * 
		 * BreakdownID: CPHH-Date: 14082000501-23/02/1999
		 */
		
		// Initialise a Hashtable to store the CPH and the dates of isolates from those CPHs
		Hashtable<String, IsolateData[]> cphhs = new Hashtable<String, IsolateData[]>();
		
		// Open the isolate information file
		InputStream input = new FileInputStream(isolateInfoFileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise variables to deal with the date
		IsolateData[] isolates = new IsolateData[1];
		int[] dateFormat = {0, 1, 2};
		Calendar date;
		
		// Initialise a variable to store the CPHH
		String cphh = "";
		
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		int lineNo = 0;
										
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			lineNo++;
			
			// Skip the header line
			if(lineNo == 1){
				continue;
			}
			
			// Reset the array of dates
			isolates = new IsolateData[1];
			
			// Split the line into its columns
			cols = line.split(",");
			
			// Get the date for the current isolate
			date = CalendarMethods.parseDate(cols[16].split("-")[1], "/", dateFormat);
						
			// Get the CPHH
			cphh = cols[16].split("-")[0];
			
			// Check if we want to only use the CPH
			if(useCPH == 1){
				cphh = cphh.substring(0, cphh.length() - 2);
			}
			
			// Set the isolates data
			isolates[0] = new IsolateData(cols[35], cphh, date, cols[51], cols);
			
			// Check whether we have come across this CPHH before
			if(cphhs.get(cphh) != null){
				cphhs.put(cphh, IsolateData.append(cphhs.get(cphh), isolates[0]));
			}else{
				cphhs.put(cphh, isolates);
			}
		}
		
		// Close the cattle isolate information table
		input.close();
		reader.close();
		
		return cphhs;
	}
}
