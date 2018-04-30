package identifyingHomoplasies;

import java.awt.EventQueue;

import javax.swing.JFrame;

import geneticDistances.Sequence;
import methods.CalendarMethods;
import methods.GeneticMethods;
import phylogeneticTree.Node;

import javax.swing.JButton;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.JFileChooser;
import javax.swing.SpringLayout;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

public class HomoplasyFinderGUI extends JFrame {

	/**
	 * Create public variables - accessible throughout class
	 */
	
	// Weird variable to avoid error in GUI
	private static final long serialVersionUID = 1L;
	
	// Get the current date
	public String date = CalendarMethods.getCurrentDate("dd-MM-yy");
	
	// Create two variables to record the tree, FASTA, and report files selected
	public String treeFile = "None provided";
	public String fastaFile = "None provided";
	public String reportFile = "homoplasyReport_" + date + ".txt";
	public String outputFastaFile = "sequences_withoutHomoplasies_" + date + ".fasta";
	
	// Create matching labels that will record to the user the tree and FASTA file selected
	public JLabel lblTreeFileSelected;
	public JLabel lblFastaFileSelected;
	public JLabel lblReportFileSelected;
	public JLabel lblOutputFastaFileSelected;
	
	// Set the directory that the file browsers will opne into - starts with home but will be updated to the previous directory opened
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
		setBounds(100, 100, 589, 496);
		setTitle("HomoplasyFinder v. 1.0");
		getContentPane().setLayout(null);
		
		// Create a label for the tree file - located beside button and will update once tree file found
		this.lblTreeFileSelected = new JLabel(this.treeFile);
		this.lblTreeFileSelected.setBounds(270, 14, 303, 14);
		getContentPane().add(this.lblTreeFileSelected);
		
		// Create browse button for tree file
		JButton buttonTreeFile = new JButton("Find tree file");
		buttonTreeFile.setBounds(10, 10, 236, 23);
		buttonTreeFile.setToolTipText("Select Newick formatted phylogenetic tree file");
		
