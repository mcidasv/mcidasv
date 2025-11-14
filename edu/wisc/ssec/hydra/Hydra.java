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

package edu.wisc.ssec.hydra;

import static edu.wisc.ssec.mcidasv.McIDASV.getStaticMcv;

import edu.wisc.ssec.hydra.data.DataSource;
import edu.wisc.ssec.hydra.data.DataSourceFactory;

import java.awt.Component;
import java.awt.geom.Rectangle2D;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;

import java.io.File;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.net.URL;
import java.text.DecimalFormat;

import java.lang.Float;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wisc.ssec.mcidasv.Constants;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Range;
import ucar.unidata.util.ColorTable;
import edu.wisc.ssec.hydra.data.DataChoice;
import edu.wisc.ssec.hydra.data.DataSelection;
import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.LongitudeLatitudeCoordinateSystem;
import edu.wisc.ssec.adapter.ReprojectSwath;
import edu.wisc.ssec.adapter.SunRelativePosition;

import ucar.visad.display.DisplayMaster;
import ucar.visad.display.MapLines;

import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.MapProjectionDisplayJ3D;


import visad.data.mcidas.BaseMapAdapter;
import visad.georef.MapProjection;
import visad.georef.TrivialMapProjection;
import visad.georef.LatLonPoint;
import visad.georef.EarthLocationTuple;


import visad.*;

import java.rmi.RemoteException;

import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.jdom2.Document;
import org.jdom2.Element;

import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.StringTokenizer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileSystemView;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

