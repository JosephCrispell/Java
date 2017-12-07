package badgerPopulation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Random;
import methods.*;

import org.uncommons.maths.random.MersenneTwisterRNG;
import org.uncommons.maths.random.PoissonGenerator;


public class PopulationMethods {

	public static void main(String[] args) throws IOException {
		// Methods Testing Area
		
	}

	// Methods for Creating Initial Population

	// Methods for Population Dynamics

	// Methods for Infection Dynamics
	
	// Method to Survey Population
	public static void surveyPopulation(Badger[][] population, int seasonCounter, BufferedWriter surveyFile, int toFile) throws IOException{
		
		// Surveying the status of the Badger Population
		int noAdults = 0;
		int noJuveniles = 0;
		int noCubs = 0;
		
		int noSusceptible = 0;
		int noExposed = 0;
		int noInfected = 0;
		int noGeneralised = 0;
		
		// Examine the Current Population
		for(Badger[] group : population ){
			for( Badger badger : group ){
								
				// Record Age category
				if(badger.getAge() > 1 ){
					noAdults++;
				}else if(badger.getAge() == 1){ 
					noJuveniles++;
				}else if(badger.getAge() == 0){
					noCubs++;
				}
				
				// Record Infection Status Category
				if(badger.getInfectionStatus() == 'S'){
					noSusceptible++;
				}else if(badger.getInfectionStatus() == 'E'){
					noExposed++;
				}else if(badger.getInfectionStatus() == 'I'){
					noInfected++;
				}else if(badger.getInfectionStatus() == 'G'){
					noGeneralised++;
				}
			}
		}
		
		
		// Open the File to Write to. If this is the first Survey - then wipe the file
		if(toFile == 1){
			String string = seasonCounter + "\t" + noAdults + "\t" + noJuveniles + "\t" + noCubs + "\t" + noSusceptible + "\t" + noExposed + "\t" + noInfected + "\t" + noGeneralised;
			WriteToFile.writeLn(surveyFile, string);
			
		}else if(toFile == 0){
			System.out.println(seasonCounter + "\t" + noAdults + "\t" + noJuveniles + "\t" + noCubs + "\t" + noSusceptible + "\t" + noExposed + "\t" + noInfected + "\t" + noGeneralised);
		}

	}

	public static void takeSnapshotOfPopulation(Parameters parameters, Grid grid, Badger[] population, int season){
		
		// Print out the parameters
		System.out.println("Begin: Parameters");
		
		System.out.println("End: Parameters");
		
	}
}
