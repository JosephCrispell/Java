package newProcessingSequencingData;

public class FilterSet {

	public int readDepth;
	public int highQualityBaseDepth;
	public int mappingQuality;
	public double alleleSupport;
	public double qualityScore;
	public double proportionIsolatesWithCoverage;
	public int fq;
	
	public FilterSet(int dp, int hqdp, int mq, double support, double qual, double prop, int fqValue){
		
		this.readDepth = dp;
		this.highQualityBaseDepth = hqdp;
		this.mappingQuality = mq;
		this.alleleSupport = support;
		this.qualityScore = qual;
		this.proportionIsolatesWithCoverage = prop;
		this.fq = fqValue;
	}
	
	// Getting Methods
	public int getReadDepth(){
		return this.readDepth;
	}
	public int getHighQualityBaseDepth(){
		return this.highQualityBaseDepth;
	}
	public int getMappingQuality(){
		return this.mappingQuality;
	}
	public double getAlleleSupport(){
		return this.alleleSupport;
	}
	public double getQualityScore(){
		return this.qualityScore;
	}
	public double getProportionIsolatesWithCoverage(){
		return this.proportionIsolatesWithCoverage;
	}
	public int getFq(){
		return this.fq;
	}
	
}
