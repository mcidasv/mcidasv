/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
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

package edu.wisc.ssec.mcidasv.data.hydra;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.data.PreviewSelection;

import edu.wisc.ssec.mcidasv.data.HydraDataSource;
import edu.wisc.ssec.mcidasv.data.hydra.ProfileAlongTrack;
import edu.wisc.ssec.mcidasv.data.hydra.ProfileAlongTrack3D;
import edu.wisc.ssec.mcidasv.data.hydra.Calipso2D;
import edu.wisc.ssec.mcidasv.control.LambertAEA;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

// import thredds.catalog.ThreddsMetadata.Variable;
import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.data.GeoSelectionPanel;

import ucar.unidata.util.Misc;
import ucar.unidata.idv.ViewContext;

import visad.Data;
import visad.FlatField;
import visad.GriddedSet;
import visad.Gridded2DSet;
import visad.SampledSet;
import visad.VisADException;
import visad.georef.MapProjection;
import visad.data.mcidas.BaseMapAdapter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;

import javax.swing.*;
import javax.swing.event.*;

import org.jdom.Attribute;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;

import java.awt.geom.Rectangle2D;

import visad.*;
import visad.bom.RubberBandBoxRendererJ3D;
import visad.java3d.DisplayImplJ3D;
import visad.java3d.TwoDDisplayRendererJ3D;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.control.DisplayControlBase;
import ucar.unidata.view.geoloc.MapProjectionDisplayJ3D;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import ucar.visad.display.XYDisplay;
import ucar.visad.display.MapLines;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.RubberBandBox;

import ucar.visad.ProjectionCoordinateSystem;
import ucar.unidata.geoloc.projection.LatLonProjection;

import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;

/**
 * A data source for NPOESS Preparatory Project (NPP) data
 * This will probably move, but we are placing it here for now
 * since we are leveraging some existing code used for HYDRA.
 */

public class NPPDataSource extends HydraDataSource {

    /** Sources file */
    protected String filename;

    protected MultiDimensionReader reader;

    protected MultiDimensionAdapter[] adapters = null;

    protected SpectrumAdapter spectrumAdapter;

    private static final String DATA_DESCRIPTION = "Multi Dimension Data";

    private HashMap defaultSubset;
    public TrackAdapter track_adapter;
    private MultiSpectralData multiSpectData;

    private List categories;
    private boolean hasImagePreview = false;
    private boolean hasTrackPreview = false;
    private boolean hasChannelSelect = false;
    
    private HashMap geoHM;
    
    private static int XSCAN = 768;
    private static int YSCAN = 3200;

    /**
     * Zero-argument constructor for construction via unpersistence.
     */
    
    public NPPDataSource() {}

