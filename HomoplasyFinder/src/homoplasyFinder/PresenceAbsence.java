package homoplasyFinder;

import java.util.ArrayList;

public class PresenceAbsence {

	public ArrayList<int[]> regionCoords;
	public ArrayList<Sequence> booleanSequences; // Stored as sequences of 1s and 0s as characters
	
	public PresenceAbsence(ArrayList<int[]> coords, ArrayList<Sequence> sequences) {
		
		this.regionCoords = coords;
		this.booleanSequences = sequences;
	}

	// Getting methods
	public ArrayList<int[]> getRegionCoords() {
		return regionCoords;
	}
	public ArrayList<Sequence> getBooleanSequences() {
		return booleanSequences;
	}	
}
