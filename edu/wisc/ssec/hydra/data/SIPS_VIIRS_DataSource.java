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

package edu.wisc.ssec.hydra.data;

import edu.wisc.ssec.adapter.AggregationRangeProcessor;
import edu.wisc.ssec.adapter.ArrayAdapter;
import edu.wisc.ssec.adapter.GranuleAggregation;

import java.io.File;
import java.util.HashMap;
import java.util.ArrayList;

import edu.wisc.ssec.adapter.SwathAdapter;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.MultiDimensionReader;
import edu.wisc.ssec.adapter.MultiSpectralAggr;
import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.adapter.NetCDFFile;
import edu.wisc.ssec.adapter.RangeProcessor;
import visad.VisADException;
import visad.Data;
import visad.FlatField;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import visad.CoordinateSystem;
import visad.FunctionType;
import visad.Linear2DSet;
import visad.RealTupleType;
import visad.util.Util;


public abstract class SIPS_VIIRS_DataSource extends DataSource {

    String dateTimeStamp = null;

    Date dateTime = null;

    String description = null;

    File[] files = null;

    ArrayList<SwathAdapter> swathAdapters = new ArrayList<SwathAdapter>();

    String[] bandNames = null;
    float[] centerWavelength = null;

    ArrayList<NetCDFFile> ncdfal;
    ArrayList<NetCDFFile> ncdfalGeo;

    MultiDimensionReader reader;
    MultiDimensionReader geoReader;

    ArrayAdapter solzenAdapter;

    int numGrans;

    ArrayList<MultiSpectralData> Memis = new ArrayList<>();
    ArrayList<MultiSpectralData> Mrefl = new ArrayList<>();
    ArrayList<MultiSpectralData> Iemis = new ArrayList<>();
    ArrayList<MultiSpectralData> Irefl = new ArrayList<>();

    MultiSpectralData IemisMSD;
    MultiSpectralData MemisMSD;
    MultiSpectralData IreflMSD;
    MultiSpectralData MreflMSD;

    final DataGroup catI = new DataGroup("I-Band");
    final DataGroup catM = new DataGroup("M-Band");
    final DataGroup catDNB = new DataGroup("DNB-Band");

    DataGroup datGrp;

    public SIPS_VIIRS_DataSource(File directory) throws Exception {
        this(directory.listFiles());
    }

