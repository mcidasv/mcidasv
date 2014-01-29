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
package edu.wisc.ssec.mcidasv.supportform;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import ucar.unidata.idv.IdvObjectStore;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ui.IdvUIManager;

import edu.wisc.ssec.mcidasv.util.BackgroundTask;
import edu.wisc.ssec.mcidasv.util.CollectionHelpers;
import edu.wisc.ssec.mcidasv.util.Contract;
import edu.wisc.ssec.mcidasv.util.FocusTraveller;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class handles all the GUI elements of a McIDAS-V support request.
 */
public class SupportForm extends JFrame {
    
    public static final String PROP_SUPPORTREQ_BUNDLE = "mcv.supportreq.bundle";
    
    public static final String PROP_SUPPORTREQ_CC = "mcv.supportreq.cc";
    
    public static final String PROP_SUPPORTREQ_CONFIRMEMAIL = "mcv.supportreq.confirmedemail";
    
    private static final String HELP_ID = "idv.tools.supportrequestform";
    
    private static ExecutorService exec = Executors.newCachedThreadPool();
    
    private final IdvObjectStore store;
    
    private final StateCollector collector;
    
    private final CancelListener listener = new CancelListener();
    
    private JPanel contentPane;
    private JTextField userField;
    private JTextField emailField;
    private JTextField confirmField;
    private JTextField organizationField;
    private JTextField subjectField;
    private JTextField attachmentOneField;
    private JTextField attachmentTwoField;
    private JTextArea descriptionArea;
    private JCheckBox bundleCheckBox;
    private JCheckBox ccCheckBox;
    private JButton sendButton;
    private JButton cancelButton;
    private JButton helpButton;
    
    /**
     * Creates a support request form that collects information about
     * the current McIDAS-V session.
     * 
     * @param store Storage for persisted user input. Should not be {@code null}.
     * @param collector Collects information about the current session.
     */
    public SupportForm(IdvObjectStore store, StateCollector collector) {
        this.store = Contract.notNull(store);
        this.collector = Contract.notNull(collector);
        initComponents();
        unpersistInput();
        otherDoFocusThingNow();
    }
    
    /**
     * Saves user input for the following: name, email address, email address
     * confirmation, organization, whether or not to CC the user a copy, and 
     * whether or not a {@literal "state"} bundle should be included.
     * 
     * <p>You should initialize the GUI components before calling this method.
     */
    private void persistInput() {
        store.put(IdvUIManager.PROP_HELP_NAME, getUser());
        store.put(IdvUIManager.PROP_HELP_EMAIL, getEmail());
        store.put(PROP_SUPPORTREQ_CONFIRMEMAIL, getConfirmedEmail());
        store.put(IdvUIManager.PROP_HELP_ORG, getOrganization());
        store.put(PROP_SUPPORTREQ_CC, getSendCopy());
        store.put(PROP_SUPPORTREQ_BUNDLE, getSendBundle());
        store.save();
    }
    
    /**
     * Loads user input for the following: name, email address, email address
     * confirmation, organization, whether or not to CC the user a copy, and 
     * whether or not a {@literal "state"} bundle should be included.
     * 
     * <p>You should initialize the GUI components before calling this method.
     */
    private void unpersistInput() {
        userField.setText(store.get(IdvUIManager.PROP_HELP_NAME, ""));
        emailField.setText(store.get(IdvUIManager.PROP_HELP_EMAIL, ""));
        confirmField.setText(store.get(PROP_SUPPORTREQ_CONFIRMEMAIL, ""));
        organizationField.setText(store.get(IdvUIManager.PROP_HELP_ORG, ""));
        ccCheckBox.setSelected(store.get(PROP_SUPPORTREQ_CC, true));
        bundleCheckBox.setSelected(store.get(PROP_SUPPORTREQ_BUNDLE, false));
    }
    
    /**
     * Create the frame.
     */
    public void initComponents() {
        setTitle("Request McIDAS-V Support");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 682, 538);
        contentPane = new JPanel();
        setContentPane(contentPane);
        contentPane.setLayout(new MigLayout("", "[][grow]", "[][][][][][][grow][][][][][][]"));
        
