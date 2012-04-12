/**
 * 
 */
package edu.wisc.ssec.mcidasv.servermanager;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.miginfocom.swing.MigLayout;

/**
 * 
 * 
 */
public class ImportUrl extends JDialog {
    
    protected static final String CMD_DEFAULT_ACCOUNTING = "use_default_accounting";
    protected static final String CMD_IMPORT = "import";
    protected static final String CMD_CANCEL = "cancel";
    
    private TabbedAddeManager serverManagerGui;
    
    private static final Logger logger = LoggerFactory.getLogger(ImportUrl.class);
    
    private JCheckBox acctBox;
    private JTextField mctableField;
    private JTextField userField;
    private JTextField projField;
    private JButton okButton;
    private JButton cancelButton;
    
    private final JPanel contentPanel = new JPanel();
    
    
    /**
     * Create the dialog.
     */
    public ImportUrl() {
        initComponents();
    }
    
    public ImportUrl(final TabbedAddeManager serverManagerGui) {
        this.serverManagerGui = serverManagerGui;
    }

    public void initComponents() {
        setTitle("Import from URL");
        setBounds(100, 100, 450, 215);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(new MigLayout("", "[][grow]", "[][][][]"));
        
        JLabel mctableLabel = new JLabel("MCTABLE.TXT URL:");
        contentPanel.add(mctableLabel, "cell 0 0,alignx trailing");
        
        mctableField = new JTextField();
        contentPanel.add(mctableField, "cell 1 0,growx");
        mctableField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(final DocumentEvent e) {
                int len = mctableField.getText().trim().length();
//                okButton.setEnabled(mctableField.getText().trim().length() > 0);
                okButton.setEnabled(len > 0);
                logger.trace("len={}", len);
            }
            public void insertUpdate(final DocumentEvent e) {}
            public void removeUpdate(final DocumentEvent e) {}
        });
        
        acctBox = new JCheckBox("Use default accounting?");
        acctBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                userField.setEnabled(!acctBox.isSelected());
                projField.setEnabled(!acctBox.isSelected());
            }
        });
        acctBox.setSelected(true);
        contentPanel.add(acctBox, "cell 1 1");
        
        JLabel userLabel = new JLabel("Username:");
        contentPanel.add(userLabel, "cell 0 2,alignx trailing");
        
        userField = new JTextField();
        contentPanel.add(userField, "cell 1 2,growx");
        userField.setColumns(4);
        userField.setEnabled(!acctBox.isSelected());
        
        JLabel projLabel = new JLabel("Project #:");
        contentPanel.add(projLabel, "cell 0 3,alignx trailing");
        
        projField = new JTextField();
        contentPanel.add(projField, "cell 1 3,growx");
        projField.setColumns(4);
        projField.setEnabled(!acctBox.isSelected());
        
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                okButton = new JButton("Import MCTABLE.TXT");
                okButton.setActionCommand(CMD_IMPORT);
                buttonPane.add(okButton);
//                getRootPane().setDefaultButton(okButton);
            }
            {
                cancelButton = new JButton("Cancel");
                cancelButton.setActionCommand(CMD_CANCEL);
                buttonPane.add(cancelButton);
            }
        }
    }
    
    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    ImportUrl dialog = new ImportUrl();
                    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    dialog.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }
}
