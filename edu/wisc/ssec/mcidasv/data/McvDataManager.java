/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
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

package edu.wisc.ssec.mcidasv.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataContext;
import ucar.unidata.data.DataManager;
import ucar.unidata.data.DataSource;

import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlResourceCollection;

import edu.wisc.ssec.mcidasv.control.HydraControl;
import edu.wisc.ssec.mcidasv.control.MultiSpectralControl;

import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;

/**
 * <p>
 * The McvDataManager exists purely as a UI nicety. In the IDV, the list of
 * {@link DataSource}s are presented in the same ordering found in
 * {@code datasources.xml}.
 * </p>
 * 
 * <p>
 * While ordering the contents of {@code datasources.xml} certainly would have
 * been easier, the approach taken here is a bit more future-proof. McV simply
 * sorts the data sources known to the IDV.
 * </p>
 */
public class McvDataManager extends DataManager {

    /**
     * ID of the "I'm Still Feeling Lucky" data source. The IDV lowercases it
     * automatically.
     */
    private static final String STILL_LUCKY_ID = "file.any";

    private final Map<DataChoice, HydraControl> hydraDataToControl = new HashMap<DataChoice, HydraControl>();
    private final Map<DataChoice, MultiSpectralDisplay> hydraDataToDisplay = new HashMap<DataChoice, MultiSpectralDisplay>();

    /**
     * Default constructor.
     */
    public McvDataManager() {
        super(null);
    }

    /**
     * Creates a new DataManager with the given {@link DataContext}.
     * 
     * @param dataContext The {@code DataContext} that this DataManager exists
     *        within (this is usually an instance of
     *        {@link ucar.unidata.idv.IntegratedDataViewer}).
     */
    public McvDataManager(final DataContext dataContext) {
        super(dataContext);
    }

    public boolean containsHydraControl(final DataChoice choice) {
        return hydraDataToControl.containsKey(choice);
    }

    public boolean containsHydraDisplay(final DataChoice choice) {
        return hydraDataToDisplay.containsKey(choice);
    }

    public void setHydraControl(final DataChoice choice, final HydraControl control) {
        hydraDataToControl.put(choice, control);
    }

    public void setHydraDisplay(final DataChoice choice, final MultiSpectralDisplay display) {
        hydraDataToDisplay.put(choice, display);
    }

    public HydraControl getHydraControl(final DataChoice choice) {
        return hydraDataToControl.get(choice);
    }

    public MultiSpectralDisplay getHydraDisplay(final DataChoice choice) {
        return hydraDataToDisplay.get(choice);
    }

    /**
     * Process the list of xml documents that define the different
     * {@link DataSource}s used within the idv. Overridden so that McIDAS-V
     * can alphabetize the lists of {@link DataSource}s presented in the UI.
     * 
     * @param resources The {@link XmlResourceCollection} that holds the set of
     *        datasource xml documents. This may be null.
     */
    @Override public void loadDataSourceXml(
        final XmlResourceCollection resources) {
        super.loadDataSourceXml(resources);
        allDataSourceIds = sortTwoFacedObjects(allDataSourceIds);
        fileDataSourceIds = sortTwoFacedObjects(fileDataSourceIds);
    }

    /**
     * <p>
     * Sorts an {@link ArrayList} of {@link TwoFacedObject}s by label. Case is
     * ignored.
     * </p>
     * 
     * <p>
     * <b>NOTE:</b> If the ID of one of the objects represents the "I'm Still
     * Feeling Lucky" data source, it'll always wind up at the end of the list.
     * </p>
     * 
     * @param objs The list that needs some sortin' out.
     * 
     * @return The sorted contents of {@code objs}.
     */
    private ArrayList<TwoFacedObject> sortTwoFacedObjects(final ArrayList<TwoFacedObject> objs) {
        Comparator<TwoFacedObject> comp = new Comparator<TwoFacedObject>() {

            public int compare(final TwoFacedObject a, final TwoFacedObject b) {

                // make sure "I'm still feeling lucky" is always last.
                if (a.getId().equals(STILL_LUCKY_ID))
                    return 1;

                // same as above!
                if (b.getId().equals(STILL_LUCKY_ID))
                    return -1;

                // otherwise sorting by label is just fine.
                return ((String)a.getLabel()).compareToIgnoreCase((String)b.getLabel());
            }

            @Override public boolean equals(Object o) {
                return (o == this);
            }
        };

        ArrayList<TwoFacedObject> reordered = new ArrayList<TwoFacedObject>(objs);
        Collections.sort(reordered, comp);
        return reordered;
    }
}