        JLabel nameLabel = new JLabel("Your Name:");
        contentPane.add(nameLabel, "cell 0 0,alignx right");
        
        userField = new JTextField();
        contentPane.add(userField, "cell 1 0,growx");
        userField.setName("user");
        userField.setColumns(10);
        
        JLabel emailLabel = new JLabel("Your Email:");
        contentPane.add(emailLabel, "cell 0 1,alignx right");
        
        emailField = new JTextField();
        contentPane.add(emailField, "cell 1 1,growx");
        emailField.setName("email");
        emailField.setColumns(10);
        
        JLabel confirmLabel = new JLabel("Confirm Email:");
        contentPane.add(confirmLabel, "cell 0 2,alignx right");
        
        confirmField = new JTextField();
        contentPane.add(confirmField, "cell 1 2,growx");
        confirmField.setName("confirm");
        confirmField.setColumns(10);
        
        JLabel organizationLabel = new JLabel("Organization:");
        contentPane.add(organizationLabel, "cell 0 3,alignx right");
        
        organizationField = new JTextField();
        contentPane.add(organizationField, "cell 1 3,growx");
        organizationField.setName("organization");
        organizationField.setColumns(10);
        
        JLabel subjectLabel = new JLabel("Subject:");
        contentPane.add(subjectLabel, "cell 0 4,alignx right");
        
        subjectField = new JTextField();
        contentPane.add(subjectField, "cell 1 4,growx");
        subjectField.setName("subject");
        subjectField.setColumns(10);
        
        JLabel descriptiveLabel = new JLabel("Please provide a thorough description of the problem you encountered.");
        contentPane.add(descriptiveLabel, "cell 1 5");
        
        JLabel descriptionLabel = new JLabel("Description:");
        contentPane.add(descriptionLabel, "cell 0 6,alignx right,aligny top");
        
        descriptionArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(descriptionArea);
        contentPane.add(scrollPane, "cell 1 6,grow");
        descriptionArea.setName("description");
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setColumns(20);
        descriptionArea.setRows(6);
        
        JLabel attachmentOneLabel = new JLabel("Attachment 1:");
        contentPane.add(attachmentOneLabel, "cell 0 7,alignx right");
        
