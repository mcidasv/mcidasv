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

import java.util.HashMap;

/**
 * Utility class to support Joint Polar Satellite System (JPSS) functionality.
 * Some of this is temporary hashmaps, needed because the currently available
 * NPP data does not fully, correctly implement the specs yet as defined in
 * the NPP Common Data Format Control Book.  See:
 * http://jointmission.gsfc.nasa.gov/project/science-documents.html
 * 
 * @author tommyj
 *
 */

public abstract class JPSSUtilities {
   
    private static HashMap<String, String> geoHM = new HashMap<String, String>();
    private static HashMap<String, String> prodHM = new HashMap<String, String>();
    
    static {
    	
    	// populate geolocation hashmap
    	// Collection Short Name -> Geolocation Product ID
    	// This mapping is based on Table A-8, "Geolocation Identifiers", from
    	// NPP CDFCB Volume 1
    	// http://jointmission.gsfc.nasa.gov/projects/science-documents.html
    	// NOTE: I have found some mappings to be missing!  I.E., the table
    	// in the spec is apparently not complete!
    	
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
    	geoHM.put("VIIRS-CLD-AGG-GEO", "GCLDO");
    	geoHM.put("VIIRS-SIC-GEO", "GSICO");
    	
    	// populate product hashmap
    	// This table is needed because the product names in the actual granules
    	// do not match those in the XML Product Profiles, which appear to be 
    	// a short-hand version of the longer names.  Swell.
    	// Mapping:  Product name in granule -> Product name in XML Product Profile
    	
    	prodHM.put("BulkSeaSurfaceTemperature", "bulkSST");
    	prodHM.put("CloudBaseHeightLayer", "cbhLyr");
    	prodHM.put("CloudBaseHeightTotal", "cbhTot");
    	prodHM.put("IceAge", "iceAge");
    	prodHM.put("IceAgeWeight", "iceAgeWeight");
    	prodHM.put("Land_Temp", "lst");
    	prodHM.put("Radiance", "radiance");
    	prodHM.put("Reflectance", "reflect");
    	prodHM.put("SkinSeaSurfaceTemperature", "skinSST");
    	prodHM.put("SnowCoverBinaryMap", "scdBinaryMap");
    	prodHM.put("TopOfAtmosphereNormalizedDifferenceVegetationIndex", "TOA_NDVI");
    	prodHM.put("TopOfCanopyEnhancedVegetationIndex", "TOC_EVI");
    	
    }
    
    /**
     * Map the NPP global attribute N_GEO_Ref to the appropriate geolocation
     * product id prefix.
     * 
     * @param geoRef
     * @return product id for the corresponding geolocation data 
     */
    
    public static String mapGeoRefToProductID(String geoRef) {
    	String s = null;
    	if (geoHM != null) {
    		s = (String) geoHM.get(geoRef);
    	}
    	return s;
    }
    
    /**
     * Map the NPP product name to those used in the XML Product Profiles,
     * which seem to be short-hand versions of the long names.
     * 
     * @param prodName
     * @return product id for the corresponding geolocation data 
     */
    
    public static String mapProdNameToProfileName(String prodName) {
    	String s = null;
    	if (prodHM != null) {
    		s = (String) prodHM.get(prodName);
    	}
    	return s;
    }
	
}