		// Add mouse listener that will open file chooser when tree file button clicked
		buttonTreeFile.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				treeFile = findFile(lblTreeFileSelected, "open");
			}
		});
		getContentPane().add(buttonTreeFile);
		
		// Create a label for the FASTA file - located beside button and will update once FASTA file found
		this.lblFastaFileSelected = new JLabel(this.fastaFile);
		this.lblFastaFileSelected.setBounds(270, 43, 303, 14);
		getContentPane().add(this.lblFastaFileSelected);
		
		// Create browse button for FASTA file
		JButton buttonFastaFile = new JButton("Find FASTA file");
		buttonFastaFile.setBounds(10, 39, 236, 23);
		buttonFastaFile.setToolTipText("Select FASTA formatted file containing nucleotide alignment");
		
		// Add mouse listener that will open file chooser when FASTA file button clicked
		buttonFastaFile.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				fastaFile = findFile(lblFastaFileSelected, "open");
			}
		});
		getContentPane().add(buttonFastaFile);
		
		// Create a label for the Report file - located beside button and will update if file changed
		this.lblReportFileSelected = new JLabel(this.reportFile);
		this.lblReportFileSelected.setBounds(270, 72, 303, 14);
		getContentPane().add(this.lblReportFileSelected);
		
		// Create browse button for Report file
		JButton buttonReportFile = new JButton("Change report file");
		buttonReportFile.setBounds(10, 68, 236, 23);
		buttonReportFile.setToolTipText("Select file to save homoplasyFinder report to");
		
		// Add mouse listener that will open file chooser when report file button clicked
		buttonReportFile.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				reportFile = findFile(lblReportFileSelected, "save");
			}
		});
		getContentPane().add(buttonReportFile);
		
		// Add text area to print progress out to
		this.textArea = new JTextArea();
		this.textArea.setText(details());
		this.textArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(this.textArea);
		scrollPane.setBounds(10, 216, 563, 242);
		getContentPane().add(scrollPane);
		
		// Add run button to run homoplasyFinder
		JButton buttonRun = new JButton("Run");
		buttonRun.setBounds(264, 160, 67, 23);
		buttonRun.setToolTipText("Run homoplasyFinder on the selected tree and FASTA file");
		buttonRun.addMouseListener(new MouseAdapter() {
			
			// Add listener for mouse click
			@Override
			public void mouseClicked(MouseEvent e) {
				
				// Run homoplasy finder when mouse clicked
				try {
					
					if(treeFile.matches("None provided") == false && fastaFile.matches("None provided") == false){
						
						// Run homoplasyFinder
						runHomoplasyFinder();
						
						// Print information about output files
						textArea.append("\nCreated report: " + reportFile + "\nCreate FASTA nucleotide alignment file without homoplasies: " + outputFastaFile + "\n");
					}else{
						textArea.setText("Please select tree and FASTA files...");
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
		
		// Create a label indicating the chosen output fasta file
		this.lblOutputFastaFileSelected = new JLabel(this.outputFastaFile);
		this.lblOutputFastaFileSelected.setBounds(270, 101, 303, 14);
		getContentPane().add(this.lblOutputFastaFileSelected);
		
		// Create a label to choose the output FASTA file
		JButton buttonOutputFastaFile = new JButton("Change output sequences file");
		buttonOutputFastaFile.setBounds(10, 97, 236, 23);
		buttonOutputFastaFile.setToolTipText("Select the file to print the sequences without homoplasies into");
		
		// Add mouse listener that will open file chooser when report file button clicked
		buttonOutputFastaFile.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				outputFastaFile = findFile(lblOutputFastaFileSelected, "save");
			}
		});
		getContentPane().add(buttonOutputFastaFile);
		
	}
	
	/**
	 * General methods area
	 * @throws IOException 
	 */
	
	public String details(){
		
		String homoplasyFinderDetails = "";
		
		homoplasyFinderDetails += "HomoplasyFinder - a tool to identify homoplasies present on a phylogenetic tree\n";
		homoplasyFinderDetails += "Version: 1.0\n";
		homoplasyFinderDetails += "Author: Joseph Crispell\n";
		homoplasyFinderDetails += "Date created: 24-02-18\n";
		homoplasyFinderDetails += "Licence: GPL 3.0\n\n";
		
		homoplasyFinderDetails += "Results table structure:\n";
		homoplasyFinderDetails += "Position\tAlleles\tIsolatesForAlleles\n";
		homoplasyFinderDetails += "10\tA,G\tisolateA-isolateB-isolateC,isolateD-isolateE\n\n";
		homoplasyFinderDetails += "Position: in the FASTA nucleotide alignment\n";
		homoplasyFinderDetails += "Alleles: nucleotides found at position\n";
		homoplasyFinderDetails += "IsolatesForAlleles: isolates (\":\" separated) found with each allele separated by a \",\"\n";
		
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
		
		// Read in tree
		Node tree = HomoplasyFinder6.readNewickTree(this.treeFile, false);
		
		// Read in the FASTA file
		Sequence[] sequences = GeneticMethods.readFastaFile(this.fastaFile, false);
		
		// Get the alleles in the population and the isolates they are associated with
		Hashtable<String, ArrayList<String>> alleles = HomoplasyFinder6.noteAllelesInPopulation(sequences, false);
		ArrayList<String> positions = HomoplasyFinder5.getAllelePositions(alleles);
		
		// Assign alleles
		ArrayList<String> unassigned = HomoplasyFinder6.assignAllelesToNodes(alleles, positions, tree, HomoplasyFinder6.getSequenceIDs(sequences));
		
		// Examine the homoplasies
		int[] homoplasyPositions = HomoplasyFinder6.examineUnAssignedAlleles(unassigned, alleles, false, path, this.reportFile, this.date, this.textArea);
		
		/**
		 * Return a FASTA file without the homoplasy sites
		 */
		HomoplasyFinder6.printFASTAWithoutHomoplasies(homoplasyPositions, path, this.outputFastaFile, this.date, sequences, false);
	}
}
