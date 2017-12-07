package hungarianAlgorithm;

import java.util.Hashtable;

import methods.HashtableMethods;

public class CoveredRowsAndCols {

	public Hashtable<Integer, Integer> coveredRows;
	public Hashtable<Integer, Integer> coveredCols;
	public int[] rows = new int[0];
	public int[] cols = new int[0];
	
	public CoveredRowsAndCols(Hashtable<Integer, Integer> rows2Cover, Hashtable<Integer, Integer> cols2Cover){
		
		this.coveredRows = rows2Cover;
		this.coveredCols = cols2Cover;
		this.rows = HashtableMethods.getKeysInt(rows2Cover);
		this.cols = HashtableMethods.getKeysInt(cols2Cover);
	}
	
	// Setting Methods
	public void setCoveredRows(Hashtable<Integer, Integer> rowsCovered){
		this.coveredRows = rowsCovered;
		this.rows = HashtableMethods.getKeysInt(rowsCovered);
	}
	public void setCoveredCols(Hashtable<Integer, Integer> colsCovered){
		this.coveredCols = colsCovered;
		this.cols = HashtableMethods.getKeysInt(colsCovered);	
	}
	
	// Getting Methods
	public Hashtable<Integer, Integer> getCoveredRows(){
		return this.coveredRows;
	}
	public Hashtable<Integer, Integer> getCoveredCols(){
		return this.coveredCols;
	}
	public int[] getRows(){
		return this.rows;
	}
	public int[] getCols(){
		return this.cols;
	}

}
