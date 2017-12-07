package ExamineWoodchesterParkCaptureData;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.HashtableMethods;
import methods.WriteToFile;

public class Consolidate {

	public static void main(String[] args) throws IOException{
		
		// Get the date
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
		
		// Set the path
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_13-07-17/BadgerCaptureData/";
		
		// Open the capture data file
		String captureDataFile = path + "WP_CaptureData.csv";
		CaptureEvent[] captureData = ExamineCaptureData.readBadgerCaptureData(captureDataFile);
		
		// Assign the capture events to individual badgers
		Hashtable<String, CaptureEvent[]> badgerCaptureHistories = ExamineCaptureData.assignCaptureEventsToBadgers(captureData);
		
		// Order each badgers capture events by their date
		ExamineCaptureData.sortCaptureEventInfoForEachBadgerByDate(badgerCaptureHistories);
		
		// Informatively remove NA social groups (If caught in same group before and after - replace)
		ExamineCaptureData.informativelyReplaceNASocialGroups(badgerCaptureHistories);
		
		// Print the capture data out to file
		String consolidatedDataFile = path + "WP_CaptureData_Consolidated_" + date + ".txt";
		printCaptureDataInConsolidatedForm(consolidatedDataFile, badgerCaptureHistories);

	}
	
	public static void printCaptureDataInConsolidatedForm(String fileName, Hashtable<String, CaptureEvent[]> badgerCaptureHistories) throws IOException{
		
		/**
		 * Output file structure:
		 * 	Tattoo	Sex	Age_FC	@CaptureDates	@Groups	@GammaIFN	@StatPak	@ELISA	@Culture	@VNTRs
		 * 	0		1	2		3				4		5			6			7		8			9
		 * 
		 * 	@Spoligotypes	@PostMortem	@Statuses
		 * 	10				11			12
		 * 
		 * 	Columns with @ prefix represent arrays, separated by ";"
		 */
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Print out a header
		String header = "Tattoo\tSex\tAge_FC\t@CaptureDates\t@Groups\t@GammaIFN\t@StatPak\t@ELISA\t@Culture";
		header += "\t@VNTRs\t@Spoligotypes\t@PostMortem\t@Statuses";
		WriteToFile.writeLn(bWriter, header);
		
		// Examine each badger
		for(String tattoo : HashtableMethods.getKeysString(badgerCaptureHistories)){
			
			WriteToFile.writeLn(bWriter, consolidateBadgersCaptureData(tattoo, badgerCaptureHistories.get(tattoo)));
		}
				
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
	public static String consolidateBadgersCaptureData(String tattoo, CaptureEvent[] captureEvents){
		
		/**
		 * Consolidated string structure:
		 * 	Tattoo	Sex	Age_FC	@CaptureDates	@Groups	@GammaIFN	@StatPak	@ELISA	@Culture	@VNTRs
		 * 	0		1	2		3				4		5			6			7		8			9
		 * 
		 * 	@Spoligotypes	@PostMortem	@Statuses
		 * 	10				11			12
		 */
		
		// Initialise an array to store the consolidate data
		String[] consolidated = ArrayMethods.repeat("", 13);
		
		// Store the badger tattoo
		consolidated[0] = tattoo;
		consolidated[1] = "NA";
		consolidated[2] = "NA";
		
		// Examine each capture event
		for(int i = 0; i < captureEvents.length; i++){
			
			// Is the sex available?
			if(consolidated[1].matches("NA") && captureEvents[i].getSex() != 'N'){
				consolidated[1] = "FEMALE";
				if(captureEvents[i].getSex() == 'M'){
					consolidated[1] = "MALE";
				}
			}
			
			// Is the age at first capture available?
			if(consolidated[2].matches("NA") && captureEvents[i].getAgeAtFirstCapture() != null){
				consolidated[2] = captureEvents[i].getAgeAtFirstCapture();
			}
			
			// Date - 3
			if(i == 0){
				consolidated[3] = CalendarMethods.toString(captureEvents[i].getDate(), "-");
			}else{
				consolidated[3] += ";" + CalendarMethods.toString(captureEvents[i].getDate(), "-");
			}
			
			// Social Group - 4
			if(i == 0){
				consolidated[4] = captureEvents[i].getSocialGroup();
			}else{
				consolidated[4] += ";" + captureEvents[i].getSocialGroup();
			}
			
			// Gamma IFN - 5
			if(i == 0){
				consolidated[5] = returnResultAsString(captureEvents[i].getGamma());
			}else{
				consolidated[5] += ";" + returnResultAsString(captureEvents[i].getGamma());
			}
			
			// Stat Pak - 6
			if(i == 0){
				consolidated[6] = returnResultAsString(captureEvents[i].getStatpak());
			}else{
				consolidated[6] += ";" + returnResultAsString(captureEvents[i].getStatpak());
			}
			
			// ELISA - 7
			if(i == 0){
				consolidated[7] = returnResultAsString(captureEvents[i].getElisa());
			}else{
				consolidated[7] += ";" + returnResultAsString(captureEvents[i].getElisa());
			}
			
			// Culture - 8
			if(i == 0){
				consolidated[8] = returnResultAsString(captureEvents[i].getCulture());
			}else{
				consolidated[8] += ";" + returnResultAsString(captureEvents[i].getCulture());
			}
			
			// VNTR - 9
			if(i == 0){
				consolidated[9] = captureEvents[i].getVntr();
			}else{
				consolidated[9] += ";" + captureEvents[i].getVntr();
			}
			
			// Spoligotypes - 10
			if(i == 0){
				consolidated[10] = captureEvents[i].getSpoligotype();
			}else{
				consolidated[10] += ";" + captureEvents[i].getSpoligotype();
			}
			
			
			// Post Mortem - 11
			if(i == 0){
				consolidated[11] = returnPostMortemResult(captureEvents[i].getPostMortem());
			}else{
				consolidated[11] += ";" + returnPostMortemResult(captureEvents[i].getPostMortem());
			}
			
			// Status - 12
			if(i == 0){
				consolidated[12] = captureEvents[i].getStatus();
			}else{
				consolidated[12] += ";" + captureEvents[i].getStatus();
			}
		}
		
		return ArrayMethods.toString(consolidated, "\t");
	}
	
	public static String returnResultAsString(boolean result){
		String output = "Negative";
		if(result == true){
			output = "Positive";
		}
		
		return(output);
	}
	
	public static String returnPostMortemResult(boolean result){
		String output = "NO";
		if(result == true){
			output = "YES";
		}
		
		return(output);
	}
}
