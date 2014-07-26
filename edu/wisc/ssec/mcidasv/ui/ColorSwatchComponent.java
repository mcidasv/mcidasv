package edu.wisc.ssec.mcidasv.ui;

import org.jdesktop.beans.AbstractBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.idv.IdvObjectStore;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LayoutUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlObjectStore;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This is largely the same as {@link ucar.unidata.util.GuiUtils.ColorSwatch},
 * but it remembers the user's recently selected colors.
 */
public class ColorSwatchComponent extends JPanel implements PropertyChangeListener {

//    ColorTracker tracker;

    /** flag for alpha */
    boolean doAlpha = false;

    /** color of the swatch */
    Color color;

    /** clear button */
    JButton clearBtn;

    /** set button */
    JButton setBtn;

    /** label */
    String label;

    private XmlObjectStore store;

    /**
     * Create a new ColorSwatch for the specified color
     *
     * @param c  Color
     * @param dialogLabel  label for the dialog
     */
    public ColorSwatchComponent(XmlObjectStore store, Color c, String dialogLabel) {
        this(store, c, dialogLabel, false);
    }

    /**
     * Create a new color swatch
     *
     * @param c   the color
     * @param dialogLabel label for the dialog
     * @param alphaOk  use alpha?
     */
    public ColorSwatchComponent(XmlObjectStore store, Color c, String dialogLabel, boolean alphaOk) {
        this.doAlpha = alphaOk;
        this.color   = c;
        this.label   = dialogLabel;
        this.store   = store;
        setMinimumSize(new Dimension(40, 10));
        setPreferredSize(new Dimension(40, 10));
        setToolTipText("Click to change color");
        setBackground(color);
        setBorder(BorderFactory.createLoweredBevelBorder());

        clearBtn = new JButton("Clear");
        clearBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                ColorSwatchComponent.this.setBackground(null);
            }
        });

        setBtn = new JButton("Set");
        setBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setColorFromChooser();
            }
        });


        this.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                Misc.run(new Runnable() {
                    public void run() {
                        showColorChooser();
                    }
                });
            }
        });
    }


    /**
     * Show the color chooser
     */
    private void showColorChooser() {
        PersistableSwatchChooserPanel.ColorTracker tracker = new PersistableSwatchChooserPanel.ColorTracker();
        tracker.addPropertyChangeListener("colors", this);
        Color         oldColor    = this.getBackground();
        int           alpha       = oldColor.getAlpha();
//        JColorChooser chooser     = new JColorChooser(oldColor);
        JColorChooser chooser = createChooser(tracker);
        JSlider alphaSlider = new JSlider(0, 255, alpha);


        JComponent contents;
        if (doAlpha) {
            contents =
                LayoutUtil.centerBottom(chooser,
                    LayoutUtil.inset(LayoutUtil.hbox(new JLabel("Transparency:"),
                        alphaSlider), new Insets(5, 5, 5,
                        5)));
        } else {
            contents = chooser;
        }
        if ( !GuiUtils.showOkCancelDialog(null, label, contents, null)) {
            return;
        }
        alpha = alphaSlider.getValue();
        //                    Color newColor = JColorChooser.showDialog(null, label,
        //                                                              oldColor);
        Color newColor = chooser.getColor();
        if (newColor != null) {
            newColor = new Color(newColor.getRed(), newColor.getGreen(),
                newColor.getBlue(), alpha);
            ColorSwatchComponent.this.userSelectedNewColor(newColor);
        }
    }

    private JColorChooser createChooser(PersistableSwatchChooserPanel.ColorTracker tracker) {
        JColorChooser chooser = new JColorChooser();
        List<AbstractColorChooserPanel> choosers =
            new ArrayList<>(Arrays.asList(chooser.getChooserPanels()));
        choosers.remove(0);
        PersistableSwatchChooserPanel swatch = new PersistableSwatchChooserPanel();
        List<Color> savedColors = (List<Color>)store.get(PROP_RECENT_COLORS);
//        if (savedColors != null) {
//            savedColors = new ArrayList<>();
//        }
        tracker.setColors(savedColors);
        swatch.setColorTracker(tracker);

//        swatch.setAction(doubleClickAction);
        choosers.add(0, swatch);
        chooser.setChooserPanels(choosers.toArray(new AbstractColorChooserPanel[0]));
        swatch.updateRecentSwatchPanel();
        return chooser;
    }

