package homoplasyFinderV2;

public class Arguments {

	public boolean verbose = false;
	public String fastaFile;
	public String traitsFile;
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
		System.out.println("\t--tree [fullPathToTreeFile]\tThe input Newick formatted tree file");
		System.out.println("ONE of the following:");
		System.out.println("\t--fasta [fullPathToFASTAFile]\tThe input FASTA containing a nucleotide alignment (sequential format)");
		System.out.println("\t--traits [fullPathToTraitsFile]\tThe input CSV containing traits associated with each tip/sequence (first column must contain IDs)");
		
		System.out.println("\nOptional command line arguments:");
		System.out.println("\t--verbose\t\tA flag to turn on detailed progress information");
		System.out.println("\t--createFasta\t\tA flag to create a FASTA file without inconsistent sites");
		System.out.println("\t--createAnnotatedTree\tA flag to create an annotated Newick formatted tree file");
		System.out.println("\t--includeConsistent\tA flag to include information about sites that are consistent (non-homoplasious) in report");
		System.out.println("\t--multithread\t\tA flag to turn on multithreading");
		System.out.println("\t--help\t\t\tA flag to print this information and exit");
	}

	// Getting methods
	public boolean isVerbose() {
		return this.verbose;
	}
	public String getFastaFile() {
		return this.fastaFile;
	}
	public String getTraitsFile() {
		return this.traitsFile;
	}
	public String getTreeFile() {
		return this.treeFile;
	}
	public boolean isCreateFasta() {
		return this.createFasta;
	}
	public boolean isCreateAnnotatedNewickTree() {
		return this.createAnnotatedNewickTree;
	}
	public boolean isIncludeConsistentSitesInReport() {
		return this.includeConsistentSitesInReport;
	}
	public boolean isMultithread() {
		return this.multithread;
	}

	
	// Method to retrieve command line options from arguments
	private void examineCommandLineArgumentsProvided(String[] args) {
		
		// Examine each argument
		for(int i = 0; i < args.length; i++) {
			
			// Check if FASTA file has been provided
			if(args[i].equals("--fasta")) {
				this.fastaFile = args[i + 1];
				i++;
			
			// Check that TREE file has been provided
			}else if(args[i].equals("--tree")) {
				this.treeFile = args[i + 1];
				i++;
			
			// Check if traits file has been provided
			}else if(args[i].equals("--traits")) {
				this.traitsFile = args[i + 1];
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
		
		// Check both FASTA|traits|INDELs and tree files were provided
		if((this.fastaFile == null && this.traitsFile == null) || this.treeFile == null) {
			System.err.println((char)27 + "[31mERROR!! HomoplasyFinder requires the full path names to a newick formatted tree file and either a FASTA file or traits csv.\n" + (char)27 + "[0m");
			printHelp();
			System.exit(0);
		}
			
		// Check that only one of FASTA and traits file provided
		if(this.fastaFile != null && this.traitsFile != null) {
			System.err.println((char)27 + "[31mERROR!! HomoplasyFinder can only work with FASTA or traits file. Please run on each separately.\n" + (char)27 + "[0m");
			printHelp();
			System.exit(0);
		}
	}
}
