package ucar.unidata.idv.control.mcidas;


import ucar.unidata.idv.control.ControlWidget;
import ucar.unidata.idv.control.DisplayControlImpl;

import ucar.unidata.util.GuiUtils;

import java.util.ArrayList;
import java.util.List;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;




/**
 * Class for wrapping a set of widgets.
 */
public class McIDASWrapperWidget extends ControlWidget {

    /** first component */
    Component c1;

    /** second component */
    Component c2;

    /** third component */
    Component c3;

    /** fourth component */
    Component c4;


    /**
     * Wrap four components.
     *
     * @param control   control for widget
     * @param c1        first component
     * @param c2        second component
     * @param c3        third component
     * @param c4        fourth component
     *
     */
    public McIDASWrapperWidget(DisplayControlImpl control, Component c1,
                         Component c2, Component c3, Component c4) {
        super(control);
        this.c1 = c1;
        this.c2 = c2;
        this.c3 = c3;
        this.c4 = c4;
    }

    /**
     * Fill the list of components with this widgets components.
     *
     * @param l            list to fill
     * @param columns      number of columns
     */
    public void fillList(List l, int columns) {
        l.add(c1);
        l.add(GuiUtils.doLayout(new Component[]{ c2, new Label(" "), c3 , new Label(" "), c4},
                                5, GuiUtils.WT_YNY, GuiUtils.WT_N));
    }
}