    /**
     * Construct a new NPP hdf data source.
     * @param  descriptor  descriptor for this <code>DataSource</code>
     * @param  fileName  name of the hdf file to read
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    
    public NPPDataSource(DataSourceDescriptor descriptor,
                                 String fileName, Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.newList(fileName), properties);
        System.out.println("NPPDataSource called, single file selected: " + fileName);
    }

    /**
     * Construct a new NPP hdf data source.
     * @param  descriptor  descriptor for this <code>DataSource</code>
     * @param  sources   List of filenames
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    
    public NPPDataSource(DataSourceDescriptor descriptor,
                                 List newSources, Hashtable properties)
            throws VisADException {
        super(descriptor, newSources, DATA_DESCRIPTION, properties);
        System.out.println("NPPDataSource constructor called, filename: " + (String) sources.get(0));

        this.filename = (String)sources.get(0);

        try {
          setup();
        }
        catch (Exception e) {
          e.printStackTrace();
          throw new VisADException();
        }
    }

    public void setup() throws Exception {

    	// looking to populate 3 things - path to lat, path to lon, path to products
    	String pathToLat = null;
    	String pathToLon = null;
    	String geoProductID = null;
    	ArrayList<String> pathToProducts = new ArrayList<String>();
    	ArrayList<String> unsignedFlags = new ArrayList<String>();
    	ArrayList<String> unpackFlags = new ArrayList<String>();
    	
    	// set up a temporary hashmap for geo products.  This will move to a utility
    	// class once we shake out the issues
    	// TODO - what we said above
    	geoHM = new HashMap();
    	geoHM.put("ATMS-SDR-GEO", "GATMO");
    	geoHM.put("CrIMSS-AUX-EDR", "GCRIO");
    	geoHM.put("CrIS-SDR-GEO", "GCRSO");
    	geoHM.put("VIIRS-MOD-EDR-GEO", "GMGTO");
    	geoHM.put("VIIRS-MOD-GEO", "GMODO");
    	geoHM.put("VIIRS-MOD-GEO-TC", "GMTCO");
    	geoHM.put("VIIRS-MOD-MAP-IP", "IVMIM");
    	geoHM.put("VIIRS-MOD-UNAGG-GEO", "VMUGE");
    	geoHM.put("VIIRS-NCC-EDR-GEO", "GNCCO");
    	geoHM.put("VIIRS-DNB-GEO", "GDNBO");
    	geoHM.put("VIIRS-IMG-EDR-GEO", "GIGTO");
    	geoHM.put("VIIRS-IMG-GEO", "GIMGO");
    	geoHM.put("VIIRS-IMG-GEO-TC", "GITCO");
    	
    	try {
    		System.err.println("Setting up data adapter for file: " + filename);
    		// need to open the main NetCDF file to determine the geolocation product
    		NetcdfFile ncfile = null;
    		try {
    			System.err.println("Trying to open file: " + filename);
    			ncfile = NetcdfFile.open(filename);
    			ucar.nc2.Attribute a = ncfile.findGlobalAttribute("N_GEO_Ref");
    			System.err.println("Value of GEO global attribute: " + a.getStringValue());
    			geoProductID = mapGeoRefToProductID(a.getStringValue());
    			System.err.println("Value of corresponding Product ID: " + geoProductID);
    		} catch (Exception e) {
    			System.err.println("Exception during open file: " + filename);
    			e.printStackTrace();
    		} finally {
    			ncfile.close();
    		}
    		
			// build an XML (NCML actually) representation of the union aggregation of these two files
			Namespace ns = Namespace.getNamespace("http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2");
			org.jdom.Element root = new org.jdom.Element("netcdf", ns);
			org.jdom.Document document = new org.jdom.Document(root);
			//document.setDocType(docType)
			org.jdom.Element agg = new org.jdom.Element("aggregation", ns);
			agg.setAttribute("type", "union");
			org.jdom.Element fData = new org.jdom.Element("netcdf", ns);
			fData.setAttribute("location", filename);
			org.jdom.Element fGeo  = new org.jdom.Element("netcdf", ns);
			//String geoFilename = filename.replaceFirst("VSSTO", geoProductID);
			String geoFilename = filename.substring(0, filename.lastIndexOf(File.separatorChar) + 1);
			geoFilename += geoProductID;
			geoFilename += filename.substring(filename.lastIndexOf(File.separatorChar) + 6);
			System.err.println("Cobbled together GEO file name: " + geoFilename);
			fGeo.setAttribute("location", geoFilename);
			agg.addContent(fData);
			agg.addContent(fGeo);
			root.addContent(agg);    
		    XMLOutputter xmlOut = new XMLOutputter();
		    String ncmlStr = xmlOut.outputString(document);
		    ByteArrayInputStream is = new ByteArrayInputStream(ncmlStr.getBytes());			
		    reader = new NetCDFFile(is);
    	} catch (Exception e) {
    		e.printStackTrace();
    		System.out.println("cannot create NPP reader for file: " + filename);
    	}
    	
    	// let's try and look through the reader and see what we can learn...
    	NetcdfFile ncdff = ((NetCDFFile) reader).getNetCDFFile();
    	
    	Group rg = ncdff.getRootGroup();
    	System.err.println("Root group name: " + rg.getName());
    	List<Group> gl = rg.getGroups();
    	// count the number of adapters we'll need
    	int aCount = 0;
    	if (gl != null) {
    		for (Group g : gl) {
    			System.err.println("Group name: " + g.getName());
    			// XXX just temporary - we are looking through All_Data, finding displayable data
    			if (g.getName().contains("All_Data")) {
    				List<Group> adg = g.getGroups();
    				// again, iterate through
    				for (Group subG : adg) {
    					System.err.println("Sub group name: " + subG.getName());
    					String subName = subG.getName();
    					if (subName.contains("MOD-GEO")) {
    						// this is the geolocation data
    						List<Variable> vl = subG.getVariables();
    						for (Variable v : vl) {
    							if (v.getName().contains("Latitude")) {
    								pathToLat = v.getName();
        							System.err.println("Lat/Lon Variable: " + v.getName());
    							}
    							if (v.getName().contains("Longitude")) {
    								pathToLon = v.getName();
        							System.err.println("Lat/Lon Variable: " + v.getName());
    							}
    						} 
    					} else {
    						// this is the product data
    						List<Variable> vl = subG.getVariables();
    						for (Variable v : vl) {
    							boolean useThis = false;
    							String vName = v.getName();
    							System.err.println("Variable: " + vName);
    							String firstChar = vName.substring(0, 1);
    							DataType dt = v.getDataType();
    							if ((dt.getSize() != 4) && (dt.getSize() != 2) && (dt.getSize() != 1)) {
    								System.err.println("Skipping data of size: " + dt.getSize());
    								continue;
    							}
    							List al = v.getAttributes();
    							int[] shape = v.getShape();
    							List<Dimension> dl = v.getDimensions();
    							boolean xScanOk = false;
    							boolean yScanOk = false;
    							for (Dimension d : dl) {
    								// in order to consider this a displayable product, make sure
    								// both scan direction dimensions are present and look like a granule
    								if (d.getLength() == XSCAN) {
    									xScanOk = true;
    								}
    								if (d.getLength() == YSCAN) {
    									yScanOk = true;
    								}    								
    							}
    							
    							if (xScanOk && yScanOk) {
    								useThis = true;
    							}
    							
    							if (useThis) { 
    								// loop through the variable list again, looking for a corresponding "Factors"
    								float scaleVal = 1f;
    								float offsetVal = 0f;
    								boolean unpackFlag = false;
    								
    								// for variable list
    								//   if name startswith first char of this variable, and ends with Factors
    								//     get the data, data1 = scale, data2 = offset
    								//     create and poke attributes with this data
    								//   endif
    								// endfor
    								
    								for (Variable fV : vl) {
    									if ((fV.getName().startsWith(firstChar)) && (fV.getName().endsWith("Factors"))) {
    										ucar.ma2.Array a = fV.read();
    										ucar.ma2.Index i = a.getIndex();
    										scaleVal = a.getFloat(i);
    										System.err.println("Scale value: " + scaleVal);
    										i.incr();
    										offsetVal = a.getFloat(i);
    										System.err.println("Offset value: " + offsetVal);
    										unpackFlag = true;
    										break;
    									}
    								}

    								// poke in scale/offset attributes for now

    								ucar.nc2.Attribute a1 = new ucar.nc2.Attribute("scale_factor", scaleVal);
    								v.addAttribute(a1);
    								ucar.nc2.Attribute a2 = new ucar.nc2.Attribute("add_offset", offsetVal);
    								v.addAttribute(a2);   		
    								ucar.nc2.Attribute aFill = v.findAttribute("_FillValue");
    								System.err.println("_FillValue attribute value: " + aFill.getNumericValue());
    								ucar.nc2.Attribute aUnsigned = v.findAttribute("_Unsigned");
    								if (aUnsigned != null) {
    									System.err.println("_Unsigned attribute value: " + aUnsigned.getStringValue());
    									unsignedFlags.add(aUnsigned.getStringValue());
    								} else {
    									unsignedFlags.add("false");
    								}
    								
    								if (unpackFlag) {
    									unpackFlags.add("true");
    								} else {
    									unpackFlags.add("false");
    								}
    								
    								System.err.println("Adding product: " + v.getName());
    								pathToProducts.add(v.getName());
    								
    							}
    						}    						
   						
    					}
    				}
    			}
    		}
    	}

    	// make sure we found valid data
    	if (pathToProducts.size() == 0) {
    		throw new VisADException("No data found in file selected");
    	}
    	
    	System.err.println("Number of adapters needed: " + pathToProducts.size());
    	adapters = new MultiDimensionAdapter[pathToProducts.size()];
    	Hashtable<String, String[]> properties = new Hashtable<String, String[]>(); 

    	String name = (new File(filename)).getName();

    	// test code block for NPP data
    	
    	for (int pIdx = 0; pIdx < pathToProducts.size(); pIdx++) {
    		System.err.println("Working on adapter number " + (pIdx + 1));
        	HashMap table = SwathAdapter.getEmptyMetadataTable();
        	table.put("array_name", (String) pathToProducts.get(pIdx));
        	table.put("array_dimension_names", new String[] {"Track", "XTrack"});
        	table.put("lon_array_name", pathToLon);
        	table.put("lat_array_name", pathToLat);
        	table.put("lon_array_dimension_names", new String[] {"Track", "XTrack"});
        	table.put("lat_array_dimension_names", new String[] {"Track", "XTrack"});
        	table.put("XTrack", "XTrack");
        	table.put("Track", "Track");
        	table.put("geo_Track", "Track");
        	table.put("geo_XTrack", "XTrack");
        	table.put("scale_name", "scale_factor");
        	table.put("offset_name", "add_offset");
        	table.put("fill_value_name", "_FillValue");
        	String unsignedAttributeStr = unsignedFlags.get(pIdx);
        	if (unsignedAttributeStr.equals("true")) {
        		table.put("unsigned", unsignedAttributeStr);
        	}
        	String unpackFlagStr = unpackFlags.get(pIdx);
        	if (unpackFlagStr.equals("true")) {
        		table.put("unpack", "true");
        	}
        	adapters[pIdx] = new SwathAdapter(reader, table);
    	}

    	categories = DataCategory.parseCategories("2D grid;GRID-2D;");
    	defaultSubset = adapters[0].getDefaultSubset();

    	setProperties(properties);
    }

    public void initAfterUnpersistence() {
      try {
        setup();
      } 
      catch (Exception e) {
      }
    }

    /**
     * Make and insert the <code>DataChoice</code>-s for this
     * <code>DataSource</code>.
     */
    
