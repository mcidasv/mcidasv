package edu.wisc.ssec.mcidasv.util;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.jdesktop.layout.GroupLayout;
import org.jdesktop.layout.LayoutStyle;

/**
 * {@code WelcomeWindow} is really just intended to <i>try</i> to detect known
 * hardware problems and inform the user about any problems.
 *
 * <p>The current implementation does not perform <i>any</i> detection, but
 * expect this to change.
 */
// NOTE TO MCV CODERS: 
// **DOCUMENT WHAT CHECKS AND/OR DETECTION ARE BEING PERFORMED**
public class WelcomeWindow extends javax.swing.JFrame {

    /** Path to the HTML to display within {@link #textPane}. */
    private static final String WELCOME_HTML = 
        "/edu/wisc/ssec/mcidasv/resources/welcome.html";

    /** 
     * Message to display if there was a problem loading 
     * {@link #WELCOME_HTML}. 
     */
    private static final String ERROR_MESSAGE = 
        "McIDAS-V had a problem displaying its welcome message. Please"
        + " contact the McIDAS Help Desk for assistance.";

    /** Dimensions of the welcome window frame. */
    private static final Dimension WINDOW_SIZE = new Dimension(495, 431);

    /** Java-friendly location of the path to the welcome message. */
    private final java.net.URL contents;

    /** 
     * Creates a new welcome window (just a fairly basic window).
     */
    public WelcomeWindow() {
        this.contents = WelcomeWindow.class.getResource(WELCOME_HTML);
        initComponents();
    }

    /**
     * Handles the user either clicking {@link #quitButton} or closing the
     * window.
     * 
     * TODO(jon): and so what should the meat of this one be?
     */
    private void handleQuit() {
        dispose();
    }

    /**
     * Mostly courtesy of Netbeans and its GUI generator!
     */
    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Welcome to McIDAS-V");
        setLocationByPlatform(true);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent evt) {
                // hmm... for consistency's sake should there be something 
                // like quitButtonActionListener() instead of just calling 
                // handleQuit()?
                handleQuit();
            }
        });

        try {
            textPane.setPage(contents);
        } catch (java.io.IOException e1) {
            textPane.setText(ERROR_MESSAGE);
            e1.printStackTrace();
        }

        textPane.setEditable(false);
        textPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(final HyperlinkEvent evt) {
                textPaneHyperlinkUpdate(evt);
            }
        });
        scrollPane.setViewportView(textPane);

        GroupLayout mainPanelLayout = new GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(GroupLayout.LEADING)
            .add(scrollPane, GroupLayout.DEFAULT_SIZE, 498, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(GroupLayout.LEADING)
            .add(scrollPane, GroupLayout.DEFAULT_SIZE, 280, Short.MAX_VALUE)
        );

//        showCheckbox.setText("Show upon startup");
        showCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent evt) {
                showCheckboxActionPerformed(evt);
            }
        });

//        startButton.setText("Start McIDAS-V");
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

//        quitButton.setText("Quit");
        quitButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent evt) {
                quitButtonActionPerformed(evt);
            }
        });

        GroupLayout commandPanelLayout = new GroupLayout(commandPanel);
        commandPanel.setLayout(commandPanelLayout);
        commandPanelLayout.setHorizontalGroup(
            commandPanelLayout.createParallelGroup(GroupLayout.LEADING)
            .add(commandPanelLayout.createSequentialGroup()
                .add(showCheckbox)
                .addPreferredGap(LayoutStyle.RELATED, 132, Short.MAX_VALUE)
                .add(quitButton)
                .addPreferredGap(LayoutStyle.UNRELATED)
                .add(startButton))
        );
        commandPanelLayout.setVerticalGroup(
            commandPanelLayout.createParallelGroup(GroupLayout.LEADING)
            .add(showCheckbox)
            .add(commandPanelLayout.createParallelGroup(GroupLayout.BASELINE)
                .add(startButton)
                .add(quitButton))
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.LEADING)
            .add(GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(GroupLayout.TRAILING)
                    .add(GroupLayout.LEADING, commandPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(GroupLayout.LEADING, mainPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(20, 20, 20)
                .add(mainPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(commandPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );

        pack();
        setSize(WINDOW_SIZE);
    }// </editor-fold>

    /**
     * Listens to {@link #textPane} in order to handle the user clicking on
     * HTML links.
     *
     * @param evt Event to handle. Anything other than
     * {@link javax.swing.event.HyperlinkEvent.EventType#ACTIVATED} is ignored.
     * 
     * @see WebBrowser#browse(String)
     */
    private void textPaneHyperlinkUpdate(final HyperlinkEvent evt) {
        if (evt.getEventType() != HyperlinkEvent.EventType.ACTIVATED)
            return;

        String url = null;
        if (evt.getURL() == null)
            url = evt.getDescription();
        else
            url = evt.getURL().toString();

        WebBrowser.browse(url);
    }

    /**
     * Handles the user interacting with the 
     * {@literal "continue showing the welcome window"} checkbox. 
     * Defaults to unselected.
     * 
     * <p>If selected, the welcome window will appear the next time McIDAS-V
     * starts up. If unselected, the welcome window won't appear again (unless
     * the user opts to turn the preference back on).
     * 
     * @param evt Event to handle.
     */
    private void showCheckboxActionPerformed(final ActionEvent evt) {
        
    }

    /**
     * Handles the user clicking on {@link #startButton}.
     * 
     * TODO(jon): what should happen??
     * 
     * @param evt Event to handle... Basically ignored.
     */
    private void startButtonActionPerformed(final ActionEvent evt) {
        
    }

    /**
     * Handles the user clicking on {@link #quitButton}. 
     * 
     * TODO(jon): aside from killing the window, what should happen??
     * 
     * @param evt Event to handle... Basically ignored.
     */
    private void quitButtonActionPerformed(final ActionEvent evt) {
        handleQuit();
    }

    /**
     * Allows you to start up new welcome window in a way that is entirely
     * independent from McIDAS-V. Probably only useful for testing, but who
     * knows?
     *
     * @param args Command line arguments.
     */
    public static void main(final String[] args) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new WelcomeWindow().setVisible(true);
            }
        });
    }

    // boring ol' GUI components
    private final JPanel commandPanel = new JPanel();
    private final JPanel mainPanel = new JPanel();
    private final JButton quitButton = new JButton("Quit");
    private final JScrollPane scrollPane = new JScrollPane();
    private final JCheckBox showCheckbox = 
        new JCheckBox("Show upon startup?", false);
    private final JButton startButton = new JButton("Start McIDAS-V");
    private final JTextPane textPane = new JTextPane();
}
