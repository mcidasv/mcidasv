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

package edu.wisc.ssec.mcidasv;

import java.util.List;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.VMManager;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.ComponentHolder;
import ucar.unidata.util.GuiUtils;

import edu.wisc.ssec.mcidasv.ui.McvComponentHolder;
import edu.wisc.ssec.mcidasv.ui.UIManager;

/**
 * <p>McIDAS-V needs to manage ViewManagers in a slightly different way than 
 * the IDV. The key differences between the two are the way previously active 
 * ViewManagers are ordered and a (hopefully) more consistent way of handling 
 * active ViewManagers.</p>
 * 
 * <p>The IDV only keeps track of the ViewManager used immediately before the 
 * current one. McV keeps track of the previously active ViewManagers in a 
 * stack. This mimics window z-ordering and always returns the user to the most
 * recently active ViewManager upon removal of the active ViewManager.</p>
 * 
 * <p>Newly created ViewManagers and their first layer now become the active
 * ViewManager and layer. If there is only one ViewManager, it is now displayed
 * as active instead of just implying it. When the active ViewManager is 
 * removed, the last active ViewManager and its first layer become active.</p>
 * 
 * <p><b>A note to the future</b>: McV/IDV supports two notions of active and 
 * selected ViewManagers. Say you have NxN ViewManagers in a ComponentHolder, 
 * and you want to share views among some of these ViewManagers. When one of 
 * these shared ViewManagers is activated, should all of them become the active
 * ViewManager? We're going to have to work out how to convey which 
 * ViewManagers are shared and active, and maybe more? Good luck!</p>
 */
// TODO: should accesses to previousVMs should be synchronized?
// TODO: should keep track of the ordering of active layers per VM as well.
public class ViewManagerManager extends VMManager {

    private static final Logger logger = LoggerFactory.getLogger(ViewManagerManager.class);

    /** Whether or not to display debug messages. */
    private final boolean DEBUG = false;

    /** The stack that stores the order of previously active ViewManagers. */
    private final Stack<ViewManager> previousVMs = new Stack<ViewManager>();

    /** Convenient reference back to the UIManager. */
    private UIManager uiManager;

    /**
     * Yet another constructor.
     */
    public ViewManagerManager(IntegratedDataViewer idv) {
        super(idv);
        uiManager = (UIManager)getIdvUIManager();
    }

    public int getViewManagerCount() {
        return getViewManagers().size();
    }

    /**
     * Add the new view manager into the list if we don't have
     * one with the {@link ViewDescriptor} of the new view manager
     * already.
     *
     * @param newViewManager The new view manager
     */
    @Override public void addViewManager(ViewManager newViewManager) {
        super.addViewManager(newViewManager);
        focusLayerControlsOn(newViewManager, false);
    }

    /**
     * @return Reference to the stack of previously active ViewManagers.
     */
    public Stack<ViewManager> getViewManagerOrder() {
        return previousVMs;
    }

    /**
     * Overridden so that McV can set the active ViewManager even if there is
     * only one ViewManager. This is just a UI nicety; it'll allow the McV UI
     * to show the active ViewManager no matter what.
     * 
     * @return Always returns true.
     */
    @Override public boolean haveMoreThanOneMainViewManager() {
        return true;
    }

    /**
     * Handles the removal of a ViewManager. McV needs to override this so that
     * the stack of previously active ViewManagers is ordered properly. McV uses
     * this method to make the ViewPanel respond immediately to the change.
     * 
     * @param viewManager The ViewManager being removed.
     */
    @Override public void removeViewManager(ViewManager viewManager) {
        // the ordering of the stack must be preserved! this is the only chance
        // to ensure the ordering if the incoming VM is inactive.
        if (getLastActiveViewManager() != viewManager) {
            previousVMs.remove(viewManager);
            inspectStack("removing inactive vm");
        }

        // now just sit back and let the IDV and setLastActiveViewManager work
        // their magic.
        super.removeViewManager(viewManager);

        // inform UIManager that the VM needs to be dissociated from its
        // ComponentHolder.
        uiManager.removeViewManagerHolder(viewManager);

        // force the layer controls tabs to layout the remaining components, 
        // but we don't want to bring it to the front!
        uiManager.getViewPanel().getContents().validate();
    }