    public void doMakeDataChoices() {
    	DataChoice choice = null;
    	if (adapters != null) {
    		for (int idx = 0; idx < adapters.length; idx++) {
    			try {
    				choice = doMakeDataChoice(idx, adapters[idx].getArrayName());
    			} 
    			catch (Exception e) {
    				e.printStackTrace();
    				System.out.println("doMakeDataChoice failed");
    			}

    			if (choice != null) {
    				addDataChoice(choice);
    			}
    		}
    	}
    }

    private DataChoice doMakeDataChoice(int idx, String var) throws Exception {
        String name = var;
        DataSelection dataSel = new MultiDimensionSubset(defaultSubset);
        Hashtable subset = new Hashtable();
        subset.put(new MultiDimensionSubset(), dataSel);
        DirectDataChoice ddc = new DirectDataChoice(this, idx, name, name, categories, subset);
        return ddc;
    }

    /**
     * Check to see if this <code>NPPDataSource</code> is equal to the object
     * in question.
     * @param o  object in question
     * @return true if they are the same or equivalent objects
     */
    
    public boolean equals(Object o) {
        if ( !(o instanceof NPPDataSource)) {
            return false;
        }
        return (this == (NPPDataSource) o);
    }

    public MultiSpectralData getMultiSpectralData() {
      return multiSpectData;
    }

