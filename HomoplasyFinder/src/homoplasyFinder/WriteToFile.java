package homoplasyFinder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class WriteToFile {

	public static BufferedWriter openFile(String fileName, boolean append) throws IOException{
		
		// Find the File
		File file = new File(fileName);
		
		// Check exists
		if(!file.exists()){
			file.createNewFile();
		}
		
		// Open the File for Writing
		FileWriter writer = new FileWriter(file.getAbsoluteFile(), append);
		
		return new BufferedWriter(writer);
	}

	public static void write(BufferedWriter bWriter, String string) throws IOException{
		bWriter.write(string);
	}
	public static void writeLn(BufferedWriter bWriter, String string) throws IOException{
		bWriter.write(string + "\n");
	}
	public static void write(BufferedWriter bWriter, int number) throws IOException{
		bWriter.write(Integer.toString(number));
	}
	public static void writeLn(BufferedWriter bWriter, int number) throws IOException{
		bWriter.write(Integer.toString(number) + "\n");
	}
	public static void write(BufferedWriter bWriter, double number) throws IOException{
		bWriter.write(String.valueOf(number));
	}
	public static void writeLn(BufferedWriter bWriter, double number) throws IOException{
		bWriter.write(String.valueOf(number) + "\n");
	}

	public static void close(BufferedWriter bWriter) throws IOException{
		bWriter.close();
	}
}
