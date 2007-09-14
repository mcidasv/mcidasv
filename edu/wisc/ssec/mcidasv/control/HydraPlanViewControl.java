package edu.wisc.ssec.mcidasv.control;


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.control.ImagePlanViewControl;

import ucar.unidata.ui.colortable.ColorTableManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.ColorTable;

import ucar.visad.display.DisplayableData;
import ucar.visad.display.Grid2DDisplayable;

import visad.*;
import visad.VisADException;

import java.rmi.RemoteException;

import java.util.List;

/**
 * Class for controlling the display of images.
 * @author IDV Development Group
 * @version $Revision$
 */
public class HydraPlanViewControl extends ImagePlanViewControl {

    /**
     * Default constructor.  Sets the attribute flags used by
     * this particular <code>PlanViewControl</code>
     */
    public HydraPlanViewControl() {
        super();
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DISPLAYUNIT
                          | FLAG_ZPOSITION | FLAG_SKIPFACTOR);
    }

    /**
     * Method to create the particular <code>DisplayableData</code> that
     * this this instance uses for data depictions.
     * @return Contour2DDisplayable for this instance.
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   RMI error
     */
    protected DisplayableData createPlanDisplay()
            throws VisADException, RemoteException {
        Grid2DDisplayable gridDisplay = 
            new Grid2DDisplayable("ImagePlanViewControl_" + ((datachoice != null)
                ? datachoice.toString()
                : ""), true);
        gridDisplay.setTextureEnable(true);
        /* TODO: Find out why this causes redisplays
        if (BaseImageControl.EMPTY_IMAGE != null) {
            gridDisplay.loadData(BaseImageControl.EMPTY_IMAGE);
        }
        */
        //gridDisplay.setUseRGBTypeForSelect(true);
        addAttributedDisplayable(gridDisplay);
        return gridDisplay;
    }

    /**
     * Get the initial color table for the data
     *
     * @return  intitial color table
     */
    protected ColorTable getInitialColorTable() {
/*
        DisplayConventions dc = getDisplayConventions();
        List colorNames = dc.getColorNameList();
        ColorTable colorTable = super.getInitialColorTable();
        if (colorTable.getName().equalsIgnoreCase("default")) {
            colorTable = dc.getParamColorTable("image");
        }
*/
        ColorTableManager ctm = getControlContext().getColorTableManager();
        ColorTable colorTable = ctm.getColorTable("ColorTable_AOD");
        return colorTable;
    }


    /**
     * Get whether this display should allow smoothing
     * @return true if allows smoothing.
     */
    public boolean getAllowSmoothing() {
        return false;
    }


    /**
     * Get the initial range for the data and color table.
     * @return  initial range
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Range getInitialRange() throws RemoteException, VisADException {
/*
        Range range = getDisplayConventions().getParamRange(paramName,
                          getDisplayUnit());
        //Don't do this for now
        if (range == null) {
            range = getRangeFromColorTable();
            if ((range != null) && (range.getMin() == range.getMax())) {
                range = null;
            }
        }

        if (range == null) {
            range = getDisplayConventions().getParamRange("image",
                    getDisplayUnit());
        }
        if (range == null) {
            return new Range(0, 255);
        }
*/
        Range range = new Range((double)0.0, (double)0.99);
        return range;
    }

    /**
     * Get the slice for the display
     *
     * @param slice  slice to use
     *
     * @return slice with skip value applied
     *
     * @throws VisADException  problem subsetting the slice
     */
    protected FieldImpl getSliceForDisplay(FieldImpl slice)
            throws VisADException {
        checkImageSize(slice);
        if (getSkipValue() <= 0) {
            return slice;
        }
        return GridUtil.subset(slice, getSkipValue() + 1);
    }

    /**
     * Return the label that is to be used for the skip widget
     * This allows derived classes to override this and provide their
     * own name,
     *
     * @return Label used for the line width widget
     */
    public String getSkipWidgetLabel() {
        return "Pixel Sampling";
    }

}
