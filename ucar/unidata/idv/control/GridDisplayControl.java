/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2022
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

package ucar.unidata.idv.control;


import static ucar.unidata.geoloc.LatLonPointImpl.latNormal;
import static ucar.unidata.geoloc.LatLonPointImpl.lonNormal;
import static visad.RealType.Latitude;
import static visad.RealType.Longitude;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.ContourInfo;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.Trace;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.data.CalendarDateTime;
import visad.CommonUnit;
import visad.Data;
import visad.DateTime;
import visad.DisplayRealType;
import visad.FieldImpl;
import visad.MathType;
import visad.Real;
import visad.RealTupleType;
import visad.RealType;
import visad.ScalarMap;
import visad.Unit;
import visad.VisADException;

import visad.georef.EarthLocation;
import visad.georef.MapProjection;
import visad.georef.TrivialMapProjection;


import java.awt.event.ActionEvent;

import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JComboBox;



/**
 * Class with methods used by a DisplayControlImpl. For gridded data.
 * @author Unidata Development Team
 * @version $Revision: 1.107 $
 */

public abstract class GridDisplayControl extends DisplayControlImpl {

    /** flag for the set levels command */
    public static final String CMD_SETLEVELS = "cmd.setlevels";

    /** command for showing cont level dialog */
    public static final String CMD_CONTOURDIALOG = "cmd.contourdialog";


    /** logging category */
    protected static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(GridDisplayControl.class.getName());

    /** flag for setting levels */
    protected boolean settingLevel = false;

    /** Key for setting intial probe position */
    public static final String INITIAL_PROBE_EARTHLOCATION =
        "INITIAL_PROBE_EARTHLOCATION";

    /** RealType for vertical mapping */
    private RealType topoType;

    /**
     * For legacy code.
     * @deprecated Should use getGridDataInstance
     */
    protected GridDataInstance gridDataInstance;


    /**
     *  cstr does nothing yet; usually made from a subclass.
     */
    public GridDisplayControl() {}

    /**
     * @override
     */
    protected boolean canDoProgressiveResolution() {
        return canDataDoProgressiveResolution();
    }



    /**
     * A utility to cast the getDataInstance as a GridDataInstance
     *
     * @return the GridDataInstance
     */
    public GridDataInstance getGridDataInstance() {
        return (GridDataInstance) getDataInstance();
    }



    /**
     * Get the cursor readout data
     *
     * @return the data
     *
     * @throws Exception problem getting data
     */
    protected Data getCursorReadoutData() throws Exception {
        return getData(getGridDataInstance());
    }


    /**
     * Get cursor readout
     *
     * @param el  earth location
     * @param animationValue animation value
     * @param animationStep animation step
     * @param samples the list of samples
     *
     * @return list of values
     *
     * @throws Exception problem getting values
     */
    protected List getCursorReadoutInner(EarthLocation el,
                                         Real animationValue,
                                         int animationStep,
                                         List<ReadoutInfo> samples)
            throws Exception {
        Data data = getCursorReadoutData();
        if ((data == null) || !(data instanceof FieldImpl)) {
            return null;
        }
        FieldImpl field = (FieldImpl) data;
        if (field == null) {
            return null;
        }
        List result = new ArrayList();
        Real r = GridUtil.sampleToReal(
                     field, el, animationValue,
                     getSamplingModeValue(
                         getObjectStore().get(
                             PREF_SAMPLING_MODE, DEFAULT_SAMPLING_MODE)));
        if ((r != null) && !r.isMissing()) {
            result.add("<tr><td>" + getMenuLabel()
                       + ":</td><td align=\"right\">"
                       + formatForCursorReadout(r) + "</td></tr>");
        }
        return result;
    }

    private static final Logger logger = LoggerFactory.getLogger(GridDisplayControl.class);

    /**
     * Override superclass method to get the initial color table.
     * @return  color table for the parameter
     */
    protected ColorTable getInitialColorTable() {
        // If we have more than one fields in the range then we assume 
        // we'll color by the second field so we use the unit:unit name 
        // to look up the default colortable
        return getDisplayConventions().getParamColorTable(
            getColorParamName());
    }