//    private static final Logger logger = LoggerFactory.getLogger(ColorSwatchComponent.class);

    public static final String PROP_RECENT_COLORS = "mcidasv.colorchooser.recentcolors";

    public void propertyChange(PropertyChangeEvent evt) {
        store.put(PROP_RECENT_COLORS, evt.getNewValue());
//        logger.trace("old='{}' new='{}", evt.getOldValue(), evt.getNewValue());
    }

    /**
     * Set color from chooser
     */
    private void setColorFromChooser() {
        Color newColor = JColorChooser.showDialog(null, label,
            this.getBackground());
        if (newColor != null) {
            ColorSwatchComponent.this.userSelectedNewColor(newColor);
        }
    }

    /**
     * Get the set button
     *
     * @return the set button
     */
    public JButton getSetButton() {
        return setBtn;
    }

    /**
     * Get the clear button
     *
     * @return the clear button
     */
    public JButton getClearButton() {
        return clearBtn;
    }

    /**
     * Get the Color of the swatch
     *
     * @return the swatch color
     */
    public Color getSwatchColor() {
        return color;
    }

    /**
     * the user chose a new color. Set the background. THis can be overwritted by client code to act on the color change
     *
     * @param c color
     */
    public void userSelectedNewColor(Color c) {
        setBackground(c);
    }

    /**
     * Set the background to the color
     *
     * @param c  Color for background
     */
    public void setBackground(Color c) {
        color = c;
        super.setBackground(c);
    }

    /**
     * Paint this swatch
     *
     * @param g  graphics
     */
    public void paint(Graphics g) {
        Rectangle b = getBounds();
        if (color != null) {
            g.setColor(Color.black);
            for (int x = 0; x < b.width; x += 4) {
                g.fillRect(x, 0, 2, b.height);
            }
        }

        super.paint(g);
        if (color == null) {
            g.setColor(Color.black);
            g.drawLine(0, 0, b.width, b.height);
            g.drawLine(b.width, 0, 0, b.height);
        }
    }

    /**
     * Get the panel
     *
     * @return the panel
     */
    public JComponent getPanel() {
        return GuiUtils.hbox(this, clearBtn, 4);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Color getColor() {
        return color;
    }


    /**
     * Get the panel that shows the swatch and the Set button.
     *
     * @return the panel
     */
    public JComponent getSetPanel() {
        List comps    = Misc.newList(this);
        JButton popupBtn = new JButton("Change");
        popupBtn.addActionListener(GuiUtils.makeActionListener(ColorSwatchComponent.this,
            "popupNameMenu", popupBtn));
        comps.add(popupBtn);
        return GuiUtils.hbox(comps, 4);
    }


    /**
     * Popup the named list menu
     *
     * @param popupBtn Popup near this button
     */
    public void popupNameMenu(JButton popupBtn) {
        List items = new ArrayList();
        for (int i = 0; i < GuiUtils.COLORNAMES.length; i++) {
            items.add(GuiUtils.makeMenuItem(GuiUtils.COLORNAMES[i], this, "setColorName",
                GuiUtils.COLORNAMES[i]));
        }
        items.add(GuiUtils.MENU_SEPARATOR);
        items.add(GuiUtils.makeMenuItem("Custom", this, "setColorName", "custom"));
        GuiUtils.showPopupMenu(items, popupBtn);
    }

    /**
     * Set the color based on name
     *
     * @param name color name
     */
    public void setColorName(String name) {
        if (name.equals("custom")) {
            setColorFromChooser();
            return;
        }
        Color newColor = GuiUtils.decodeColor(name, getBackground());
        if (newColor != null) {
            ColorSwatchComponent.this.setBackground(newColor);
        }
    }

//    class MainSwatchListener extends MouseAdapter implements Serializable {
//        @Override
//        public void mousePressed(MouseEvent e) {
//            if (!isEnabled())
//                return;
//            if (e.getClickCount() == 2) {
//                handleDoubleClick(e);
//                return;
//            }
//
//            Color color = ColorSwatchComponent.this.getColorForLocation(e.getX(), e.getY());
//            ColorSwatchComponent.this,setSelectedColor(color);
//            if (tracker != null) {
//                tracker.addColor(color);
//            } else {
//                recentSwatchPanel.setMostRecentColor(color);
//            }
//        }
//
//        /**
//         * @param e
//         */
//        private void handleDoubleClick(MouseEvent e) {
//            if (action != null) {
//                action.actionPerformed(null);
//            }
//        }
//    };


}