        attachmentOneField = new JTextField();
        attachmentOneField.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent evt) {
                attachmentOneFieldMouseClicked(evt);
            }
        });
        contentPane.add(attachmentOneField, "flowx,cell 1 7,growx");
        attachmentOneField.setName("attachment1");
        attachmentOneField.setColumns(10);
        
        JButton attachmentOneButton = new JButton("Browse...");
        attachmentOneButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                attachmentOneButtonActionPerformed(evt);
            }
        });
        contentPane.add(attachmentOneButton, "cell 1 7,alignx left");
        
        JLabel attachmentTwoLabel = new JLabel("Attachment 2:");
        contentPane.add(attachmentTwoLabel, "cell 0 8,alignx right");
        
        attachmentTwoField = new JTextField();
        attachmentTwoField.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent evt) {
                attachmentTwoFieldMouseClicked(evt);
            }
        });
        contentPane.add(attachmentTwoField, "flowx,cell 1 8,growx");
        attachmentTwoField.setName("attachment2");
        attachmentTwoField.setColumns(10);
        
        JButton attachmentTwoButton = new JButton("Browse...");
        attachmentTwoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                attachmentTwoButtonActionPerformed(evt);
            }
        });
        contentPane.add(attachmentTwoButton, "cell 1 8,alignx left");
        
        bundleCheckBox = new JCheckBox("Include current application state as a bundle.");
        bundleCheckBox.setName("sendstate");
        contentPane.add(bundleCheckBox, "cell 1 9,alignx left");
        
        ccCheckBox = new JCheckBox("Send copy of support request to the email address I provided.");
        ccCheckBox.setName("ccrequest");
        ccCheckBox.setSelected(true);
        contentPane.add(ccCheckBox, "cell 1 10,alignx left");
        
        helpButton = new JButton("Help");
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ucar.unidata.ui.Help.getDefaultHelp().gotoTarget(HELP_ID);
            }
        });
        contentPane.add(helpButton, "flowx,cell 1 12,alignx right");
        
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(listener);
        contentPane.add(cancelButton, "cell 1 12,alignx right");
        
        sendButton = new JButton("Send Request");
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                sendRequest(evt);
            }
        });
        contentPane.add(sendButton, "cell 1 12,alignx right");
        contentPane.setFocusTraversalPolicy(new FocusTraveller(userField, emailField, confirmField, organizationField, subjectField, descriptionArea, attachmentOneButton, attachmentTwoButton, bundleCheckBox, ccCheckBox, helpButton, cancelButton, sendButton));
    }
    
    /**
     * Checks {@link #emailField} and {@link #confirmField} to see if they 
     * match (case is ignored).
     * 
     * @return {@code true} if there is a match, {@code false} otherwise.
     */
    public boolean checkEmailAddresses() {
        return emailField.getText().equalsIgnoreCase(confirmField.getText());
    }
    
    /**
     * Returns whatever occupies {@link #userField}.
     * 
     * @return User's name.
     */
    public String getUser() {
        return userField.getText();
    }
    
    /**
     * Returns whatever currently lives in {@link #emailField}.
     * 
     * @return User's email address.
     */
    public String getEmail() {
        return emailField.getText();
    }
    
    /**
     * Returns whatever currently lives in {@link #confirmField}.
     * 
     * @return User's confirmed email address.
     */
    public String getConfirmedEmail() {
        return confirmField.getText();
    }
    
    /**
     * Returns whatever resides in {@link #subjectField}.
     * 
     * @return Subject of the support request.
     */
    public String getSubject() {
        return subjectField.getText();
    }
    
    /**
     * Returns whatever has commandeered {@link #organizationField}.
     * 
     * @return Organization to which the user belongs.
     */
    public String getOrganization() {
        return organizationField.getText();
    }
    
    /**
     * Returns whatever is ensconced inside {@link #descriptionArea}.
     * 
     * @return Body of the user's email.
     */
    public String getDescription() {
        return descriptionArea.getText();
    }
    
    /**
     * Checks whether or not the user has attached a file in the 
     * {@literal "first file"} slot.
     * 
     * @return {@code true} if there's a file, {@code false} otherwise.
     */
    public boolean hasAttachmentOne() {
        return new File(attachmentOneField.getText()).exists();
    }
    
    /**
     * Checks whether or not the user has attached a file in the 
     * {@literal "second file"} slot.
     * 
     * @return {@code true} if there's a file, {@code false} otherwise.
     */
    public boolean hasAttachmentTwo() {
        return new File(attachmentTwoField.getText()).exists();
    }
    
    /**
     * Returns whatever file path has monopolized {@link #attachmentOneField}.
     * 
     * @return Path to the first file attachment, or a blank string if no file
     * has been selected.
     */
    public String getAttachmentOne() {
        return attachmentOneField.getText();
    }
    
    /**
     * Returns whatever file path has appeared within 
     * {@link #attachmentTwoField}.
     * 
     * @return Path to the second file attachment, or a blank string if no 
     * file has been selected.
     */
    public String getAttachmentTwo() {
        return attachmentTwoField.getText();
    }
    
    // TODO: javadocs!
    public boolean getSendCopy() {
        return ccCheckBox.isSelected();
    }
    
    public boolean getSendBundle() {
        return bundleCheckBox.isSelected();
    }
    
    public byte[] getExtraState() {
        return collector.getContents();
    }
    
    public String getExtraStateName() {
        return collector.getExtraAttachmentName();
    }
    
    public boolean canBundleState() {
        return collector.canBundleState();
    }
    
    public byte[] getBundledState() {
        return collector.getBundledState();
    }
    
    public String getBundledStateName() {
        return collector.getBundleAttachmentName();
    }
    
    public boolean canSendLog() {
        String path = collector.getLogPath();
        if (path == null || path.isEmpty()) {
            return false;
        }
        return new File(path).exists();
    }
    
    public String getLogPath() {
        return collector.getLogPath();
    }
    
    // TODO: dialogs are bad news bears.
    public void showSuccess() {
        setVisible(false);
        dispose();
        JOptionPane.showMessageDialog(null, "Support request sent successfully.", "Success", JOptionPane.DEFAULT_OPTION);
    }
    
    // TODO: dialogs are bad news hares.
    public void showFailure(final String reason) {
        String msg = "";
        if (reason == null || reason.isEmpty()) {
            msg = "Error sending request, could not determine cause.";
        } else {
            msg = "Error sending request:\n"+reason;
        }
        JOptionPane.showMessageDialog(this, msg, "Problem sending support request", JOptionPane.ERROR_MESSAGE);
        if (sendButton != null) {
            sendButton.setEnabled(true);
        }
    }
    
    /**
     * Checks to see if there is <i>anything</i> in the name, email, 
     * email confirmation, subject, and description.
     * 
     * @return {@code true} if all of the required fields have some sort of 
     * input, {@code false} otherwise.
     */
    private boolean validInput() {
        if (userField.getText().isEmpty()) {
            return false;
        }
        if (emailField.getText().isEmpty()) {
            return false;
        }
        if (confirmField.getText().isEmpty()) {
            return false;
        }
        if (subjectField.getText().isEmpty()) {
            return false;
        }
        if (descriptionArea.getText().isEmpty()) {
            return false;
        }
        return checkEmailAddresses();
    }
    
    private void attachmentOneButtonActionPerformed(ActionEvent evt) {
        attachFileToField(attachmentOneField);
    }
    
    private void attachmentTwoButtonActionPerformed(ActionEvent evt) {
        attachFileToField(attachmentTwoField);
    }
    
    private void attachmentOneFieldMouseClicked(MouseEvent evt) {
        if (attachmentOneField.getText().isEmpty()) {
            attachFileToField(attachmentOneField);
        }
    }
    
    private void attachmentTwoFieldMouseClicked(MouseEvent evt) {
        if (attachmentTwoField.getText().isEmpty()) {
            attachFileToField(attachmentTwoField);
        }
    }
    
    private void showInvalidInputs() {
        // how to display these?
        JOptionPane.showMessageDialog(this, "You must provide at least your name, email address, subject, and description.", "Missing required input", JOptionPane.ERROR_MESSAGE);
    }
    
    private void sendRequest(ActionEvent evt) {
        // check input validity
        if (!validInput()) {
            showInvalidInputs();
            return;
        }
        
        // disable the ability to send more requests until we get a status
        // reply from the server.
        if (sendButton != null) {
            sendButton.setEnabled(false);
        }
        
        // persist things that need it.
        persistInput();
        
        // create a background thread
        listener.task = new Submitter(this);
        
        // send the worker thread to the mines
        exec.execute(listener.task);
    }
    
    /**
     * Due to some fields persisting user input between McIDAS-V sessions we
     * set the focus to be on the first of these fields <i>lacking</i> input.
     */
    private void otherDoFocusThingNow() {
        List<JTextComponent> comps = CollectionHelpers.list(userField, 
            emailField, confirmField, organizationField, subjectField, descriptionArea);
        
        for (JTextComponent comp : comps) {
            if (comp.getText().isEmpty()) {
                comp.requestFocus(true);
                break;
            }
        }
    }
    
    private static void attachFileToField(final JTextField field) {
        String current = field.getText();
        JFileChooser jfc = new JFileChooser(current);
        if (jfc.showOpenDialog(field) == JFileChooser.APPROVE_OPTION) {
            field.setText(jfc.getSelectedFile().toString());
        }
    }
    
    private class CancelListener implements ActionListener {
        BackgroundTask<?> task;
        public void actionPerformed(ActionEvent e) {
            if (task != null) {
                task.cancel(true);
            }
            setVisible(false);
            dispose();
        }
    }
    
    /**
     * Launch a test of the Support Request Form.
     * 
     * @param args Ignored.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new SupportForm(
                        new IntegratedDataViewer().getStore(), 
                        new SimpleStateCollector()
                    ).setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
