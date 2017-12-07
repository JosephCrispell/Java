package phylogeneticTree;

import java.util.Hashtable;

public class BeastNewick {

    public Hashtable<Integer, String> sampleNames;
    public int[] treeStates;
    public String[] newickTrees;
    public Hashtable<String, double[]>[] treeInfo;
    
    public BeastNewick(Hashtable<Integer, String> samples, int[] states, String[] trees, Hashtable<String, double[]>[] info){
    	this.sampleNames = samples;
    	this.treeStates = states;
    	this.newickTrees = trees;
    	this.treeInfo = info;
    	
	}
    
    // Methods for Setting 
    
    public void setSampleNames(Hashtable<Integer, String> samples){
    	this.sampleNames = samples;
    }
    public void setTreeStates(int[] states){
    	this.treeStates = states;
    }
    public void setNewickTrees(String[] trees){
    	this.newickTrees = trees;
    }
    public void setTreeInfo(Hashtable<String, double[]>[] info4Trees){
    	this.treeInfo = info4Trees;
    }
    
    
    // Methods for Getting

    public Hashtable<Integer, String> getSampleNames(){
   	 	return this.sampleNames;
    }
    public int[] getTreeStates(){
    	return this.treeStates;
    }
    public String[] getNewickTrees(){
   	 	return this.newickTrees;
    }
    public Hashtable<String, double[]>[] getTreeInfo(){
    	return this.treeInfo;
    }
    public int getNoTrees(){
    	return treeStates.length;
    }
}
