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

package edu.wisc.ssec.mcidasv.collaboration;

import ucar.unidata.collab.Sharable;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.collab.CollabManager;
import ucar.unidata.idv.collab.CollabMsgType;

import edu.wisc.ssec.mcidasv.McIDASV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollaborationManager extends CollabManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CollaborationManager.class);
    
    private ViewManager recordingFrom;
    
    public CollaborationManager(IntegratedDataViewer idv) {
        super(idv);
    }
    
//    public void startRecording(ViewManager vm) {
//        recordingFrom = vm;
//    }
//    
//    public void stopRecording(ViewManager vm) {
//        recordingFrom = null;
//    }
//    
//    @Override public void write(CollabMsgType type, String message) {
//        super.write(type, message);
//    }
//    
//    @Override protected String makeMsg(CollabMsgType type, String body) {
//        logger.trace("type: {}, body: {}", type, body);
//        return super.makeMsg(type, body);
//    }
    
    @Override protected Sharable findSharable(String id) {
        Sharable sharable = super.findSharable(id);
        if (sharable == null) {
            if (id.startsWith("view_")) {
                if (recordingFrom == null) {
                    sharable = ((McIDASV) getIdv()).getVMManager().getLastActiveViewManager();
                } else {
                    return recordingFrom;
                }
            }
        }
        return sharable;
    }
    

}
