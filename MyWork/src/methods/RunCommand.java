package methods;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RunCommand {

	public String output = "";
	public String error = "";
	
	public RunCommand(String command) throws IOException {

		Runtime runtime = Runtime.getRuntime();
		Process process = runtime.exec(command);
		
		// Get the Output
		BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
		
		String line = "";
		while ((line = stdOut.readLine()) != null) {
			this.output = this.output + line + "\n";
		}
		
		// Get any error messages that may have occurred
		BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		
		line = "";
		while ((line = stdError.readLine()) != null) {
		    this.error = this.error + line + "\n";
		}
	}
	
	// Getting Methods
	public String getOutput(){
		return this.output;
	}
	public String getError(){
		return this.error;
	}

}