    /**
     * Return whether the Data held by this display control contains multiple
     * fields (e.g., for the isosurface colored by another parameter
     * @return  true if there are multiple fields
     */
    protected boolean haveMultipleFields() {
        if (getGridDataInstance() == null) {
            return false;
        }
        return getGridDataInstance().getNumRealTypes() > 1;
    }

    /**
     * See if the display unit is also the color unit
     * @return true if the units are the same
     */
    protected boolean isDisplayUnitAlsoColorUnit() {
        return !haveMultipleFields();
    }

    /**
     * Get the parameter name for color.
     * @return  color parameter name
     */
    protected String getColorParamName() {
        if (haveMultipleFields() && (getGridDataInstance() != null)) {
            return getGridDataInstance().getDataChoice().getIndexedName(
                getColorRangeIndex());
        }
        return paramName;
    }

    /**
     * Return the default range
     * @return   the default range.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Range getInitialRange() throws RemoteException, VisADException {
        Unit colorUnit = getColorUnit();
        Range range =
            getDisplayConventions().getParamRange(getColorParamName(),
                colorUnit);
        if ((range == null) && !haveMultipleFields()) {
            //            range = getRangeFromColorTable ();
        }

        if ((range == null) && (getGridDataInstance() != null)) {
            range = getDataRangeInColorUnits();
            /*
            range = getGridDataInstance().getRange(getColorRangeIndex());
            Unit u = getGridDataInstance().getRawUnit(getColorRangeIndex());
            if ( !Misc.equals(u, colorUnit)
                    && Unit.canConvert(u, colorUnit)) {
                range = new Range(colorUnit.toThis(range.getMin(), u),
                                  colorUnit.toThis(range.getMax(), u));
            }
            */
        }
        return range;
    }

    /**
     * Get the range of the data in color units.
     * @return the range or null
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Range getDataRangeInColorUnits()
            throws RemoteException, VisADException {
        if (getGridDataInstance() == null) {
            return null;
        }
        Unit  colorUnit = getColorUnit();
        Range range     =
            getGridDataInstance().getRange(getColorRangeIndex());
        Unit  u = getGridDataInstance().getRawUnit(getColorRangeIndex());
        if (colorUnit != null  && !Misc.equals(u, colorUnit) && Unit.canConvert(u, colorUnit)) {
            range = new Range(colorUnit.toThis(range.getMin(), u),
                              colorUnit.toThis(range.getMax(), u));
        }
        return range;
    }


    /**
     * Create the GridDataInstance from the dataChoice.
     * Set the dataInstance and its paramName
     * in the superclass member data. Returns whether the dataInstance is ok.
     *
     * @param dataChoice    data choice defining the data
     * @return  the data instance
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected DataInstance doMakeDataInstance(DataChoice dataChoice)
            throws RemoteException, VisADException {
        DataSelection ds = getDataSelection();
        Object t = ds.getProperty(
                DataSelection.PROP_PROGRESSIVERESOLUTION);
        if (t != null) {
            isProgressiveResolution = ((Boolean) t).booleanValue();
        }

        return gridDataInstance = new GridDataInstance(dataChoice,
                ds, getRequestProperties());
    }



    /**
     * Returns the index to use in the GridDataInstance array of ranges
     * for color ranges. The default is 1 though if there is not more than
     * one field in the range then we end up with the 0th value.
     * @return  1
     */
    protected int getColorRangeIndex() {
        return 1;
    }


    /**
     * Get the raw data unit.
     * @return  unit for the data values
     */
    public Unit getRawDataUnit() {
        return ((getGridDataInstance() == null)
                ? null
                : getGridDataInstance().getRawUnit(0));
    }


    /**
     * Get the default contour info to use
     *
     * @return default contour info to use
     */
    protected ContourInfo getDefaultContourInfo() {
        return null;
    }

    /**
     * A hook for derived classes to set any state. ex: color filled contours turn off
     * labels
     *
     * @param contourInfo The contour info to initialize
     */
    protected void initializeDefaultContourInfo(ContourInfo contourInfo) {}


    /**
     * Get the contour information for any contours
     *
     * @return  the contour information
     */

    public ContourInfo getContourInfo() {
        ContourInfo contourInfo = super.getContourInfo();
        try {
            //Are we real
            if ((getControlContext() != null)
                    && (getDisplayConventions() != null)
                    && (contourInfo == null)) {

                ContourInfo dflt = getDefaultContourInfo();
                contourInfo =
                    getDisplayConventions().findDefaultContourInfo(paramName,
                        null);
                if (contourInfo == null) {
                    if (getGridDataInstance() != null) {
                        contourInfo = getDisplayConventions().findContourInfo(
                            paramName, getGridDataInstance().getRealType(0),
                            getDisplayUnit(),
                            getGridDataInstance().getRange(0),
                            getDefaultContourInfo());
                    } else {
                        //                        System.err.println("No data instance: "
                        //                                           + getClass().getName());
                    }
                }
                // set the default labelling stuff from preferences
                if (contourInfo != null) {
                    contourInfo
                        .setLabelSize((int) getIdv().getStateManager()
                            .getPreferenceOrProperty(ViewManager
                                .PREF_CONTOUR_LABELSIZE, ContourInfo
                                .DEFAULT_LABEL_SIZE));
                    contourInfo
                        .setFont(ContLevelDialog
                            .getContourFont(getIdv().getStateManager()
                                .getPreferenceOrProperty(ViewManager
                                    .PREF_CONTOUR_LABELFONT)));
                    contourInfo.setAlignLabels(
                        getIdv().getStateManager().getPreferenceOrProperty(
                            ViewManager.PREF_CONTOUR_LABELALIGN, true));
                }
                //              System.err.println (contourInfoParams);
                if (contourInfoParams != null) {

                    contourInfo.processParamString(contourInfoParams);
                }
                initializeDefaultContourInfo(contourInfo);
            }
        } catch (Exception exc) {
            logException("setting contour info", exc);
        }
        return contourInfo;
    }



    /**
     * By default we color by the second index (if it is defined)
     * @return   the unit for the color parameter
     */
    protected Unit getColorUnit() {
        Unit unit = super.getColorUnit();
        if (unit == null) {
            if (haveMultipleFields()) {
                unit = getDisplayConventions().getDisplayUnit(
                    getColorParamName(), null);
                if ((unit == null) && (getGridDataInstance() != null)) {
                    unit = getGridDataInstance().getRawUnit(
                        getColorRangeIndex());
                }
            }
            if (unit == null) {
                unit = getDisplayUnit();
            }
            if (unit != null) {
                setUnitForColor(unit);
            }
        }
        return unit;
    }


    /**
     * Get the unit for the data display.
     * @return  unit to use for displaying the data
     */
    public Unit getDisplayUnit() {
        Unit unit = super.getDisplayUnit();
        if (unit == null) {
            setDisplayUnit(unit = getDisplayUnit(getRawDataUnit()));
        }
        return unit;
    }


    /**
     * Does nothing yet.  Subclasses should override
     *
     * @param r    level for data
     */
    public void setLevel(Object r) {}

    /**
     * The user has changed the level
     *
     * @param pl The new level
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void setLevelFromUser(Object pl)
            throws VisADException, RemoteException {
        setLevel(pl);
    }

    /**
     *  Return the range attribute of the colorTable  (if non-null)
     *  else return null;
     * @return The range from the color table attribute
     */
    public Range getColorRangeFromData() {
        if (getGridDataInstance() != null) {
            return convertColorRange(
                getGridDataInstance().getRange(getColorRangeIndex()));
        }
        return null;
    }


    /**
     * Utility to convert the given raw data range into the display units
     *
     * @param rawRange Raw data range
     *
     * @return Converted range
     */
    public Range convertColorRange(Range rawRange) {
        return convertColorRange(
            rawRange, getGridDataInstance().getRawUnit(getColorRangeIndex()));
    }


    /**
     * Set the value "l" in the level combo box "levelBox."
     *
     * @param l    the level
     * @param levelBox  the level box
     */
    public void setLevel(Object l, JComboBox levelBox) {
        //      (The settingLevel code is there to prevent us from 
        // responding to the changed level in the ActionListener event.)

        if ( !settingLevel) {
            settingLevel = true;
            if (levelBox != null) {
                levelBox.setSelectedItem(l);
            }
            settingLevel = false;
        }
    }


    /**
     * Make and return a JButton which will summon a ContlevelDialog;
     * @deprecated Don't use this, rely on the
     * @return  the action button
     */
    public JButton doMakeContourLevelControl() {
        return GuiUtils.makeJButton("Contour", new Object[] {
            "-listener", this, "-tooltip", "Set contour levels", "-command",
            CMD_CONTOURDIALOG
        });
    }


    /**
     * Make and return a JComboBox with all native raw grid levels to choose,
     * from the current getGridDataInstance(), if any.
     * @return   the combobox
     */
    public JComboBox doMakeLevelControl() {
        Real[] levels = null;
        if (getGridDataInstance() != null) {
            Trace.call1("GDI.getLevels");
            levels = getGridDataInstance().getLevels();
            Trace.call2("GDI.getLevels");
        }
        return doMakeLevelControl(levels);
    }


    /**
     * Make and return a JComboBox with the supplied "levels"  to choose from;
     * see action commmand "levels".
     *
     * @param levels    the levels to populat the combo box with
     * @return   the combo box
     */
    public JComboBox doMakeLevelControl(Object[] levels) {
        JComboBox box;
        if (levels != null) {
            box = new JComboBox(formatLevels(levels));
        } else {
            box = new JComboBox();
        }
        box.addActionListener(this);
        box.setEditable(true);
        box.setActionCommand(CMD_SETLEVELS);
        return box;
    }

    /**
     * Add a topography map for the parameter at the specified index
     *
     * @param typeIndex  index of the RealType to use
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException Unable to set the ScalarMap
     */
    protected void addTopographyMap(int typeIndex)
            throws VisADException, RemoteException {
        NavigatedDisplay nd = getNavigatedDisplay();
        if (nd == null) {
            return;
        }
        if (topoType != null) {
            nd.removeVerticalMap(topoType);
        }
        topoType = getGridDataInstance().getRealType(typeIndex);
        nd.addVerticalMap(topoType);
    }

    /**
     * Add a topography map for the parameter at the specified index
     *
     * @param type  the RealType to use
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException Unable to set the ScalarMap
     */
    protected void addTopographyMap(RealType type)
            throws VisADException, RemoteException {
        NavigatedDisplay nd = getNavigatedDisplay();
        if (nd == null) {
            return;
        }
        if (topoType != null) {
            nd.removeVerticalMap(topoType);
        }
        topoType = type;
        nd.addVerticalMap(topoType);
    }
    /**
     * Deal with action event commands from the levels and contours buttons
     * made by this class.
     *
     * @param event    event to handle
     */
    public void actionPerformed(ActionEvent event) {

        if ( !getOkToFireEvents()) {
            return;
        }
        try {
            String cmd = event.getActionCommand();
            //            if (cmd.equals(CMD_CONTOURDIALOG)) {
            //                ContLevelDialog contDialog = new ContLevelDialog(this, "");
            //                contDialog.showDialog(new ContourInfo(contourInfo));
            //                return;
            //            }
            if (cmd.equals(CMD_SETLEVELS)) {
                if (settingLevel || !getHaveInitialized()) {
                    return;
                }
                final JComboBox box = (JComboBox) event.getSource();
                //Do it in a thread
                Misc.run(new Runnable() {
                    public void run() {
                        try {
                            setLevelFromBox(box);
                        } catch (Exception exc) {
                            logException("DisplayControl.actionPerformed",
                                         exc);
                        }
                    }
                });

                return;
            }
            super.actionPerformed(event);
        } catch (Exception exc) {
            logException("DisplayControl.actionPerformed", exc);
        }
    }


    /**
     * The level comobo box has changed. Set the level.
     *
     * @param box The box
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void setLevelFromBox(JComboBox box)
            throws RemoteException, VisADException {
        //Real   levelValue;
        Object levelValue = box.getSelectedItem();
        /*
        if (value instanceof TwoFacedObject) {
            levelValue = (Real) ((TwoFacedObject) value).getId();
        } else if (value instanceof Real) {
            levelValue = (Real) value;
        } else {
            try {
                //Get an example Real to clone from the list
                Object o   = box.getItemAt(0);
                Real   tmp = null;
                if (o instanceof TwoFacedObject) {
                    tmp = (Real) ((TwoFacedObject) o).getId();
                } else {
                    tmp = (Real) o;
                }
                double dv = Misc.parseNumber(value.toString());
                levelValue = tmp.cloneButValue(dv);
            } catch (NumberFormatException nfe) {
                userErrorMessage("Incorrect format:" + value.toString());
                return;
            }
        }
        */
        settingLevel = true;
        setLevelFromUser(levelValue);
        settingLevel = false;
    }


    /**
     * Get MapProjection of data to display.  Override the superclass
     * method because we have more info for this type of data
     *
     * @return The native projection of the data
     */
    public MapProjection getDataProjection() {
        MapProjection mp = null;
        if (getGridDataInstance() != null) {
            FieldImpl data = getGridDataInstance().getGrid(false);
            if (data != null) {
                try {
                    mp = GridUtil.getNavigation(data);
                    if (mp instanceof TrivialMapProjection) {
                        // mp will be unchanged if we didn't need to use
                        // normalizeRectangle
                        mp = validateProjection((TrivialMapProjection)mp);
                    }
                } catch (Exception e) {
                    mp = null;
                }
            }
        }
        // TODO:  Should we just return null instead of calling super?
        return (mp != null)
               ? mp
               : super.getDataProjection();
    }
    
    /**
     * Ensure that the given projection has a {@literal "sensible"} bounding
     * box.
     * 
     * <p>For our purposes, {@literal "sensible"} means that the {@code x} and
     * {@code y} coordinates of the bounding box are valid longitude and
     * latitude values.</p>
     * 
     * <p>If the bounding box associated with {@code mp} could not be
     * {@link #normalizeRectangle(Rectangle2D) normalized}, this method will
     * use the default bounding box from
     * {@link TrivialMapProjection#TrivialMapProjection() VisAD}.</p>
     * 
     * @param mp Projection to validate. Cannot be {@code null}.
     * 
     * @return If the original bounding box is fine, then {@code mp} will be
     *         returned. Otherwise a new {@code TrivialMapProjection} with a
     *         {@link #normalizeRectangle(Rectangle2D) normalized} bounding box
     *         will be returned.
     * 
     * @throws VisADException if there was a problem creating a new
     *                        {@link TrivialMapProjection}.
     */
    private static MapProjection validateProjection(TrivialMapProjection mp)
        throws VisADException
    {
        RealTupleType tupType = mp.getReference();
        MathType[] comps = tupType.getComponents();
        Rectangle2D mapArea = mp.getDefaultMapArea();
        TrivialMapProjection result = mp;
        
        if ((mapArea != null) &&
            Objects.equals(comps[0], Longitude) &&
            Objects.equals(comps[1], Latitude))
        {
            double x = Math.round(mapArea.getX());
            double y = Math.round(mapArea.getY());
            boolean badX = (x >= 180.0) || (x <= -180.0);
            boolean badY = (y >= 90.0) || (y <= -90.0);
            if (badX || badY) {
                // our validated projection should do one of the following:
                //   A) Use the normalized Rectangle2D.
                //   B) Couldn't normalize our existing Rectangle2D, so use
                //      TrivialMapProjection's default Rectangle2D instead.
                Rectangle2D r2d = normalizeRectangle(mapArea);
                if (r2d != null) {
                    result = new TrivialMapProjection(tupType, r2d);
                } else {
                    result = new TrivialMapProjection(tupType);
                }
            }
        }
        return result;
    }
    
    /**
     * Ensure the given bounding box's {@code x} and {@code y} coordinates are
     * valid longitude and latitude values.
     * 
     * @param bb Bounding box to normalize. If {@code null}, {@code null} is
     *           returned.
     * 
     * @return If {@code bb} is not {@code null}, return the given bounding
     *         box with valid latitude and longitude values. Otherwise return
     *         {@code null}.
     * 
     * @see LatLonPointImpl#lonNormal(double)
     * @see LatLonPointImpl#latNormal(double)
     */
    public static Rectangle2D normalizeRectangle(final Rectangle2D bb) {
        Rectangle2D r2d = null;
        if (bb != null) {
            float normalizedMinLon = (float)lonNormal((float)bb.getX());
            float normalizedMinLat = (float)latNormal((float)bb.getY());
            r2d = new Rectangle2D.Float(normalizedMinLon,
                                        normalizedMinLat,
                                        (float)bb.getWidth(),
                                        (float)bb.getHeight());
        }
        return r2d;
    }

    /**
     * Export displayed data to file
     * @param type  type of data
     */
    public void exportDisplayedData(String type) {
        // HACK for now
        if ( !((this instanceof CrossSectionControl)
                || (this instanceof RadarSweepControl))) {
            try {
                Data d = getDisplayedData();
                if (d == null) {
                    return;
                }
                if (d instanceof FieldImpl) {
                    JComboBox publishCbx =
                        getIdv().getPublishManager().getSelector("nc.export");
                    String filename =
                        FileManager.getWriteFile(FileManager.FILTER_NETCDF,
                            FileManager.SUFFIX_NETCDF, ((publishCbx != null)
                            ? GuiUtils.top(publishCbx)
                            : null));
                    if (filename == null) {
                        return;
                    }
                    GridUtil.exportGridToNetcdf((FieldImpl) d, filename);
                    getIdv().getPublishManager().publishContent(filename,
                            null, publishCbx);
                }

            } catch (Exception e) {
                logException("Unable to export the data", e);
            }
        } else {
            super.exportDisplayedData(type);
        }
    }

    /**
     * Export displayed data to file
     * @param what type of data netcdf
     */
    public void doExport(String what, String filename) throws Exception {

        if (what.contains("netcdf")) {
            if ( !((this instanceof CrossSectionControl)
                    || (this instanceof RadarSweepControl))) {
                try {
                    Data d = getDisplayedData();
                    if (d == null) {
                        return;
                    }
                    if(!filename.endsWith(".nc"))
                        filename = filename + ".nc";
                    if (d instanceof FieldImpl) {
                        GridUtil.exportGridToNetcdf((FieldImpl) d, filename);
                    }

                } catch (Exception e) {
                    logException("Unable to export the data", e);
                }
            }
        }
    }
    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean shouldAddDisplayListener() {
        return true;
    }

    /**
     * Signal base class to add this as a control listener
     *
     * @return Add as control listener
     */
    protected boolean shouldAddControlListener() {
        return true;
    }

    /**
     * Apply the forecast hour macro
     *
     * @param t label string
     * @param currentTime first time
     *
     * @return modified string
     */
    protected String applyForecastHourMacro(String t, DateTime currentTime) {
        if (hasForecastHourMacro(t)) {
            String v = "";
            CalendarDateTime fTime = (CalendarDateTime)getDataChoice().getAllDateTimes().get(0);
            if ((currentTime != null) && (fTime != null)) {
                try {
                    double diff =
                            currentTime.getValue(CommonUnit.secondsSinceTheEpoch)
                                    - fTime.getValue(CommonUnit.secondsSinceTheEpoch);
                    v = ((int) (diff / 60 / 60)) + "";
                } catch (Exception exc) {
                    logger.error("Problem applying forecast hour macro", exc);
                }
            }
            return t.replace(MACRO_FHOUR2, v).replace(MACRO_FHOUR, v + "H");
        }
        return t;
    }
}
