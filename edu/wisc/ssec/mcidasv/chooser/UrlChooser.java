/*
 * $Id$
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package edu.wisc.ssec.mcidasv.chooser;


import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.w3c.dom.Element;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Position;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.TextColor;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;

import ucar.unidata.data.DataManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


/**
 * Allows the user to select a url as a data source
 *
 * @author IDV development team
 * @version $Revision$Date: 2008/12/01 21:06:31 $
 */


public class UrlChooser extends ucar.unidata.idv.chooser.UrlChooser implements Constants {

    /** Manages the pull down list of urls */
    private PreferenceList prefList;

    /** The list of urls */
    private JComboBox box;
    private JTextField boxEditor;

    /** The text area for multi-line urls */
    private JTextArea textArea;

    /** text scroller */
    private JScrollPane textScroller;

    /** Holds the combo box */
    private JPanel urlPanel;

    /** Holds the text area */
    private JPanel textPanel;

    /** Are we showing the combo box */
    private boolean showBox = true;

    /** Swtich */
    private JButton switchBtn;

    /** panel */
    private GuiUtils.CardLayoutPanel cardLayoutPanel;
    
    /**
     * Get a handle on the IDV
     */
    protected IntegratedDataViewer idv = getIdv();

    /**
     * Create the <code>UrlChooser</code>
     *
     * @param mgr The <code>IdvChooserManager</code>
     * @param root  The xml root that defines this chooser
     *
     */
    public UrlChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        
        loadButton = McVGuiUtils.makeImageTextButton(ICON_ACCEPT_SMALL, getLoadCommandName());
        loadButton.setActionCommand(getLoadCommandName());
        loadButton.addActionListener(this);

    }

    /**
     * toggle the combobox with the text area
     */
    public void switchFields() {
        if (showBox) {
            cardLayoutPanel.show(urlPanel);
        } else {
            cardLayoutPanel.show(textPanel);
        }
        updateStatus();
    }

    /**
     * Disable/enable any components that depend on the server.
     * Try to update the status label with what we know here.
     */
    protected void updateStatus() {
        if (boxEditor==null || textArea==null) return;
        if (showBox) {
        	if (!boxEditor.getText().trim().equals(""))
        		setHaveData(true);
        	else
        		setHaveData(false);
        } else {
        	if (!textArea.getText().trim().equals(""))
        		setHaveData(true);
        	else
        		setHaveData(false);
        }
        super.updateStatus();
        if (!getHaveData()) {
        	if (showBox) setStatus("Enter a URL");
        	else setStatus("Enter one or more URLs");
        }
    }
    
    /**
     * Load the url
     */
    private void loadURLInner() {

        String url = "";
        String    dataSourceId = getDataSourceId();
        if (showBox) {
            Object selectedItem = box.getSelectedItem();
            if (selectedItem != null) {
                url = selectedItem.toString().trim();
            }
            if (url.length() == 0 && dataSourceId == null) {
                userMessage("Please specify a url");
                return;
            }
        }

        Hashtable properties   = new Hashtable();
        if (dataSourceId != null) {
            properties.put(DataManager.DATATYPE_ID, dataSourceId);
        }

        if (showBox) {
            if (idv.handleAction(url, properties)) {
                closeChooser();
                prefList.saveState(box);
            }
        } else {
            List urls = StringUtil.split(textArea.getText(), "\n", true,
                                         true);

            if ((urls.size() > 0)
                    && makeDataSource(urls, dataSourceId, properties)) {
                closeChooser();
            }
        }
    }
        
    /**
     * Make the contents
     *
     * @return  the contents
     */
    protected JPanel doMakeInnerPanel() {
        JRadioButton singleBtn = new JRadioButton("Single", true);
        JRadioButton multipleBtn = new JRadioButton("Multiple", false);
        singleBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showBox = true;
				switchFields();
			}
        });
        multipleBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showBox = false;
				switchFields();
			}
        });
        GuiUtils.buttonGroup(singleBtn, multipleBtn);
        JPanel radioPanel = GuiUtils.hbox(singleBtn, multipleBtn);
        
        prefList = getPreferenceList(PREF_URLLIST);
        box = prefList.createComboBox(CMD_LOAD, this);
        boxEditor = (JTextField)box.getEditor().getEditorComponent();
        boxEditor.addKeyListener(new KeyListener() {
        	public void keyPressed(KeyEvent e) {}
        	public void keyReleased(KeyEvent e) {
        		updateStatus();
        	}
        	public void keyTyped(KeyEvent e) {}
        });
        
        textArea = new JTextArea(5, 30);
        textScroller = new JScrollPane(textArea);
        textArea.addKeyListener(new KeyListener() {
        	public void keyPressed(KeyEvent e) {}
        	public void keyReleased(KeyEvent e) {
        		updateStatus();
        	}
        	public void keyTyped(KeyEvent e) {}
        });
        
        urlPanel = GuiUtils.top(box);
        textPanel = GuiUtils.top(textScroller);
        
        cardLayoutPanel = new GuiUtils.CardLayoutPanel();
        cardLayoutPanel.addCard(urlPanel);
        cardLayoutPanel.addCard(textPanel);
        
        JPanel showPanel = McVGuiUtils.topBottom(radioPanel, cardLayoutPanel, null);
        
        setHaveData(false);
        updateStatus();
        return McVGuiUtils.makeLabeledComponent("URL:", showPanel);
    }

    private JLabel statusLabel = new JLabel("Status");

    @Override
    public void setStatus(String statusString, String foo) {
    	if (statusString == null)
    		statusString = "";
    	statusLabel.setText(statusString);
    }
        
    /**
     * Create a more McIDAS-V-like GUI layout
     */
    protected JComponent doMakeContents() {
        JComponent typeComponent = getDataSourcesComponent();
        if (typeComponent==null) typeComponent=new JLabel("No data type specified");

        JPanel innerPanel = doMakeInnerPanel();
        
        // Start building the whole thing here
    	JPanel outerPanel = new JPanel();

        JLabel typeLabel = McVGuiUtils.makeLabelRight("Data Type:");
            	
        JLabel statusLabelLabel = McVGuiUtils.makeLabelRight("");
        
        statusLabel.setText("Status");
        McVGuiUtils.setLabelPosition(statusLabel, Position.RIGHT);
        McVGuiUtils.setComponentColor(statusLabel, TextColor.STATUS);
        
        JButton helpButton = McVGuiUtils.makeImageButton(ICON_HELP, "Show help");
        helpButton.setActionCommand(GuiUtils.CMD_HELP);
        helpButton.addActionListener(this);
        
        JButton refreshButton = McVGuiUtils.makeImageButton(ICON_REFRESH, "Refresh");
        refreshButton.setActionCommand(GuiUtils.CMD_UPDATE);
        refreshButton.addActionListener(this);
        
        McVGuiUtils.setComponentWidth(loadButton, Width.DOUBLE);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(outerPanel);
        outerPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(helpButton)
                        .add(GAP_RELATED)
                        .add(refreshButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(loadButton))
                        .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(innerPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(layout.createSequentialGroup()
                                .add(typeLabel)
                                .add(GAP_RELATED)
                                .add(typeComponent))
                            .add(layout.createSequentialGroup()
                                .add(statusLabelLabel)
                                .add(GAP_RELATED)
                                .add(statusLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
            	.addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(typeLabel)
                    .add(typeComponent))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(innerPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(statusLabelLabel)
                    .add(statusLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(loadButton)
                    .add(refreshButton)
                    .add(helpButton))
                .addContainerGap())
        );
    
        return outerPanel;

    }

}

