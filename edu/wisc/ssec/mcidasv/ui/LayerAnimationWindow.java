package edu.wisc.ssec.mcidasv.ui;

import java.awt.EventQueue;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import net.miginfocom.swing.MigLayout;
import javax.swing.JTextField;
import javax.swing.JLabel;

public class LayerAnimationWindow extends JFrame {

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
        btnSlower = new JButton("Slower");
        btnFaster = new JButton("Faster");
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
