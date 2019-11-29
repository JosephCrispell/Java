package homoplasyFinderV2;

import java.awt.EventQueue;

import javax.swing.JFrame;

import javax.swing.JButton;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JRadioButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class HomoplasyFinderGUI extends JFrame {

	/**
	 * Create public variables - accessible throughout class
	 */
	
	// Weird variable to avoid error in GUI
	private static final long serialVersionUID = 1L;
	
	// Get the current date
	public String date = Methods.getCurrentDate("dd-MM-yy");
	
	// Create variables to record the tree, FASTA, and report files selected
	public String treeFile = "None provided";
	public String fastaFile = "None provided";
	public String presenceAbsenceFile = "None provided";
	public String reportFile = "consistencyIndexReport_" + date + ".txt";
	public String outputFastaFile = "sequences_noInconsistentSites_" + date + ".fasta";
	public String outputTreeFile = "annotatedNewickTree_" + date + ".tree";
	
	// Create boolean variables to record whether consistent sites are to be included in report and whether to multithread
	public boolean includeConsistentSitesInReport = false;
	public boolean multithread = false;
	
	// Create matching labels that will record to the user the tree and FASTA file selected
	public JLabel labelTreeFileSelected;
	public JLabel labelFastaFileSelected;
	public JLabel labelPresenceAbsenceFileSelected;
	public JLabel labelReportFileSelected;
	public JLabel labelOutputFastaFileSelected;
	public JLabel labelOutputTreeFileSelected;
	
	// Set the directory that the file browsers will open into - starts with home but will be updated to the previous directory opened
	public File directory = new File(System.getProperty("user.home"));
	
	// Console
	public JTextArea textArea;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					HomoplasyFinderGUI frame = new HomoplasyFinderGUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public HomoplasyFinderGUI() {
		
		// Set the characteristics of the frame
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 645, 496);
		setTitle("HomoplasyFinder");
		getContentPane().setLayout(null);

		/**
		 *  Create a label for the tree file - located beside button and will update once tree file found
		 */
		this.labelTreeFileSelected = new JLabel(this.treeFile);
		this.labelTreeFileSelected.setBounds(300, 14, 326, 14);
		getContentPane().add(this.labelTreeFileSelected);
		
		// Create browse button for tree file
		JButton buttonTreeFile = new JButton("Find tree file");
		buttonTreeFile.setBounds(10, 10, 272, 23);
		buttonTreeFile.setToolTipText("Select Newick formatted phylogenetic tree file");
		
		// Add mouse listener that will open file chooser when tree file button clicked
		buttonTreeFile.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				treeFile = findFile(labelTreeFileSelected, "open");
			}
		});
		getContentPane().add(buttonTreeFile);
		
		/**
		 *  Create a label for the FASTA file - located beside button and will update once FASTA file found
		 */
		this.labelFastaFileSelected = new JLabel(this.fastaFile);
		this.labelFastaFileSelected.setBounds(300, 43, 326, 14);
		getContentPane().add(this.labelFastaFileSelected);
		
		// Create browse button for FASTA file
		JButton buttonFastaFile = new JButton("Find FASTA file");
		buttonFastaFile.setBounds(10, 39, 272, 23);
		buttonFastaFile.setToolTipText("Select FASTA formatted file containing nucleotide alignment");
		
		// Add mouse listener that will open file chooser when FASTA file button clicked
		buttonFastaFile.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				fastaFile = findFile(labelFastaFileSelected, "open");
			}
		});
		getContentPane().add(buttonFastaFile);
		
		/**
		 *  Create a label for the presence/absence file - located beside button and will update once presence/absence file found
		 */
		this.labelPresenceAbsenceFileSelected = new JLabel(this.presenceAbsenceFile);
		this.labelPresenceAbsenceFileSelected.setBounds(300, 72, 326, 14);
		getContentPane().add(this.labelPresenceAbsenceFileSelected);
		
		// Create browse button for FASTA file
		JButton buttonPresenceAbsenceFile = new JButton("Find presence/absence file");
		buttonPresenceAbsenceFile.setBounds(10, 68, 272, 23);
		buttonPresenceAbsenceFile.setToolTipText("A CSV file containing a table reporting the presence/absence of INDELS");
		
		// Add mouse listener that will open file chooser when FASTA file button clicked
		buttonPresenceAbsenceFile.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				presenceAbsenceFile = findFile(labelPresenceAbsenceFileSelected, "open");
			}
		});
		getContentPane().add(buttonPresenceAbsenceFile);
		
		/**
		 *  Create a label for the Report file - located beside button and will update if file changed
		 */
		this.labelReportFileSelected = new JLabel(this.reportFile);
		this.labelReportFileSelected.setBounds(300, 101, 326, 14);
		getContentPane().add(this.labelReportFileSelected);
		
		// Create browse button for Report file
		JButton buttonReportFile = new JButton("Change report file");
		buttonReportFile.setBounds(10, 97, 272, 23);
		buttonReportFile.setToolTipText("Select file to save homoplasyFinder report to");
		
		// Add mouse listener that will open file chooser when report file button clicked
		buttonReportFile.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				reportFile = findFile(labelReportFileSelected, "save");
			}
		});
		getContentPane().add(buttonReportFile);
		
		/**
		 *  Create a label indicating the chosen output FASTA file
		 */
		this.labelOutputFastaFileSelected = new JLabel(this.outputFastaFile);
		this.labelOutputFastaFileSelected.setBounds(300, 130, 326, 14);
		getContentPane().add(this.labelOutputFastaFileSelected);
		
		// Create a label to choose the output FASTA file
		JButton buttonOutputFastaFile = new JButton("Change output sequences file");
		buttonOutputFastaFile.setBounds(10, 126, 272, 23);
		buttonOutputFastaFile.setToolTipText("Select the file to print the FASTA formatted nucleotide sequences without inconsistent sites into");
		
		// Add mouse listener that will open file chooser when report file button clicked
		buttonOutputFastaFile.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				outputFastaFile = findFile(labelOutputFastaFileSelected, "save");
			}
		});
		getContentPane().add(buttonOutputFastaFile);
		
		/**
		 *  Create a label indicating the chosen output tree file
		 */
		this.labelOutputTreeFileSelected = new JLabel(this.outputTreeFile);
		this.labelOutputTreeFileSelected.setBounds(300, 159, 326, 14);
		getContentPane().add(this.labelOutputTreeFileSelected);
		
		// Create a label to choose the output tree file
		JButton buttonOutputTreeFile = new JButton("Change output tree file");
		buttonOutputTreeFile.setBounds(10, 155, 272, 23);
		buttonOutputTreeFile.setToolTipText("Select the file to print the annotated NEWICK formatted phylogetic tree into");
		
		// Add mouse listener that will open file chooser when report file button clicked
		buttonOutputTreeFile.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				outputFastaFile = findFile(labelOutputTreeFileSelected, "save");
			}
		});
		getContentPane().add(buttonOutputTreeFile);
		
		/**
		 *  Add text area to print progress out to
		 */
		this.textArea = new JTextArea();
		this.textArea.setText(details());
		this.textArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(this.textArea);
		scrollPane.setBounds(10, 250, 620, 200);
		getContentPane().add(scrollPane);
		
		/**
		 *  Add a button to allow the user to decide whether to include consistent sites in report
		 */
		JRadioButton includeConsistentSitesButton = new JRadioButton("Include consistent sites");
		includeConsistentSitesButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				if(includeConsistentSitesButton.isSelected()) {
					includeConsistentSitesInReport = true;
				}else {
					includeConsistentSitesInReport = false;
				}
			}
		});
		includeConsistentSitesButton.setBounds(20, 187, 186, 23);
		getContentPane().add(includeConsistentSitesButton);
		includeConsistentSitesButton.setToolTipText("Click button to include sites that are consistent with phylogeny in report");
		
		/**
		 *  Add a button to allow the user to turn on multithreading
		 */
		JRadioButton multithreadButton = new JRadioButton("Multithread");
		multithreadButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				if(multithreadButton.isSelected()) {
					multithread = true;
				}else {
					multithread = false;
				}
			}
		});
		multithreadButton.setBounds(20, 214, 114, 23);
		getContentPane().add(multithreadButton);
		multithreadButton.setToolTipText("Click button to turn on multithreading");
		
		/**
		 *  Add run button to run homoplasyFinder
		 */
		JButton buttonRun = new JButton("Run");
		buttonRun.setBounds(280, 200, 67, 23);
		buttonRun.setToolTipText("Run homoplasyFinder on the selected tree and FASTA file");
		buttonRun.addMouseListener(new MouseAdapter() {
			
			// Add listener for mouse click
			@Override
			public void mouseClicked(MouseEvent e) {
				
				// Run homoplasy finder when mouse clicked
				try {
					
					// Check that both a tree file and the FASTA or presence/absence file have been provided
					if(treeFile.matches("None provided") == false && 
							(fastaFile.matches("None provided") == false ||	presenceAbsenceFile.matches("None provided") == false)){
						
						// Run homoplasyFinder
						runHomoplasyFinder();
						
						// Print information about output files
						textArea.append("\nFinished! :-)\nCreated report: " + reportFile + 
								"\nCreated annotated version of input Newick formatted tree file: " + outputTreeFile + "\n");
						
						// Check if running on FASTA file
						if(fastaFile.matches("None provided") == false) {
							textArea.append("Created FASTA nucleotide alignment file without inconsistent sites: " + outputFastaFile);
						}

					}else{
						textArea.setText("Please select tree and FASTA or presence/absence files...");
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		getContentPane().add(buttonRun);		
	}
	
	/**
	 * General methods area
	 * @throws IOException 
	 */
	
	public String details(){
		
		String homoplasyFinderDetails = "";
		
		homoplasyFinderDetails += "HomoplasyFinder - a tool to identify homoplasies present on a phylogenetic tree\n";
		homoplasyFinderDetails += "Version: 0.0.9999\n";
		homoplasyFinderDetails += "Author: Joseph Crispell\n";
		homoplasyFinderDetails += "Date created: 13-08-18\n";
		homoplasyFinderDetails += "Licence: GPL 3.0\n\n";
		
//		homoplasyFinderDetails += "Example report table structure:\n";
//		homoplasyFinderDetails += "Position\tConsistencyIndex\tCountsACGT\tMinimumNumberChangesOnTree\n";
//		homoplasyFinderDetails += "10\t0.5\t\t10:2:0:0\t2\n";
//		homoplasyFinderDetails += "Position: in the FASTA nucleotide alignment\n";
//		homoplasyFinderDetails += "ConsistencyIndex: the consistency index calculated for position\n";
//		homoplasyFinderDetails += "CountsACGT: number of isolates with A, C, G, or T (\":\" separated) at the position\n";
//		homoplasyFinderDetails += "MinimumNumberChangesOnTree: the minimum number of nucleotide changes required at this position to explain the structure of the phylogenetic tree\n";
		
		return homoplasyFinderDetails;
	}
	
	public String findFile(JLabel label, String task){
		
		// Initialise a variable to store the file name selected
		String fileName = "";
		
		// Start a file browser
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(this.directory);
		
		int result;
		if(task.matches("save")){
			result = fileChooser.showSaveDialog(null);
		}else{
			result = fileChooser.showOpenDialog(null);
		}
		
		// Wait for file selection
		if(result == JFileChooser.APPROVE_OPTION){
			
			// Record the file selected - update the associated report label
			File selectedFile = fileChooser.getSelectedFile();
			fileName = selectedFile.getAbsolutePath();
			if(task.matches("save")){
				fileName = selectedFile.getName();
			}
			label.setText(selectedFile.getName());
			
			// Update the directory to open browser into
			this.directory = selectedFile.getParentFile();					
		}
		
		return fileName;
	}
	
	public void runHomoplasyFinder() throws IOException, InterruptedException{
		
		// Edit the path according to the operating system
		String path = this.directory.getAbsolutePath();
		if(System.getProperty("os.name").matches("(.*)Windows(.*)") == true){
			path += "\\";
		}else{
			path += "/";
		}
		
		// Read the NEWICK tree and store as a traversable node set
		Tree tree = new Tree(this.treeFile);
		
		// Check if FASTA file provided
		if(this.fastaFile.matches("None provided") == false) {
		
			// Read in the sequences
			ArrayList<Sequence> sequences = Methods.readFastaFile(this.fastaFile, false);
			
			// Calculate the consistency index of each position in the alignment on the phylogeny
			ConsistencyIndex consistency = new ConsistencyIndex(tree, sequences, false, this.multithread, 4);
			
			// Create a FASTA file without inconsistent sites
			consistency.printSequencesWithoutInConsistentSites(path + this.outputFastaFile);
			
			// Create an annotated NEWICK tree file
			consistency.printAnnotatedTree(path + "annotatedNewickTree_" + date + ".tree");
			
			// Create a report file
			consistency.printSummary(path + "consistencyIndexReport_" + date + ".txt", this.includeConsistentSitesInReport);
			
			// Print summary to text area of GUI
			consistency.printSummary(this.textArea);
			
		// Read in presence absence file instead
		}else {
			
			// Read in the presence absence matrix
			PresenceAbsence presenceAbsenceMatrix = Methods.readPresenceAbsenceTable(this.presenceAbsenceFile, 
					false);
			
			// Calculate the consistency index of each position in the alignment on the phylogeny
			ConsistencyIndex consistency = new ConsistencyIndex(tree, presenceAbsenceMatrix.getBooleanSequences(), false,
					this.multithread, 2);
			
			// Create an annotated NEWICK tree file
			consistency.printAnnotatedTree(path + "annotatedNewickTree_" + date + ".tree");
			
			// Create a report file
			consistency.printSummary(path + "consistencyIndexReport_" + date + ".txt", presenceAbsenceMatrix.getRegionCoords(),
					this.includeConsistentSitesInReport);
			
			// Print summary to text area of GUI
			consistency.printSummary(this.textArea, presenceAbsenceMatrix.getRegionCoords());
		}		
	}
}
