/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
 *
 * All Rights Reserved
 *
 * McIDAS-V is built on Unidata's IDV, as well as SSEC's VisAD and HYDRA
 * projects. Parts of McIDAS-V source code are based on IDV, VisAD, and
 * HYDRA source code.
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

package edu.wisc.ssec.hydra.data;

import edu.wisc.ssec.adapter.GOESGridAdapter;
import edu.wisc.ssec.hydra.GEOSProjection;
import edu.wisc.ssec.hydra.GEOSTransform;

import edu.wisc.ssec.adapter.NetCDFFile;
import edu.wisc.ssec.adapter.HDFArray;
import edu.wisc.ssec.adapter.MultiDimensionAdapter;
import edu.wisc.ssec.adapter.MultiDimensionReader;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.adapter.SpectrumAdapter;
import edu.wisc.ssec.adapter.SwathAdapter;

import java.io.File;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.rmi.RemoteException;
import java.util.Date;

import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.unidata.util.Range;

import visad.Data;
import visad.FlatField;
import visad.VisADException;
import visad.georef.MapProjection;

public class GEOSDataSource extends DataSource {

    NetCDFFile reader;
    ArrayList<Variable> projVarList = new ArrayList<>();
    ArrayList<Variable> varsWithProj = new ArrayList<>();
    HashMap<String, Variable> projXCoordVars = new HashMap<>();
    HashMap<String, Variable> projYCoordVars = new HashMap<>();
    HashMap<String, Variable> timeCoordVars = new HashMap<>();

    private ArrayList<GOESGridAdapter> adapters = new ArrayList<>();

    private MultiSpectralData msd;

    HashMap<DataChoice, MultiSpectralData> msdMap = new HashMap();

    String bandName;

    float centerWavelength;

    String sensorName;

    DataGroup cat;

    double default_stride = 10;

    boolean unpack = false;

    String dateTimeStamp;

    Date dateTime;

    String description;

    boolean zeroBased;

    boolean isMultiSpectral = true;

    String prodStr = null;

//  public GEOSDataSource(File file) {
//     this(file, 10);
//  }

    public static final HashMap<String, String> productNameMap;

    static {
        productNameMap = new HashMap();
        productNameMap.put("TEMP", "Cld_Top_Temp");
        productNameMap.put("PRES", "Cld_Top_Pres");
        productNameMap.put("HT", "Cld_Top_Hght");
        productNameMap.put("Phase", "Cloud_Phase");
        productNameMap.put("BCM", "Binary_Cld_Msk");
        productNameMap.put("Mask", "fire_mask_abi");
    }

    public GEOSDataSource(File file, double default_stride) {
        this(file, default_stride, false, null, Float.NaN, null, null, true);
    }

