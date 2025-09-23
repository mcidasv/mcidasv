/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package ucar.unidata.idv.control;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.vecmath.Point3d;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.control.chart.LineState;
import ucar.unidata.idv.control.chart.VerticalProfileChart;
import ucar.unidata.ui.FileManager;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Range;

import ucar.visad.display.Animation;
import ucar.visad.quantities.CommonUnits;
import visad.CommonUnit;
import visad.CoordinateSystem;
import visad.Data;
import visad.ErrorEstimate;
import visad.FieldImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded1DSet;
import visad.MathType;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.Set;
import visad.SetType;
import visad.TupleType;
import visad.Unit;
import visad.VisADException;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

/**
 * Given a VisAD Field, make a 2D plot of the range data against
 * one of the 3 domain coordinates.
 *
 * @author IDV Development Team
 * @version $Revision: 1.16 $Date: 2007/07/24 15:59:26 $
 */

public class VerticalProfileControl extends LineProbeControl {
    
    private static final Logger logger =
        LoggerFactory.getLogger(VerticalProfileControl.class);
    
    /** Column name property */
    public static final int COL_NAME = 0;

    /** Column sampling property */
    public static final int COL_SAMPLING = 1;

    /** number of columns */
    public static final int NUM_COLS = 2;

    /** list of infos */
    private List infos = new ArrayList();

    /** profile sharing property */
    public static final String SHARE_PROFILE =
        "VerticalProfileControl.SHARE_PROFILE";

    /** labels for sampling selections */
    private String[] samplingLabels = { WEIGHTED_AVERAGE, NEAREST_NEIGHBOR };

    /** display chart */
    private VerticalProfileChart chart;

    /** table for output */
    private JTable paramsTable;

    /** Holds the table */
    private JComponent tablePanel;

    /** table model */
    private AbstractTableModel tableModel;

    /** The latlon widget */
    private LatLonWidget latLonWidget;

    /** The animation widget */
    private JComponent aniWidget;

    /** Show the jtable */
    private boolean showTable = true;

    /** _more_ */
    private Unit altUnit = CommonUnit.meter;

    private String altCoord = "PLACEHOLDER";

    /**
     * Default constructor; set attribute flags
     */
    public VerticalProfileControl() {
        setAttributeFlags(FLAG_COLOR | FLAG_DATACONTROL | FLAG_DISPLAYUNIT);
    }

