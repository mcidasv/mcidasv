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