    public SIPS_VIIRS_DataSource(File[] files) throws Exception {

        if (!canUnderstand(files)) {
            throw new Exception("SIPS_VIIRS_DataSource doesn't understand these files");
        }

        try {
            initReader(files);
            init();

            if (Irefl.size() > 0) {
                IreflMSD = new MultiSpectralAggr((MultiSpectralData[]) Irefl.toArray(new MultiSpectralData[1]));
            }
            if (Iemis.size() > 0) {
                IemisMSD = new MultiSpectralAggr((MultiSpectralData[]) Iemis.toArray(new MultiSpectralData[1]));
            }
            if (Mrefl.size() > 0) {
                MreflMSD = new MultiSpectralAggr((MultiSpectralData[]) Mrefl.toArray(new MultiSpectralData[1]));
            }
            if (Memis.size() > 0) {
                MemisMSD = new MultiSpectralAggr((MultiSpectralData[]) Memis.toArray(new MultiSpectralData[1]));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    void init() throws Exception {
    }

    void initReader(File[] files) throws Exception {
        ArrayList<File> sortedList = DataSource.getTimeSortedFileList(files);
        Object[] sortedFiles = sortedList.toArray();
        files = new File[files.length];
        for (int k = 0; k < sortedFiles.length; k++) {
            files[k] = (File) sortedFiles[k];
        }

        ArrayList<File> geoFileList = new ArrayList<File>();

        File file = files[0];
        String name = file.getName();

        File[] geoFiles = new File[files.length];

        String prefix = null;

        if (name.startsWith("VNP02MOD")) {
            prefix = "VNP03MOD";
        } else if (name.startsWith("VJ102MOD")) {
            prefix = "VJ103MOD";
        } else if (name.startsWith("VJ202MOD")) {
            prefix = "VJ203MOD";
        } else if (name.startsWith("VNP02DNB")) {
            prefix = "VNP03DNB";
        } else if (name.startsWith("VJ102DNB")) {
            prefix = "VJ103DNB";
        } else if (name.startsWith("VJ202DNB")) {
            prefix = "VJ203DNB";
        } else if (name.startsWith("VNP02IMG")) {
            prefix = "VNP03IMG";
        } else if (name.startsWith("VJ102IMG")) {
            prefix = "VJ103IMG";
        } else if (name.startsWith("VJ202IMG")) {
            prefix = "VJ203IMG";
        } else if (name.startsWith("VNP02FSN")) {
            prefix = "VNP03MOD";
        } else if (name.startsWith("VJ102FSN")) {
            prefix = "VJ103MOD";
        } else if (name.startsWith("VJ202FSN")) {
            prefix = "VJ203MOD";
        } else {
            throw new Exception("unknown VIIRS filename prefix: " + name);
        }
        File dir = new File(file.getParent());

        File[] list = dir.listFiles();
        for (int k = 0; k < list.length; k++) {
            if (list[k].getName().startsWith(prefix)) {
                geoFileList.add(list[k]);
            }
        }

        for (int i = 0; i < files.length; i++) {
            name = files[i].getName();
            String[] strs = name.split("\\.");
            String regex = prefix + "\\." + strs[1] + "\\." + strs[2] + ".*";
            Pattern pattern = Pattern.compile(regex);
            for (int k = 0; k < geoFileList.size(); k++) {
                File geoFile = geoFileList.get(k);
                Matcher matcher = pattern.matcher(geoFile.getName());
                if (matcher.find()) {
                    geoFiles[i] = geoFile;
                    break;
                }
            }
        }

        ncdfal = new ArrayList<NetCDFFile>();
        ncdfalGeo = new ArrayList<NetCDFFile>();

        numGrans = files.length;

        for (int k = 0; k < numGrans; k++) {
            ncdfal.add(new NetCDFFile(files[k].getAbsolutePath()));
            ncdfalGeo.add(new NetCDFFile(geoFiles[k].getAbsolutePath()));
        }

        reader = new GranuleAggregation(ncdfal, "number_of_lines");
        geoReader = new GranuleAggregation(ncdfalGeo, "number_of_lines");


        dateTimeStamp = DataSource.getDateTimeStampFromFilename(files[0].getName());
        dateTime = DataSource.getDateTimeFromFilename(files[0].getName());
    }

    RangeProcessor buildRadToBT(HashMap metadata, String btLUTname) throws Exception {
        RangeProcessor[] rngProcessors = new RangeProcessor[numGrans];

        for (int k = 0; k < numGrans; k++) {
            rngProcessors[k] = new BTbyLUT(ncdfal.get(k), metadata, btLUTname);
        }

        RangeProcessor rangeProcessor = new AggregationRangeProcessor(rngProcessors);
        ((GranuleAggregation) reader).addPreProcessor((String) metadata.get(SwathAdapter.array_name), rangeProcessor);

        return rangeProcessor;
    }

    public HashMap fillSwathMetadata(String xtrack, String track,
                                     String array, String range, String geoXtrack, String geoTrack,
                                     String lonArray, String latArray, String[] arrayDims, String[] lonArrayDims, String[] latArrayDims,
                                     String fillValueName) {

        HashMap metadata = SwathAdapter.getEmptyMetadataTable();

        metadata.put(SwathAdapter.xtrack_name, xtrack);
        metadata.put(SwathAdapter.track_name, track);
        metadata.put(SwathAdapter.geo_xtrack_name, geoXtrack);
        metadata.put(SwathAdapter.geo_track_name, geoTrack);
        metadata.put(SwathAdapter.array_name, array);
        metadata.put(SwathAdapter.range_name, range);
        metadata.put(SwathAdapter.lon_array_name, lonArray);
        metadata.put(SwathAdapter.lat_array_name, latArray);
        //metadata.put(SwathAdapter.scale_name, "scale_factor");
        metadata.put("unsigned", "true");
        //metadata.put("valid_low", "valid_min");
        //metadata.put("valid_high", "valid_max");
        if (lonArrayDims != null) {
            metadata.put(SwathAdapter.lon_array_dimension_names, lonArrayDims);
        }
        if (latArrayDims != null) {
            metadata.put(SwathAdapter.lat_array_dimension_names, latArrayDims);
        }
        if (arrayDims != null) {
            metadata.put(SwathAdapter.array_dimension_names, arrayDims);
        }
        if (fillValueName != null) {
            //metadata.put(SwathAdapter.fill_value_name, fillValueName);
        }
        return metadata;
    }

    public SwathAdapter buildReflAdapter(String xtrack, String track,
                                         String array, String range, String geoXtrack, String geoTrack,
                                         String lonArray, String latArray, String[] arrayDims, String[] lonArrayDims, String[] latArrayDims,
                                         String fillValueName) throws Exception {


        HashMap metadata = fillSwathMetadata(xtrack, track, array, range, geoXtrack, geoTrack, lonArray, latArray, arrayDims, lonArrayDims, latArrayDims, fillValueName);

        String geoGroupName = "geolocation_data/";
        HashMap szmetadata = new HashMap();
        szmetadata.put(SwathAdapter.track_name, "number_of_lines");
        szmetadata.put(SwathAdapter.xtrack_name, "number_of_pixels");
        szmetadata.put("array_name", geoGroupName + "solar_zenith");
        szmetadata.put("array_dimension_names", new String[]{"number_of_lines", "number_of_pixels"});
        szmetadata.put("unsigned", "true");
        solzenAdapter = new ArrayAdapter(geoReader, szmetadata);

        SwathAdapter swathAdapter;

        if (geoReader != null) {
            swathAdapter = new SwathAdapter(reader, metadata, geoReader);
        } else {
            swathAdapter = new SwathAdapter(reader, metadata);
        }

        return swathAdapter;
    }

    public SwathAdapter buildEmisAdapter(String xtrack, String track,
                                         String array, String range, String geoXtrack, String geoTrack,
                                         String lonArray, String latArray, String[] arrayDims, String[] lonArrayDims, String[] latArrayDims,
                                         String fillValueName, String btLUTName) throws Exception {


        HashMap metadata = fillSwathMetadata(xtrack, track, array, range, geoXtrack, geoTrack, lonArray, latArray, arrayDims, lonArrayDims, latArrayDims, fillValueName);


        SwathAdapter swathAdapter;
        buildRadToBT(metadata, btLUTName);

        if (geoReader != null) {
            swathAdapter = new SwathAdapter(reader, metadata, geoReader);
        } else {
            swathAdapter = new SwathAdapter(reader, metadata);
        }


        return swathAdapter;
    }

    public SwathAdapter buildAdapter(String xtrack, String track,
                                     String array, String range, String geoXtrack, String geoTrack,
                                     String lonArray, String latArray, String[] arrayDims, String[] lonArrayDims, String[] latArrayDims,
                                     String fillValueName) {


        HashMap metadata = fillSwathMetadata(xtrack, track, array, range, geoXtrack, geoTrack, lonArray, latArray, arrayDims, lonArrayDims, latArrayDims, fillValueName);
        SwathAdapter swathAdapter;

        if (geoReader != null) {
            swathAdapter = new SwathAdapter(reader, metadata, geoReader);
        } else {
            swathAdapter = new SwathAdapter(reader, metadata);
        }

        return swathAdapter;
    }

    public SwathAdapter getSwathAdapter(DataChoice dataChoice) {
        for (int k = 0; k < myDataChoices.size(); k++) {
            if (myDataChoices.get(k).equals(dataChoice)) {
                return swathAdapters.get(k);
            }
        }
        return null;
    }

    @Override
    public String getDescription() {
        return "SNPP VIIRS";
    }

    public Date getDateTime() {
        return dateTime;
    }

    @Override
    public boolean getDoFilter(DataChoice choice) {
        return true;
    }

    @Override
    public boolean getOverlayAsMask(DataChoice choice) {
        return false;
    }

    @Override
    public String getDescription(DataChoice choice) {
        String name = choice.getName();

        float cntrWvln = 0;
        for (int k = 0; k < bandNames.length; k++) {
            if (name.equals(bandNames[k])) {
                cntrWvln = centerWavelength[k];
                break;
            }
        }

        if (cntrWvln == 0) {
            return null;
        } else {
            return "(" + cntrWvln + ")";
        }

    }

    void setDataChoice(SwathAdapter adapter, int idx, String name) {
        HashMap subset = adapter.getDefaultSubset();
        DataSelection dataSel = new MultiDimensionSubset(subset);
        DataChoice dataChoice = new DataChoice(this, name, null);
        dataChoice.setDataSelection(dataSel);
        myDataChoices.add(dataChoice);
        swathAdapters.add(adapter);
    }

    public Data getData(DataChoice dataChoice, DataSelection dataSelection)
            throws VisADException, RemoteException {
        try {
            SwathAdapter adapter = getSwathAdapter(dataChoice);

            MultiDimensionSubset select = (MultiDimensionSubset) dataChoice.getDataSelection();
            HashMap subset = select.getSubset();

            Data data = adapter.getData(subset);
            String name = dataChoice.getName();
            if (name.equals("I01") || name.equals("I02") || name.equals("I03") ||
                    name.equals("M01") || name.equals("M02") || name.equals("M03") ||
                    name.equals("M04") || name.equals("M05") || name.equals("M06") ||
                    name.equals("M07") || name.equals("M08") || name.equals("M09") ||
                    name.equals("M10") || name.equals("M11")) {

                float[] refls = (((FlatField) data).getFloats(false))[0];
                float[] solzen = ((FlatField) solzenAdapter.getData(subset)).getFloats()[0];
                for (int k = 0; k < refls.length; k++) {
                    float refl = refls[k];
                    float solz = solzen[k];
                    if (solz < 80.0f) {
                        refls[k] = refl / ((float) Math.cos((Math.PI / 180.0) * solz));
                    }
                }
            } else if (name.equals("DNB")) {
                float[][] rngVals = ((FlatField) data).getFloats(false);
                for (int k = 0; k < rngVals[0].length; k++) {
                    float fval = rngVals[0][k];
                    if (fval <= 0f) {
                        rngVals[0][k] = Float.NaN;
                    } else {
                        rngVals[0][k] = (float) Math.log10((double) fval);
                    }
                }
            }

            CoordinateSystem cs = ((RealTupleType) ((FunctionType) data.getType()).getDomain()).getCoordinateSystem();

            if (datGrp.equals(catM)) {
                if (MreflMSD != null) {
                    MreflMSD.setCoordinateSystem(cs);
                    MreflMSD.setSwathDomainSet((Linear2DSet) ((FlatField) data).getDomainSet());
                }
                if (MemisMSD != null) {
                    MemisMSD.setCoordinateSystem(cs);
                    MemisMSD.setSwathDomainSet((Linear2DSet) ((FlatField) data).getDomainSet());
                }
            } else if (datGrp.equals(catI)) {
                if (IreflMSD != null) {
                    IreflMSD.setCoordinateSystem(cs);
                    IreflMSD.setSwathDomainSet((Linear2DSet) ((FlatField) data).getDomainSet());
                }
                if (IemisMSD != null) {
                    IemisMSD.setCoordinateSystem(cs);
                    IemisMSD.setSwathDomainSet((Linear2DSet) ((FlatField) data).getDomainSet());
                }
            }

            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public MultiSpectralData[] getMultiSpectralData() {
        ArrayList<MultiSpectralData> list = new ArrayList();

        if (IreflMSD != null) {
            list.add(IreflMSD);
        }
        if (MreflMSD != null) {
            list.add(MreflMSD);
        }
        if (IemisMSD != null) {
            list.add(IemisMSD);
        }
        if (MemisMSD != null) {
            list.add(MemisMSD);
        }

        return list.toArray(new MultiSpectralData[1]);
    }

    public abstract float getNadirResolution(DataChoice choice);

}

class BTbyLUT extends RangeProcessor {

    float[] btLUT;


    public BTbyLUT(MultiDimensionReader reader, HashMap metadata, String btLUTname) throws Exception {
        super(reader, metadata);

        int numLUTvals = (reader.getDimensionLengths(btLUTname))[0];
        btLUT = reader.getFloatArray(btLUTname, new int[]{0}, new int[]{numLUTvals}, new int[]{1});
    }


    /**
     * calls super to unscale radiances then converts to BT
     */
    public float[] processRange(short[] values, HashMap subset) {

        float[] brightnessTemps = new float[values.length];
        java.util.Arrays.fill(brightnessTemps, Float.NaN);

        for (int k = 0; k < values.length; k++) {
            int ii = Util.unsignedShortToInt(values[k]);
            if (ii >= 0 && ii < 65536) {
                float bt = btLUT[ii];
                if (bt != -999.9f) {
                    brightnessTemps[k] = btLUT[ii];
                }
            }
        }

        return brightnessTemps;
    }
}
