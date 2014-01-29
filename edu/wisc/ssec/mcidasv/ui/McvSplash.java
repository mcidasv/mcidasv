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

package edu.wisc.ssec.mcidasv.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.border.BevelBorder;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.servermanager.EntryStore;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.ui.RovingProgress;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Msg;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Resource;
import ucar.unidata.util.StringUtil;

/**
 * <p>This is a straight up copy of {@link ucar.unidata.idv.ui.IdvSplash} with
 * the easter egg taken out.</p>
 * 
 * <p>Control+double click isn't enough with all the OS X users here at SSEC ;)</p>
 * 
 * @author IDV development team
 */
public class McvSplash extends JWindow {
    
    private IntegratedDataViewer idv;

    /** The JLabel to show messages */
    private JLabel splashLbl;

    /** The text to use in the splash screen */
    private String splashTitle = null;

    /** The icon to use in the splash screen */
    private ImageIcon splashIcon;

    /** The icon to use when the mouse rolls over the splash icon */
    private ImageIcon splashRolloverIcon;

    /**
     *  Keep the splash progress bar around to tell it to stop.
     */
    private RovingProgress splashProgressBar;

    /**
     * Create the splash screen
     *
     * @param idv The IDV
     *
     */
    public McvSplash(IntegratedDataViewer idv) {
        this.idv = idv;
        init();
    }

    /**
     *  Show a message in the splash screen (if it exists)
     *
     * @param m The message
     */
    public void splashMsg(String m) {
        if (splashLbl != null) {
            splashLbl.setText(" " + Msg.msg(m) + " ");
        }
    }

    /**
     *  Close and dispose of the splash window (if it has been created).
     */
    public void doClose() {
        if (splashProgressBar != null) {
            splashProgressBar.stop();
        }
        setVisible(false);
        dispose();
    }

    /**
     *  Create and return (if not in test mode) the splash screen.
     */
    private void init() {

        try {
            splashTitle = idv.getProperty("idv.ui.splash.title", "");

            splashTitle =
                idv.getResourceManager().getResourcePath(splashTitle);

            splashTitle =
                StringUtil.replace(splashTitle, "%IDV.TITLE%",
                                   (String) idv.getProperty("idv.title",
                                       "McIDAS-V"));

            splashIcon =
                GuiUtils.getImageIcon(idv.getProperty("idv.ui.splash.icon",
                    "/edu/wisc/ssec/mcidasv/images/mcidasv_logo.gif"));
            splashRolloverIcon = GuiUtils.getImageIcon(
                idv.getProperty(
                    "idv.ui.splash.iconroll",
                    "/edu/wisc/ssec/mcidasv/images/mcidasv_logo.gif"));
        } catch (Exception exc) {}

        JLabel image = ((splashIcon != null)
                        ? new JLabel(splashIcon)
                        : new JLabel("McIDAS-V Nightly"));

        if ((splashIcon != null) && (splashRolloverIcon != null)) {
            int width = Math.max(splashIcon.getIconWidth(),
                                 splashRolloverIcon.getIconWidth());
            int height = Math.max(splashIcon.getIconHeight(),
                                  splashRolloverIcon.getIconHeight());
            image.setPreferredSize(new Dimension(width, height));
        }

        image.addMouseListener(new ObjectListener(image) {
            public void mouseEntered(MouseEvent e) {
                if (splashRolloverIcon != null) {
                    ((JLabel) e.getSource()).setIcon(splashRolloverIcon);
                }
            }

            public void mouseExited(MouseEvent e) {
                if (splashIcon != null) {
                    ((JLabel) e.getSource()).setIcon(splashIcon);
                }
            }
        });

        splashLbl = GuiUtils.cLabel(" ");
        splashLbl.setForeground(Color.gray);
                
        splashProgressBar = new RovingProgress(Constants.MCV_BLUE);
        splashProgressBar.start();
        splashProgressBar.setBorder(
            BorderFactory.createLineBorder(Color.gray));

        JButton cancelButton = McVGuiUtils.makePrettyButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                EntryStore serverManager = (EntryStore)(((McIDASV)idv).getServerManager());
                if (serverManager != null) {
                    serverManager.stopLocalServer();
                }
                ((McIDASV)idv).exitMcIDASV(0);
            }
        });
        if ((splashTitle == null) || splashTitle.trim().equals("")) {
            String version = idv.getStateManager().getVersion();
            String title   = idv.getStateManager().getTitle();
            splashTitle = title + " " + version;
        }

        JLabel versionLabel = GuiUtils.cLabel("<html><center><b>"
                                  + hiliteRevision(splashTitle) + "</center></b></html>");

        JPanel imagePanel = GuiUtils.inset(image, new Insets(4, 35, 0, 35));
        JPanel titlePanel = GuiUtils.center(versionLabel);

        JPanel barPanel = GuiUtils.inset(splashProgressBar,
                                         new Insets(4, 4, 1, 4));

        JPanel topPanel = GuiUtils.vbox(imagePanel, titlePanel, barPanel);
        topPanel = GuiUtils.centerBottom(topPanel, splashLbl);
        JPanel contents =
            GuiUtils.topCenter(topPanel,
                               GuiUtils.inset(GuiUtils.wrap(cancelButton),
                                   4));
        JPanel outer = GuiUtils.center(contents);
//        contents.setBorder(
//            BorderFactory.createBevelBorder(BevelBorder.RAISED));
        outer.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED,
                Color.gray, Color.gray));
        getContentPane().add(outer);
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width / 2 - getWidth() / 2,
                    screenSize.height / 2 - getHeight() / 2);

        ucar.unidata.util.Msg.translateTree(this);

        //show();
        setVisible(true);
        toFront();
    }
    
    /**
     * Highlight the minor version number if it exists.
     * 
     * @param version Version string. {@code null} is allowed.
     *
     * @return {@code null} if {@code version} is {@code null}, otherwise
     * a {@code String} containing HTML markup.
     */
    private String hiliteRevision(String version) {
		String hilited = version;
		if (version == null) return null;
		
		try {
			int p = version.indexOf("beta");
			if (p > 0) {
				hilited += "<br><font color=red>THIS IS BETA SOFTWARE</font>";
			}
			else {
				p = version.indexOf("alpha");
				if (p > 0) {
					hilited += "<br><font color=red>THIS IS ALPHA SOFTWARE</font>";
				}
			}
		} catch (Exception e) {}

		return hilited;
    }
}
