/**
 * 
 */
package edu.wisc.ssec.mcidasv.servermanager;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import net.miginfocom.swing.MigLayout;

/**
 * 
 * 
 */
public class ImportUrl extends JDialog {

    private final JPanel contentPanel = new JPanel();

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        try {
            ImportUrl dialog = new ImportUrl();
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JCheckBox acctBox;
    private JTextField mctableField;
    private JTextField userField;
    private JTextField projField;
    
    
    /**
     * Create the dialog.
     */
    public ImportUrl() {
        setBounds(100, 100, 450, 215);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(new MigLayout("", "[][grow]", "[][][][]"));
        
        JLabel mctableLabel = new JLabel("MCTABLE.TXT URL:");
        contentPanel.add(mctableLabel, "cell 0 0,alignx trailing");
        
        mctableField = new JTextField();
        contentPanel.add(mctableField, "cell 1 0,growx");
        
        acctBox = new JCheckBox("Use default accounting?");
        acctBox.setSelected(true);
        contentPanel.add(acctBox, "cell 1 1");
        
        JLabel userLabel = new JLabel("Username:");
        contentPanel.add(userLabel, "cell 0 2,alignx trailing");
        
        userField = new JTextField();
        contentPanel.add(userField, "cell 1 2,growx");
        userField.setColumns(4);
        
        JLabel projLabel = new JLabel("Project #:");
        contentPanel.add(projLabel, "cell 0 3,alignx trailing");
        
        projField = new JTextField();
        contentPanel.add(projField, "cell 1 3,growx");
        projField.setColumns(4);
        
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                JButton okButton = new JButton("OK");
                okButton.setActionCommand("OK");
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
            }
            {
                JButton cancelButton = new JButton("Cancel");
                cancelButton.setActionCommand("Cancel");
                buttonPane.add(cancelButton);
            }
        }
    }

}
