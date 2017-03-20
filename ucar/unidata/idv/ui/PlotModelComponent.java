/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2017
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

package ucar.unidata.idv.ui;


import ucar.unidata.idv.IntegratedDataViewer;

import ucar.unidata.ui.symbol.*;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;

import java.awt.*;
import java.awt.event.*;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;


/**
 *
 *
 * @author MetApps Development Team
 * @version $Revision: 1.228 $
 */

public class PlotModelComponent extends JPanel {

    /** calling object */
    IntegratedDataViewer idv;

    /** the listener */
    Object plotModelListener;

    /** the method to call on the listener */
    Method method;

    /** widget */
    private JButton changeButton;

    /** gui comp */
    private JLabel label;

    /** station model to use */
    StationModel plotModel;

    /** ??? */
    private boolean addNone = false;


    /**
     * Create a new PlotModelComponent
     *
     * @param idv   the associated IDV
     * @param plotModelListener the listener
     * @param methodName the method to call on listener
     * @param plotModel the plot model
     */
    public PlotModelComponent(IntegratedDataViewer idv,
                              Object plotModelListener, String methodName,
                              StationModel plotModel) {
        this(idv, plotModelListener, methodName, plotModel, false);
    }

    /**
     * Create a new PlotModelComponent
     *
     * @param idv   the associated IDV
     * @param plotModelListener the listener
     * @param methodName method on listener to call
     * @param plotModel the plot model
     * @param addNone should we add the 'none' entry to the widget
     */
    public PlotModelComponent(IntegratedDataViewer idv,
                              Object plotModelListener, String methodName,
                              StationModel plotModel, boolean addNone) {
        this.idv     = idv;
        this.addNone = addNone;
        setLayout(new BorderLayout());
        this.add(makeStationModelWidget());
        this.plotModelListener = plotModelListener;
        method = Misc.findMethod(plotModelListener.getClass(), methodName,
                                 new Class[] { StationModel.class });
        setPlotModel(plotModel);
    }


    /**
     * Make the gui widget for setting the station model
     *
     * @return the widget
     */
    private JComponent makeStationModelWidget() {
        JButton editButton =
            GuiUtils.getImageButton("/ucar/unidata/idv/images/edit.gif",
                                    getClass());
        editButton.setToolTipText("Show the plot model editor");
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {}
        });

        label = new JLabel(" ");
        changeButton =
            GuiUtils.getImageButton("/auxdata/ui/icons/DownDown.gif",
                                    getClass());
        changeButton.setToolTipText("Click to change plot model");
        changeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                StationModelManager smm      = idv.getStationModelManager();
                ObjectListener      listener = new ObjectListener(null) {
                    public void actionPerformed(ActionEvent ae) {
                        Misc.run(new Runnable() {
                            public void run() {
                                idv.showWaitCursor();
                                try {
                                    plotModel = (StationModel) theObject;
                                    if (plotModel != null) {
                                        label.setText(
                                            plotModel.getDisplayName());
                                    }
                                    method.invoke(plotModelListener,
                                            new Object[] { plotModel });
                                } catch (Exception exc) {
                                    idv.logException("Changing plot model",
                                            exc);
                                }
                                idv.showNormalCursor();
                            }
                        });
                    }
                };

                List items = StationModelCanvas.makeStationModelMenuItems(
                                 smm.getStationModels(), listener, smm);
                items.add(0, GuiUtils.MENU_SEPARATOR);
                if (addNone) {
                    items.add(0, GuiUtils.makeMenuItem("None",
                            PlotModelComponent.this, "setNone"));
                }
                items.add(0, GuiUtils.makeMenuItem("Edit",
                        PlotModelComponent.this, "editPlotModel"));
                JPopupMenu popup = GuiUtils.makePopupMenu(items);
                popup.show(changeButton, changeButton.getSize().width / 2,
                           changeButton.getSize().height);
            }
        });

        return GuiUtils.centerRight(label,
                                    GuiUtils.inset(changeButton,
                                        new Insets(0, 4, 0, 0)));

    }


    /**
     * user selected 'none'
     */
    public void setNone() {
        plotModel = null;
        label.setText("None");
        try {
            method.invoke(plotModelListener, new Object[] { plotModel });
        } catch (Exception exc) {
            idv.logException("Clearing plot model", exc);
        }
    }

    /**
     * edit the plot model
     */
    public void editPlotModel() {
        if (plotModel != null) {
            idv.getStationModelManager().show(plotModel);
        } else {
            idv.getStationModelManager().show(new StationModel(StationModelManager.NEW_LAYOUT_MODEL, new ArrayList()));
        }
    }

    /**
     * get the plot model
     *
     * @return the plot model
     */
    public StationModel getPlotModel() {
        return plotModel;
    }

    /**
     * Utility method to set the plot model by name
     *
     * @param name the name of the plot model
     */
    public void setPlotModelByName(String name) {
        setPlotModel(idv.getStationModelManager().getStationModel(name));
    }

    /**
     * set the plot model
     *
     * @param sm the plot model
     */
    public void setPlotModel(StationModel sm) {
        this.plotModel = sm;
        if (sm != null) {
            label.setText(sm.getDisplayName());
        } else {
            label.setText("None");
        }
        label.repaint();
    }

}
