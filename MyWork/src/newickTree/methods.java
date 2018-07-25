package newickTree;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class methods {
	
	public static void main(String[] args) throws IOException {
		
		// Set the path
		String path = "/home/josephcrispell/Desktop/Research/Homoplasy/DataForTesting/";
		
		// Get the NEWICK string from file
		StringBuffer newick = readNewickFile(path + "example-TRUE_09-04-18.tree");
		
		// Store the tree as a traversable node set
	}
	
	public static StringBuffer readNewickFile(String fileName) throws IOException{
		
		// Open the animals table file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
											
		// Initialise a variable to store the newick string
		String line = null;
		StringBuffer tree = new StringBuffer();
												
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			
			tree = tree.append(line);			
		}
		
		// Close the input file
		input.close();
		reader.close();
		
		return tree;
	}

}
