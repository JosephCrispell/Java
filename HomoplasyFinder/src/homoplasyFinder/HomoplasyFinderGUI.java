package homoplasyFinder;

import java.awt.BorderLayout;
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
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 589, 496);
		SpringLayout springLayout = new SpringLayout();
		getContentPane().setLayout(springLayout);
		setTitle("HomoplasyFinder v. 1.0");
		
		// Create a label for the tree file - located beside button and will update once tree file found
		lblTreeFileSelected = new JLabel(treeFile);
		springLayout.putConstraint(SpringLayout.NORTH, lblTreeFileSelected, 14, SpringLayout.NORTH, getContentPane());
		springLayout.putConstraint(SpringLayout.WEST, lblTreeFileSelected, 270, SpringLayout.WEST, getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, lblTreeFileSelected, -10, SpringLayout.EAST, getContentPane());
		getContentPane().add(lblTreeFileSelected);
		
		// Create browse button for tree file
		JButton buttonTreeFile = new JButton("Find tree file");
		springLayout.putConstraint(SpringLayout.WEST, buttonTreeFile, 10, SpringLayout.WEST, getContentPane());
		buttonTreeFile.setToolTipText("Select Newick formatted phylogenetic tree file");
		
		// Add mouse listener that will open file chooser when tree file button clicked
		buttonTreeFile.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				// Start a file browser
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setCurrentDirectory(directory);
				int result = fileChooser.showOpenDialog(null);
				
				// Wait for file selection
				if(result == JFileChooser.APPROVE_OPTION){
					
					// Record the file selected - update the associated tree label
					File selectedFile = fileChooser.getSelectedFile();
					treeFile = selectedFile.getAbsolutePath();
					lblTreeFileSelected.setText(selectedFile.getName());
					
					// Update the directory to open browser into
					directory = selectedFile.getParentFile();					
				}				
			}
		});
		springLayout.putConstraint(SpringLayout.NORTH, buttonTreeFile, 10, SpringLayout.NORTH, getContentPane());
		getContentPane().add(buttonTreeFile);
		
		// Create a label for the FASTA file - located beside button and will update once FASTA file found
		lblFastaFileSelected = new JLabel(fastaFile);
		springLayout.putConstraint(SpringLayout.NORTH, lblFastaFileSelected, 15, SpringLayout.SOUTH, lblTreeFileSelected);
		springLayout.putConstraint(SpringLayout.EAST, lblFastaFileSelected, -10, SpringLayout.EAST, getContentPane());
		getContentPane().add(lblFastaFileSelected);
		
		// Create browse button for FASTA file
		JButton buttonFastaFile = new JButton("Find FASTA file");
		springLayout.putConstraint(SpringLayout.NORTH, buttonFastaFile, 6, SpringLayout.SOUTH, buttonTreeFile);
		springLayout.putConstraint(SpringLayout.EAST, buttonTreeFile, 0, SpringLayout.EAST, buttonFastaFile);
		springLayout.putConstraint(SpringLayout.WEST, lblFastaFileSelected, 24, SpringLayout.EAST, buttonFastaFile);
		springLayout.putConstraint(SpringLayout.WEST, buttonFastaFile, 10, SpringLayout.WEST, getContentPane());
		buttonFastaFile.setToolTipText("Select FASTA file containing nucleotide alignment");
		
		// Add mouse listener that will open file chooser when FASTA file button clicked
		buttonFastaFile.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				// Start a file browser
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setCurrentDirectory(directory);
				int result = fileChooser.showOpenDialog(null);
				
				// Wait for file selection
				if(result == JFileChooser.APPROVE_OPTION){
					
					// Record the file selected - update the associated tree label
					File selectedFile = fileChooser.getSelectedFile();
					fastaFile = selectedFile.getAbsolutePath();
					lblFastaFileSelected.setText(selectedFile.getName());
					
					// Update the directory to open browser into
					directory = selectedFile.getParentFile();					
				}				
			}
		});
		getContentPane().add(buttonFastaFile);
		
		// Create a label for the Report file - located beside button and will update if file changed
		lblReportFileSelected = new JLabel(reportFile);
		springLayout.putConstraint(SpringLayout.NORTH, lblReportFileSelected, 15, SpringLayout.SOUTH, lblFastaFileSelected);
		springLayout.putConstraint(SpringLayout.WEST, lblReportFileSelected, 0, SpringLayout.WEST, lblTreeFileSelected);
		springLayout.putConstraint(SpringLayout.EAST, lblReportFileSelected, -10, SpringLayout.EAST, getContentPane());
		getContentPane().add(lblReportFileSelected);
		
		// Create browse button for Report file
		JButton buttonReportFile = new JButton("Change report file");
		springLayout.putConstraint(SpringLayout.NORTH, buttonReportFile, 6, SpringLayout.SOUTH, buttonFastaFile);
		springLayout.putConstraint(SpringLayout.EAST, buttonFastaFile, 0, SpringLayout.EAST, buttonReportFile);
		springLayout.putConstraint(SpringLayout.WEST, buttonReportFile, 10, SpringLayout.WEST, getContentPane());
		buttonReportFile.setToolTipText("Select file to save homoplasyFinder report to");
		
		// Add mouse listener that will open file chooser when report file button clicked
		buttonReportFile.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				// Start a file browser
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setCurrentDirectory(directory);
				int result = fileChooser.showSaveDialog(null);
				
				// Wait for file selection
				if(result == JFileChooser.APPROVE_OPTION){
					
					// Record the file selected - update the associated report label
					File selectedFile = fileChooser.getSelectedFile();
					reportFile = selectedFile.getName();
					lblReportFileSelected.setText(selectedFile.getName());
					
					// Update the directory to open browser into
					directory = selectedFile.getParentFile();					
				}				
			}
		});
		getContentPane().add(buttonReportFile);
		
		// Add text area to print progress out to
		textArea = new JTextArea();
		textArea.setText("Console");
		textArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(textArea);
		springLayout.putConstraint(SpringLayout.NORTH, scrollPane, -252, SpringLayout.SOUTH, getContentPane());
		springLayout.putConstraint(SpringLayout.WEST, scrollPane, 10, SpringLayout.WEST, getContentPane());
		springLayout.putConstraint(SpringLayout.SOUTH, scrollPane, -10, SpringLayout.SOUTH, getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, scrollPane, 0, SpringLayout.EAST, lblTreeFileSelected);
		getContentPane().add(scrollPane);
		
		// Add run button to run homoplasyFinder
		JButton buttonRun = new JButton("Run");
		springLayout.putConstraint(SpringLayout.SOUTH, buttonRun, -33, SpringLayout.NORTH, scrollPane);
		springLayout.putConstraint(SpringLayout.EAST, buttonRun, -262, SpringLayout.EAST, getContentPane());
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
				}
			}
		});
		getContentPane().add(buttonRun);
		
		// Create a label indicating the chosen output fasta file
		lblOutputFastaFileSelected = new JLabel(outputFastaFile);
		springLayout.putConstraint(SpringLayout.NORTH, lblOutputFastaFileSelected, 15, SpringLayout.SOUTH, lblReportFileSelected);
		springLayout.putConstraint(SpringLayout.WEST, lblOutputFastaFileSelected, 0, SpringLayout.WEST, lblTreeFileSelected);
		springLayout.putConstraint(SpringLayout.EAST, lblOutputFastaFileSelected, -10, SpringLayout.EAST, getContentPane());
		getContentPane().add(lblOutputFastaFileSelected);
		
		// Create a label to choose the output FASTA file
		JButton buttonOutputFastaFile = new JButton("Change output sequences file");
		buttonOutputFastaFile.setToolTipText("Select the file to print the sequences without homoplasies into");
		springLayout.putConstraint(SpringLayout.NORTH, buttonOutputFastaFile, 6, SpringLayout.SOUTH, buttonReportFile);
		springLayout.putConstraint(SpringLayout.WEST, buttonOutputFastaFile, 10, SpringLayout.WEST, getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, buttonOutputFastaFile, -24, SpringLayout.WEST, lblOutputFastaFileSelected);
		springLayout.putConstraint(SpringLayout.EAST, buttonReportFile, 0, SpringLayout.EAST, buttonOutputFastaFile);
		
		// Add mouse listener that will open file chooser when report file button clicked
		buttonOutputFastaFile.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				// Start a file browser
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setCurrentDirectory(directory);
				int result = fileChooser.showSaveDialog(null);
				
				// Wait for file selection
				if(result == JFileChooser.APPROVE_OPTION){
					
					// Record the file selected - update the associated report label
					File selectedFile = fileChooser.getSelectedFile();
					outputFastaFile = selectedFile.getName();
					lblOutputFastaFileSelected.setText(selectedFile.getName());
					
					// Update the directory to open browser into
					directory = selectedFile.getParentFile();					
				}				
			}
		});
		getContentPane().add(buttonOutputFastaFile);
		
	}
	
	/**
	 * General methods area
	 * @throws IOException 
	 */
	
	public void runHomoplasyFinder() throws IOException{
		
		// Read in tree
		Node tree = HomoplasyFinder4.readNewickTree(treeFile, false);
		
		// Read in the FASTA file
		Sequence[] sequences = GeneticMethods.readFastaFile(fastaFile, false);
		
		// Get the alleles in the population and the isolates they are associated with
		Hashtable<String, ArrayList<String>> alleles = HomoplasyFinder4.noteAllelesInPopulation(sequences, false);
		ArrayList<String> positions = HomoplasyFinder4.getAllelePositions(alleles);
		
		// Assign alleles
		Hashtable<String, Integer> assigned = new Hashtable<String, Integer>();
		HomoplasyFinder4.assignAllelesToCurrentNode(tree, alleles, positions, assigned,  HomoplasyFinder4.getSequenceIDs(sequences), false);
		
		int[] homoplasyPositions = HomoplasyFinder4.examineUnAssignedAlleles(assigned, alleles, false, directory.getAbsolutePath() + "\\", reportFile, date, textArea);
		
		/**
		 * Return a FASTA file without the homoplasy sites
		 */
		HomoplasyFinder4.printFASTAWithoutHomoplasies(homoplasyPositions, directory.getAbsolutePath() + "\\", outputFastaFile, date, sequences, false);
	}
}