    /**
     * <p>This method is a bit strange. If the given ViewManager is null, then 
     * the IDV has removed the active ViewManager. McV will use the stack of 
     * last active ViewManagers to make the last active ViewManager active once 
     * again.</p>
     * 
     * <p>If the given ViewManager is not null, but cannot be found in the stack
     * of previously active ViewManagers, the IDV has created a new ViewManager 
     * and McV must push it on the stack.</p>
     * 
     * <p>If the given ViewManager is not null and has been found in the stack,
     * then the user has selected an inactive ViewManager. McV must remove the
     * ViewManager from the stack and then push it back on top.</p>
     * 
     * <p>These steps allow McV to make the behavior of closing tabs a bit more
     * user-friendly. The user is always returned to whichever ViewManager was
     * last active.</p>
     * 
     * @param vm See above. :(
     */
    // TODO: when you start removing the debug stuff, just convert the messages
    // to comments.
    @Override public void setLastActiveViewManager(ViewManager vm) {
        String debugMsg = "created new vm";
        if (vm != null) {
            if (previousVMs.search(vm) >= 0) {
                debugMsg = "reset active vm";
                previousVMs.remove(vm);
                focusLayerControlsOn(vm, false);
            }
            previousVMs.push(vm);
        } else {
            debugMsg = "removed active vm";

            ViewManager lastActive = getLastActiveViewManager();
            if (lastActive == null)
                return;

            lastActive.setLastActive(false);

            previousVMs.pop();

            // if there are no more VMs, make sure the IDV code knows about it
            // by setting the last active VM to null.
            if (previousVMs.isEmpty()) {
                super.setLastActiveViewManager(null);
                return;
            }

            lastActive = previousVMs.peek();
            lastActive.setLastActive(true);

            focusLayerControlsOn(lastActive, false);
        }

        inspectStack(debugMsg);
        super.setLastActiveViewManager(previousVMs.peek());

        // start active tab testing
        ComponentHolder holder = 
            uiManager.getViewManagerHolder(previousVMs.peek());
        if ((holder != null) && (holder instanceof McvComponentHolder)) {
            ((McvComponentHolder)holder).setAsActiveTab();
        }
        // stop active tab testing
    }

    /**
     * <p>Overwrite the stack containing the ordering of previously active 
     * ViewManagers.</p>
     * 
     * <p>Use this if you want to mess with the user's mind a little bit.</p>
     * 
     * @param newOrder The stack containing the new ordering of ViewManagers.
     */
    public void setViewManagerOrder(Stack<ViewManager> newOrder) {
        previousVMs.clear();
        previousVMs.addAll(newOrder);
    }

    public int getComponentHolderCount() {
        return -1;
    }

    public int getComponentGroupCount() {
        // should be the same as the number of windows (or perhaps numWindows-1).
        return -1;
    }

    /**
     * Sets the active tab of the dashboard to the layer controls and makes the
     * first layer (TODO: fix that!) of the given ViewManager the active layer.
     * 
     * @param vm The ViewManager to make active.
     * @param doShow Whether or not the layer controls should become the active
     *               tab in the dashboard.
     */
    private void focusLayerControlsOn(ViewManager vm, boolean doShow) {
        List<DisplayControlImpl> controls = vm.getControlsForLegend();
        if (controls != null && !controls.isEmpty()) {
            DisplayControlImpl control = controls.get(0);
            if (doShow) {
                GuiUtils.showComponentInTabs(control.getOuterContents(), false);
            }
        }
    }

    /**
     * Helper method that'll display the ordering of the stack and a helpful
     * debug message!
     */
    private void inspectStack(String msg) {
        if (!DEBUG) {
            return;
        }
        StringBuilder sb = new StringBuilder(this.hashCode()).append(": ").append(msg).append(": [");
        for (ViewManager vm : previousVMs) {
            sb.append(vm.hashCode()).append(',');
        }
        logger.trace(sb.append("] Size=").append(previousVMs.size()).toString());
    }

    /**
     * Turns off layer visibility animation for all {@code ViewManager}s. This
     * is typically only useful for when the user has removed all layers 
     * <i>without</i> turning off the layer animation setting.
     */
    protected void disableAllLayerVizAnimations() {
        for (ViewManager vm : getViewManagers()) {
            vm.setAnimatedVisibilityCheckBox(false);
        }
    }
}
