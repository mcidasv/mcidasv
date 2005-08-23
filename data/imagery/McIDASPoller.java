package ucar.unidata.data.imagery;

import ucar.unidata.util.Poller;
import ucar.unidata.util.PollingInfo;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Class for handling polling McIDAS-X for display changes
 */

public class McIDASPoller extends Poller {

   private FrmsubsImpl fsi = new FrmsubsImpl();

    /** Holds polling information */
    PollingInfo pollingInfo;

    /**
     * Create a new McIDAS poller
     *
     * @param listener    the listener for the polling info
     * @param info        polling info
     */
    public McIDASPoller(ActionListener listener, PollingInfo info) {
        super(listener, info.getInterval());
        this.pollingInfo = new PollingInfo(info);
        init();
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
        int dirty_flag = fsi.getDirtyFlag(frame);
        if (dirty_flag != 0) {
          if (listener != null) {
            Integer idf = new Integer(dirty_flag);
            listener.actionPerformed(new ActionEvent(idf, 1, "REFRESH"));
          }
        }
    }

}