    public String getDatasetName() {
      return filename;
    }

    public String mapGeoRefToProductID(String geoRef) {
    	String s = null;
    	if (geoHM != null) {
    		s = (String) geoHM.get(geoRef);
    	}
    	return s;
    }
    
    public void setDatasetName(String name) {
      filename = name;
    }

    public HashMap getSubsetFromLonLatRect(MultiDimensionSubset select, GeoSelection geoSelection) {
      GeoLocationInfo ginfo = geoSelection.getBoundingBox();
      return adapters[0].getSubsetFromLonLatRect(select.getSubset(), ginfo.getMinLat(), ginfo.getMaxLat(),
                                        ginfo.getMinLon(), ginfo.getMaxLon());
    }


    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection, Hashtable requestProperties)
                                throws VisADException, RemoteException {

        //- this hack keeps the HydraImageProbe from doing a getData()
        //- TODO: need to use categories?
        if (requestProperties != null) {
          if ((requestProperties.toString()).equals("{prop.requester=MultiSpectral}")) {
            return null;
          }
        }

        GeoLocationInfo ginfo = null;
        GeoSelection geoSelection = null;
        
        if ((dataSelection != null) && (dataSelection.getGeoSelection() != null)) {
          geoSelection = (dataSelection.getGeoSelection().getBoundingBox() != null) ? dataSelection.getGeoSelection() :
                                    dataChoice.getDataSelection().getGeoSelection();
        }

        if (geoSelection != null) {
          ginfo = geoSelection.getBoundingBox();
        }

        Data data = null;
        if (adapters == null) {
          return data;
        }

        MultiDimensionAdapter adapter = null;
        
        // pick the adapter with the same index as the current data choice
        int aIdx = 0;
        List<DataChoice> dcl = getDataChoices();
        for (DataChoice dc : dcl) {
        	if (dc.equals(dataChoice)) {
        		aIdx = dcl.indexOf(dc);
        		break;
        	}
        }
        
        System.err.println("Found dataChoice index: " + aIdx);
        adapter = adapters[aIdx];

        try {
            HashMap subset = null;
            if (ginfo != null) {
              subset = adapter.getSubsetFromLonLatRect(ginfo.getMinLat(), ginfo.getMaxLat(),
                                                       ginfo.getMinLon(), ginfo.getMaxLon(),
                                                       geoSelection.getXStride(),
                                                       geoSelection.getYStride(),
                                                       geoSelection.getZStride());
            }
            else {

              MultiDimensionSubset select = null;
              Hashtable table = dataChoice.getProperties();
              Enumeration keys = table.keys();
              while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                if (key instanceof MultiDimensionSubset) {
                  select = (MultiDimensionSubset) table.get(key);
                }
              }  
              subset = select.getSubset();

              Hashtable props = dataSelection.getProperties();
              if (props != null) {
                if (props.containsKey(SpectrumAdapter.channelIndex_name)) {
                  double[] coords = (double[]) subset.get(SpectrumAdapter.channelIndex_name);
                  int idx = ((Integer) props.get(SpectrumAdapter.channelIndex_name)).intValue();
                  coords[0] = (double)idx;
                  coords[1] = (double)idx;
                  coords[2] = (double)1;
                }
              }
            }

            if (subset != null) {
              data = adapter.getData(subset);
              data = applyProperties(data, requestProperties, subset, aIdx);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("getData exception e=" + e);
        }
        return data;
    }

    protected Data applyProperties(Data data, Hashtable requestProperties, HashMap subset, int adapterIndex) 
          throws VisADException, RemoteException {
      Data new_data = data;

      if (requestProperties == null) {
        new_data = data;
        return new_data;
      }

      if (requestProperties.containsKey("medianFilter")) {
        String[] items = (String[]) requestProperties.get("medianFilter");
        double window_lenx = Double.parseDouble(items[0]);
        double window_leny = Double.parseDouble(items[1]);
        GriddedSet domainSet = (GriddedSet) ((FlatField)data).getDomainSet();
        int[] lens = domainSet.getLengths();
        float[] range_values = (((FlatField)data).getFloats())[0];
        range_values =
           ProfileAlongTrack.medianFilter(range_values, lens[0], lens[1],
                               (int)window_lenx, (int)window_leny);
        ((FlatField)new_data).setSamples(new float[][] {range_values});
      }
      if (requestProperties.containsKey("setBelowSfcMissing")) {
        String[] items = (String[]) requestProperties.get("setBelowSfcMissing");
        FlatField track = (FlatField) track_adapter.getData(subset);
        float[] sfcElev = (track.getFloats())[0];
        FlatField field = (FlatField) new_data;
        GriddedSet gset = (GriddedSet) field.getDomainSet();
        float[][] samples = gset.getSamples(false);
        int[] lens = gset.getLengths();
        float[] range_values = (field.getFloats())[0];

        int trkIdx = ((ProfileAlongTrack3D) adapters[adapterIndex]).adapter2D.getTrackTupIdx();
        int vrtIdx = ((ProfileAlongTrack3D) adapters[adapterIndex]).adapter2D.getVertTupIdx();

        int k = 0;
        for (int j=0; j<lens[trkIdx]; j++) {
          float val = sfcElev[j];
          for (int i=0; i<lens[vrtIdx]; i++) {
            if (vrtIdx < trkIdx) k = i + j*lens[0];
            if (trkIdx < vrtIdx) k = j + i*lens[0];
            if (samples[2][k] <= val || samples[2][k] < 0.0) {
              range_values[k] = Float.NaN;
            }
          }
        }
        field.setSamples(new float[][] {range_values});
      }
      return new_data;
    }

    protected void initDataSelectionComponents(
         List<DataSelectionComponent> components,
             final DataChoice dataChoice) {

      if (hasImagePreview) {
        try {
          FlatField image = multiSpectData.getImage(multiSpectData.init_wavenumber, defaultSubset);
          components.add(new PreviewSelection(dataChoice, image, null));
        } catch (Exception e) {
          System.out.println("Can't make PreviewSelection: "+e);
          e.printStackTrace();
        }
      }
      if (hasTrackPreview) {
        try {
          FlatField track = track_adapter.getData(track_adapter.getDefaultSubset());
          components.add(new NPPTrackSelection(dataChoice, track));
        } catch (Exception e) {
          System.out.println("Can't make PreviewSelection: "+e);
          e.printStackTrace();
        }
      }
      if (hasChannelSelect) {
        try {
          components.add(new NPPChannelSelection(dataChoice));
        } 
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
}

class NPPChannelSelection extends DataSelectionComponent {

   DataChoice dataChoice;
   MultiSpectralDisplay display;

   NPPChannelSelection(DataChoice dataChoice) throws Exception {
     super("Channels");
     this.dataChoice = dataChoice;
     display = new MultiSpectralDisplay((DirectDataChoice)dataChoice);
     display.showChannelSelector();
   }

  protected JComponent doMakeContents() {
    try {
      JPanel panel = new JPanel(new BorderLayout());
      panel.add("Center", display.getDisplayComponent());
      return panel;
    }
    catch (Exception e) {
      System.out.println(e);
    }
    return null;
  }
                                                                                                                                                   
                                                                                                                                                   
  public void applyToDataSelection(DataSelection dataSelection) {
      try {
        dataSelection.putProperty(Constants.PROP_CHAN, display.getWaveNumber());
        dataSelection.putProperty(SpectrumAdapter.channelIndex_name, display.getChannelIndex());
      } catch (Exception e) {
        e.printStackTrace();
      }
  }

}


class NPPTrackSelection extends DataSelectionComponent {
      DataChoice dataChoice;
      FlatField track;

      double[] x_coords = new double[2];
      double[] y_coords = new double[2];
      boolean hasSubset = true;
      MapProjectionDisplayJ3D mapProjDsp;
      DisplayMaster dspMaster;

      int trackStride;
      int verticalStride;

      JTextField trkStr;
      JTextField vrtStr;


   NPPTrackSelection(DataChoice dataChoice, FlatField track) throws VisADException, RemoteException {
        super("track");
        this.dataChoice = dataChoice;
        this.track = track;
        mapProjDsp = new MapProjectionDisplayJ3D(MapProjectionDisplay.MODE_2Din3D);
        mapProjDsp.enableRubberBanding(false);
        dspMaster = mapProjDsp;
        mapProjDsp.setMapProjection(getDataProjection());
        LineDrawing trackDsp = new LineDrawing("track");
        trackDsp.setLineWidth(2f);
        trackDsp.setData(track);
        mapProjDsp.addDisplayable(trackDsp);


        MapLines mapLines  = new MapLines("maplines");
        URL      mapSource =
        mapProjDsp.getClass().getResource("/auxdata/maps/OUTLSUPU");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            System.out.println("Can't open map file " + mapSource);
            System.out.println(excp);
        }
                                                                                                                                                     
        mapLines  = new MapLines("maplines");
        mapSource =
        mapProjDsp.getClass().getResource("/auxdata/maps/OUTLSUPW");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            System.out.println("Can't open map file " + mapSource);
            System.out.println(excp);
        }
                                                                                                                                                     
        mapLines  = new MapLines("maplines");
        mapSource =
        mapProjDsp.getClass().getResource("/auxdata/maps/OUTLHPOL");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            System.out.println("Can't open map file " + mapSource);
            System.out.println(excp);
        }

        final LineDrawing selectBox = new LineDrawing("select");
        selectBox.setColor(Color.green);

        final RubberBandBox rbb =
            new RubberBandBox(RealType.Longitude, RealType.Latitude, 1);
        rbb.setColor(Color.green);
        rbb.addAction(new CellImpl() {
          public void doAction()
             throws VisADException, RemoteException
           {
              Gridded2DSet set = rbb.getBounds();
              float[] low = set.getLow();
              float[] hi = set.getHi();
              x_coords[0] = low[0];
              x_coords[1] = hi[0];
              y_coords[0] = low[1];
              y_coords[1] = hi[1];
              
              SampledSet[] sets = new SampledSet[4];
              sets[0] = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple, new float[][] {{low[0], hi[0]}, {low[1], low[1]}}, 2);
              sets[1] = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple, new float[][] {{hi[0], hi[0]}, {low[1], hi[1]}}, 2);
              sets[2] = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple, new float[][] {{hi[0], low[0]}, {hi[1], hi[1]}}, 2);
              sets[3] = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple, new float[][] {{low[0], low[0]}, {hi[1], low[1]}}, 2);
              UnionSet uset = new UnionSet(sets);
              selectBox.setData(uset);
           }
        });
        dspMaster.addDisplayable(rbb);
        dspMaster.addDisplayable(selectBox);

        dspMaster.draw();
   }

       public MapProjection getDataProjection() {
         MapProjection mp = null;
         try {
           mp = new ProjectionCoordinateSystem(new LatLonProjection());
         } catch (Exception e) {
             System.out.println(" getDataProjection"+e);
         }
         return mp;
       }

      protected JComponent doMakeContents() {
        try {
          JPanel panel = new JPanel(new BorderLayout());
          panel.add("Center", dspMaster.getDisplayComponent());

          JPanel stridePanel = new JPanel(new FlowLayout());
          trkStr = new JTextField(Integer.toString(5), 3);
          vrtStr = new JTextField(Integer.toString(2), 3);
          trkStr.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ae) {
                setTrackStride(Integer.valueOf(trkStr.getText().trim()));
              }
          });
          vrtStr.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ae) {
                setVerticalStride(Integer.valueOf(vrtStr.getText().trim()));
              }
          });

          stridePanel.add(new JLabel("track stride: "));
          stridePanel.add(trkStr);
          stridePanel.add(new JLabel("vertical stride: "));
          stridePanel.add(vrtStr);
          panel.add("South", stridePanel);

          return panel;
        }
        catch (Exception e) {
          System.out.println(e);
        }
        return null;
      }
                                                                                                                                                     
      public void setTrackStride(int stride) {
        trackStride = stride;
      }

      public void setVerticalStride(int stride) {
        verticalStride = stride;
      }

      public void setTrackStride() {
        trackStride = Integer.valueOf(trkStr.getText().trim());
      }

      public void setVerticalStride() {
        verticalStride = Integer.valueOf(vrtStr.getText().trim());
      }

      public void applyToDataSelection(DataSelection dataSelection) {
         setTrackStride();
         setVerticalStride();
         if (hasSubset) {
           GeoSelection geoSelect = new GeoSelection(
                new GeoLocationInfo(y_coords[1], x_coords[0], y_coords[0], x_coords[1]));
           geoSelect.setXStride(trackStride);
           geoSelect.setYStride(verticalStride);
           dataSelection.setGeoSelection(geoSelect);
         }
      }
}
