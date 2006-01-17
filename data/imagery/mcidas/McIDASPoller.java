package ucar.unidata.data.imagery.mcidas;

import ucar.unidata.util.Poller;
import ucar.unidata.util.PollingInfo;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Class for handling polling McIDAS-X for display changes
 */

public class McIDASPoller extends Poller {

/* ???
   private FrmsubsImpl fsi = new FrmsubsImpl();
*/
   //private FrmsubsMM fsi = new FrmsubsMM();
   private static FrmsubsMM fsi;
   static {
     try {
       fsi = new FrmsubsMM();
       fsi.getMemoryMappedUC();
     } catch (Exception e) { }
   }

   private FrameComponentInfo frameComponentInfo;

    /** Holds polling information */
    PollingInfo pollingInfo;

    /**
     * Create a new McIDAS poller
     *
     * @param listener    the listener for the polling info
     * @param info        polling info
     */
    public McIDASPoller(FrameComponentInfo fci, ActionListener listener, PollingInfo info) {
        super(listener, info.getInterval());
        this.pollingInfo = new PollingInfo(info);
        init();
        frameComponentInfo = fci;
    }


    /**
     * Initialize the class
     *
     */
    protected void init() {
        super.init();
    }


    /**
     * Poll!
     */
    protected void doPoll() {
      int frame = -1;
      int dirty_flag = 0;
      try {
        dirty_flag = fsi.getDirtyFlag(frame);
      } catch (Exception e) {
        System.out.println("McIDASPoller: File not found");
      }
      if (dirty_flag != 0) {
        if ((dirty_flag&1)>0) {
          frameComponentInfo.setDirtyImage(true);
        }
        if ((dirty_flag&2)>0) {
          frameComponentInfo.setDirtyGraphics(true);
        }
        if ((dirty_flag&4)>0) {
          frameComponentInfo.setDirtyColorTable(true);
        }
        if (listener != null) {
          Integer idf = new Integer(dirty_flag);
          listener.actionPerformed(new ActionEvent(idf, 1, "REFRESH"));
        }
      }
   }
}