    /**
     * Construct the vertical profile display and control buttons
     *
     * @param dataChoice   data description
     *
     * @return  true if successful
     *
     * @throws VisADException  couldn't create a VisAD object needed
     * @throws RemoteException  couldn't create a remote object needed
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        return init(Misc.newList(dataChoice));
    }


    /**
     * Construct the vertical profile display and control buttons
     *
     * @param choices  data choices
     *
     * @return  true if successful
     *
     * @throws VisADException  couldn't create a VisAD object needed
     * @throws RemoteException  couldn't create a remote object needed
     */
    public boolean init(List choices) throws VisADException, RemoteException {

        ActionListener llListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                handleLatLonWidgetChange();
            }
        };
        latLonWidget = new LatLonWidget("Lat: ", "Lon: ", llListener);
        aniWidget    = getAnimationWidget().getContents(false);
        setContents(doMakeContents());
        doMakeProbe();
        return true;
    }

    /**
     * Called after init().  Load profile into display.
     */
    public void initDone() {
        super.initDone();
        try {
            setTimesForAnimation();
            //loadProfile(getPosition());
            if(getDataChoices().get(0).toString().startsWith("Conserved Sounding")){
                ((DataChoice)(getDataChoices().get(0))).setName("theta");
                ((DataChoice)(getDataChoices().get(0))).setId("theta");
                doMoveProbe();

            List cdcs =
                        ((DerivedDataChoice) getDataChoices().get(0)).getChoices();
                DataChoice dc1 = (DataChoice)cdcs.get(1);
                addNewData(Misc.newList(dc1));
                DataChoice dc2 = (DataChoice)cdcs.get(2);
                addNewData(Misc.newList(dc2));
            } else {
                doMoveProbe();
            }
        } catch (Exception exc) {
            logException("initDone", exc);
        }
        // Enable table panel after UI is initialized
        tablePanel.setVisible(showTable);
    }

    /**
     * Disable progressive resolution for this control.
     *
     * @return Always {@code false}.
     */
    @Override protected boolean canDoProgressiveResolution() {
        return false;
    }

    /**
     * Assume that any display controls that have a color table widget
     * will want the color table to show up in the legend.
     *
     * @param  legendType  type of legend
     * @return The extra JComponent to use in legend
     */
    protected JComponent getExtraLegendComponent(int legendType) {
        JComponent parentComp = super.getExtraLegendComponent(legendType);
        if (legendType == BOTTOM_LEGEND) {
            return parentComp;
        }
        return GuiUtils.vbox(parentComp, getChart().getThumb());
    }


    /**
     * Respond to a timeChange event
     *
     * @param time new time
     */
    protected void timeChanged(Real time) {
        try {
            getChart().timeChanged(time);
        } catch (Exception exc) {
            logException("changePosition", exc);
        }
        super.timeChanged(time);
    }

    /**
     * Make the UI contents for this control.
     *
     * @return  container for UI
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {

        if (chart != null) {
            chart.setControl(this);
        }

        tableModel = new AbstractTableModel() {

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if (columnIndex == COL_SAMPLING) {
                    return true;
                }
                return false;
            }

            public int getRowCount() {
                List dataChoices = getDataChoices();
                if (dataChoices == null) {
                    return 0;
                }
                return dataChoices.size();
            }

            public int getColumnCount() {
                return NUM_COLS;
            }

            public void setValueAt(Object aValue, int rowIndex,
                                   int columnIndex) {
                if (columnIndex == COL_SAMPLING) {
                    getVPInfo(rowIndex).setSamplingMode(
                        getSamplingModeValue(aValue.toString()));
                    doMoveProbe();
                    return;
                }
            }

            public Object getValueAt(int row, int column) {
                if (column == COL_NAME) {
                    return getFieldName(row);
                }
                if (column == COL_SAMPLING) {
                    return getSamplingModeName(
                        getVPInfo(row).getSamplingMode());
                }
                return "";
            }

            public String getColumnName(int column) {

                switch (column) {

                  case COL_NAME :
                      return "Parameter";

                  case COL_SAMPLING :
                      return "Sampling";
                }

                return "";
            }
        };



        paramsTable = new JTable(tableModel);

        paramsTable.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {

                if ( !SwingUtilities.isRightMouseButton(e)) {
                    return;
                }
                final int row = paramsTable.rowAtPoint(e.getPoint());
                if ((row < 0) || (row >= getDataChoices().size())) {
                    return;
                }

                List       choices   = getDataChoices();
                JPopupMenu popupMenu = new JPopupMenu();
                JMenuItem  jmi       = doMakeChangeParameterMenuItem();
                popupMenu.add(jmi);
                popupMenu.addSeparator();

                List items = getParameterMenuItems(row);
                GuiUtils.makePopupMenu(popupMenu, items);

                // Display choices
                JMenu dataChoiceMenu =
                    getControlContext().doMakeDataChoiceMenu(
                        getDataChoiceAtRow(row));
                popupMenu.add(dataChoiceMenu);
                popupMenu.show(paramsTable, e.getX(), e.getY());

            }

        });
        paramsTable.setToolTipText("Right click to edit");

        JScrollPane scrollPane = new JScrollPane(paramsTable);

        paramsTable.getColumnModel().getColumn(COL_SAMPLING).setCellEditor(
            new SamplingEditor());

        scrollPane.setPreferredSize(new Dimension(200, 100));
        tablePanel = new JPanel();
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
        tablePanel.add(scrollPane);
        // TJJ - don't enable until UI is completely initialized
        tablePanel.setVisible(false);
        JComponent bottomPanel = GuiUtils.leftRight(aniWidget, latLonWidget);
        bottomPanel = GuiUtils.inset(bottomPanel, 5);
        JComponent bottom = GuiUtils.centerBottom(tablePanel, bottomPanel);

        return GuiUtils.centerBottom(getChart().getContents(), bottom);

    }


    /**
     * Override base class method which is called when the user has selected
     * new data choices.
     *
     * @param newChoices    new list of choices
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void processNewData(List newChoices)
            throws VisADException, RemoteException {
        List newInfos = new ArrayList();
        showWaitCursor();
        for (int i = 0; i < newChoices.size(); i++) {
            VerticalProfileInfo info = new VerticalProfileInfo(this);
            newInfos.add(info);
            DataChoice dc = (DataChoice) newChoices.get(i);
            info.setDataInstance(new GridDataInstance(dc, getDataSelection(),
                    getRequestProperties()));

            initializeVerticalProfileInfo(info);
        }
        showNormalCursor();
        appendDataChoices(newChoices);
        infos.addAll(newInfos);
        resetData();
    }

    /**
     * Override base class method to just trigger a redisplay of the data.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void resetData() throws VisADException, RemoteException {
        updateLegendLabel();
        setTimesForAnimation();
        doMoveProbe();
        fireStructureChanged();
    }

    /**
     * Method called when probe is moved.
     */
    protected void doMoveProbe() {
        super.doMoveProbe();
        //        getChart().updateThumb();
    }


    /**
     * Override base class method which is called when the user has selected
     * new data choices.
     *
     * @param newChoices  new list of choices
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void addNewData(List newChoices)
            throws VisADException, RemoteException {
        processNewData(newChoices);
        doShare(SHARE_CHOICES, newChoices);
    }

    /**
     * Handle the user pressing return
     */
    private void handleLatLonWidgetChange() {
        try {
            double   lat = latLonWidget.getLat();
            double   lon = latLonWidget.getLon();
            double[] xyz = earthToBox(makeEarthLocation(lat, lon, 0));
            setProbePosition(xyz[0], xyz[1]);

        } catch (Exception exc) {
            logException("Error setting lat/lon", exc);
        }

    }

    /**
     * Get the DataChoice associated with the parameter at a particular
     * row.
     *
     * @param row    row index
     * @return   the associated DataChoice
     */
    public DataChoice getDataChoiceAtRow(int row) {
        List choices = getDataChoices();
        if (row >= choices.size()) {
            return null;
        }
        return (DataChoice) choices.get(row);
    }

    /**
     * Create and initialize a new ProbeRowInfo if needed. Return it.
     *
     * @param row The row
     *
     * @return The info
     */
    private VerticalProfileInfo getVPInfo(int row) {
        while (row >= infos.size()) {
            VerticalProfileInfo info = new VerticalProfileInfo(this);
            infos.add(info);
        }
        VerticalProfileInfo info = (VerticalProfileInfo) infos.get(row);
        if (info.getDataInstance() == null) {  // first time through
            List choices = getDataChoices();
            try {
                DataChoice dc = (DataChoice) choices.get(row);
                showWaitCursor();
                info.setDataInstance(new GridDataInstance(dc,
                        getDataSelection(), getRequestProperties()));
                showNormalCursor();
                initializeVerticalProfileInfo(info);
            } catch (VisADException exc) {}
            catch (RemoteException exc) {}
        }
        return info;
    }


    /**
     * initialize
     *
     * @param info info to initialize from
     *
     * @throws RemoteException on badness
     * @throws VisADException on badness
     */
    private void initializeVerticalProfileInfo(VerticalProfileInfo info)
            throws VisADException, RemoteException {
        Unit   vpUnit = info.getUnit();
        String name   = info.getDataInstance().getParamName();
        if (vpUnit == null) {  // is null or hasn't been set
            vpUnit = getDisplayConventions().selectDisplayUnit(name,
                    info.getDataInstance().getRawUnit(0));
        }
        info.setUnit(vpUnit);
        if (altCoord.equals(PRESSURE_COORD)) {
            altUnit = CommonUnits.HECTOPASCAL;
        } else {
            altUnit = CommonUnit.meter;
        }
        info.setAltitudeUnit(altUnit);
        Range vpRange = info.getLineState().getRange();

        if (vpRange == null) {
            vpRange = getDisplayConventions().getParamRange(name, vpUnit);
            if (vpRange == null) {
                //            range = getRangeFromColorTable ();
                vpRange = info.getDataInstance().getRange(0);
                Unit u = info.getDataInstance().getRawUnit(0);
                if ( !Misc.equals(u, vpUnit) && Unit.canConvert(u, vpUnit)) {
                    vpRange = new Range(vpUnit.toThis(vpRange.getMin(), u),
                                        vpUnit.toThis(vpRange.getMax(), u));
                }
            }
            info.getLineState().setRange(vpRange);
        }

    }


    /**
     * Get the field name (parameter) at a particular row
     *
     * @param row  row index
     * @return   name of the parameter
     */
    String getFieldName(int row) {
        return getVPInfo(row).getDataInstance().getDataChoice().getName();
    }


    /**
     * Return the appropriate label text for the menu.
     * @return  the label text
     */
    protected String getChangeParameterLabel() {
        return "Add Parameter...";
    }


    /**
     * Get the menu items for the parameter at the given row
     *
     * @param row the row
     *
     * @return List of menu items
     */
    private List getParameterMenuItems(final int row) {
        List  items     = new ArrayList();
        JMenu paramMenu = new JMenu("Parameter " + getFieldName(row));
        items.add(paramMenu);

        VerticalProfileInfo rowInfo = getVPInfo(row);
        paramMenu.add(GuiUtils.makeMenuItem("Chart Properties",
                                            VerticalProfileControl.this,
                                            "showLineProperties", rowInfo));



        // change unit choice
        JMenuItem jmi;
        jmi = new JMenuItem("Change Unit...");
        paramMenu.add(jmi);
        jmi.addActionListener(new ObjectListener(Integer.valueOf(row)) {
            public void actionPerformed(ActionEvent ev) {

                Unit newUnit = getDisplayConventions().selectUnit(
                                   getVPInfo(row).getUnit(), null);
                if (newUnit != null) {
                    getVPInfo(row).setUnit(newUnit);
                    try {
                        doMoveProbe();
                    } catch (Exception exc) {
                        logException("After changing units", exc);
                    }
                }

            }
        });


        // Remove this parameter
        jmi = new JMenuItem("Remove");
        paramMenu.add(jmi);
        jmi.addActionListener(new ObjectListener(Integer.valueOf(row)) {
            public void actionPerformed(ActionEvent ev) {
                removeField(((Integer) theObject).intValue());
            }
        });

        return items;

    }

    /**
     * Make a profile (Time-> (Altitude->Param)
     *
     * @param fi a VisAD FlatField or seqence of FlatFields with 3 or more
     *           domain coordinates, manifold dimension 1.
     *
     *
     * @return the profile.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private FieldImpl makeProfile(FieldImpl fi)
            throws VisADException, RemoteException {

        boolean    isSequence = GridUtil.isTimeSequence(fi);
        TupleType  parm       = GridUtil.getParamType(fi);
        SampledSet domain     = GridUtil.getSpatialDomain(fi);

        RealType height =
            (RealType) ((SetType) domain.getType()).getDomain().getComponent(
                2);
        RealTupleType domainType    = new RealTupleType(RealType.Altitude);
        FunctionType  pType         = new FunctionType(domainType, parm);
        FunctionType  profileType   = null;
        FieldImpl     profile       = null;
        Gridded1DSet  profileDomain = null;
        int           numIters      = 1;
        if (isSequence) {
            SampledSet timeDomain = (SampledSet) fi.getDomainSet();
            numIters = timeDomain.getLength();
            MathType tMT = ((SetType) timeDomain.getType()).getDomain();
            profileType = new FunctionType(tMT, pType);
            profile     = new FieldImpl(profileType, timeDomain);
        } else {
            profileType = pType;
        }

        for (int i = 0; i < numIters; i++) {
            SampledSet ss = (SampledSet) GridUtil.getSpatialDomain(fi, i);
            if ((profileDomain == null)
                    || !GridUtil.isConstantSpatialDomain(fi)) {
                Unit      vUnit      = ss.getSetUnits()[2];
                float[][] domainVals = ss.getSamples();
                if ( !height.equals(RealType.Altitude)) {
                    CoordinateSystem cs = ss.getCoordinateSystem();
                    domainVals =
                        ss.getCoordinateSystem().toReference(domainVals,
                            ss.getSetUnits());
                    vUnit = cs.getReferenceUnits()[2];
                }
                float[] alts = domainVals[2];
                if (altUnit.isConvertible(CommonUnits.HECTOPASCAL)) {
                    CoordinateSystem pressToHeightCS =
                            DataUtil.getPressureToHeightCS(DataUtil.STD_ATMOSPHERE);
                    float[][] hvals = pressToHeightCS.fromReference(new float[][]{alts});
                    alts = hvals[0];  //altUnit.toThis(alts, vUnit);
                } else {
                    alts = CommonUnit.meter.toThis(alts, vUnit);
                }
                try {  // domain might have NaN's in it
                    profileDomain = new Gridded1DSet(domainType,
                            new float[][] {
                        alts
                    }, domainVals[0].length, (CoordinateSystem) null,
                       (Unit[]) null, (ErrorEstimate[]) null, false);
                } catch (Exception e) {
                    break;
                }
            }
            FlatField ff = new FlatField(pType, profileDomain);
            if (isSequence) {
                ff.setSamples(((FlatField) fi.getSample(i)).getFloats(false));
                profile.setSample(i, ff);
            } else {
                ff.setSamples(((FlatField) fi).getFloats(false));
                profile = ff;
            }
        }
        return profile;

    }


    /**
     * This gets called by the base class LineProbeControl class
     * when the probe positon has changed (either through user interaction
     * or through the sharing framework.
     *
     * @param position   new position
     */
    protected void probePositionChanged(RealTuple position) {
        try {
            loadProfile(position);
        } catch (Exception exc) {
            logException("probePositionChanged", exc);
        }

    }

    /**
     * Given the location of the profile SelectorPoint,
     * and a FieldImpl for one or more times for animation,
     * create a data set for a profile at the profile's SP location.
     * Create a vertical line showing where profile is in the data.
     *
     * @param position   new position for profile
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void loadProfile(RealTuple position)
            throws VisADException, RemoteException {

        if ( !getHaveInitialized()) {
            return;
        }

        if ((infos == null) || (position == null)) {
            return;
        }
        LatLonPoint   llp    = null;
        RealTupleType rttype = (RealTupleType) position.getType();
        if (rttype.equals(RealTupleType.SpatialCartesian2DTuple)) {
            // get earth location of the x,y position in the VisAD display
            double[] values = position.getValues();
            EarthLocationTuple elt =
                (EarthLocationTuple) boxToEarth(new double[] { values[0],
                    values[1], 1.0 });
            llp = elt.getLatLonPoint();
        } else if (rttype.equals(RealTupleType.SpatialCartesian3DTuple)) {
            //If is a 3d point then we are probably in the globe view
            double[] values = position.getValues();
            double length = new Point3d(0, 0,
                                        0).distance(new Point3d(values[0],
                                            values[1], values[2]));


            if (length != 0) {
                values[0] = values[0] * (1 / length);
                values[1] = values[1] * (1 / length);
                values[2] = values[2] * (1 / length);
            }
            EarthLocationTuple elt =
                (EarthLocationTuple) boxToEarth(new double[] { values[0],
                    values[1], values[2] });
            llp = elt.getLatLonPoint();
        } else if (rttype.equals(RealTupleType.SpatialEarth2DTuple)) {
            Real[] reals = position.getRealComponents();
            llp = new LatLonTuple(reals[1], reals[0]);
        } else if (rttype.equals(RealTupleType.LatitudeLongitudeTuple)) {
            Real[] reals = position.getRealComponents();
            llp = new LatLonTuple(reals[0], reals[1]);
        } else {
            throw new VisADException(
                "Can't convert position to navigable point");
        }

        List localInfos = new ArrayList(infos);
        List chartInfos = new ArrayList();
        for (int i = 0; i < localInfos.size(); i++) {

            VerticalProfileInfo info =
                (VerticalProfileInfo) localInfos.get(i);
            if ((info == null) || (info.getDataInstance() == null)) {
                continue;
            }
            FieldImpl newFI = GridUtil.getProfileAtLatLonPoint(
                                  info.getDataInstance().getGrid(), llp,
                                  info.getSamplingMode());

            if (newFI != null) {
                info.setProfile(makeProfile(newFI), llp);
                chartInfos.add(info);
            }
        }

        getChart().setProfiles(chartInfos);

        // set location label, if available.
        if (llp != null) {
            positionText = getDisplayConventions().formatLatLonPoint(llp);

            // update lat/lon widget
            if (latLonWidget != null) {
                latLonWidget.setLat(
                    getDisplayConventions().formatLatLon(
                        llp.getLatitude().getValue()));
                latLonWidget.setLon(
                    getDisplayConventions().formatLatLon(
                        llp.getLongitude().getValue()));
            }
        }
        updateLegendLabel();
    }

    /**
     * Add the  relevant file menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getSaveMenuItems(List items, boolean forMenuBar) {
        super.getSaveMenuItems(items, forMenuBar);

        items.add(GuiUtils.makeMenuItem("Save Chart Image...", getChart(),
                                        "saveImage"));
        items.add(GuiUtils.makeMenuItem("Export Current Time as CSV...", this,
                                        "exportCsv"));
        items.add(GuiUtils.makeMenuItem("Export All Times as CSV...", this,
                                        "exportCsvAllTimes"));

    }

    /**
     * Export the current time as csv
     */
    public void exportCsv() {
        try {
            Animation animation = getInternalAnimation();
            int       step      = animation.getCurrent();
            Set       aniSet    = animation.getSet();
            Real[]    times     = Animation.getDateTimeArray(aniSet);
            if (times.length == 0) {
                return;
            }
            exportToCsv(new Real[] { times[step] });
            //        GuiUtils.exportAsCsv(tableModel);
            paramsTable.repaint();
        } catch (Exception exc) {
            logException("Exporting to csv", exc);
        }
    }

    /**
     * Export all times as csv
     */
    public void exportCsvAllTimes() {
        try {
            Animation animation = getInternalAnimation();
            Set       aniSet    = animation.getSet();
            Real[]    times     = Animation.getDateTimeArray(aniSet);
            exportToCsv(times);
            paramsTable.repaint();
        } catch (Exception exc) {
            logException("Exporting to csv", exc);
        }
    }

    /**
     * Export all times as csv
     *
     * @param times export given times as csv
     */
    public void exportToCsv(Real[] times) {
        try {
            String filename =
                    FileManager.getWriteFile(Misc.newList(FileManager.FILTER_CSV,
                            FileManager
                                    .FILTER_XLS), FileManager
                            .SUFFIX_CSV);
            if (filename == null) {
                return;
            }
           // amExporting = true;
            List choices = getDataChoices();
            Real [] levels = getGridDataInstance().getLevels();
            if (times.length == 0) {
                LogUtil.userMessage("No times to export");
                return;
            }

            //Force the sampling. This sets the sample at the current location, time set, etc.
            //setData(times[0], 0);
            List rows = new ArrayList();
            List cols;
            cols = Misc.newList("Time", "Level");

            for (int row = 0; row < choices.size(); row++) {
                VerticalProfileInfo info = getVPInfo(row);
                cols.add(getFieldName(row));
            }
            rows.add(cols);

            for (int timeIdx = 0; timeIdx < times.length; timeIdx++) {
                Real aniValue = times[timeIdx];
                for(int level = 0; level < levels.length; level++) {
                    Real lev = levels[level];
                    cols = Misc.newList("" + aniValue, "" + lev);
                    rows.add(cols);
                }

            }

            for (int timeIdx = 0; timeIdx < times.length; timeIdx++) {
                Real aniValue = times[timeIdx];
                for (int row = 0; row < choices.size(); row++) {
                    VerticalProfileInfo info = getVPInfo(row);
                    //Set          timeSet1 = info..getTimeSet();
                    Data rt = null;
                    FieldImpl sample = info.getProfile(); //.getPointSample();
                    if ((sample != null)) {
                        rt = sample.evaluate(aniValue,
                                info.getSamplingMode(),
                                Data.NO_ERRORS);
                    } else {
                        rt = info.getProfile().getSample(0); //.getPointSample().getSample(0);
                    }

                    float[][] levelData = ((FlatField) rt).getFloats();

                    for(int l = 0; l < levels.length; l++) {
                        cols = (List) rows.get(timeIdx * levels.length  + 1 +l);
                        cols.add(levelData[0][l]);
                    }
                }
            }
            DataUtil.writeCsv(filename, rows);
        } catch (Exception exc) {
            logException("Exporting to csv", exc);
        }
       // amExporting = false;
    }

    /**
     * Make the view menu items
     *
     * @param items List of menu items
     * @param forMenuBar  forMenuBar
     */
    protected void getViewMenuItems(List items, boolean forMenuBar) {
        super.getViewMenuItems(items, forMenuBar);
        items.add(GuiUtils.MENU_SEPARATOR);
        List paramItems = new ArrayList();
        paramItems.add(GuiUtils.makeCheckboxMenuItem("Show Parameter Table",
                this, "showTable", null));
        paramItems.add(doMakeChangeParameterMenuItem());
        List choices = getDataChoices();
        for (int i = 0; i < choices.size(); i++) {
            paramItems.addAll(getParameterMenuItems(i));
        }

        items.add(GuiUtils.makeMenu("Parameters", paramItems));

        JMenu chartMenu = new JMenu("Chart");
        chartMenu.add(
            GuiUtils.makeCheckboxMenuItem(
                "Show Thumbnail in Legend", getChart(), "showThumb", null));
        List chartMenuItems = new ArrayList();
        getChart().addViewMenuItems(chartMenuItems);
        GuiUtils.makeMenu(chartMenu, chartMenuItems);
        items.add(chartMenu);
        items.add(doMakeProbeMenu(new JMenu("Probe")));

    }

    /**
     * If user clicks on the "sampling" column, a popup menu appears
     * with choices for the grid value sampling method.
     *
     */
    public class SamplingEditor extends DefaultCellEditor {

        /**
         * The sampling mode editor
         *
         */
        public SamplingEditor() {
            super(new JComboBox());
        }

        /**
         * Get the component for editing the sampling methods
         *
         * @param table           the JTable
         * @param value           the value
         * @param isSelected      flag for selection
         * @param rowIndex        row index
         * @param vColIndex       column index.
         * @return   the editing component
         */
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int rowIndex,
                int vColIndex) {
            JComboBox box = (JComboBox) getComponent();
            GuiUtils.setListData(box, samplingLabels);
            box.setSelectedItem(value);
            return box;
        }
    }

    /**
     * Show the line properties editor
     *
     * @param vpInfo  the VerticalProfileInfo
     */
    public void showLineProperties(VerticalProfileInfo vpInfo) {
        PropertyChangeListener listener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                try {
                    VerticalProfileControl.this.doMoveProbe();
                } catch (Exception exc) {
                    logException("Updating position", exc);
                }
            }
        };
        LineState lineState = vpInfo.getLineState();
        lineState.showPropertiesDialog(getStore(), listener,
                                       getChart().getPlotNames(),
                                       getChart().getCurrentRanges());
    }


    /**
     * Remove a parameter
     *
     * @param row  row to remove
     */
    private void removeField(int row) {
        VerticalProfileInfo info = getVPInfo(row);
        DataInstance        di   = info.getDataInstance();
        if (di != null) {
            removeDataChoice(di.getDataChoice());
        }
        infos.remove(row);
        try {
            setTimesForAnimation();
        } catch (Exception e) {
            logException("Error updating times: ", e);
        }
        fireStructureChanged();
        doMoveProbe();  // update the side legend label if needed
    }

    /**
     * Set the times for animation
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setTimesForAnimation()
            throws VisADException, RemoteException {
        Set myTimes = calculateTimeSet();
        if (myTimes == null) {
            return;
        }
        getAnimationWidget().setBaseTimes(myTimes);
    }

    /**
     * Create a merged time set from the DataChoices.
     *
     * @return merged set or null
     */
    private Set calculateTimeSet() {
        List choices = getDataChoices();
        if (choices.isEmpty()) {
            return null;
        }
        Set newSet = null;
        for (int i = 0; i < choices.size(); i++) {
            try {
                VerticalProfileInfo info         = getVPInfo(i);
                GridDataInstance    dataInstance = info.getDataInstance();
                Set set = GridUtil.getTimeSet(dataInstance.getGrid());
                //System.out.println("di.timeset["+i+"] = " + set);
                if (set != null) {
                    if (newSet == null) {
                        newSet = set;
                    } else {
                        newSet = newSet.merge1DSets(set);
                    }
                }
            } catch (Exception e) {
                logger.error("Problem calculating TimeSet", e);
            }
        }
        //System.out.println("merged time set = " + newSet);
        return newSet;
    }

    /**
     * Called to reset the table structure after a change
     */
    private void fireStructureChanged() {

        tableModel.fireTableStructureChanged();
        paramsTable.getColumnModel().getColumn(COL_SAMPLING).setCellEditor(
            new SamplingEditor());
    }

    /**
     * Set the ShowThumbNail property.
     *
     * @param value The new value for ShowThumbNail
     */
    public void setShowThumbNail(boolean value) {
        getChart().setShowThumb(value);
    }



    /**
     *  Set the ShowTable property.
     *
     *  @param value The new value for ShowTable
     */
    public void setShowTable(boolean value) {
        showTable = value;
        if (tablePanel != null) {
            tablePanel.setVisible(showTable);
            tablePanel.invalidate();
            tablePanel.validate();
        }
    }

    /**
     *  Get the ShowTable property.
     *
     *  @return The ShowTable
     */
    public boolean getShowTable() {
        return showTable;
    }

    /**
     * Set the Infos property.
     *
     * @param value The new value for Infos
     */
    public void setInfos(List value) {
        infos = value;
    }

    /**
     * Get the Infos property.
     *
     * @return The Infos
     */
    public List getInfos() {
        return infos;
    }



    /**
     * Get the chart
     *
     * @return The chart
     */
    public VerticalProfileChart getChart() {
        if (chart == null) {
            chart = new VerticalProfileChart(this, "Vertical Profile");
        }
        return chart;
    }

    /**
     * Set AltCoord
     * McIDAS Inquiry #3105-3141
     *
     * @param cname
     */
    public void setAltCoord(String cname) {
       altCoord = cname;
    }

    /**
     * Get AltCoord
     *
     * @return altCoord
     */
    public String getAltCoord() {
        return altCoord;
    }



    /**
     * Set the Chart property.
     *
     * @param value The new value for Chart
     */
    public void setVerticalProfileChart(VerticalProfileChart value) {
        chart = value;
    }

    /**
     * Get the Chart property.
     *
     * @return The Chart
     */
    public VerticalProfileChart getVerticalProfileChart() {
        return chart;
    }


}
