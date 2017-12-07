package filterSensitivity;

public class Sequence{

	public String sampleName;
	public char[] sequence;
   
    public Sequence(String sample, char[] nucleotides){
    	this.sampleName = sample;
	   	this.sequence = nucleotides;
	}

    
    // Methods for Setting
    
    public void setSampleName(String sample){
    	this.sampleName = sample;
    }
    
    public void setSequence(char[] nucleotides){
    	this.sequence = nucleotides;
    }
    
    
    // Methods for Getting

    public String getSampleName(){
   	 	return sampleName;
    }
    public char[] getSequence(){
   	 	return sequence;
    }
}