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
package edu.wisc.ssec.mcidasv.supportform;

import edu.wisc.ssec.mcidasv.Constants;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.LogUtil;

public class IdvStateCollector implements StateCollector {

    private IntegratedDataViewer idv;

    private static final String BUNDLE_FILENAME = "bundle" + Constants.SUFFIX_MCV;
    private static final String EXTRA_FILENAME = "extra.html";

    public IdvStateCollector(IntegratedDataViewer idv) {
        this.idv = idv;
    }

    public String getBundleAttachmentName() {
        return BUNDLE_FILENAME;
    }

    public String getExtraAttachmentName() {
        return EXTRA_FILENAME;
    }

    public String getContentsAsString() {
        return "";
    }

    public byte[] getContents() {
        return getContentsAsString().getBytes();
    }

    public String toString() {
        return String.format("[IdvStateCollector@%x: canBundleState=%s, bundle=%s, extra=%s]", hashCode(), canBundleState(), getBundleAttachmentName(), getExtraAttachmentName());
    }

    public boolean canBundleState() {
        return true;
    }

    public byte[] getBundledState() {
        String data = "";
        try {
            data = idv.getPersistenceManager().getBundleXml(true);
        } catch (Exception e) {
            LogUtil.logException("Error saving state for support request", e);
        }
        return data.getBytes();
    }

    public String getLogPath() {
        return "";
    }
}
