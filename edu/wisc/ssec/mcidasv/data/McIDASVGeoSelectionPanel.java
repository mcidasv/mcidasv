/*
 * $Id$
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Found2ation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package edu.wisc.ssec.mcidasv.data;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.GeneralPath;

import javax.swing.*;

import java.util.ArrayList;
import java.util.List;

import ucar.unidata.data.*;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import ucar.unidata.gis.mcidasmap.McidasMap;

import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.ui.LatLonWidget;

import ucar.unidata.util.GuiUtils;

import ucar.unidata.view.CompositeRenderer;
import ucar.unidata.view.Renderer;

import ucar.unidata.view.geoloc.*;

/**
 * Holds geo-location information  - lat/lon bounding box, image size, etc.
 * This is used to pass information from a chooser into a datasource.
 */
public class McIDASVGeoSelectionPanel extends GeoSelectionPanel {


    /**
     * This is the extra component at the top of the panel. It can be null. It is used
     * for, e.g., to show the grid size
     */
    private JComponent extraComponent;

    /** Shows the area widgets */
    private JComponent areaComponent;

    /** The geo selection */
    private GeoSelection geoSelection;

    /** for properties */
    private NavigatedMapPanel mapPanel;

    /** for properties */
    private LatLonWidget ulLatLon;

    /** for properties */
    private LatLonWidget lrLatLon;

    /** for properties */
    private JComponent latLonPanel;

    /** Are we enabled */
    private boolean enabled = false;

    /** for properties */
    private JCheckBox enabledCbx;

    private ProjectionImpl sampleProj;

    private int lineMag;
    private int elementMag;

    /** name of default map */
    public static final String DEFAULT_MAP = "/auxdata/maps/OUTLSUPW";


    /**
     * ctor
     *
     * @param geoSelection The geo selection
     * @param fullVersion If true we show the decimation along with the area subset in one component
     * @param enabled Initially enabled
     * @param doStride Show decimation
     * @param doBoundingBox Show area subset
     * @param sampleProjection Sample projection for the area map. May be null.
     * @param extraComponent Extra stuff for gui. example: the grid size label. May be null.
     */
    public McIDASVGeoSelectionPanel(GeoSelection geoSelection, boolean fullVersion,
                             boolean enabled, boolean doStride,
                             boolean doBoundingBox,
                             ProjectionImpl sampleProjection,
                             JComponent extraComponent,
                             int lineMag, int elementMag) {
        super(geoSelection, fullVersion, enabled, doStride, doBoundingBox,
              sampleProjection, extraComponent);
        this.sampleProj = sampleProjection;
        this.geoSelection   = geoSelection;
        this.extraComponent = extraComponent;
        this.enabled        = enabled;
        this.lineMag = lineMag;
        this.elementMag = elementMag;
        setLayout(new BorderLayout());
        this.add(BorderLayout.CENTER,
                 makePanel(fullVersion, doStride, doBoundingBox,
                           sampleProjection));
    }


    /**
     * Make the panel for the propeties dialog
     *
     *
     * @param fullVersion If true we show the decimation along with the area subset in one component
     * @param doStride Show stride widgets
     * @param doBoundingBox Show subset widget
     * @param sampleProjection Use in map panel. May be null.
     *
     * @return The properties panel
     */

    private JComponent makePanel(boolean fullVersion, boolean doStride,
                                 boolean doBoundingBox,
                                 ProjectionImpl sampleProjection) {
        List boxComps    = new ArrayList();

        if (doBoundingBox) {
            ActionListener actionListener = new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    setBoundsFromFields();
                }
            };

            ulLatLon = new LatLonWidget("Lat:", "Lon:", actionListener);

            lrLatLon = new LatLonWidget("Lat:", "Lon:", actionListener);
            latLonPanel = GuiUtils.doLayout(new Component[] {
                GuiUtils.inset(GuiUtils.wrap(ulLatLon.getLatField()), 5),
                GuiUtils.hbox(ulLatLon.getLonField(), lrLatLon.getLonField(),
                              5),
                GuiUtils.inset(GuiUtils.wrap(lrLatLon.getLatField()),
                               5) }, 1, GuiUtils.WT_N, GuiUtils.WT_N);
            JPanel leftPanel = GuiUtils.vbox(latLonPanel, null);

            mapPanel = new MyNavigatedMapPanel(this, sampleProjection,
                    fullVersion);

