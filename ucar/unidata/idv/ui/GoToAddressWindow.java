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
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.idv.MapViewManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LayoutUtil;
import ucar.unidata.util.Msg;
import ucar.unidata.xml.XmlObjectStore;
import ucar.visad.GeoUtils;

import visad.RealTupleType;
import visad.VisADException;
import visad.georef.LatLonPoint;
import visad.georef.TrivialMapProjection;


public class GoToAddressWindow extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(GoToAddressWindow.class);

    /** {@link #addressComboBox} tool tip. */
    public static final String ADDRESS_TOOL_TIP = "<html>Examples:<br>12345 Oak Street, My Town, My State<br>Or: My Town, My State<br>Or: 80303 (zip code)<br>Or: latitude longitude<br>Or: \"ip\" for the location of this computer</html>";

    /** {@link #addressComboBox} label. */
    public static final String ADDRESS_LABEL = "Address: ";

    /** Label used by the {@literal "Apply"} button. */
    public static final String APPLY_LABEL = "Apply";

    /** Label used by the {@literal "OK"} button. */
    public static final String OK_LABEL = "OK";

    /** Label used by the {@literal "Cancel"} button. */
    public static final String CANCEL_LABEL = "Cancel";

    /** Label used by {@link #reprojectCheckBox}. */
    public static final String REPROJECT_LABEL = "Reproject";

    /** {@link #reprojectCheckBox} tool tip. */
    public static final String REPROJECT_TOOL_TIP = "When checked make a simple map projection over the location.";

    /** Title of the window. */
    public static final String WINDOW_TITLE = "Go To Address";

    /** {@link MapViewManager} that created this instance. */
    private final MapViewManager viewManager;

    /** The application's {@literal "object store"}. */
    private final XmlObjectStore store;

    /**
     * {@link JCheckBox} that controls whether or not the {@link #viewManager}
     * will be {@literal "reprojected"} using a given address.
     */
    private JCheckBox reprojectCheckBox;

    /**
     * {@link JComboBox} that contains the last <i>valid</i> addresses that
     * the user has supplied.
     */
    private JComboBox addressComboBox;

    /**
     * Initializes the {@literal "Go To Address"} window. Client code will need
     * to call {@link #setVisible(boolean)} explicity. Please be aware that
     * this should only ever be called from Swing's
     * {@literal "Event Dispatch Thread"}.
     *
     * @param viewManager Cannot be {@code null}.
     * @param store Cannot be {@code null}.
     */
    public GoToAddressWindow(MapViewManager viewManager, XmlObjectStore store) {
        this.viewManager = viewManager;
        this.store = store;
        initComponents();
    }

    /**
     * Initialize the various GUI components.
     */
    private void initComponents() {
        setTitle(WINDOW_TITLE);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                handleCloseEvent();
            }
        });

        JButton applyButton = new JButton(APPLY_LABEL);
        applyButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                handleApplyEvent();
            }
        });

        JButton okButton = new JButton(OK_LABEL);
        okButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                handleOkEvent();
            }
        });

        JButton cancelButton = new JButton(CANCEL_LABEL);
        cancelButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                handleCancelEvent();
            }
        });

        reprojectCheckBox = new JCheckBox(REPROJECT_LABEL);
        reprojectCheckBox.setToolTipText(REPROJECT_TOOL_TIP);
        reprojectCheckBox.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                handleReprojectEvent();
            }
        });

        if (store != null) {
            List<String> savedAddresses =
                (List<String>)store.get(MapViewManager.PREF_ADDRESS_LIST);
            if (savedAddresses != null) {
                GeoUtils.setSavedAddresses(savedAddresses);
            }

            boolean reproject =
                store.get(MapViewManager.PREF_ADDRESS_REPROJECT, true);
            reprojectCheckBox.setSelected(reproject);
        }

        addressComboBox =
            new JComboBox(new Vector<String>(GeoUtils.getSavedAddresses()));
        addressComboBox.setToolTipText(ADDRESS_TOOL_TIP);
        addressComboBox.setEditable(true);
        addressComboBox.getEditor().addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                handleEditEvent();
            }
        });

        JComponent contents = GuiUtils.label(ADDRESS_LABEL, addressComboBox);
        contents = LayoutUtil.inset(contents, 5);
        if (!viewManager.getUseGlobeDisplay()) {
            contents = LayoutUtil.vbox(contents, reprojectCheckBox);
            contents = LayoutUtil.inset(contents, 5);
        }

        List<JButton> buttons = new ArrayList<JButton>(3);
        buttons.add(applyButton);
        buttons.add(okButton);
        buttons.add(cancelButton);

        JPanel tempPanel = new JPanel();
        tempPanel.setLayout(new BoxLayout(tempPanel, BoxLayout.X_AXIS));

        JPanel buttonPanel =
            LayoutUtil.doLayout(tempPanel, LayoutUtil.getComponentArray(buttons), buttons.size(), GuiUtils.WT_N, GuiUtils.WT_N, null, null, new Insets(5, 5, 5, 5));
        JPanel contentPane = LayoutUtil.centerBottom(contents, buttonPanel);
        setContentPane(contentPane);
        Msg.translateTree(this);
        pack();
    }

    /**
     * Responds to the user closing the window. Currently a no-op.
     */
    public void handleCloseEvent() {
        logger.trace("closing");
    }

    /**
     * Responds to the user clicking the {@literal "Apply"} button.
     */
    public void handleApplyEvent() {
        logger.trace("applying");
        String address = getAddressBoxContents();
        if (address != null) {
            LatLonPoint llp =
                GeoUtils.getLocationFromAddress(address, null);
            if (llp != null) {
                List<String> addresses = GeoUtils.getSavedAddresses();
                GuiUtils.setListData(addressComboBox, addresses);
                changeProjection(llp);
            } else {
                logger.trace("could not locate '{}'", address);
            }
        }
    }

    /**
     * Responds to the user clicking the {@literal "OK"} button. If the user
     * has entered an address that can be geolocated, this window is closed,
     * the address list is persisted, and
     * {@link #changeProjection(visad.georef.LatLonPoint)} is called.
     */
    public void handleOkEvent() {
        logger.trace("okay");
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

    /**
     * Responds to the user clicking the {@literal "Cancel"} button. The window
     * is simply closed (without changing projection or persisting).
     */
    public void handleCancelEvent() {
        logger.trace("canceling");
        closeWindow(false);
    }

    /**
     * Responds to the user selecting or deselecting
     * {@link #reprojectCheckBox}. The status of {@code reprojectCheckBox} is
     * then persisted.
     */
    public void handleReprojectEvent() {
        logger.trace("reproject status={}", reprojectCheckBox.isSelected());
        store.put(MapViewManager.PREF_ADDRESS_REPROJECT, reprojectCheckBox.isSelected());
    }

    /**
     * Responds to the user hitting the {@literal "enter/return"} key while
     * {@link #addressComboBox} has focus. This is handled just like clicking
     * the {@literal "OK"} button.
     *
     * @see #handleOkEvent()
     */
    public void handleEditEvent() {
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

    /**
     * Closes this window by calling {@link #dispose()}.
     *
     * @param persist if {@code true}, the contents of {@link #addressComboBox}
     * will be persisted by the application.
     */
    public void closeWindow(boolean persist) {
        if (persist) {
            logger.trace("hrm: {}", GeoUtils.getSavedAddresses());
            store.put(MapViewManager.PREF_ADDRESS_LIST, GeoUtils.getSavedAddresses());
        }
        if (isDisplayable()) {
            dispose();
        }
    }

    /**
     * Change the {@literal "location"} of {@link #viewManager} to
     * {@code location}.
     *
     * @param location Location of the new projection. Cannot be {@code null}.
     */
    private void changeProjection(LatLonPoint location) {
        float x = (float)location.getLongitude().getValue();
        float y = (float)location.getLatitude().getValue();
        float offset = (float)(1.0 / 60.0f);
        Rectangle2D.Float rect =
            new Rectangle2D.Float(x - offset, y - offset, offset * 2, offset * 2);
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

    /**
     * Utility method used to extract the currently-selected item from
     * {@link #addressComboBox}.
     *
     * @return Either the currently selected address or {@code null}.
     */
    private String getAddressBoxContents() {
        String address = null;
        if (addressComboBox != null) {
            address = (String)addressComboBox.getSelectedItem();
            if (address == null) {
                address = (String)addressComboBox.getEditor().getItem();
            }
        }
        logger.trace("returning address={}", address);
        return address;
    }
}