public class Hydra {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceFactory.class);

    private static float textSizeFactor = 1;
    private static int fontSize = 24;

    Selection selection = null;

    DataSource dataSource = null;

    public static ColorTable grayTable = new ColorTable("gray", "gray", grayTable(256, false));

    public static ColorTable invGrayTable = new ColorTable("invGray", "invGray", grayTable(256, true));

    public static ColorTable rainbow = null;

    public static ColorTable invRainbow = null;

    public static ColorTable heat = null;

    public static RealType reflectance = RealType.getRealType("Reflectance");

    public static RealType brightnessTemp = RealType.getRealType("BrightnessTemp");

    public static RealType radiance = RealType.getRealType("Radiance");

    float nadirResolution = 1000.0f; // default

    float wavenumber = 0;

    int channelIndex = 0;

    boolean doReproject = true;

    String dateTimeStamp = null;
    String sourceDescription = null;
    String fldName = null;

    JFrame selectFrame = null;

    JComponent actionComponent = null;

    boolean selectorIconified = false;

    static int numImageDisplays = 0;

    JComponent selectComponent = null;

    DataBrowser dataBrowser = null;

    private static boolean regionMatch = true;

    private static int reprojectMode = 0;

    private static boolean doParallel = true;

    private static boolean replicateCompute = false;

    private static MapProjection sharedMapProj = null;

    public static HashMap<HydraRGBDisplayable, FlatField> displayableToImage =
            new HashMap<HydraRGBDisplayable, FlatField>();

    public static GSHHG GSHHGadapter = null;

    public static WorldDataBank WDBIIadapter = null;

    private static int numDataSourcesOpened = 0;

    public boolean multiDisplay = false;
    public boolean singleChannelDisplay = true;
    public boolean multiChannelDisplay = false;
    public boolean atmRetrievalDisplay = false;

    private static int uniqueID = 0;

    private int dataSourceId;

    private DataSourceFactory dataSourceFactory = new DataSourceFactory();

    public static HashMap<Integer, DataSource> dataSourceMap = new HashMap<>();

    public static HashMap<Integer, Selection> dataSourceIdToSelector = new HashMap();

    public Hydra() {
    }

    public Hydra(DataBrowser dataBrowser) {
        this.dataBrowser = dataBrowser;
    }

    public void dataSourceSelected(File dir) {
        try {
            logger.trace("Bef createDataSource()");
            dataSource = dataSourceFactory.createDataSource(dir);
        } catch (Exception e) {
            logger.trace("dataSourceSelected Exception: " + e.getMessage());
            e.printStackTrace();
        }

        sourceDescription = dataSource.getDescription();
        dateTimeStamp = dataSource.getDateTimeStamp();
        numDataSourcesOpened++;

        dataSourceId = numDataSourcesOpened;
        dataSource.setDataSourceId(dataSourceId);
        dataSourceMap.put(dataSourceId, dataSource);

        selection = new BasicSelection(dataSource, this, numDataSourcesOpened);

        dataSourceIdToSelector.put(dataSourceId, selection);

        if (replicateCompute) {
            Compute.replicateCompute(dataBrowser, dataSource, selection);
        }
    }

    public DataBrowser getDataBrowser() {
        return dataBrowser;
    }

    public void dataSourceSelected(File[] files) {
        String filename = files[0].getName();

        try {
            dataSource = dataSourceFactory.createDataSource(files);
        } catch (Exception e) {
            e.printStackTrace();
        }

        dateTimeStamp = dataSource.getDateTimeStamp();
        sourceDescription = dataSource.getDescription();
        numDataSourcesOpened++;

        dataSourceId = numDataSourcesOpened;
        dataSource.setDataSourceId(dataSourceId);
        dataSourceMap.put(dataSourceId, dataSource);

        if (dataSource.isSounder()) {
            multiChannelDisplay = true;
            multiDisplay = true;
        } else if (dataSource.isAtmRetrieval()) {
            atmRetrievalDisplay = true;
            multiDisplay = true;
        } else if (dataSource.isImager()) {
            multiChannelDisplay = false;
            multiDisplay = false;
        }

        selection = new BasicSelection(dataSource, this, numDataSourcesOpened);

        dataSourceIdToSelector.put(dataSourceId, selection);

        if (replicateCompute) {
            Compute.replicateCompute(dataBrowser, dataSource, selection);
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public static DataSource getDataSource(int dataSourceId) {
        return dataSourceMap.get(dataSourceId);
    }

    public static void removeDataSource(DataSource dataSource) {
        DataSourceFactory.removeDataSource(dataSource);
        Compute.removeCompute(dataSource.getDataSourceId());
    }

    @Override
    public String toString() {
        return dataSourceId + ": " + sourceDescription + " " + dateTimeStamp;
    }

    public void setCursorToWait() {
        selectFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        selectComponent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public void setCursorToDefault() {
        selectFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        selectComponent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public String getSourceDescription() {
        return sourceDescription;
    }

    public Selection getSelection() {
        return selection;
    }

//--------------------- DISPLAY -------------------------------------

    public boolean createImageDisplay() {
        return createImageDisplay(0);
    }

    public boolean createImageDisplay(int mode) {
        return createImageDisplay(mode, 0);
    }

    public boolean createImageDisplay(int mode, int windowNumber) {
        DataChoice choice;

        if (multiChannelDisplay) {
            DataSelection dataSelection = new MultiDimensionSubset();
            selection.applyToDataSelection(dataSelection);
            choice = selection.getSelectedDataChoice();
            try {
                MultiChannelViewer mcv = new MultiChannelViewer(this, choice, sourceDescription, dateTimeStamp, windowNumber, dataSourceId);
                selectFrame = mcv.getFrame();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        } else if (atmRetrievalDisplay) {
            DataSelection dataSelection = new MultiDimensionSubset();
            selection.applyToDataSelection(dataSelection);
            choice = selection.getSelectedDataChoice();
            try {
                AtmSoundingViewer asv = new AtmSoundingViewer(choice, sourceDescription, dateTimeStamp, windowNumber, dataSourceId);
                asv.setDataChoices(dataSource.getDataChoices());
                selectFrame = asv.getFrame();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }


        boolean imageCreated = false;
        DataSelection dataSelection = new MultiDimensionSubset();


        selection.applyToDataSelection(dataSelection);
        choice = selection.getSelectedDataChoice();
        fldName = selection.getSelectedName();

        doReproject = dataSource.getDoReproject(choice);

        try {
            if (doReproject) {
                nadirResolution = dataSource.getNadirResolution(choice);
            }
        } catch (Exception e) {
            System.out.println("could not determine nadir resolution, using default: 1000m");
        }

        try {
            //- get the data
            FlatField image = (FlatField) dataSource.getData(choice, dataSelection);
            FlatField swathImage = image;

            if (doReproject) {
                if (dataSource.getReduceBowtie(choice)) {
                    String sensorName = dataSource.getSensorName(choice);
                    reduceSwathBowtie(image, sensorName);
                }
            }

            MapProjection mapProj;

            //- reproject
            if (doReproject) {
                float[][] corners = MultiSpectralData.getLonLatBoundingCorners(image.getDomainSet());
                mapProj = getSwathProjection(corners); //TODO: How do we know this is a swath?
                Linear2DSet grid = makeGrid(mapProj, corners, nadirResolution);
                boolean filter = true;
                if (reprojectMode == 0) {
                    if (!dataSource.getDoFilter(choice)) {
                        filter = false;
                    }
                }
                image = ReprojectSwath.swathToGrid(grid, image, reprojectMode, filter);
            } else {
                mapProj = getDataProjection(image);
            }

            if (regionMatch) {
                if (sharedMapProj == null) {
                    sharedMapProj = mapProj;
                } else {
                    mapProj = sharedMapProj;
                }
            }

            ColorTable clrTbl = dataSource.getDefaultColorTable(choice);
            Range range = dataSource.getDefaultColorRange(choice);

            DataSourceInfo datSrcInfo = new DataSourceInfo(sourceDescription, dateTimeStamp, dataSourceId);
            DatasetInfo dsInfo = new DatasetInfo(fldName, nadirResolution, datSrcInfo);

            if (mode == 0 || ImageDisplay.getTarget() == null) {
                //-- make the displayable
                HydraRGBDisplayable imageDsp = makeImageDisplayable(image, range, clrTbl, fldName);
                imageDsp.setUseFastRendering(true);
                displayableToImage.put(imageDsp, swathImage);
                ImageDisplay iDisplay = new ImageDisplay(imageDsp, mapProj, windowNumber, dsInfo);
            } else if (mode == 1) {
                displayableToImage.put((HydraRGBDisplayable) ImageDisplay.getTarget().getReplaceableImageDisplayable(), swathImage);
                ImageDisplay.getTarget().updateImageData(image, clrTbl, mapProj, dsInfo);
            } else if (mode == 2) {
                // TODO: Need to understand why this is necessary when doing 'overlay'.  IDV or VisAD issue?
                image = makeFlatFieldWithUniqueRange(image);
                HydraRGBDisplayable imageDsp = makeImageDisplayable(image, range, clrTbl, fldName);
                imageDsp.setUseFastRendering(true);
                displayableToImage.put(imageDsp, swathImage);
                if (dataSource.getOverlayAsMask(choice)) {
                    imageDsp.addConstantMap(new ConstantMap(1.0, Display.RenderOrderPriority));
                    ImageDisplay.getTarget().addOverlayImage(imageDsp, dsInfo, true);
                } else {
                    ImageDisplay.getTarget().addOverlayImage(imageDsp, dsInfo);
                }
            }

            imageCreated = true;
        } catch (VisADException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return imageCreated;
    }

    public static int[][][] getMouseFunctionMap() {

        boolean isMac = false;
        String os = java.lang.System.getProperty("os.name").toLowerCase();
        if (os.indexOf("mac") >= 0) isMac = true;

        int[][][] map = DisplayMaster.defaultMouseFunctions;
        int[][][] myMap = new int[3][2][2];
        myMap[0][0][0] = map[0][0][0];
        myMap[0][0][1] = map[0][0][1];
        myMap[0][1][0] = map[0][1][0];
        myMap[0][1][1] = map[0][1][1];
        myMap[1][0][0] = map[1][0][0];
        myMap[1][0][1] = map[1][0][1];
        myMap[1][1][0] = map[1][1][0];
        myMap[1][1][1] = map[1][1][1];
        myMap[2][0][0] = map[2][0][0];
        myMap[2][0][1] = map[2][0][1];
        myMap[2][1][0] = map[2][1][0];
        myMap[2][1][1] = map[2][1][1];

        if (isMac) {
            myMap[0][0][1] = MouseHelper.ZOOM;
        }

        return myMap;
    }

    public static float[] minmax(FlatField ffield, EarthLocationTuple[] earthLocs)
            throws VisADException, RemoteException {
        int[] indexes = new int[2];
        float[] minmax = Hydra.minmax(ffield.getFloats(false)[0], indexes);
        Gridded2DSet domSet = (Gridded2DSet) ffield.getDomainSet();
        float[][] grdVal = domSet.indexToValue(new int[]{indexes[0]});
        CoordinateSystem cs = domSet.getCoordinateSystem();
        float[][] lonlat = cs.toReference(grdVal);
        EarthLocationTuple lla0 = new EarthLocationTuple(lonlat[1][0], lonlat[0][0], 0.0);

        grdVal = domSet.indexToValue(new int[]{indexes[1]});
        lonlat = cs.toReference(grdVal);
        EarthLocationTuple lla1 = new EarthLocationTuple(lonlat[1][0], lonlat[0][0], 0.0);

        if (earthLocs != null) {
            earthLocs[0] = lla0;
            earthLocs[1] = lla1;
        }

        return minmax;
    }

    public static float[] minmax(float[] values, int length, int[] indexes) {
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        int minIdx = 0;
        int maxIdx = 0;
        for (int k = 0; k < length; k++) {
            float val = values[k];
            if ((val == val) && (val < Float.POSITIVE_INFINITY) && (val > Float.NEGATIVE_INFINITY)) {
                if (val < min) {
                    min = val;
                    minIdx = k;
                }
                if (val > max) {
                    max = val;
                    maxIdx = k;
                }
            }
        }
        if (indexes != null) {
            indexes[0] = minIdx;
            indexes[1] = maxIdx;
        }
        return new float[]{min, max};
    }

    public static float[] minmax(float[] values, int length) {
        return minmax(values, length, null);
    }

    public static float[] minmax(float[] values, int[] indexes) {
        return minmax(values, values.length, indexes);
    }

    public static float[] minmax(float[] values) {
        return minmax(values, values.length);
    }

    public static double[] minmax(double[] values, int length, int[] indexes) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        int minIdx = 0;
        int maxIdx = 0;
        for (int k = 0; k < length; k++) {
            double val = values[k];
            if ((val == val) && (val < Float.POSITIVE_INFINITY) && (val > Float.NEGATIVE_INFINITY)) {
                if (val < min) {
                    min = val;
                    minIdx = k;
                }
                if (val > max) {
                    max = val;
                    maxIdx = k;
                }
            }
        }
        if (indexes != null) {
            indexes[0] = minIdx;
            indexes[1] = maxIdx;
        }
        return new double[]{min, max};
    }

    public static double[] minmax(double[] values, int length) {
        return minmax(values, length, null);
    }

    public static double[] minmax(double[] values) {
        return minmax(values, values.length);
    }

    public static FieldImpl infiniteToNaN(FieldImpl fltFld) throws VisADException, RemoteException {
        float[][] fvals = fltFld.getFloats(false);
        for (int t = 0; t < fvals.length; t++) {
            for (int i = 0; i < fvals[0].length; i++) {
                float flt = fvals[0][i];
                if (Float.isInfinite(flt)) {
                    fvals[0][i] = Float.NaN;
                }
            }
        }
        return fltFld;
    }

    public static boolean getIsLatLonOrder(FlatField field) throws VisADException, RemoteException {
        boolean isLL = false;
        FunctionType fnc_type = (FunctionType) field.getType();
        RealTupleType rtt = fnc_type.getDomain();
        if (rtt.equals(RealTupleType.LatitudeLongitudeTuple)) {
            isLL = true;
        } else if (!rtt.equals(RealTupleType.SpatialEarth2DTuple)) {
            rtt = fnc_type.getDomain().getCoordinateSystem().getReference();
            if (rtt.equals(RealTupleType.LatitudeLongitudeTuple)) {
                isLL = true;
            }
        }
        return isLL;
    }

    public static FlatField cloneButRangeType(RealType newRange, FlatField ffield, boolean copy) throws VisADException, RemoteException {
        if (newRange == null) {
            newRange = RealType.Generic;
        }
        FunctionType ftype = (FunctionType) ffield.getType();
        Set domainSet = ffield.getDomainSet();
        ftype = new FunctionType(ftype.getDomain(), newRange);
        float[][] rangeValues = ffield.getFloats(false);

        ffield = new FlatField(ftype, domainSet);
        ffield.setSamples(rangeValues, copy);
        return ffield;
    }

    public static RealType makeRealType(String name, String bandName) {
        String newName = null;
        bandName = bandName.replace('.', ',');

        if (name.contains("Reflectance")) {
            newName = "Reflectance_" + bandName;
        } else if (name.contains("BrightnessTemperature") || name.contains("BrightnessTemp")) {
            newName = "BrightnessTemp_" + bandName;
        } else if (name.contains("Radiance")) {
            newName = "Radiance_" + bandName;
        } else {
            return null;
        }

        return RealType.getRealType(newName);
    }

    public static FlatField appendToRangeType(FlatField image, String wavenum) {
        try {
            FunctionType ftype = (FunctionType) image.getType();
            String name = ((RealType) ftype.getRange()).getName();
            RealType rtype = Hydra.makeRealType(name, wavenum);
            if (rtype != null) {
                image = Hydra.cloneButRangeType(rtype, image, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return image;
    }


    public static DecimalFormat getDecimalFormat(double number) {
        double OofM = java.lang.Math.log10(Math.abs(number));
        DecimalFormat numFmt;
        if (OofM <= -4 || OofM >= 4) {
            numFmt = new DecimalFormat("0.00E00");
        } else {
            numFmt = new DecimalFormat();
            numFmt.setMaximumFractionDigits(3);
        }

        return numFmt;

    }

    public static void reduceSwathBowtie(FlatField image, String sensorName) throws VisADException, RemoteException {
        if (sensorName == null) {
            return;
        }
        Linear2DSet domSet = (Linear2DSet) image.getDomainSet();
        float start = (float) domSet.getY().getFirst();
        int fovStart = (int) domSet.getX().getFirst();
        float[][] newRngVals = image.getFloats(true);
        int[] lens = domSet.getLengths();
        int XTrkLen = lens[0];
        int TrkLen = lens[1];

        int numDetectors = 0;

        if (sensorName.equals("MODIS_1KM")) {
            numDetectors = 10;
        } else if (sensorName.equals("MODIS_HKM")) {
            numDetectors = 20;
        } else if (sensorName.equals("MODIS_QKM")) {
            numDetectors = 40;
        } else if (sensorName.equals("MERSI_QKM")) {
            numDetectors = 40;
        } else if (sensorName.equals("MERSI_1KM")) {
            numDetectors = 10;
        } else {
            return;
        }

        switch (sensorName) {
            case "MODIS_1KM":
                for (int j = 0; j < TrkLen; j++) {
                    int detIdx = (j + ((int) start)) % numDetectors;
                    for (int i = 0; i < XTrkLen; i++) {
                        int idx = j * XTrkLen + i;
                        int fov = fovStart + i;
                        if ((fov >= 0 && fov < 250) || (fov < 1354 && fov >= 1104)) {
                            if ((detIdx == 9) || (detIdx == 0)) {
                                newRngVals[0][idx] = Float.NaN;
                            }
                        }
                        if ((fov >= 250 && fov < 340) || (fov >= 1014 && fov <= 1104)) {
                            if ((detIdx == 9)) {
                                newRngVals[0][idx] = Float.NaN;
                            }
                        }
                    }
                }
                break;

            case "MODIS_HKM":
                for (int j = 0; j < TrkLen; j++) {
                    int detIdx = (j + ((int) start)) % numDetectors;
                    for (int i = 0; i < XTrkLen; i++) {
                        int idx = j * XTrkLen + i;
                        int fov = fovStart + i;

                        if ((fov >= 0 && fov < 500) || (fov < 2708 && fov >= 2208)) {
                            if ((detIdx >= 0 && detIdx < 2) || (detIdx >= 18)) {
                                newRngVals[0][idx] = Float.NaN;
                            }
                        }
                        if ((fov >= 500 && fov < 800) || (fov >= 1908 && fov <= 2208)) {
                            if ((detIdx == 0) || (detIdx == 19)) {
                                newRngVals[0][idx] = Float.NaN;
                            }
                        }
                    }
                }
                break;

            case "MODIS_QKM":
                for (int j = 0; j < TrkLen; j++) {
                    int detIdx = (j + ((int) start)) % numDetectors;
                    for (int i = 0; i < XTrkLen; i++) {
                        int idx = j * XTrkLen + i;
                        int fov = fovStart + i;

                        if ((fov >= 0 && fov < 1000) || (fov < 5416 && fov >= 4416)) {
                            if ((detIdx < 3) || (detIdx >= 37)) {
                                newRngVals[0][idx] = Float.NaN;
                            }
                        }
                        if ((fov >= 1000 && fov < 1300) || (fov >= 4116 && fov < 4416)) {
                            if ((detIdx < 2) || (detIdx >= 38)) {
                                newRngVals[0][idx] = Float.NaN;
                            }
                        }
                        if ((fov >= 1300 && fov < 1600) || (fov >= 3816 && fov < 4116)) {
                            if ((detIdx <= 1) || (detIdx >= 38)) {
                                newRngVals[0][idx] = Float.NaN;
                            }
                        }
                        if ((fov >= 1600 && fov < 2600) || (fov >= 2860 && fov < 3816)) {
                            if ((detIdx == 0) || (detIdx == 39)) {
                                newRngVals[0][idx] = Float.NaN;
                            }
                        }
                    }
                }
                break;

            case "MERSI_QKM":
                for (int j = 0; j < TrkLen; j++) {
                    int detIdx = (j + ((int) start)) % numDetectors;
                    for (int i = 0; i < XTrkLen; i++) {
                        int idx = j * XTrkLen + i;
                        int fov = fovStart + i;

                        if ((fov >= 0 && fov < 1024) || (fov < 8192 && fov >= 7168)) {
                            if ((detIdx < 6) || (detIdx > 33)) {
                                newRngVals[0][idx] = Float.NaN;
                            }
                        }
                        if ((fov >= 1024 && fov < 1576) || (fov >= 6516 && fov <= 7168)) {
                            if ((detIdx < 5) || (detIdx > 35)) {
                                newRngVals[0][idx] = Float.NaN;
                            }
                        }
                        if ((fov >= 1576 && fov < 2176) || (fov >= 6016 && fov <= 6516)) {
                            if ((detIdx < 2) || (detIdx > 37)) {
                                newRngVals[0][idx] = Float.NaN;
                            }
                        }
                        if ((fov >= 2176 && fov < 3176) || (fov >= 5016 && fov <= 6016)) {
                            if ((detIdx < 1) || (detIdx > 38)) {
                                newRngVals[0][idx] = Float.NaN;
                            }
                        }
                        if ((fov >= 3176 && fov < 3600) || (fov >= 4416 && fov <= 5016)) {
                            if ((detIdx < 1) || (detIdx >= 40)) {
                                newRngVals[0][idx] = Float.NaN;
                            }
                        }
                    }
                }
                break;
        }

        image.setSamples(newRngVals, false);
    }


    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    public static JFrame createAndShowFrame(final String title, final Component component, final JMenuBar menuBar, final Dimension size, final Point loc, final boolean exitOnClose) {

        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        final JFrame frame = new JFrame(title);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE);

                //JFrame frame = new JFrame(title);

                if (menuBar != null) {
                    frame.setJMenuBar(menuBar);
                }

                String iconPath = getStaticMcv().getProperty(Constants.PROP_APP_ICON, null);
                ImageIcon icon = GuiUtils.getImageIcon(iconPath, getClass(), true);
                frame.setIconImage(icon.getImage());

                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

                //Add content to the window.
                if (component != null) {
                    frame.getContentPane().add(component);
                }

                if (size != null) {
                    frame.setPreferredSize(size);
                }

                if (loc != null) {
                    frame.setLocation(loc.x, loc.y);
                }

                //Display the window.
                frame.pack();
                frame.setVisible(true);
            }
        });

        return frame;
    }

    public static JFrame createAndShowFrame(final String title, final Component component, final Dimension size) {
        return createAndShowFrame(title, component, null, size, null, false);
    }

    public static JFrame createAndShowFrame(final String title, final Component component, final Dimension size, final Point loc) {
        return createAndShowFrame(title, component, null, size, loc, false);
    }

    /**
     * Use this if already on the EDT
     */
    public static JFrame createAndShowFrameFromEDT(final String title, final Component component, final Dimension size, final Point loc) {
        return createAndShowFrameFromEDT(title, component, null, size, loc);
    }

    /**
     * Use this if already on the EDT
     */
    public static JFrame createAndShowFrameFromEDT(final String title, final Component component, final JMenuBar menuBar, final Dimension size, final Point loc) {
        //Turn off metal's use of bold fonts
        UIManager.put("swing.boldMetal", Boolean.FALSE);

        JFrame frame = new JFrame(title);

        if (menuBar != null) {
            frame.setJMenuBar(menuBar);
        }

        //Add content to the window.
        if (component != null) {
            frame.getContentPane().add(component);
        }

        if (size != null) {
            frame.setPreferredSize(size);
        }

        if (loc != null) {
            frame.setLocation(loc.x, loc.y);
        }

        //Display the window.
        frame.pack();
        frame.setVisible(true);
        return frame;
    }

    public static JFrame createAndShowFrame(final String title, final Component component, JMenuBar menuBar, final Dimension size) {
        return createAndShowFrame(title, component, menuBar, size, null, false);
    }

    public static JFrame createAndShowFrame(final String title, final Component component, JMenuBar menuBar, final Dimension size, final Point loc) {
        return createAndShowFrame(title, component, menuBar, size, loc, false);
    }

    public static JFrame createAndShowFrame(final String title, final Component component) {
        return createAndShowFrame(title, component, null, null, null, false);
    }

    public static JFrame createAndShowFrame(final String title, final Component component, JMenuBar menuBar) {
        return createAndShowFrame(title, component, menuBar, null, null, false);
    }

    public static JFrame createAndShowFrame(final String title, final Component component, final boolean exitOnClose) {
        return createAndShowFrame(title, component, null, null, null, exitOnClose);
    }


    public static HydraRGBDisplayable makeImageDisplayable(FlatField image, Range range, ColorTable colorTable, String name) throws VisADException, RemoteException {

        RealType imageRangeType =
                (((FunctionType) image.getType()).getFlatRange().getRealComponents())[0];

        boolean alphaflag = true;
        HydraRGBDisplayable imageDsp = new HydraRGBDisplayable(name, imageRangeType, null, ColorTable.addAlpha(colorTable), colorTable.getName(), alphaflag, range);
        imageDsp.addConstantMap(new ConstantMap(0.0, Display.RenderOrderPriority));
        //imageDsp.addConstantMap(new ConstantMap(1.0, Display.CurvedSize));
        imageDsp.setData(image);

        return imageDsp;
    }

    public static ImageRGBDisplayable makeRGBImageDisplayable(FlatField rgbImage) throws VisADException, RemoteException {
        return makeRGBImageDisplayable(rgbImage, null);
    }

    public static ImageRGBDisplayable makeRGBImageDisplayable(FlatField rgbImage, String name) throws VisADException, RemoteException {
        ImageRGBDisplayable rgbDisplayable = new ImageRGBDisplayable(name, grayTable.getTable(), false, rgbImage);
        rgbDisplayable.addConstantMap(new ConstantMap(0.0, Display.RenderOrderPriority));
        rgbDisplayable.setData(rgbImage);

        return rgbDisplayable;
    }

    /**
     * Takes a FlatField, returns one with a unique RangeType name.
     */

    public static FlatField makeFlatFieldWithUniqueRange(FlatField ffield) throws VisADException, RemoteException {
        FunctionType fncType = (FunctionType) ffield.getType();
        RealTupleType imageDomType = fncType.getDomain();
        RealType[] comps = fncType.getFlatRange().getRealComponents();
        int numRangeComps = comps.length;

        FlatField new_image = null;
        float[][] rngValues = ffield.getFloats(false);

        if (numRangeComps == 1) {
            RealType imageRangeType = ((fncType).getFlatRange().getRealComponents())[0];

            String str = imageRangeType.getName();
            String new_name = str + "_" + numImageDisplays;

            imageRangeType = RealType.getRealType(new_name);
            new_image = new FlatField(new FunctionType(imageDomType, imageRangeType), ffield.getDomainSet());
        } else {
            RealType[] rtypes = new RealType[numRangeComps];
            for (int k = 0; k < rtypes.length; k++) {
                String str = comps[k].getName();
                String new_name = str + "_" + numImageDisplays;
                rtypes[k] = RealType.getRealType(new_name);
            }
            new_image = new FlatField(new FunctionType(imageDomType, new RealTupleType(rtypes)), ffield.getDomainSet());
        }
        new_image.setSamples(rngValues);

        numImageDisplays++;
        return new_image;
    }


    //- TODO:  break out these map routines

    private static UnionSet mapVHRES = null;
    private static UnionSet mapSUPU = null;
    private static UnionSet mapSUPW = null;
    private static UnionSet mapHPOL = null;

    public static void initializeMapBoundaries() {
        try {
            URL mapSource = Hydra.class.getResource("/auxdata/maps/OUTLSUPU");
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapSUPU = mapAdapter.getData();

            mapSource = Hydra.class.getResource("/auxdata/maps/OUTLSUPW");
            mapAdapter = new BaseMapAdapter(mapSource);
            mapSUPW = mapAdapter.getData();

            mapSource = Hydra.class.getResource("/auxdata/maps/OUTLHPOL");
            mapAdapter = new BaseMapAdapter(mapSource);
            mapHPOL = mapAdapter.getData();

            mapSource = Hydra.class.getResource("/resources/geographic/gshhs_h.b");
            GSHHGadapter = new GSHHG(mapSource);

            mapSource = Hydra.class.getResource("/resources/geographic/cia_wdb2-bdy-pby.txt");
            WDBIIadapter = new WorldDataBank(mapSource);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initializeColorTables() {
        float[][] table = null;
        float[][] inv_table = null;
        ArrayList<float[]> colors = new ArrayList<float[]>();

        try {
            URL url = Hydra.class.getResource("/resources/color/rainbow.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

            int clrDim = 3;
            int cnt = 0;
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                StringTokenizer tokens = new StringTokenizer(line);
                if (cnt == 0) {
                    clrDim = tokens.countTokens();
                }
                String red = tokens.nextToken();
                String grn = tokens.nextToken();
                String blu = tokens.nextToken();
                float[] tmp = new float[clrDim];
                tmp[0] = Float.parseFloat(red);
                tmp[1] = Float.parseFloat(grn);
                tmp[2] = Float.parseFloat(blu);
                colors.add(tmp);
                cnt++;
            }
            int numClrs = colors.size();
            table = new float[clrDim][numClrs];
            inv_table = new float[clrDim][numClrs];
            for (int k = 0; k < numClrs; k++) {
                float[] tmp = colors.get(k);
                table[0][k] = tmp[0] / numClrs;
                table[1][k] = tmp[1] / numClrs;
                table[2][k] = tmp[2] / numClrs;
                inv_table[0][(numClrs - 1) - k] = tmp[0] / numClrs;
                inv_table[1][(numClrs - 1) - k] = tmp[1] / numClrs;
                inv_table[2][(numClrs - 1) - k] = tmp[2] / numClrs;
            }

            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        rainbow = new ColorTable("rainbow", "rainbow", table);
        invRainbow = new ColorTable("invRainbow", "invRainbow", inv_table);

        table = new float[3][256];
        table = BaseColorControl.initTableVis5D(table);
        heat = new ColorTable("heat", "heat", table);
    }

    public static MapLines addBaseMapToDisplay(MapProjectionDisplay mapProjDsp, UnionSet set) throws VisADException, RemoteException {
        return addBaseMapToDisplay(mapProjDsp, set, Color.cyan);
    }

    public static MapLines addBaseMapToDisplay(MapProjectionDisplay mapProjDsp, UnionSet set, Color color) throws VisADException, RemoteException {
        MapLines mapLines = new MapLines("maplines");
        //mapLines.setUseFastRendering(true);
        mapLines.addConstantMap(new ConstantMap(5.0, Display.RenderOrderPriority));
        try {
            mapLines.setMapLines(set);
            mapLines.setColor(color);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            System.out.println(excp);
        }

        return mapLines;
    }

    public static void addBaseMap(MapProjectionDisplayJ3D mapProjDsp) throws VisADException, RemoteException {
        ArrayList<MapLines> baseMap = new ArrayList<MapLines>();

        UnionSet local = new UnionSet(mapSUPU.getSets());
        baseMap.add(addBaseMapToDisplay(mapProjDsp, local));

        local = new UnionSet(mapHPOL.getSets());
        baseMap.add(addBaseMapToDisplay(mapProjDsp, local));

        local = new UnionSet(mapSUPW.getSets());
        baseMap.add(addBaseMapToDisplay(mapProjDsp, local));
    }

    public static ArrayList addBaseMapVHRES(MapProjectionDisplayJ3D mapProjDsp) throws VisADException, RemoteException {
        ArrayList<MapLines> baseMap = new ArrayList<MapLines>();

        WDBIIadapter.setRegion(-90f, 90f, -180f, 180f);
        baseMap.add(addBaseMapToDisplay(mapProjDsp, WDBIIadapter.getData()));

        setRegion(mapProjDsp.getMapProjection());
        UnionSet mapVHRES = GSHHGadapter.getData();
        baseMap.add(addBaseMapToDisplay(mapProjDsp, mapVHRES));

        return baseMap;
    }

    public static void updateBaseMapVHRES(ArrayList<MapLines> mapList, MapProjection mapProj) throws VisADException, RemoteException {
        setRegion(mapProj);
        mapList.get(1).setData(GSHHGadapter.getData());
    }

    public static void setRegion(MapProjection mapProj) throws VisADException, RemoteException {
        Rectangle2D rect = mapProj.getDefaultMapArea();
        double x = rect.getX();
        double w = rect.getWidth();
        double y = rect.getY();
        double h = rect.getHeight();

        // traverse projection box perimeter to determine lon/lat range.
        int npts = 40;
        float[][] gringLonLat = new float[2][npts * 4];

        int cnt = 0;
        double[] leftUp = new double[]{x, y + h};
        double[] leftDn = new double[]{x, y};
        double[] rghtDn = new double[]{x + w, y};
        double[] rghtUp = new double[]{x + w, y + h};
        double[] cntrUp = new double[]{leftUp[0] + w / 2, leftUp[1]};

        double[][] xy = new double[2][1];
        double delx = (leftDn[0] - leftUp[0]) / (npts);
        double dely = (leftDn[1] - leftUp[1]) / (npts);
        for (int k = 0; k < npts; k++) {
            xy[0][0] = leftUp[0] + k * delx;
            xy[1][0] = leftUp[1] + k * dely;
            gringLonLat[0][cnt] = (float) mapProj.getLatLon(xy).getLongitude().getValue();
            gringLonLat[1][cnt] = (float) mapProj.getLatLon(xy).getLatitude().getValue();
            cnt++;
        }

        delx = (rghtDn[0] - leftDn[0]) / (npts);
        dely = (rghtDn[1] - leftDn[1]) / (npts);
        for (int k = 0; k < npts; k++) {
            xy[0][0] = leftDn[0] + k * delx;
            xy[1][0] = leftDn[1] + k * dely;
            gringLonLat[0][cnt] = (float) mapProj.getLatLon(xy).getLongitude().getValue();
            gringLonLat[1][cnt] = (float) mapProj.getLatLon(xy).getLatitude().getValue();
            cnt++;
        }

        delx = (rghtUp[0] - rghtDn[0]) / (npts);
        dely = (rghtUp[1] - rghtDn[1]) / (npts);
        for (int k = 0; k < npts; k++) {
            xy[0][0] = rghtDn[0] + k * delx;
            xy[1][0] = rghtDn[1] + k * dely;
            gringLonLat[0][cnt] = (float) mapProj.getLatLon(xy).getLongitude().getValue();
            gringLonLat[1][cnt] = (float) mapProj.getLatLon(xy).getLatitude().getValue();
            cnt++;
        }

        delx = (leftUp[0] - rghtUp[0]) / (npts);
        dely = (leftUp[1] - rghtUp[1]) / (npts);
        for (int k = 0; k < npts; k++) {
            xy[0][0] = rghtUp[0] + k * delx;
            xy[1][0] = rghtUp[1] + k * dely;
            gringLonLat[0][cnt] = (float) mapProj.getLatLon(xy).getLongitude().getValue();
            gringLonLat[1][cnt] = (float) mapProj.getLatLon(xy).getLatitude().getValue();
            cnt++;
        }
        float[] lonlohi = Hydra.minmax(gringLonLat[0]);
        float[] latlohi = Hydra.minmax(gringLonLat[1]);

        float latMin = latlohi[0];
        float latMax = latlohi[1];
        float lonWest = lonlohi[0];
        float lonEast = lonlohi[1];


        LatLonPoint llp_leftUp = mapProj.getLatLon(new double[][]{{leftUp[0]}, {leftUp[1]}});
        LatLonPoint llp_leftDn = mapProj.getLatLon(new double[][]{{leftDn[0]}, {leftDn[1]}});
        LatLonPoint llp_rghtUp = mapProj.getLatLon(new double[][]{{rghtUp[0]}, {rghtUp[1]}});
        LatLonPoint llp_rghtDn = mapProj.getLatLon(new double[][]{{rghtDn[0]}, {rghtDn[1]}});
        LatLonPoint llp_cntrUp = mapProj.getLatLon(new double[][]{{cntrUp[0]}, {cntrUp[1]}});

        boolean anyCornerMissing = false;

        float latA = (float) (llp_leftUp.getLatitude()).getValue();
        float latB = (float) (llp_leftDn.getLatitude()).getValue();
        float latC = (float) (llp_rghtDn.getLatitude()).getValue();
        float latD = (float) (llp_rghtUp.getLatitude()).getValue();

        anyCornerMissing = Float.isNaN(latA) || Float.isNaN(latB) || Float.isNaN(latC) || Float.isNaN(latD);

        float lonA = (float) (llp_leftUp.getLongitude()).getValue();
        float lonB = (float) (llp_leftDn.getLongitude()).getValue();
        float lonC = (float) (llp_rghtDn.getLongitude()).getValue();
        float lonD = (float) (llp_rghtUp.getLongitude()).getValue();

        anyCornerMissing = Float.isNaN(lonA) || Float.isNaN(lonB) || Float.isNaN(lonC) || Float.isNaN(lonD);

        float latCntr = (float) (llp_cntrUp.getLatitude()).getValue();
        if (latA > 0 && latB > 0 && latC > 0 && latD > 0) {
            if (latCntr > latMax) latMax = 90;
        }

        int numCrossed = 0;

        // TODO: implement pole logic if 1 to 3 corners off Earth, for now skip...
        //       assumes pole not contained in geotationary projection.
        if (!anyCornerMissing) {
            if (crossesGreenwich(leftUp, leftDn, rghtDn, rghtUp, lonA, lonB, lonC, lonD, 0, mapProj)) numCrossed++;
            if (crossesGreenwich(leftUp, leftDn, rghtDn, rghtUp, lonA, lonB, lonC, lonD, 1, mapProj)) numCrossed++;
            if (crossesGreenwich(leftUp, leftDn, rghtDn, rghtUp, lonA, lonB, lonC, lonD, 2, mapProj)) numCrossed++;
            if (crossesGreenwich(leftUp, leftDn, rghtDn, rghtUp, lonA, lonB, lonC, lonD, 3, mapProj)) numCrossed++;
        }

        // if GM crossed zero or two, then  box does not contain North or South Pole
        if (numCrossed != 1 || numCrossed == 0) {
            GSHHGadapter.setRegion(latMin, latMax, lonWest, lonEast);
        } else { // if GM crosses only one box side then box contains North or South Pole
            if (latMin < 0) latMin = -90f;
            if (latMin > 0) latMax = 90f;
            GSHHGadapter.setRegion(latMin, latMax);
        }
    }

    public static boolean crossesGreenwich(double[] leftUp, double[] leftDn, double[] rghtDn, double[] rghtUp,
                                           double lonA, double lonB, double lonC, double lonD,
                                           int which, MapProjection mapProj) throws VisADException {
        double itrvl = 0;
        double[] pt = null;
        double[] testPt = new double[2];
        LatLonPoint llpt;
        double lon;
        double delLon = 0;

        if (lonA < 0) lonA += 360;
        if (lonB < 0) lonB += 360;
        if (lonC < 0) lonC += 360;
        if (lonD < 0) lonD += 360;

        if (which == 0) {
            itrvl = (leftDn[1] - leftUp[1]) / 20;
            pt = leftUp;
            testPt[0] = leftUp[0];
            testPt[1] = leftUp[1] + itrvl;
            delLon = lonB - lonA;
        } else if (which == 1) {
            itrvl = (rghtDn[0] - leftDn[0]) / 20;
            pt = leftDn;
            testPt[0] = leftDn[0] + itrvl;
            testPt[1] = leftDn[1];
            delLon = lonC - lonB;
        } else if (which == 2) {
            itrvl = (rghtUp[1] - rghtDn[1]) / 20;
            pt = rghtDn;
            testPt[0] = rghtDn[0];
            testPt[1] = rghtDn[1] + itrvl;
            delLon = lonD - lonC;
        } else if (which == 3) {
            itrvl = (leftUp[0] - rghtUp[0]) / 20;
            pt = rghtUp;
            testPt[0] = rghtUp[0] + itrvl;
            testPt[1] = rghtUp[1];
            delLon = lonA - lonD;
        }

        llpt = mapProj.getLatLon(new double[][]{{pt[0]}, {pt[1]}});
        lon = llpt.getLongitude().getValue();

        llpt = mapProj.getLatLon(new double[][]{{testPt[0]}, {testPt[1]}});
        double testLon = llpt.getLongitude().getValue();

        if (lon < 0) lon += 360;
        if (testLon < 0) testLon += 360;

        if (delLon > 0) {
            if (testLon < lon) return true;
        } else {
            if (testLon > lon) return true;
        }

        return false;
    }

    public static MapProjection getSwathProjection(float[][] corners) throws VisADException {
        MapProjection mp = new LambertAEA(corners);
        return mp;
    }

    public static Linear2DSet makeGrid(MapProjection mp, float[][] corners, float res) throws Exception {
        float[][] xy = mp.fromReference(corners);

        float min_x = Float.MAX_VALUE;
        float min_y = Float.MAX_VALUE;
        float max_x = -Float.MAX_VALUE;
        float max_y = -Float.MAX_VALUE;

        for (int k = 0; k < xy[0].length; k++) {
            if (xy[0][k] < min_x) min_x = xy[0][k];
            if (xy[1][k] < min_y) min_y = xy[1][k];
            if (xy[0][k] > max_x) max_x = xy[0][k];
            if (xy[1][k] > max_y) max_y = xy[1][k];
        }

        RealType xmap = RealType.getRealType("xmap", CommonUnit.meter);
        RealType ymap = RealType.getRealType("ymap", CommonUnit.meter);

        RealTupleType rtt = new visad.RealTupleType(xmap, ymap, mp, null);

        min_x = ((int) (min_x / res)) * res;
        max_x = ((int) (max_x / res)) * res;
        min_y = ((int) (min_y / res)) * res;
        max_y = ((int) (max_y / res)) * res;

        float del_x = max_x - min_x;
        float del_y = max_y - min_y;

        int xLen = (int) (del_x / res);
        int yLen = (int) (del_y / res);

        Linear2DSet grid = new Linear2DSet(rtt, min_x, min_x + (xLen - 1) * res, xLen,
                min_y, min_y + (yLen - 1) * res, yLen);

        return grid;
    }

    public static MapProjection getDataProjection(FlatField image) throws VisADException, RemoteException {
        MapProjection mp = null;
        //- get MapProjection from incoming image.  If none, use default method
        FunctionType fnc_type = (FunctionType) image.getType();
        RealTupleType rtt = fnc_type.getDomain();
        CoordinateSystem cs = rtt.getCoordinateSystem();
        Set domainSet = image.getDomainSet();

        if (cs instanceof visad.CachingCoordinateSystem) {
            cs = ((visad.CachingCoordinateSystem) cs).getCachedCoordinateSystem();
        }

        if (cs instanceof MapProjection) {
            return (MapProjection) cs;
        } else if (cs instanceof LongitudeLatitudeCoordinateSystem) {
            //- get approximate center lon,lat
            int[] lens = ((GriddedSet) domainSet).getLengths();
            float[][] center = ((GriddedSet) domainSet).gridToValue(new float[][]{{lens[0] / 2}, {lens[1] / 2}});
            center = cs.toReference(center);
            float[][] corners = MultiSpectralData.getLonLatBoundingCorners(image.getDomainSet());
            try {
                mp = new LambertAEA(corners, center[0][0], center[1][0]);
            } catch (Exception e) {
                System.out.println(" getDataProjection" + e);
            }
            return mp;
        }

        float minLon = Float.NaN;
        float minLat = Float.NaN;
        float delLon = Float.NaN;
        float delLat = Float.NaN;

        if (domainSet instanceof LinearLatLonSet) {
            MathType type0 = ((SetType) domainSet.getType()).getDomain().getComponent(0);
            int latI = RealType.Latitude.equals(type0) ? 0 : 1;
            int lonI = (latI == 1) ? 0 : 1;

            float[] min = ((LinearLatLonSet) domainSet).getLow();
            float[] max = ((LinearLatLonSet) domainSet).getHi();
            minLon = min[lonI];
            minLat = min[latI];
            delLon = max[lonI] - min[lonI];
            delLat = max[latI] - min[latI];

            try {
                mp = new TrivialMapProjection(RealTupleType.SpatialEarth2DTuple,
                        new Rectangle2D.Float(minLon, minLat, delLon, delLat));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return mp;
        } else if (domainSet instanceof Gridded2DSet) {
            rtt = ((SetType) domainSet.getType()).getDomain();
            rtt = RealTupleType.SpatialEarth2DTuple;
            if (!(rtt.equals(RealTupleType.SpatialEarth2DTuple) || rtt.equals(RealTupleType.LatitudeLongitudeTuple))) {
                minLon = -180f;
                minLat = -90f;
                delLon = 360f;
                delLat = 180f;
            } else {
                int latI = rtt.equals(RealTupleType.SpatialEarth2DTuple) ? 1 : 0;
                int lonI = (latI == 1) ? 0 : 1;

                float[] min = ((Gridded2DSet) domainSet).getLow();
                float[] max = ((Gridded2DSet) domainSet).getHi();
                minLon = min[lonI];
                minLat = min[latI];
                delLon = max[lonI] - min[lonI];
                delLat = max[latI] - min[latI];
            }
        }

        try {
            mp = new TrivialMapProjection(RealTupleType.SpatialEarth2DTuple,
                    new Rectangle2D.Float(minLon, minLat, delLon, delLat));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mp;
    }

    public static String makeKML(double south, double north, double west, double east, String kmlPath, String imagePath) {
        Object obj = new Object();
        URL url = obj.getClass().getResource("/resources/hydra.kml");
        SAXBuilder builder = new SAXBuilder();
        Document doc = null;

        try {
            doc = builder.build(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();

        List list = root.getChildren();

        List parms = ((Element) list.get(0)).getChildren();

        Element icon = (Element) parms.get(2);
        Element href = (Element) (icon.getChildren()).get(0);
        href.setText(imagePath);

        Element latlonbox = (Element) parms.get(3);
        List vals = latlonbox.getChildren();

        org.jdom2.Element elemN = (Element) vals.get(0);
        org.jdom2.Element elemW = (Element) vals.get(1);
        org.jdom2.Element elemS = (Element) vals.get(2);
        org.jdom2.Element elemE = (Element) vals.get(3);

        elemN.setText((Double.valueOf(north)).toString());
        elemW.setText((Double.valueOf(west)).toString());
        elemS.setText((Double.valueOf(south)).toString());
        elemE.setText((Double.valueOf(east)).toString());

        XMLOutputter xmlOut = new XMLOutputter();
        String newStr = xmlOut.outputString(doc);

        try {
            File file = new File(kmlPath);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(newStr.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        }

        return newStr;
    }

    public static ConstantMap[] makeConstantMapArray(ConstantMap[] cmaps, ConstantMap cmap) {
        ConstantMap[] constantMaps = new ConstantMap[cmaps.length + 1];
        for (int k = 0; k < cmaps.length; k++) {
            constantMaps[k] = cmaps[k];
        }
        constantMaps[cmaps.length] = cmap;
        return constantMaps;
    }

    public static ConstantMap[] makeColorMap(final Color color)
            throws VisADException, RemoteException {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;
        return new ConstantMap[]{new ConstantMap(r, Display.Red),
                new ConstantMap(g, Display.Green),
                new ConstantMap(b, Display.Blue),
                new ConstantMap(a, Display.Alpha)};
    }

    public static final float[][] grayTable(int numColors, boolean inverse)
            throws IllegalArgumentException {

        float[][] table = new float[3][numColors];

        float scale = (float) (1.0f / (float) (numColors - 1));
        for (int i = 0; i < numColors; i++) {
            float a = (inverse)
                    ? ((float) numColors - i)
                    : ((float) i);
            table[0][i] = a * scale;  // Red amount
            table[1][i] = a * scale;  // Green
            table[2][i] = a * scale;  // Blue
        }
        return table;
    }

    DecimalFormat getValueDisplayFormat(double maxValue) {
        DecimalFormat numFmt;

        //TODO: this is pretty kludgy but will do for now.
        double OofM = java.lang.Math.log10(Math.abs(maxValue));
        if (OofM <= -2 || OofM >= 4) {
            numFmt = new DecimalFormat("0.00E00");
        } else {
            numFmt = new DecimalFormat();
            numFmt.setMaximumFractionDigits(2);
        }

        return numFmt;
    }


    public static void setRegionMatching(boolean on) {
        regionMatch = on;
    }

    public static boolean getRegionMatching() {
        return regionMatch;
    }

    public static void resetSharedProjection() {
        sharedMapProj = null;
    }

    public static void setReprojectMode(int mode) {
        reprojectMode = mode;
    }

    public static int getReprojectMode() {
        return reprojectMode;
    }

    public static void setDoParallel(boolean enable) {
        doParallel = enable;
        ReprojectSwath.setDoParallel(enable);
    }

    public static boolean getDoParallel() {
        return doParallel;
    }

    public static boolean getReplicateCompute() {
        return replicateCompute;
    }

    public static void setReplicateCompute(boolean flag) {
        replicateCompute = flag;
    }

    public static void setTextSizeFactor(float factor) {
        textSizeFactor = factor;
    }

    public static float getTextSizeFactor() {
        return textSizeFactor;
    }

    public static void setFontSize(int size) {
        fontSize = size;
    }

    public static int getFontSize() {
        return fontSize;
    }

    public synchronized static int getUniqueID() {
        uniqueID++;
        return uniqueID;
    }

    public static double azimuthOnSphere(double lonA, double latA, double lonB, double latB) {
        double degToRad = Math.PI / 180;
        double radToDeg = 180 / Math.PI;
        double arc = greatCircleAngle(lonA, latA, lonB, latB);
        if (arc > 90) {
            return Double.NaN;
        }

        double delLon = lonB - lonA;
        double delLat = latB - latA;

        double azimuth = Math.atan(Math.sin((lonB - lonA) * degToRad) / (Math.cos(latA * degToRad) * Math.tan(latB * degToRad) - Math.sin(latA * degToRad) * Math.cos((lonB - lonA) * degToRad))) * radToDeg;

        // Force to (0 - 360) range, clockwise from North
        if (delLon > 0) {
            if (azimuth < 0) {
                azimuth += 180;
            }
        } else if (delLon < 0) {
            if (azimuth > 0) {
                azimuth += 180;
            } else {
                azimuth += 360;
            }
        } else {
            if (delLat >= 0) {
                azimuth = 0;
            } else {
                azimuth = 180;
            }
        }

        /* This handles spanning dateline */
        if (lonA < 0 && lonB > 0) {
            azimuth += 180;
        } else if (lonA > 0 && lonB < 0) {
            azimuth -= 180;
        }

        return azimuth;
    }

    public static double geoSatZenithOnSphere(double lon, double lat, double satLon) {
        double degToRad = Math.PI / 180;
        double radius = 6370; //km
        double satAlt = 35800; //km

        double a = radius;
        double b = satAlt + radius;

        double arc = greatCircleAngle(lon, lat, satLon, 0);

        double c = Math.sqrt(a * a + b * b - 2 * a * b * Math.cos(arc * degToRad));

        double A = Math.asin(Math.sin(arc * degToRad) * a / c) / degToRad;

        double B = 180 - arc - A;

        double zenith = 180 - B;

        return zenith;
    }

    public static double greatCircleAngle(double lonA, double latA, double lonB, double latB) {
        double degToRad = Math.PI / 180;
        lonA *= degToRad;
        latA *= degToRad;
        lonB *= degToRad;
        latB *= degToRad;

        double S = Math.sin((latA - latB) / 2);
        double Spow2 = S * S;

        double T = Math.sin((lonA - lonB) / 2);
        double Tpow2 = T * T;

        double arc = 2 * Math.asin(Math.sqrt(Spow2 + Math.cos(latA) * Math.cos(latB) * Tpow2));

        return arc / degToRad;
    }

    /**
     * @param T     (SxT) right-handed in the plane. Must be 3D unit vectors
     * @param S
     * @param P     Origin, can be null
     * @param V     The 2D (in S,T coordinates) vector to rotate
     * @param theta Counter-clockwise rotation in the S,T plane (degrees)
     * @param rotV  The rotated vector, can be null
     * @return The rotated 3D vector
     */
    public static double[] getRotatedVecInPlane(double[] T, double[] S, double[] P, double[] V, double theta, double[] rotV) {
        theta *= Math.PI / 180;
        if (rotV == null) rotV = new double[3];
        if (P == null) P = new double[]{0, 0, 0};

        double s = V[0] * Math.cos(theta) - V[1] * Math.sin(theta); // x
        double t = V[0] * Math.sin(theta) + V[1] * Math.cos(theta); // y

        double x = P[0] + s * S[0] + t * T[0];
        double y = P[1] + s * S[1] + t * T[1];
        double z = P[2] + s * S[2] + t * T[2];

        rotV[0] = x;
        rotV[1] = y;
        rotV[2] = z;

        return rotV;
    }

    /**
     * @param elevation (degrees)
     * @param azimuth   (degrees)
     * @return Unit  vector in direction of (azimuth, elevation)
     */
    public static double[] azimElevToVec(double azimuth, double elevation) {
        double[] T = new double[]{0, 1, 0};
        double[] S = new double[]{1, 0, 0};
        double[] V = new double[]{0, 1};
        double[] vec = getRotatedVecInPlane(T, S, null, V, azimuth, null);

        double[] Z = new double[]{0, 0, 1};
        V = new double[]{1, 0};

        vec = getRotatedVecInPlane(Z, vec, null, V, elevation, null);

        double mag = Math.sqrt(vec[0] * vec[0] + vec[1] * vec[1] + vec[2] * vec[2]);
        vec[0] /= mag;
        vec[1] /= mag;
        vec[2] /= mag;

        return vec;
    }

    public static double AdotB(double[] A, double[] B) {
        double ab = A[0] * B[0] + A[1] * B[1] + A[2] * B[2];
        return ab;
    }

    public static double vecMag(double[] vec) {
        double x = vec[0];
        double y = vec[1];
        double z = vec[2];
        return Math.sqrt(x * x + y * y + z * z);
    }

    public static double getAngleBetweenVectors(double[] uvecA, double[] uvecB) {
        double theta = Math.acos(AdotB(uvecA, uvecB));
        theta *= 180 / Math.PI;
        return theta;
    }

    public static void writeImage(FlatField image, String name, String dateTimeStamp) throws IOException, VisADException, InvalidRangeException {
        FileSystemView fsv = FileSystemView.getFileSystemView();
        File homeDir = fsv.getHomeDirectory();
        dateTimeStamp = dateTimeStamp.replace(' ', 'T');
        name = name.substring(2, name.length());
        String filename = name + "_" + dateTimeStamp + ".nc";
        filename = homeDir.getAbsolutePath() + "/" + filename;
        NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filename, null);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date time = null;
        try {
            time = format.parse(dateTimeStamp);
        } catch (Exception e) {
            e.printStackTrace();
        }


        Gridded2DSet domSet = (Gridded2DSet) image.getDomainSet();
        int[] lens = domSet.getLengths();
        int xLen = lens[0];
        int yLen = lens[1];

        float[] rngVals = (image.getFloats(false))[0];

        RealTupleType domain = ((FunctionType) image.getType()).getDomain();
        CoordinateSystem cs = domain.getCoordinateSystem();
        boolean geo = false;
        double satLon = Double.NaN;
        if (cs instanceof GEOSProjection) {
            geo = true;
            satLon = ((GEOSProjection) cs).getSatelliteNadirLongitude();
        }

        SunRelativePosition calculator = new SunRelativePosition();

        float[] lons = new float[xLen * yLen];
        float[] lats = new float[xLen * yLen];
        float[] solzen = new float[xLen * yLen];
        float[] solazm = new float[xLen * yLen];

        float[] satzen = null;
        float[] satazm = null;
        float[] phaseAng = null;
        if (geo) {
            satzen = new float[xLen * yLen];
            satazm = new float[xLen * yLen];
            phaseAng = new float[xLen * yLen];
        }

        int[] idx = new int[1];
        for (int j = 0; j < yLen; j++) {
            for (int i = 0; i < xLen; i++) {
                int k = j * xLen + i;
                idx[0] = k;
                float[][] grdVal = domSet.indexToValue(idx);

                float[][] lonlat = cs.toReference(grdVal);
                lons[k] = lonlat[0][0];
                lats[k] = lonlat[1][0];
                calculator.setDate(time);
                calculator.setCoordinate(lons[k], lats[k]);
                solzen[k] = (float) calculator.getZenith();
                solazm[k] = (float) calculator.getAzimuth();

                if (geo) {
                    satzen[k] = (float) geoSatZenithOnSphere(lons[k], lats[k], satLon);
                    satazm[k] = (float) azimuthOnSphere(lons[k], lats[k], satLon, 0);
                    double[] solV = azimElevToVec(-solazm[k], 90 - solzen[k]);
                    double[] satV = azimElevToVec(-satazm[k], 90 - satzen[k]);
                    phaseAng[k] = (float) getAngleBetweenVectors(solV, satV);
                }
            }
        }

        ucar.nc2.Dimension xDim = writer.addDimension(null, "x", xLen);
        ucar.nc2.Dimension yDim = writer.addDimension(null, "y", yLen);

        List<ucar.nc2.Dimension> dims = new ArrayList();
        dims.add(yDim);
        dims.add(xDim);

        Variable valVar = writer.addVariable(null, name, DataType.FLOAT, dims);
        Variable lonVar = writer.addVariable(null, "longitude", DataType.FLOAT, dims);
        Variable latVar = writer.addVariable(null, "latitude", DataType.FLOAT, dims);
        Variable solzenVar = writer.addVariable(null, "solarZenith", DataType.FLOAT, dims);
        Variable solazmVar = writer.addVariable(null, "solarAzimuth", DataType.FLOAT, dims);

        Variable satzenVar = null;
        Variable satazmVar = null;
        Variable phaseAngVar = null;
        if (geo) {
            satzenVar = writer.addVariable(null, "satelliteZenith", DataType.FLOAT, dims);
            satazmVar = writer.addVariable(null, "satelliteAzimuth", DataType.FLOAT, dims);
            phaseAngVar = writer.addVariable(null, "phaseAngle", DataType.FLOAT, dims);
        }

        writer.create();

        Array data = Array.factory(DataType.FLOAT, new int[]{yLen, xLen}, rngVals);
        writer.write(valVar, data);

        data = Array.factory(DataType.FLOAT, new int[]{yLen, xLen}, lons);
        writer.write(lonVar, data);

        data = Array.factory(DataType.FLOAT, new int[]{yLen, xLen}, lats);
        writer.write(latVar, data);

        data = Array.factory(DataType.FLOAT, new int[]{yLen, xLen}, solzen);
        writer.write(solzenVar, data);

        data = Array.factory(DataType.FLOAT, new int[]{yLen, xLen}, solazm);
        writer.write(solazmVar, data);

        if (geo) {
            data = Array.factory(DataType.FLOAT, new int[]{yLen, xLen}, satzen);
            writer.write(satzenVar, data);

            data = Array.factory(DataType.FLOAT, new int[]{yLen, xLen}, satazm);
            writer.write(satazmVar, data);

            data = Array.factory(DataType.FLOAT, new int[]{yLen, xLen}, phaseAng);
            writer.write(phaseAngVar, data);
        }

        writer.close();
    }


    /**
     * Locates sun terminator (if one exists) on each scan line of image
     *
     * @param image (numScans, numPixels) -> observation range (numScans slowest varying)
     * @param time  nominal time for the satellite image
     * @return pixel (elem) index (-1 if no terminator) from scan line 0 to numScans-1
     * @throws VisADException
     */
    public static int[] findSolarTerminator(FlatField image, Date time) throws VisADException {
        SunRelativePosition calculator = new SunRelativePosition();

        Gridded2DSet domSet = null;
        try {
            domSet = (Gridded2DSet) image.getDomainSet();
        } catch (ClassCastException exc) {
            throw new VisADException("image DomainSet must be Gridded2D");
        }

        RealTupleType domain = ((FunctionType) image.getType()).getDomain();
        CoordinateSystem coordSys = domain.getCoordinateSystem();
        RealTupleType reference = coordSys.getReference();

        int lonIdx = 0;
        int latIdx = 1;
        if (reference.equals(RealTupleType.SpatialEarth2DTuple)) {
        } else if (reference.equals(RealTupleType.LatitudeLongitudeTuple)) {
            lonIdx = 1;
            latIdx = 0;
        } else {
            throw new VisADException("Must be earth navigated domain");
        }

        int[] lens = domSet.getLengths();
        int numScans = lens[1]; // N-S
        int numElems = lens[0]; // W-E

        int[] termIdxs = new int[numScans];

        for (int j = 0; j < numScans; j++) {
            // This section initializes start indexes to navigatable locations
            int leftElemIdx = 0;
            int rghtElemIdx = numElems - 1;

            while (leftElemIdx < numElems) {
                int k = j * numScans + leftElemIdx;
                float[][] grdVal = domSet.indexToValue(new int[]{k});
                float[][] lonlat = coordSys.toReference(grdVal);
                if (Float.isNaN(lonlat[0][0])) {
                    leftElemIdx++;
                } else {
                    break;
                }
            }

            while (rghtElemIdx > 0) {
                int k = j * numScans + rghtElemIdx;
                float[][] grdVal = domSet.indexToValue(new int[]{k});
                float[][] lonlat = coordSys.toReference(grdVal);
                if (Float.isNaN(lonlat[0][0])) {
                    rghtElemIdx--;
                } else {
                    break;
                }
            }
            //-----------------------------------------------------------------

            // Find the pixel (elem) index on the jth scan line.
            int zIdx = getZeroIndex(leftElemIdx, rghtElemIdx, j, domSet, coordSys, lonIdx, latIdx, calculator, time);
            termIdxs[j] = zIdx;
        }

        return termIdxs;
    }

    // Helper function for findSolarTerminator: analyzes an individual scan line
    private static int getZeroIndex(int leftIdx, int rghtIdx, int scanLine, Gridded2DSet domSet, CoordinateSystem coordSys, int lonIdx, int latIdx, SunRelativePosition calculator, Date time) throws VisADException {

        int[] lens = domSet.getLengths();
        int numElems = lens[0];

        calculator.setDate(time); // once per scan

        int k = scanLine * numElems + leftIdx;
        float[][] grdVal = domSet.indexToValue(new int[]{k});
        float[][] lonlat = coordSys.toReference(grdVal);
        calculator.setCoordinate(lonlat[lonIdx][0], lonlat[latIdx][0]);
        double leftZen = calculator.getZenith();

        k = scanLine * numElems + rghtIdx;
        grdVal = domSet.indexToValue(new int[]{k});
        lonlat = coordSys.toReference(grdVal);
        calculator.setCoordinate(lonlat[lonIdx][0], lonlat[latIdx][0]);
        double rghtZen = calculator.getZenith();

        boolean leftPos = (leftZen > 0 && leftZen < 90 && !Double.isNaN(leftZen));
        boolean rghtPos = (rghtZen > 0 && rghtZen < 90 && !Double.isNaN(rghtZen));

        if (leftPos && rghtPos) {
            return -1;
        }

        int diff = rghtIdx - leftIdx;

        while (diff > 2) {
            int cntrIdx = leftIdx + diff / 2;
            k = scanLine * numElems + cntrIdx;
            grdVal = domSet.indexToValue(new int[]{k});
            lonlat = coordSys.toReference(grdVal);
            calculator.setCoordinate(lonlat[lonIdx][0], lonlat[latIdx][0]);
            double cntrZen = calculator.getZenith();
            boolean cntrPos = (cntrZen > 0 && cntrZen < 90 && !Double.isNaN(cntrZen));

            if (cntrPos) {
                if (leftPos) {
                    leftIdx = cntrIdx;
                } else if (rghtPos) {
                    rghtIdx = cntrIdx;
                }
            } else {
                if (!leftPos) {
                    leftIdx = cntrIdx;
                } else if (!rghtPos) {
                    rghtIdx = cntrIdx;
                }
            }

            diff = rghtIdx - leftIdx;
        }

        return leftIdx + 1;
    }
}
