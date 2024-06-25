package edu.wisc.ssec.mcidasv.ui;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JWindow;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PopupMessage extends JFrame {

    /** Logger for debugging*/
    private static final Logger logger = LoggerFactory.getLogger(PopupMessage.class);

    /** Window used to create the toast */
    private JWindow w = null;

    /** Date Refresh Spec */
    private final String DATA_REFRESH= "DataRefresh";

    /** Example Spec */
    private final String EXAMPLE_REFRESH = "ExampleSpec";


    /**
     * Creates a toast with the given message at the specific point on the screen
     * (x, y) with the given specification; there is no default
     * @param s message string
     * @param x screen position x
     * @param y screen position y
     * @param spec toast specification
     */

    public PopupMessage(String s, int x, int y, String spec) {
        w = new JWindow();
        w.setAlwaysOnTop(true);
        w.setBackground(new Color(0, 0, 0, 0));


        JPanel p = new JPanel() {
            public void paintComponent(Graphics g) {
                if (spec.equals(DATA_REFRESH)) {
                    int wid = g.getFontMetrics().stringWidth(s) * 2;
                    int hei = g.getFontMetrics().getHeight() * 2;

                    g.setColor(Color.WHITE);
                    g.fillRect(10, 10, wid + 30, hei + 10);
                    g.setColor(Color.BLACK);
                    g.drawRect(10, 10, wid + 30, hei + 10);

                    g.setColor(Color.BLACK);
                    Font McvFont = new Font("Arial", Font.PLAIN, 14);
                    g.setFont(McvFont);
                    g.drawString(s, (wid / 4) + 25, hei + 3);
                    g.setColor(Color.GRAY);
                    McvFont = new Font("Arial", Font.PLAIN, 10);
                    g.setFont(McvFont);
                    g.drawString("McIDAS-V", 15, 22);

                    int t = 250;
                    for (int i = 0; i < 4; i++) {
                        t -= 60;
                        g.setColor(new Color(0, 0, 0, t));
                        g.drawRect(10 - i, 10 - i, wid + 30 + i * 2,
                                hei + 10 + i * 2);
                    }

                } else if (spec.equals(EXAMPLE_REFRESH)) {
                    // TODO: if other specifications for toasts are needed, add them as branches in this if/else statement
                    // one size WILL NOT fit all

                    int wid = g.getFontMetrics().stringWidth("Example Toast") * 2;
                    int hei = g.getFontMetrics().getHeight() * 2;

                    // Font style, color, and size for the main message
                    g.setColor(Color.BLACK);
                    Font McvFont = new Font("Arial", Font.PLAIN, 14);
                    g.setFont(McvFont);

                    // Main message text
                    g.drawString("Example Toast", (wid / 4) + 25, hei + 3);

                    // Font style, color, and size for the McIDAS-V header in the top left
                    g.setColor(Color.GRAY);
                    McvFont = new Font("Arial", Font.PLAIN, 10);
                    g.setFont(McvFont);
                    g.drawString("McIDAS-V", 15, 22);

                    int t = 250;
                    for (int i = 0; i < 4; i++) {
                        t -= 60;
                        g.setColor(new Color(0, 0, 0, t));
                        g.drawRect(10 - i, 10 - i, wid + 30 + i * 2,
                                hei + 10 + i * 2);
                    }

                } else {
                    logger.error("Toast spec is not valid");
                    // If a valid toast spec is not specified, nothing will be made or displayed
                    return;
                }
            }
        };

        w.add(p);
        w.setLocation(x, y);

        // TODO: set the dimensions as tightly as possible if you want to avoid a grey background
        if (spec.equals(DATA_REFRESH)) {
            w.setSize(220, 65);
        } else if (spec.equals(EXAMPLE_REFRESH)) {
            w.setSize(220, 65);
        } else {
            logger.error("Notification spec is not valid");
        }
    }

    /**
     * Displays the toast
     * @param timeAlive time the toast remains visible
     */

    public void showPopupMessage(Integer timeAlive) {
        try {
            w.setOpacity(1);
            w.setVisible(true);

            Thread.sleep(timeAlive);

            for (double d = 1.0; d > 0.2; d -= 0.05) {
                Thread.sleep(50);
                w.setOpacity((float)d);
            }
            w.setVisible(false);
        } catch (Exception e) { logger.error(e.getMessage());}
    }
}