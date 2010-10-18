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

package edu.wisc.ssec.mcidasv.data.adde;

import edu.wisc.ssec.mcidas.adde.AddeTextReader;
import edu.wisc.ssec.mcidasv.chooser.adde.AddeTleChooser;

import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DirectDataChoice;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import visad.Data;
import visad.DataReference;
import visad.Text;
import visad.VisADException;
import visad.VisADException;

import visad.data.mcidas.AddeTextAdapter;

import java.io.FileInputStream;

import java.rmi.RemoteException;



import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;




/**
 * Class for data sources of ADDE text data.  These may be generic
 * files or weather bulletins
 *
 * @author IDV development team
 * @version $Revision$
 */

public class AddeTleDataSource extends DataSourceImpl {

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            AddeTleDataSource.class.getName());

    private List choices = new ArrayList();
    /**
     * Default bean constructor for persistence; does nothing.
     */
    public AddeTleDataSource() {}

    /**
     * Create a new AddeTleDataSource
     *
     * @param descriptor    descriptor for this source
     * @param filename      ADDE URL
     * @param properties    extra properties for this source
     *
     */
    public AddeTleDataSource(DataSourceDescriptor descriptor,
                              String filename, Hashtable properties) {
        super(descriptor, filename, "Text data source", properties);
        System.out.println("\nAddeTleDataSource:");
        System.out.println("    descriptor=" + descriptor);
        System.out.println("    filename=" + filename);

        System.out.println("properties:");
        String key = AddeTleChooser.TLE_SERVER_NAME_KEY;
        Object server = properties.get(key);
        System.out.println("    " + key + "=" + server);
        key = AddeTleChooser.TLE_GROUP_NAME_KEY;
        Object group = properties.get(key);
        System.out.println("    " + key + "=" + group);
/*
        key = AddeTleChooser.SATELLITE_SERVER_NAME_KEY;
        Object val = properties.get(key);
        System.out.println("    " + key + "=" + val);
        key = AddeTleChooser.SATELLITE_GROUP_NAME_KEY;
        val = properties.get(key);
        System.out.println("    " + key + "=" + val);
*/
        //String url = "adde://noaaport.ssec.wisc.edu/textdata?&PORT=112&COMPRESS=gzip&USER=GAD&PROJ=6999&GROUP=POESNAV&DESCR=TLE";
        String url = "adde://" + server + "/textdata?&PORT=112&COMPRESS=gzip&USER=GAD&PROJ=6999&GROUP=" + group + "&DESCR=" + filename;
        AddeTextReader reader = new AddeTextReader(url);
        List lines = null;
        if ("OK".equals(reader.getStatus())) {
            lines = reader.getLinesOfText();
        }
        if (lines == null) {
            System.out.println("\nproblem reading TLE file");
        } else {
            String[] cards = StringUtil.listToStringArray(lines);
            for (int i=0; i<cards.length; i++) {
                int indx = cards[i].indexOf(" ");
                if (indx < 0) {
                    choices.add(cards[i]);
                }
            }
        }
    }



    /**
     * Make the data choices assoicated with this source.
     */
    protected void doMakeDataChoices() {
        String category = "unknown";
        for (int i=0; i<choices.size(); i++) {
            String name  = ((String)choices.get(i)).trim();
            //name = name.toLowerCase().trim();
            addDataChoice(
                new DirectDataChoice(
                    this, name, name, name,
                    DataCategory.parseCategories(category, false)));
        }
    }

    /**
     * Actually get the data identified by the given DataChoce. The default is
     * to call the getDataInner that does not take the requestProperties. This
     * allows other, non unidata.data DataSource-s (that follow the old API)
     * to work.
     *
     * @param dataChoice        The data choice that identifies the requested
     *                          data.
     * @param category          The data category of the request.
     * @param dataSelection     Identifies any subsetting of the data.
     * @param requestProperties Hashtable that holds any detailed request
     *                          properties.
     *
     * @return The visad.Text object
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        System.out.println("\n Helloooooooooooo");
        String          filename = dataChoice.getStringId();
        AddeTextAdapter ata      = new AddeTextAdapter(filename);
        return ata.getData();
    }

}

