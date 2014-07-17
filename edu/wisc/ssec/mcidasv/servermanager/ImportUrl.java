/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
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
import javax.swing.WindowConstants;
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
public class ImportUrl extends JDialog implements ActionListener {
    
    protected static final String CMD_DEFAULT_ACCOUNTING = "use_default_accounting";
    protected static final String CMD_IMPORT = "import";
    protected static final String CMD_CANCEL = "cancel";
    
    private TabbedAddeManager serverManagerGui;
    private EntryStore serverManager;
    
    private static final Logger logger = LoggerFactory.getLogger(ImportUrl.class);
    
    private JLabel userLabel;
    private JLabel projLabel;
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
    
    public ImportUrl(final EntryStore serverManager, final TabbedAddeManager serverManagerGui) {
        this.serverManager = serverManager;
        this.serverManagerGui = serverManagerGui;
        initComponents();
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
        
        acctBox = new JCheckBox("Use ADDE accounting?");
        acctBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean selected = acctBox.isSelected();
                userLabel.setEnabled(selected);
                userField.setEnabled(selected);
                projLabel.setEnabled(selected);
                projField.setEnabled(selected);
            }
        });
        acctBox.setSelected(false);
        contentPanel.add(acctBox, "cell 1 1");
        
        userLabel = new JLabel("Username:");
        userLabel.setEnabled(acctBox.isSelected());
        contentPanel.add(userLabel, "cell 0 2,alignx trailing");
        
        userField = new JTextField();
        contentPanel.add(userField, "cell 1 2,growx");
        userField.setColumns(4);
        userField.setEnabled(acctBox.isSelected());
        
        projLabel = new JLabel("Project #:");
        projLabel.setEnabled(acctBox.isSelected());
        contentPanel.add(projLabel, "cell 0 3,alignx trailing");
        
        projField = new JTextField();
        contentPanel.add(projField, "cell 1 3,growx");
        projField.setColumns(4);
        projField.setEnabled(acctBox.isSelected());
        
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                okButton = new JButton("Import MCTABLE.TXT");
                okButton.setActionCommand(CMD_IMPORT);
                okButton.addActionListener(this);
                buttonPane.add(okButton);
//                getRootPane().setDefaultButton(okButton);
            }
            {
                cancelButton = new JButton("Cancel");
                cancelButton.setActionCommand(CMD_CANCEL);
                cancelButton.addActionListener(this);
                buttonPane.add(cancelButton);
            }
        }
        
    }
    
    
    
    public void actionPerformed(final ActionEvent e) {
        String cmd = e.getActionCommand();
        if (CMD_CANCEL.equals(cmd)) {
            dispose();
        } else if (CMD_IMPORT.equals(cmd)) {
            
            String path = mctableField.getText().trim();
            String user = AddeEntry.DEFAULT_ACCOUNT.getUsername();
            String proj = AddeEntry.DEFAULT_ACCOUNT.getProject();
            if (acctBox.isSelected()) {
                user = userField.getText().trim();
                proj = projField.getText().trim();
            }
            logger.trace("importing: path={} user={} proj={}", path, user, proj);
            if (serverManagerGui != null) {
                serverManagerGui.importMctable(path, user, proj);
            }
            dispose();
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
                    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    dialog.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }
}
