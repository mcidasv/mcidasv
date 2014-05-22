package ucar.unidata.idv.ui;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.idv.MapViewManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Msg;
import ucar.unidata.xml.XmlObjectStore;
import ucar.visad.GeoUtils;

import visad.RealTupleType;
import visad.VisADException;
import visad.georef.LatLonPoint;
import visad.georef.TrivialMapProjection;

public class GoToAddressWindow extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(GoToAddressWindow.class);

    private final MapViewManager viewManager;
    private final XmlObjectStore store;

    private JButton applyButton;
    private JButton okButton;
    private JButton cancelButton;
    private JCheckBox reprojectCheckBox;
    private JComboBox addressComboBox;


    public GoToAddressWindow() {
        this.viewManager = null;
        this.store = null;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                initComponents();
            }
        });
    }

    public GoToAddressWindow(MapViewManager viewManager, XmlObjectStore store) {
        this.viewManager = viewManager;
        this.store = store;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                initComponents();
            }
        });
    }

    public void initComponents() {
        setTitle("Go To Address");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                handleCloseEvent(e);
            }
        });

        applyButton = new JButton("Apply");
        applyButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                handleApplyEvent(e);
            }
        });

        okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                handleOkEvent(e);
            }
        });

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                handleCancelEvent(e);
            }
        });

        reprojectCheckBox = new JCheckBox("Reproject");
        reprojectCheckBox.setToolTipText("When checked make a simple map projection over the location.");
        reprojectCheckBox.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                handleReprojectEvent(e);
            }
        });

        if (store != null) {
            List<String> savedAddresses = (List<String>)store.get(MapViewManager.PREF_ADDRESS_LIST);
            if (savedAddresses != null) {
                GeoUtils.setSavedAddresses(savedAddresses);
            }

            boolean reproject = store.get(MapViewManager.PREF_ADDRESS_REPROJECT, true);
            reprojectCheckBox.setSelected(reproject);
        }

        addressComboBox = new JComboBox(new Vector<String>(GeoUtils.getSavedAddresses()));
        addressComboBox.setEditable(true);
        addressComboBox.getEditor().addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                handleEditEvent(e);
            }
        });

        JComponent contents = GuiUtils.label("Address: ", addressComboBox);
        contents = GuiUtils.inset(contents, 5);
        if (!viewManager.getUseGlobeDisplay()) {
            contents = GuiUtils.vbox(contents, reprojectCheckBox);
            contents = GuiUtils.inset(contents, 5);
        }

        List<JButton> buttons = new ArrayList<JButton>(3);
        buttons.add(applyButton);
        buttons.add(okButton);
        buttons.add(cancelButton);

        JPanel tempPanel = new JPanel();
        tempPanel.setLayout(new BoxLayout(tempPanel, BoxLayout.X_AXIS));

        JPanel buttonPanel = GuiUtils.doLayout(tempPanel, GuiUtils.getComponentArray(buttons), buttons.size(), GuiUtils.WT_N, GuiUtils.WT_N, null, null, new Insets(5, 5, 5, 5));
        JPanel contentPane = GuiUtils.centerBottom(contents, buttonPanel);
        setContentPane(contentPane);
        Msg.translateTree(this);
        pack();
    }

    public void handleCloseEvent(WindowEvent event) {
        logger.trace("closing");
    }

    public void handleApplyEvent(ActionEvent event) {
        logger.trace("applying");
        if (addressComboBox != null) {
            String address = getAddressBoxContents();
            if (address != null) {
                LatLonPoint llp = GeoUtils.getLocationFromAddress(address, null);
                if (llp != null) {
                    logger.trace("ugh: {}", GeoUtils.getSavedAddresses());
                    GuiUtils.setListData(addressComboBox, GeoUtils.getSavedAddresses());
                    changeProjection(llp);
                } else {
                    logger.trace("could not locate '{}'", address);
                }
            }
        }
    }

    public void handleOkEvent(ActionEvent event) {
        logger.trace("okay");
        if (addressComboBox != null) {
            String address = getAddressBoxContents();
            if (address != null) {
                LatLonPoint llp = GeoUtils.getLocationFromAddress(address, null);
                if (llp != null) {
                    closeWindow(true);
                    changeProjection(llp);
                } else {
                    logger.trace("could not locate '{}'", address);
                }
            }
        }
    }

    public void handleCancelEvent(ActionEvent event) {
        logger.trace("canceling");
        closeWindow(false);
    }

    public void handleReprojectEvent(ActionEvent event) {
        if (reprojectCheckBox != null) {
            logger.trace("reproject status={}", reprojectCheckBox.isSelected());
            store.put(MapViewManager.PREF_ADDRESS_REPROJECT, reprojectCheckBox.isSelected());
        } else {
            logger.trace("reproject null checkbox!!");
        }
    }

    public void handleEditEvent(ActionEvent event) {
        if (addressComboBox != null) {
            logger.trace("edit editorItem={} selectedItem={}", addressComboBox.getEditor().getItem(), addressComboBox.getSelectedItem());
        } else {
            logger.trace("addressComboBox null!!");
        }
    }

    public void closeWindow(boolean persist) {
        if (persist) {
            logger.trace("hrm: {}", GeoUtils.getSavedAddresses());
            store.put(MapViewManager.PREF_ADDRESS_LIST, GeoUtils.getSavedAddresses());
        }
        if (isDisplayable()) {
            dispose();
        }
    }

    private void changeProjection(LatLonPoint location) {
        float x = (float)location.getLongitude().getValue();
        float y = (float)location.getLatitude().getValue();
        float offset = (float)(1.0 / 60.0f);
        Rectangle2D.Float rect = new Rectangle2D.Float(x - offset, y - offset, offset * 2, offset * 2);
        try {
            if (!viewManager.getUseGlobeDisplay() && reprojectCheckBox != null && reprojectCheckBox.isSelected()) {
                TrivialMapProjection mp =
                    new TrivialMapProjection(
                        RealTupleType.SpatialEarth2DTuple, rect);
                viewManager.setMapProjection(mp, true);
            } else {
                viewManager.getMapDisplay().center(GeoUtils.toEarthLocation(location), viewManager.shouldAnimateViewChanges());
            }
        } catch (VisADException e) {
            logger.error("Problem changing projection", e);
        } catch (RemoteException e) {
            logger.error("RMI Exception while changing projection", e);
        }
    }


    private String getAddressBoxContents() {
        String address = (String)addressComboBox.getSelectedItem();
        if (address == null) {
            address = (String)addressComboBox.getEditor().getItem();
        }
        logger.trace("returning address={}", address);
        return address;
    }

    public static void main(String... args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    GoToAddressWindow frame = new GoToAddressWindow();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
