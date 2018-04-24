package identifyingHomoplasies;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.FormSpecs;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

public class TestGUI extends JFrame {

	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					TestGUI frame = new TestGUI();
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
	public TestGUI() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		JButton btnFindFile = new JButton("Find File");
		btnFindFile.setBounds(5, 5, 106, 23);
		
		JLabel lblFileSelected = new JLabel("File selected");
		lblFileSelected.setBounds(130, 9, 289, 14);
		
		JButton button = new JButton("Find File");
		button.setBounds(5, 39, 106, 23);
		
		JLabel label = new JLabel("File selected");
		label.setBounds(130, 43, 289, 14);
		
		JButton button_1 = new JButton("Find File");
		button_1.setBounds(5, 73, 106, 23);
		
		JLabel label_1 = new JLabel("File selected");
		label_1.setBounds(130, 77, 289, 14);
		contentPane.setLayout(null);
		contentPane.add(btnFindFile);
		contentPane.add(lblFileSelected);
		contentPane.add(button);
		contentPane.add(label);
		contentPane.add(button_1);
		contentPane.add(label_1);
	}
}
