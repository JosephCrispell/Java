package newProcessingSequencingData;

import java.util.Hashtable;

public class HeterozygousSite {
	
	public int position;
	public String referenceAllele;
	public String[] alternateAlleles;
	public double qualityScore;
	public Hashtable<String, double[]> infoCol;
	public Hashtable<String, double[]> formatCol;
	
	public HeterozygousSite(int pos, String ref, String[] alternates, double quality, Hashtable<String, double[]> info,
			Hashtable<String, double[]> format){
		
		this.position = pos;
		this.referenceAllele = ref;
		this.alternateAlleles = alternates;
		this.qualityScore = quality;
		this.infoCol = info;
		this.formatCol = format;
	}

	// Getting methods
	public int getPosition() {
		return position;
	}
	public String getReferenceAllele(){
		return this.referenceAllele;
	}
	public String[] getAlternateAlleles() {
		return alternateAlleles;
	}
	public double getQualityScore() {
		return qualityScore;
	}
	public Hashtable<String, double[]> getInfoCol() {
		return infoCol;
	}
	public Hashtable<String, double[]> getFormatCol() {
		return formatCol;
	}
}