    /**
     * @param file
     * @param default_stride
     * @param unpack
     */
    public GEOSDataSource(File file, double default_stride, boolean unpack, String bandName, float centerWavelength, String sensorName, DataGroup cat, boolean zeroBased) {

        this.default_stride = default_stride;
        this.dateTimeStamp = DataSource.getDateTimeStampFromFilename(file.getName());
        this.description = DataSource.getDescriptionFromFilename(file.getName());
        this.dateTime = DataSource.getDateTimeFromFilename(file.getName());
        this.unpack = unpack;
        this.bandName = bandName;
        this.centerWavelength = centerWavelength;
        this.sensorName = sensorName;
        this.cat = cat;
        this.zeroBased = zeroBased;
        this.isMultiSpectral = true;

        try {
            init(file.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public GEOSDataSource(ArrayList<File> files) throws Exception {
        this(files.toArray(new File[]{null}));
    }

    //public GEOSDataSource(File file, double default_stride, boolean unpack, String sensorName, DataGroup cat, boolean zeroBased) throws Exception {
    public GEOSDataSource(File[] files) throws Exception {
        if (!canUnderstand(files)) {
            throw new Exception("GEOSDataSource doesn't understand");
        }
        File file = files[0];

//    this.default_stride = default_stride;
        this.dateTimeStamp = DataSource.getDateTimeStampFromFilename(file.getName());
        this.description = DataSource.getDescriptionFromFilename(file.getName());
        this.dateTime = DataSource.getDateTimeFromFilename(file.getName());
        this.unpack = true;
        this.isMultiSpectral = false;

        String fname = file.getName();
        if (fname.contains("ABI-L2-FDCC")) {
            prodStr = "ABI-L2-FDCC";
        }

        try {
            init(file.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init(String filename) throws Exception {
        reader = new NetCDFFile(filename);

        HashMap varMap = reader.getVarMap();
        Iterator<Variable> iter = varMap.values().iterator();
        while (iter.hasNext()) {
            Variable var = iter.next();
            String varName = var.getShortName();
            int[] varDims = reader.getDimensionLengths(varName);
            int rank = varDims.length;

            Attribute attr = var.findAttribute("grid_mapping_name");
            if (attr != null) {
                projVarList.add(var);
            } else if (var.findAttribute("grid_mapping") != null) {
                varsWithProj.add(var);
            } else {
                attr = var.findAttribute("standard_name");
                if (attr != null) {
                    String stndName = attr.getStringValue();
                    if (stndName.equals("projection_x_coordinate")) {
                        projXCoordVars.put(varName, var);
                    } else if (stndName.equals("projection_y_coordinate")) {
                        projYCoordVars.put(varName, var);
                    } else if (stndName.equals("time")) {
                        timeCoordVars.put(varName, var);
                    }
                } else {
                    varsWithProj.add(var);
                }
            }

            if (rank == 1) {
                attr = var.findAttribute("units");
                String[] dimNames = reader.getDimensionNames(varName);
                if (attr != null) {
                    String str = attr.getStringValue();
                    visad.Unit unit = null;
                    try {
                        unit = visad.data.units.Parser.parse(str);
                    } catch (Exception e) {
                    }
                    if (unit != null && unit.isConvertible(visad.SI.second)) {
                        if (varName.equals(dimNames[0])) {
                            timeCoordVars.put(varName, var);
                        }
                    }
                }
            }
        }

        iter = varsWithProj.iterator();
        while (iter.hasNext()) {
            Variable var = iter.next();
            String varName = var.getShortName();
            if (varName.contains("longitude") || varName.contains("latitude")) { // don't want to display these
                continue;
            }

            String[] dimNames = reader.getDimensionNames(varName);

            Variable varX = null;
            Variable varY = null;
            Variable varT = null;

            for (int k = 0; k < dimNames.length; k++) {
                Iterator itr = projXCoordVars.keySet().iterator();
                while (itr.hasNext()) {
                    Object key = itr.next();
                    Variable vr = projXCoordVars.get(key);
                    String name = vr.getShortName();
                    String[] vrDimsName = reader.getDimensionNames(name);
                    if (vrDimsName != null && vrDimsName.length > 0) {
                        String coordDimName = vrDimsName[0];
                        if (dimNames[k].equals(coordDimName)) {
                            varX = vr;
                            break;
                        }
                    }
                }

                itr = projYCoordVars.keySet().iterator();
                while (itr.hasNext()) {
                    Object key = itr.next();
                    Variable vr = projYCoordVars.get(key);
                    String name = vr.getShortName();
                    String[] vrDimsName = reader.getDimensionNames(name);
                    if (vrDimsName != null && vrDimsName.length > 0) {
                        String coordDimName = vrDimsName[0];
                        if (dimNames[k].equals(coordDimName)) {
                            varY = vr;
                            break;
                        }
                    }
                }

                itr = timeCoordVars.keySet().iterator();
                while (itr.hasNext()) {
                    Object key = itr.next();
                    Variable vr = timeCoordVars.get(key);
                    String name = vr.getShortName();
                    String[] vrDimsName = reader.getDimensionNames(name);
                    if (vrDimsName != null && vrDimsName.length > 0) {
                        String coordDimName = vrDimsName[0];
                        if (dimNames[k].equals(coordDimName)) {
                            varT = vr;
                            break;
                        }
                    }
                }
            }

            Variable projVar = projVarList.get(0); //TODO: may be more than one

            if (varX != null && varY != null) {
                GEOSInfo geosInfo = new GEOSInfo(reader, var, projVar, varT, varX, varY);
                String name = var.getShortName();
                if (name.equals("DQF")) {
                    continue;
                }
                if (prodStr != null && prodStr.equals("ABI-L2-FDCC")) {
                    if (!name.equals("Mask")) {
                        continue;
                    }
                }
                String rname = productNameMap.get(name);
                if (rname != null) {
                    name = rname;
                }

                HashMap metadata = GOESGridAdapter.getEmptyMetadataTable();
                metadata.put(MultiDimensionAdapter.array_name, geosInfo.getName());
                metadata.put(GOESGridAdapter.gridX_name, geosInfo.getXDimName());
                metadata.put(GOESGridAdapter.gridY_name, geosInfo.getYDimName());
                metadata.put(MultiDimensionAdapter.fill_value_name, "_FillValue");
                if (unpack) {
                    metadata.put("unpack", "true");
                }

                GOESGridAdapter goesAdapter = new GOESGridAdapter(reader, metadata, geosInfo.getMapProjection(), default_stride, zeroBased);
                HashMap subset = goesAdapter.getDefaultSubset();
                if (geosInfo.getTDimName() != null) {
                    subset.put(geosInfo.getTDimName(), new double[]{0.0, 0.0, 1.0});
                }
                DataSelection dataSel = new MultiDimensionSubset(subset);
                DataChoice dataChoice = new DataChoice(this, name, null);
                dataChoice.setDataSelection(dataSel);
                addDataChoice(dataChoice);
                adapters.add(goesAdapter);

                if (isMultiSpectral) {
                    msd = makeMultiSpectralData(goesAdapter, reader, geosInfo.getName(), geosInfo.getXDimName(), geosInfo.getYDimName(), bandName, centerWavelength, sensorName, cat);
                    msdMap.put(dataChoice, msd);
                }
            }
        }

    }

    MultiSpectralData makeMultiSpectralData(GOESGridAdapter gridAdapter, MultiDimensionReader reader, String array, String gridX, String gridY, String band, float cntrWvlen, String sensorName, DataGroup cat) {
        HashMap table = SpectrumAdapter.getEmptyMetadataTable();
        table.put(SpectrumAdapter.array_name, array);
        table.put(SpectrumAdapter.x_dim_name, gridX);
        table.put(SpectrumAdapter.y_dim_name, gridY);
        table.put(SpectrumAdapter.channelValues, new float[]{cntrWvlen});
        table.put(SpectrumAdapter.bandNames, new String[]{band});
        table.put(SpectrumAdapter.channelType, "wavelength");
        table.put(SwathAdapter.array_dimension_names, new String[]{gridY, gridX});
        SpectrumAdapter spectrumAdapter = new SpectrumAdapter(reader, table);

        String paramName = null;

        if (cat == null) {
            paramName = null;
        } else if (cat.getName().contains("emis")) {
            paramName = "BrightnessTemp";
        } else if (cat.getName().contains("refl")) {
            paramName = "Reflectance";
        }

        MultiSpectralData multiSpectData = new MultiSpectralData(gridAdapter, spectrumAdapter, paramName, paramName, sensorName, null);
        return multiSpectData;
    }

    @Override
    public Date getDateTime() {
        return dateTime;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean getDoFilter(DataChoice choice) {
        return true;
    }

    @Override
    public boolean getOverlayAsMask(DataChoice choice) {
        if (prodStr != null && prodStr.equals("ABI-L2-FDCC")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean getDoReproject(DataChoice choice) {
        return false;
    }

    public void addDataChoice(DataChoice dataChoice) {
        myDataChoices.add(dataChoice);
    }

    public MultiSpectralData getMultiSpectralData(DataChoice choice) {
        return msdMap.get(choice);
    }


    public Data getData(DataChoice dataChoice, DataSelection dataSelection)
            throws VisADException, RemoteException {
        try {
            ArrayList dataChoices = (ArrayList) getDataChoices();
            int idx = dataChoices.indexOf(dataChoice);
            GOESGridAdapter adapter = adapters.get(idx);

            MultiDimensionSubset select = (MultiDimensionSubset) dataChoice.getDataSelection();
            HashMap subset = select.getSubset();

            Data data = adapter.getData(subset);

            if (prodStr != null && prodStr.equals("ABI-L2-FDCC")) {
                float[][] values = ((FlatField) data).getFloats(false);
                for (int k = 0; k < values[0].length; k++) {
                    short v = (short) values[0][k];
                    //if (((v & 13) == 13) || ((v & 33) == 33)) {
                    if ((v & 13) == 13) {
                        //if (v == 13) {
                        values[0][k] = 3f;
                    }
                    //else if (((v & 14) == 14) || ((v & 34) == 34)) {
                    else if ((v & 14) == 14) {
                        //else if (v == 14) {
                        values[0][k] = 2f;
                    }
                    //else if (((v & 15) == 15) || ((v & 35) == 35)) {
                    else if ((v & 15) == 15) {
                        //else if (v == 15) {
                        values[0][k] = 1f;
                    } else {
                        values[0][k] = 0f;
                    }
                }
            }
            return data;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public float getNadirResolution(DataChoice choice) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean canUnderstand(File[] files) {
        File file = files[0];
        if (file.getName().startsWith("CG_ABI-L2") ||
                file.getName().startsWith("OR_ABI-L2")) {
            return true;
        }
        return false;
    }

    public Range getDefaultColorRange(DataChoice choice) {
        String name = choice.getName();

        Range rng;

        if (name.equals("fire_mask_abi")) {
            rng = new Range(0f - 0.5f, 3f + 0.5f);
        } else {
            rng = super.getDefaultColorRange(choice);
        }

        return rng;
    }

}

class GEOSInfo {

    String xVarName;
    String xDimName;
    String yVarName;
    String yDimName;
    String varName;
    String projVarName;
    String tVarName;
    String tDimName;

    int xDimLen;
    int yDimLen;
    int tDimLen;

    double subLonDegrees;
    double perspective_point_height = Double.NaN;
    double semi_major_axis = Double.NaN;
    double semi_minor_axis = Double.NaN;
    double inverse_flattening = Double.NaN;

    MapProjection mapProj;

    /**
     * @param reader
     * @param var
     * @param projVar
     * @param timeCoordVar
     * @param xCoordVar
     * @param yCoordVar
     * @throws Exception
     */
    public GEOSInfo(NetCDFFile reader, Variable var, Variable projVar, Variable timeCoordVar, Variable xCoordVar, Variable yCoordVar) throws Exception {

        varName = var.getShortName();

        xVarName = xCoordVar.getShortName();
        yVarName = yCoordVar.getShortName();

        xDimName = (reader.getDimensionNames(xVarName))[0];
        yDimName = (reader.getDimensionNames(yVarName))[0];

        xDimLen = reader.getDimensionLength(xDimName);
        yDimLen = reader.getDimensionLength(yDimName);

        if (timeCoordVar != null) {
            tVarName = timeCoordVar.getShortName();
            tDimName = (reader.getDimensionNames(tVarName))[0];
            tDimLen = reader.getDimensionLength(tDimName);
        }

        double scale_x = Double.NaN;
        double offset_x = Double.NaN;
        double scale_y = Double.NaN;
        double offset_y = Double.NaN;

        HDFArray obj = (HDFArray) reader.getArrayAttribute(xVarName, "scale_factor");
        if (obj != null) {
            if (obj.getType().equals(Double.TYPE)) {
                scale_x = ((double[]) obj.getArray())[0];
            } else if (obj.getType().equals(Float.TYPE)) {
                scale_x = ((float[]) obj.getArray())[0];
            }
        }
        obj = (HDFArray) reader.getArrayAttribute(xVarName, "add_offset");
        if (obj != null) {
            if (obj.getType().equals(Double.TYPE)) {
                offset_x = ((double[]) obj.getArray())[0];
            } else if (obj.getType().equals(Float.TYPE)) {
                offset_x = ((float[]) obj.getArray())[0];
            }
        }

        obj = (HDFArray) reader.getArrayAttribute(yVarName, "scale_factor");
        if (obj != null) {
            if (obj.getType().equals(Double.TYPE)) {
                scale_y = ((double[]) obj.getArray())[0];
            } else if (obj.getType().equals(Float.TYPE)) {
                scale_y = ((float[]) obj.getArray())[0];
            }
        }
        obj = (HDFArray) reader.getArrayAttribute(yVarName, "add_offset");
        if (obj != null) {
            if (obj.getType().equals(Double.TYPE)) {
                offset_y = ((double[]) obj.getArray())[0];
            } else if (obj.getType().equals(Float.TYPE)) {
                offset_y = ((float[]) obj.getArray())[0];
            }
        }

        if (Double.isNaN(scale_x) || Double.isNaN(offset_x) || Double.isNaN(scale_y) || Double.isNaN(offset_y)) {
            throw new Exception("problem retrieving navigation scale/offset");
        }

        obj = (HDFArray) reader.getArrayAttribute(projVar.getShortName(), "longitude_of_projection_origin");
        if (obj == null) {
            throw new Exception("projection variable must define 'longitude_of_projection_origin'");
        }
        if (obj.getType().equals(Double.TYPE)) {
            subLonDegrees = ((double[]) obj.getArray())[0];
        } else if (obj.getType().equals(Float.TYPE)) {
            subLonDegrees = (double) ((float[]) obj.getArray())[0];
        }

        obj = (HDFArray) reader.getArrayAttribute(projVar.getShortName(), "perspective_point_height");
        if (obj.getType().equals(Double.TYPE)) {
            perspective_point_height = ((double[]) obj.getArray())[0];
        } else if (obj.getType().equals(Float.TYPE)) {
            perspective_point_height = (double) ((float[]) obj.getArray())[0];
        }

        obj = (HDFArray) reader.getArrayAttribute(projVar.getShortName(), "semi_major_axis");
        if (obj.getType().equals(Double.TYPE)) {
            semi_major_axis = ((double[]) obj.getArray())[0];
        } else if (obj.getType().equals(Float.TYPE)) {
            semi_major_axis = (double) ((float[]) obj.getArray())[0];
        }

        obj = (HDFArray) reader.getArrayAttribute(projVar.getShortName(), "semi_minor_axis");
        if (obj.getType().equals(Double.TYPE)) {
            semi_minor_axis = ((double[]) obj.getArray())[0];
        } else if (obj.getType().equals(Float.TYPE)) {
            semi_minor_axis = (double) ((float[]) obj.getArray())[0];
        }

        obj = (HDFArray) reader.getArrayAttribute(projVar.getShortName(), "inverse_flattening");
        if (obj.getType().equals(Double.TYPE)) {
            inverse_flattening = ((double[]) obj.getArray())[0];
        } else if (obj.getType().equals(Float.TYPE)) {
            inverse_flattening = (double) ((float[]) obj.getArray())[0];
        }

        obj = (HDFArray) reader.getArrayAttribute(projVar.getShortName(), "sweep_angle_axis");
        if (obj == null) {
            throw new Exception("projection variable must define: sweep_angle_axis");
        }
        String sweepAngleAxis = ((String[]) obj.getArray())[0];


        if (sweepAngleAxis.equals("x")) {
            sweepAngleAxis = "GOES";
        } else if (sweepAngleAxis.equals("y")) {
            sweepAngleAxis = "GEOS";
        }

        GEOSTransform geosTran;
        if (!Double.isNaN(semi_major_axis) && !Double.isNaN(semi_minor_axis) && !Double.isNaN(inverse_flattening)) {
            geosTran = new GEOSTransform(subLonDegrees, sweepAngleAxis,
                    perspective_point_height, semi_major_axis, semi_minor_axis, inverse_flattening);
        } else {
            geosTran = new GEOSTransform(subLonDegrees, sweepAngleAxis);
        }

        mapProj = new GEOSProjection(geosTran, 0.0, 0.0, (double) xDimLen, (double) yDimLen,
                scale_x, offset_x, scale_y, offset_y);
    }


    public String getName() {
        return varName;
    }

    public String getXDimName() {
        return xDimName;
    }

    public String getYDimName() {
        return yDimName;
    }

    public String getTDimName() {
        return tDimName;
    }

    public MapProjection getMapProjection() {
        return mapProj;
    }
}