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
            System.err.println("splash title 1: " + splashTitle);
            splashTitle =
                idv.getResourceManager().getResourcePath(splashTitle);
            System.err.println("splash title 2: " + splashTitle);
            splashTitle =
                StringUtil.replace(splashTitle, "%IDV.TITLE%",
                                   (String) idv.getProperty("idv.title",
                                       "McIDAS-V"));
            System.err.println("splash title 3: " + splashTitle);	
            splashIcon =
                GuiUtils.getImageIcon(idv.getProperty("idv.ui.splash.icon",
                    "/edu/wisc/ssec/mcidasv/images/mcidasv_logo.gif"));
            splashRolloverIcon = GuiUtils.getImageIcon(
                idv.getProperty(
                    "idv.ui.splash.iconroll",
                    "/edu/wisc/ssec/mcidasv/images/mcidasv_logo.gif"));
        } catch (Exception exc) {
        	System.err.println("Exception: " + exc.getMessage());
        	exc.printStackTrace();
        }


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

        splashProgressBar = new RovingProgress();
        splashProgressBar.start();
        splashProgressBar.setBorder(
            BorderFactory.createLineBorder(Color.gray));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                System.exit(0);
            }
        });

        if ((splashTitle == null) || splashTitle.trim().equals("")) {
            String version = idv.getStateManager().getVersion();
            String title   = idv.getStateManager().getTitle();
            splashTitle = title + " " + version;
        }

        JLabel versionLabel = GuiUtils.cLabel("<html><center><b>"
                                  + splashTitle + "</center></b></html>");

        JPanel imagePanel = GuiUtils.inset(image, new Insets(4, 35, 0, 35));
        JPanel titlePanel = GuiUtils.center(versionLabel);

        JPanel barPanel = GuiUtils.inset(splashProgressBar,
                                         new Insets(4, 1, 1, 1));

        JPanel topPanel = GuiUtils.vbox(imagePanel, titlePanel, barPanel);
        topPanel = GuiUtils.centerBottom(topPanel, splashLbl);
        JPanel contents =
            GuiUtils.topCenter(topPanel,
                               GuiUtils.inset(GuiUtils.wrap(cancelButton),
                                   4));
        JPanel outer = GuiUtils.center(contents);
        contents.setBorder(
            BorderFactory.createBevelBorder(BevelBorder.RAISED));
        outer.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED,
                Color.gray, Color.gray));
        getContentPane().add(outer);
        pack();
        Dimension size       = getSize();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width / 2 - size.width / 2,
                    screenSize.height / 2 - size.height / 2);

        ucar.unidata.util.Msg.translateTree(this);

        //show();
        setVisible(true);
        toFront();
    }
}
