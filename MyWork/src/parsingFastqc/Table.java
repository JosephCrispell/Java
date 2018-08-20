package parsingFastqc;

import java.util.Hashtable;

import methods.ArrayMethods;
import methods.HashtableMethods;

public class Table {

	private String name;
	private String[] colNames;
	
	// Recording table values
	private String[] colTypes; // String, int, double, int[]
	private Value[][] values;
	private int row = -1;
	
	public Table(String name, String[] colTypes) {
		
		// Store table name and column types
		this.name = name;
		
		// Initialise the values table
		this.colTypes = colTypes;
		this.values = new Value[100][this.colTypes.length];
	}
	
	// Getting methods
	public String getName() {
		return name;
	}
	public String[] getColNames() {
		return colNames;
	}
	public Value[][] getValues() {
		return values;
	}
	public void print() {
		System.out.println(ArrayMethods.toString(this.colNames, "\t"));
		for(int i = 0; i < this.row; i++) {
			
			for(int j = 0; j < this.values[i].length; j++) {
				
				// Get the current value type
				String type = this.colTypes[j];
				
				// Store the value
				if(type == "String") {
					System.out.print(this.values[i][j].getStringValue());
				}else if(type == "int") {
					System.out.print(this.values[i][j].getIntValue());
				}else if(type == "double") {
					System.out.print(this.values[i][j].getDoubleValue());
				}else if(type == "int[]") {
					System.out.print(ArrayMethods.toString(this.values[i][j].getIntValues(), "-"));
				}
				
				// Add separator
				if(j != this.values[i].length - 1) {
					System.out.print("\t");
				}
			}
			System.out.println();
		}
	}
	
	// Methods for building table
	// Methods for building the table
	public void finishedWithTable() {
		
		// Check if table exists
		if(this.row != -1) {
			// Remove unused rows from table
			Value[][] newTable = new Value[this.row][this.values[0].length];
			for(int i = 0; i < this.row; i++) {
				newTable[i] = this.values[i];
			}
			this.values = newTable;
		}else {
			this.values = null;
		}
		
		
		
	}
	
	public void addRow(String line) {
		
		// Split the line into its parts
		String[] parts = line.split("\t");
		
		// Check if at first row
		if(this.row == -1) {
			
			// Store the column names
			this.colNames = parts;
		
		}else {
			
			setValues(parts);			
		}
		
		// Increment the row
		this.row++;
	}


	private void setValues(String[] parts) {
		
		// Check that haven't reached the end of the table
		if(this.row == this.values.length) {
			
			// Initialise new table
			Value[][] newTable = new Value[this.values.length + 100][this.colTypes.length];
			
			// Fill the table
			for(int i = 0; i < this.values.length; i++) {
				newTable[i] = this.values[i];
			}
			
			// Replace current table with larger new table
			this.values = newTable;
		}
		
		// Add each of the values to the table
		for(int col = 0; col < parts.length; col++) {
			
			// Get the value type
			String type = this.colTypes[col];
			
			// Create a new value object
			this.values[this.row][col] = new Value(type);
			
			// Store the value
			if(type == "String") {
				this.values[this.row][col].setStringValue(parts[col]);
			}else if(type == "int") {
				this.values[this.row][col].setIntValue(Integer.parseInt(parts[col]));
			}else if(type == "double") {
				this.values[this.row][col].setDoubleValue(Double.parseDouble(parts[col]));
			}else if(type == "int[]") {
				this.values[this.row][col].setIntValues(ArrayMethods.convertToInteger(parts[col].split("-")));
			}
		}
		

	}
}
