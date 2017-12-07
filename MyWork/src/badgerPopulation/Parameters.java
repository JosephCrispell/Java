package badgerPopulation;

public class Parameters {

	// Simulation Settings
	public int simulationLength = 100;
	
	// Population
	public int noGroups = 10;
	public int carryingCapacity = 10;
	public double meanGroupSize = 8;
	public double meanAge = 3.5;
	public double probMale = 0.5;
	
	// Grid
	public int[] gridDimensions = {5, 5};
	
	// Population Dynamics
	public boolean closed = true;
	public double avgNoImmigrants = 5;
	public double[] dispersalProbs = {0.025, 0.025}; // Male, Female
	public double[] seasonalEffects = {0.5, 0.3, 0.1, 0.1}; // Spring, Summer, Autumn, Winter
	public int minDispersalAge = 1;	
	
	public int minBreedingAge = 2;
	public boolean pseudoVerticalTransmission = false;
	public double avgLitterSize = 3;
	
	public double[] deathProbs = {0.0625, 0.0625}; // Male, Female
	public double[] infectionEffects = {1, 1, 1, 2};

	// Infection
	public double[] infectionProbs = {0, 0, 0.005, 0.3}; // Susceptible, Exposed, Infected, Generalised
	public double betweenGroupFactor = 10;
	public double[] mutationRatesPerSeason = {0, 0.125, 0.125, 0.125}; // Susceptible, Exposed, Infected, Generalised
	public double[] progressionProbs = {0, 0.2, 0.05, 0}; // Susceptible, Exposed, Infected, Generalised
	public int noSeeds = 1;
	public int seedStatus = 3;
	public int seedYear = 10;
	
	public Parameters() {
		
	}
	
	// Setting methods
	public void setSimulationLength(int value){
		this.simulationLength = value;
	}
	public void setNoGroups(int value){
		this.noGroups = value;
	}
	public void setCarryingCapacity(int value){
		this.carryingCapacity = value;
	}
	public void setMeanGroupSize(double value){
		this.meanGroupSize = value;
	}
	public void setMeanAge(double value){
		this.meanAge = value;
	}	
	public void setProbMale(double value){
		this.probMale = value;
	}
	
	public void setGridDimensions(int[] values){
		this.gridDimensions = values;
	}
	
	public void setClosed(boolean value){
		this.closed = value;
	}
	public void setAvgNoImmigrants(double value){
		this.avgNoImmigrants = value;
	}
	public void setDispersalProbs(double[] values){
		this.dispersalProbs = values;
	}
	public void setSeasonalEffects(double[] value){
		this.seasonalEffects = value;
	}
	public void setMinDispersalAge(int value){
		this.minDispersalAge = value;
	}

	public void setMinBreedingAge(int value){
		this.minBreedingAge = value;
	}
	public void setPseudoVerticalTransmission(boolean value){
		this.pseudoVerticalTransmission = value;
	}
	public void setAvgLitterSize(int value){
		this.avgLitterSize = value;
	}

	public void setDeathProbs(double[] values){
		this.deathProbs = values;
	}
	public void setInfectionEffects(double[] value){
		this.infectionEffects = value;
	}

	public void setInfectionProbs(double[] values){
		this.infectionProbs = values;
	}
	public void setBetweenGroupFactor(double value){
		this.betweenGroupFactor = value;
	}
	public void setMutationRatesPerSeason(double[] value){
		this.mutationRatesPerSeason = value;
	}
	public void setProgressionProbs(double[] values){
		this.progressionProbs = values;
	}
	public void setNoSeeds(int value){
		this.noSeeds = value;
	}
	public void setSeedStatus(int character){
		this.seedStatus = character;
	}
	public void setSeedYear(int year){
		this.seedStatus = year;
	}
	
	// Getting methods
	public int getSimulationLength(){
		return this.simulationLength;
	}
	public int getNoGroups(){
		return this.noGroups;
	}
	public int getCarryingCapacity(){
		return this.carryingCapacity;
	}
	public double getMeanGroupSize(){
		return this.meanGroupSize;
	}
	public double getMeanAge(){
		return this.meanAge;
	}	
	public double getProbMale(){
		return this.probMale;
	}
	
	public int[] getGridDimensions(){
		return this.gridDimensions;
	}
	
	public boolean getClosed(){
		return this.closed;
	}
	public double getAvgNoImmigrants(){
		return this.avgNoImmigrants;
	}
	public double[] getDispersalProbs(){
		return this.dispersalProbs;
	}
	public double[] getSeasonalEffects(){
		return this.seasonalEffects;
	}
	public int getMinDispersalAge(){
		return this.minDispersalAge;
	}

	public int getMinBreedingAge(){
		return this.minBreedingAge;
	}
	public boolean getPseudoVerticalTransmission(){
		return this.pseudoVerticalTransmission;
	}
	public double getAvgLitterSize(){
		return this.avgLitterSize;
	}

	public double[] getDeathProbs(){
		return this.deathProbs;
	}
	public double[] getInfectionEffects(){
		return this.infectionEffects;
	}

	public double[] getInfectionProbs(){
		return this.infectionProbs;
	}
	public double getBetweenGroupFactor(){
		return this.betweenGroupFactor;
	}
	public double[] getMutationRatesPerSeason(){
		return this.mutationRatesPerSeason;
	}
	public double[] getProgressionProbs(){
		return this.progressionProbs;
	}
	public int getNoSeeds(){
		return this.noSeeds;
	}
	public int getSeedStatus(){
		return this.seedStatus;
	}
	public int getSeedYear(){
		return this.seedYear;
	}
}
