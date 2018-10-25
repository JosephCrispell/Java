package homoplasyFinder;

public class Arguments {

	public boolean verbose = false;
	public String fastaFile;
	public String treeFile;
	public boolean createFasta = false;
	public boolean createAnnotatedNewickTree = false;
	public boolean includeConsistentSitesInReport = false;
	public boolean multithread = false;
	
	public Arguments(String[] args) {
		
		examineCommandLineArgumentsProvided(args);
	}
	
	// Class specific functions
	private void printHelp(){
		
		System.out.println("HomoplasyFinder: a tool to identify homoplasies within a phylogenetic tree and alignment");
		
		System.out.println("\nNecessary command line arguments:");
		System.out.println("\t--fasta [fullPathToFASTAFile]\tThe input FASTA containing a nucleotide alignment (sequential format)");
		System.out.println("\t--tree [fullPathToTreeFile]\tThe input Newick formatted tree file");
		
		System.out.println("\nOptional command line arguments:");
		System.out.println("\t--verbose\t\tA flag to turn on detailed progress information");
		System.out.println("\t--createFasta\t\tA flag to create a FASTA file without inconsistent sites");
		System.out.println("\t--createAnnotatedTree\tA flag to create an annotated Newick formatted tree file");
		System.out.println("\t--includeConsistent\tA flag to include information about sites that are consistent (non-homoplasious) in report");
		System.out.println("\t--multithread\tA flag to turn on multithreading");
		System.out.println("\t--help\t\t\tA flag to print this information and exit");
	}

	// Getting methods
	public boolean isVerbose() {
		return verbose;
	}
	public String getFastaFile() {
		return fastaFile;
	}
	public String getTreeFile() {
		return treeFile;
	}
	public boolean isCreateFasta() {
		return createFasta;
	}
	public boolean isCreateAnnotatedNewickTree() {
		return createAnnotatedNewickTree;
	}
	public boolean isIncludeConsistentSitesInReport() {
		return includeConsistentSitesInReport;
	}
	public boolean isMultithread() {
		return multithread;
	}

	
	// Method to retrieve command line options from arguments
	private void examineCommandLineArgumentsProvided(String[] args) {
		
		// Examine each argument
		for(int i = 0; i < args.length; i++) {
			
			// Check that FASTA file has been provided
			if(args[i].equals("--fasta")) {
				this.fastaFile = args[i + 1];
				i++;
			
			// Check that TREE file has been provided
			}else if(args[i].equals("--tree")) {
				this.treeFile = args[i + 1];
				i++;
				
			// Check if verbose parameter present?
			}else if(args[i].equals("--verbose")) {
				this.verbose = true;
				
			// Check if flag for creating FASTA is present?
			}else if(args[i].equals("--createFasta")) {
				this.createFasta = true;
			
			// Check if flag for creating annotated tree is present?
			}else if(args[i].equals("--createAnnotatedTree")) {
				this.createAnnotatedNewickTree = true;
				
			// Check if flag for including consistent sites in FASTA is present?
			}else if(args[i].equals("--includeConsistent")) {
				this.includeConsistentSitesInReport = true;
				
			// Check if flag for including consistent sites in FASTA is present?
			}else if(args[i].equals("--multithread")) {
				this.multithread = true;
				
			// Check for help request
			}else if(args[i].matches("(.*)help")){
				printHelp();
				System.exit(0);
							
			// Else - input argument not recognised, print help
			}else {
				System.err.println((char)27 + "[31mERROR!! The input argument: \"" + args[i] + "\" provided wasn't recognised!\n" + (char)27 + "[0m");
				printHelp();
				System.exit(0);
			}
		}
		
		// Check both FASTA and tree files were provided
		if(this.fastaFile == null || this.treeFile == null) {
			System.err.println((char)27 + "[31mERROR!! HomoplasyFinder requires the full path names of both a FASTA file and a newick formatted tree file.\n" + (char)27 + "[0m");
			printHelp();
			System.exit(0);
		}
	}
}
