package edu.wisc.ssec.mcidasv.ui;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import net.miginfocom.swing.MigLayout;
import javax.swing.JTextField;
import javax.swing.JLabel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LayerAnimationWindow extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(LayerAnimationWindow.class);
    
    private JPanel contentPane;
    private JTextField fieldCurrentDwell;
    private JToggleButton tglbtnEnableAnimation;
    private JButton btnSlower;
    private JButton btnFaster;
    private JLabel lblDwell; 

    /**
     * Create the frame.
     */
    public LayerAnimationWindow() {
        setTitle("Animate Visibility");
        setResizable(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 209, 141);
        contentPane = new JPanel();
        setContentPane(contentPane);
        tglbtnEnableAnimation = new JToggleButton("Enable Animation");

        tglbtnEnableAnimation.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                tglbtnEnableAnimationChanged(event);
            }
        });
        btnSlower = new JButton("Slower");
        btnSlower.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                btnSlowerActionPerformed(event);
            }
        });

        btnFaster = new JButton("Faster");
        btnFaster.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                btnFasterActionPerformed(event);
            }
        });

        lblDwell = new JLabel("Dwell (ms):");
        lblDwell.setFont(new Font("Lucida Grande", Font.PLAIN, 11));
        lblDwell.setEnabled(false);

        fieldCurrentDwell = new JTextField();
        fieldCurrentDwell.setEnabled(false);
        fieldCurrentDwell.setEditable(false);
        fieldCurrentDwell.setText("0");
        fieldCurrentDwell.setColumns(6);

        contentPane.setLayout(new MigLayout("", "[grow][grow]", "[][][]"));
        contentPane.add(tglbtnEnableAnimation, "flowy,cell 0 0 3 1,growx,aligny top");
        contentPane.add(btnSlower, "cell 0 1,alignx right,growy");
        contentPane.add(btnFaster, "cell 2 1,alignx left,growy");
        contentPane.add(lblDwell, "cell 0 2,alignx right,aligny baseline");
        contentPane.add(fieldCurrentDwell, "cell 2 2,alignx left,aligny baseline");

        
    }

    // dear god! change these names!
    private void tglbtnEnableAnimationChanged(final ItemEvent event) {
        logger.trace("toggle: {}", event);
        boolean animationEnabled = (event.getStateChange() == ItemEvent.SELECTED);
        btnSlower.setEnabled(animationEnabled);
        btnFaster.setEnabled(animationEnabled);
    }

    private void btnFasterActionPerformed(final ActionEvent event) {
        logger.trace("faster!");
    }

    private void btnSlowerActionPerformed(final ActionEvent event) {
        logger.trace("slower");
    }

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    LayerAnimationWindow frame = new LayerAnimationWindow();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
