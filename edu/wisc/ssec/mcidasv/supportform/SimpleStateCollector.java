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

import java.util.Properties;
import java.util.Map.Entry;

// test/example only! use something else!
public class SimpleStateCollector implements StateCollector {

    public String getBundleAttachmentName() {
        return "empty.mcv";
    }

    public String getExtraAttachmentName() {
        return "system.properties";
    }

    public String getContentsAsString() {
        StringBuffer buf = new StringBuffer();
        Properties props = System.getProperties();
        for (Entry<Object, Object> entry : props.entrySet()) {
            buf.append(entry.getKey()+"="+entry.getValue()+"\n");
        }
        return buf.toString();
    }

    public byte[] getContents() {
        return getContentsAsString().getBytes();
    }

    public String toString() {
        return String.format("[SimpleStateCollector@%x: canBundleState=%s, bundle=%s, extra=%s]", hashCode(), canBundleState(), getBundleAttachmentName(), getExtraAttachmentName());
    }

    public boolean canBundleState() {
        return false;
    }

    public byte[] getBundledState() {
        return "".getBytes();
    }

    public String getLogPath() {
        return "";
    }
}