            CompositeRenderer mapRenderer = new CompositeRenderer();
            mapRenderer.addRenderer(new McidasMap("/auxdata/maps/OUTLSUPW"));
            mapRenderer.addRenderer(new McidasMap("/auxdata/maps/OUTLSUPU"));
            mapRenderer.setColor(Color.blue);

            mapPanel.setProjectionImpl(sampleProjection);
            mapPanel.addMapRenderer(mapRenderer);
            mapPanel.setMapRenderer(mapRenderer);
            mapPanel.setPreferredSize(new Dimension(400, 300));
            if (geoSelection.getBoundingBox() != null) {
                GeoLocationInfo bb = geoSelection.getBoundingBox();
                selectedRegionChanged(bb.getLatLonRect());
            } else {
                selectedRegionChanged(null);
            }
            boxComps.add(
                GuiUtils.topCenter(
                    GuiUtils.inset(
                        GuiUtils.rLabel("Bounding Box:"),
                        new Insets(10, 0, 0, 0)), GuiUtils.inset(
                            leftPanel, new Insets(0, 0, 60, 0))));
            boxComps.add(mapPanel);
            mapPanel.repaint();
        }

        JTabbedPane tab = GuiUtils.getNestedTabbedPane();
        return tab;
    }


    /**
     * Get the component that shows the area map. Used by the DataControlDialog
     * to show the map in  a separate tab
     *
     * @return area map component
     */
    public JComponent getAreaComponent() {
        if ((areaComponent == null) && (mapPanel != null)) {
            JLabel tmp  = new JLabel(" ");
            Font   font = tmp.getFont();
            font = font.deriveFont((float) (font.getSize() - 2.0f));
            mapPanel.setPreferredSize(new Dimension(100, 100));
            ulLatLon.getLatField().setColumns(4);
            ulLatLon.getLonField().setColumns(4);
            lrLatLon.getLonField().setColumns(4);
            lrLatLon.getLatField().setColumns(4);
            GuiUtils.setFontOnTree(latLonPanel, font);
            GuiUtils.tmpInsets = new Insets(3, 0, 0, 0);
            latLonPanel = GuiUtils.vbox(
                GuiUtils.wrap(ulLatLon.getLatField()),
                GuiUtils.hbox(
                    ulLatLon.getLonField(), lrLatLon.getLonField(),
                    3), GuiUtils.wrap(lrLatLon.getLatField()));
            JPanel leftPanel = GuiUtils.vbox(latLonPanel, null);
            areaComponent = mapPanel;
        }
        return areaComponent;
    }


    /**
     * Get the selected latlon area rect. May be null
     *
     * @return Area rectangle
     */
    protected LatLonRect getLatLonRect() {
        LatLonRect returnRect = null;
        if ((ulLatLon != null) && ulLatLon.isLatLonDefined()
                && lrLatLon.isLatLonDefined()) {
            LatLonPoint ul = new LatLonPointImpl(ulLatLon.getLat(),
                                 ulLatLon.getLon());
            LatLonPoint lr = new LatLonPointImpl(lrLatLon.getLat(),
                                 lrLatLon.getLon());
            returnRect = new LatLonRect(ul, lr);
            ProjectionRect pr = mapPanel.getNavigatedPanel().earthToWorld(returnRect);
            returnRect = sampleProj.projToLatLonBB(pr);
            return returnRect;
        }
        ProjectionRect pr = mapPanel.getNavigatedPanel().getSelectedRegion();
        if (pr != null)
        returnRect = sampleProj.projToLatLonBB(pr);
        return returnRect;
    }


    /**
     * Set the bounds from the field
     */
    private void setBoundsFromFields() {
        NavigatedPanel np = mapPanel.getNavigatedPanel();
        np.setSelectedRegion(getLatLonRect());
        mapPanel.repaint();
    }


    /**
     * Apply the properties
     *
     *
     * @param geoSelection The geo selection to apply to
     * @return false if it failed
     */

    public boolean applyProperties(GeoSelection geoSelection) {
        if (mapPanel != null) {
            LatLonRect llr = getLatLonRect();
            if (llr == null) {
                geoSelection.setBoundingBox(null);
            } else {
                geoSelection.setBoundingBox(new GeoLocationInfo(llr));
            }
            GeoLocationInfo bbox =geoSelection.getBoundingBox();
            if (bbox == null) return false;
            llr = bbox.getLatLonRect();
/*
            LatLonPoint ulp = llr.getUpperLeftPoint();
            LatLonPoint urp = llr.getUpperRightPoint();
            LatLonPoint lrp = llr.getLowerRightPoint();
            LatLonPoint llp = llr.getLowerLeftPoint();
*/
        }
        return true;
    }

    /**
     * Region changed. Update the latlon widgets
     *
     * @param llr new region
     */
    protected void selectedRegionChanged(LatLonRect llr) {
        if (ulLatLon == null) {
            return;
        }
        GuiUtils.enableTree(latLonPanel, llr != null);
        if (llr == null) {
            return;
        }
        LatLonPointImpl    ul = llr.getUpperLeftPoint();
        LatLonPointImpl    lr = llr.getLowerRightPoint();
        DisplayConventions dc = DisplayConventions.getDisplayConventions();
        ulLatLon.setLatLon(dc.formatLatLon(ul.getLatitude()),
                           dc.formatLatLon(ul.getLongitude()));
        lrLatLon.setLatLon(dc.formatLatLon(lr.getLatitude()),
                           dc.formatLatLon(lr.getLongitude()));
    }


    /**
     * Class MyNavigatedMapPanel shows the subset panel
     *
     *
     * @author IDV Development Team
     * @version $Revision$
     */
    public static class MyNavigatedMapPanel extends NavigatedMapPanel {

        /** The panel_ */
        McIDASVGeoSelectionPanel geoPanel;

        /** Points to draw */
        private List points = new ArrayList();

        /**
         * ctor
         *
         *
         * @param geoPanel The panel
         * @param sampleProjection may be null
         * @param fullVersion full version or truncated
         */
        public MyNavigatedMapPanel(McIDASVGeoSelectionPanel geoPanel,
                                   ProjectionImpl sampleProjection,
                                   boolean fullVersion) {
            //super(true, fullVersion);


            this.geoPanel = geoPanel;
            NavigatedPanel np = getNavigatedPanel();
            if (sampleProjection != null) {
                setProjectionImpl(sampleProjection);
                ProjectionRect r = sampleProjection.getDefaultMapArea();
                np.setSelectedRegionBounds(r);
                points.add(new ProjectionPointImpl(r.getX(), r.getY()));
                points.add(new ProjectionPointImpl(r.getX() + r.getWidth(),
                        r.getY()));
                points.add(new ProjectionPointImpl(r.getX() + r.getWidth(),
                        r.getY() + r.getHeight()));
                points.add(new ProjectionPointImpl(r.getX(),
                        r.getY() + r.getHeight()));
            }
            np.setSelectRegionMode(true);

            if (geoPanel.geoSelection.getBoundingBox() != null) {
                np.setSelectedRegion(
                    geoPanel.geoSelection.getBoundingBox().getLatLonRect());
            }

            np.zoom(0.6);
        }



        /**
         * Update
         *
         * @param llr new  region
         */
        protected void _selectedRegionChanged(LatLonRect llr) {
            geoPanel.selectedRegionChanged(llr);
        }


        /**
         * Make panel
         *
         * @return map panel
         */
        protected NavigatedPanel doMakeMapPanel() {
            return new NavigatedPanel() {
                protected void selectedRegionChanged() {
                    super.selectedRegionChanged();
                    LatLonRect llr = getSelectedEarthRegion();
                    _selectedRegionChanged(llr);
                }
            };
        }

        /**
         * Draw extra stuff on the map
         *
         * @param gNP Graphics to draw into
         */
        protected void annotateMap(Graphics2D gNP) {
            super.annotateMap(gNP);
            NavigatedPanel np = getNavigatedPanel();
            if (points.size() == 0) {
                return;
            }
            GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD,
                                   points.size());
            for (int i = 0; i <= points.size(); i++) {
                ProjectionPoint ppi;
                LatLonPoint     llp;
                if (i >= points.size()) {
                    ppi = (ProjectionPoint) points.get(0);
                } else {
                    ppi = (ProjectionPoint) points.get(i);
                }
                if (i == 0) {
                    path.moveTo((float) ppi.getX(), (float) ppi.getY());
                } else {
                    path.lineTo((float) ppi.getX(), (float) ppi.getY());
                }
            }
            gNP.draw(path);
        }


    }


    protected ProjectionImpl getSampleProjection() {
        return sampleProj;
    }

    protected int getLineMagnification() {
        return lineMag;
    }

    protected int getElementMagnification() {
        return elementMag;
    }

    protected void setMapArea(ProjectionRect pr) {
        NavigatedPanel np = mapPanel.getNavigatedPanel();
    }

}

