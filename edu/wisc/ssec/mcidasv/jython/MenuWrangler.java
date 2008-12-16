package edu.wisc.ssec.mcidasv.jython;

import javax.swing.JPopupMenu;

public interface MenuWrangler {

    
    public JPopupMenu buildMenu();
    
    public void stateChanged();
}
