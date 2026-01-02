/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2026
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

package ucar.unidata.idv.control.chart;


import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.statistics.HistogramType;


import ucar.unidata.data.DataChoice;


import ucar.unidata.data.sounding.TrackDataSource;


import ucar.unidata.util.GuiUtils;


import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import visad.*;


import java.awt.*;


import java.rmi.RemoteException;


import java.util.Hashtable;
import java.util.List;


import javax.swing.*;


/**
 * Provides a time series chart
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.16 $
 */
public class HistogramWrapper extends PlotWrapper {

    /** The plot */
    protected XYPlot plot;


    /** How many bins in the histgram */
    protected int bins = 100;

    /** Is the histogram stacked bars. Does not work right now */
    protected boolean stacked = false;


    /** for properties dialog */
    protected JTextField binFld;

    /** for properties dialog */
    protected JCheckBox stackedCbx;



    /**
     * Default ctor
     */
    public HistogramWrapper() {}



    /**
     * Ctor
     *
     * @param name The name
     * @param dataChoices List of data choices
     */
    public HistogramWrapper(String name, List dataChoices) {
        super(name, dataChoices);
    }


    /**
     * Type name
     *
     * @return Type name
     */
    public String getTypeName() {
        return "Histogram";
    }


    /**
     * Create the chart
     */
    protected void createChart() {
        if (chartPanel != null) {
            return;
        }

        MyHistogramDataset dataset = new MyHistogramDataset();
        chart = ChartFactory.createHistogram("Histogram", null, null,
                                             dataset,
                                             PlotOrientation.VERTICAL, true,
                                             false, false);
        chart.getXYPlot().setForegroundAlpha(0.75f);
        plot = (XYPlot) chart.getPlot();
        initXYPlot(plot);
        chartPanel = doMakeChartPanel(chart);
    }

    /**
     *
     * Create the chart if needed
     *
     * @return The gui contents
     */
    protected JComponent doMakeContents() {
        createChart();
        return chartPanel;
    }


    /**
     * Create the charts
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void loadData() {
        createChart();
        plotHistogram(this);
    }
    
    /**
     * Plot the displayed {@link DataChoice}.
     * 
     * @param histoWrapper Cannot be {@code null}.
     */
    public static void plotHistogram(HistogramWrapper histoWrapper) {
        XYPlot p = histoWrapper.plot;
        List<DataChoiceWrapper> dcWrappers = histoWrapper.getDataChoiceWrappers();
        
        try {
            for (int dataSetIdx = 0; dataSetIdx < p.getDatasetCount();
                 dataSetIdx++) {
                MyHistogramDataset dataset =
                    (MyHistogramDataset) p.getDataset(dataSetIdx);
                dataset.removeAllSeries();
            }
            
            Hashtable props = new Hashtable();
            props.put(TrackDataSource.PROP_TRACKTYPE,
                TrackDataSource.ID_TIMETRACE);
                
            for (int paramIdx = 0; paramIdx < dcWrappers.size();
                 paramIdx++) {
                DataChoiceWrapper wrapper = dcWrappers.get(paramIdx);
                
                DataChoice dataChoice = wrapper.getDataChoice();
                FlatField data =
                    histoWrapper.getFlatField((FieldImpl) dataChoice.getData(null, props));
                Unit unit =
                    ucar.visad.Util.getDefaultRangeUnits((FlatField) data)[0];
                double[][] samples = data.getValues(false);
                double[] actualValues = histoWrapper.filterData(samples[0],
                    histoWrapper.getTimeValues(samples, data))[0];
                NumberAxis domainAxis =
                    new NumberAxis(wrapper.getLabel(unit));
        
                XYItemRenderer renderer;
                if (histoWrapper.stacked) {
                    renderer = new StackedXYBarRenderer();
                } else {
                    renderer = new XYBarRenderer();
                }
                p.setRenderer(paramIdx, renderer);
                Color c = wrapper.getColor(paramIdx);
                domainAxis.setLabelPaint(c);
                renderer.setSeriesPaint(0, c);
                
                MyHistogramDataset dataset = new MyHistogramDataset();
                dataset.setType(HistogramType.FREQUENCY);
                dataset.addSeries(dataChoice.getName() + " [" + unit + "]",
                    actualValues, histoWrapper.bins);
                p.setDomainAxis(paramIdx, domainAxis, false);
                p.mapDatasetToDomainAxis(paramIdx, paramIdx);
                p.setDataset(paramIdx, dataset);
            }
        } catch (VisADException | RemoteException e) {
            LogUtil.logException("Error creating data set", e);
        }
    }

    /**
     * Add components to properties dialog
     *
     * @param comps  List of components
     * @param tabIdx Which tab in properties dialog
     */
    protected void getPropertiesComponents(List comps, int tabIdx) {
        super.getPropertiesComponents(comps, tabIdx);
        if (tabIdx != 0) {
            return;
        }
        comps.add(GuiUtils.rLabel("Histogram: "));

        comps.add(
            GuiUtils.left(
                GuiUtils.hbox(
                    Misc.newList(
                        new JLabel("Number of Bins: "),
                        binFld = new JTextField("" + bins, 6)), 4)));
        //                                             new JLabel("      Stacked: "),
        //stackedCbx = new JCheckBox("",stacked)),4)));
    }


    /**
     * Apply properties
     *
     *
     * @return Was successful
     */
    protected boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        try {
            bins = Integer.parseInt(binFld.getText().trim());
        } catch (NumberFormatException nfe) {
            LogUtil.userErrorMessage("Bad value for bins: "
                                     + binFld.getText());
            return false;
        }
        plotHistogram(this);
        //        stacked = stackedCbx.isSelected();
        return true;
    }


    /**
     * Can we do chart colors
     *
     * @return can do colors
     */
    protected boolean canDoColors() {
        return true;
    }

    /**
     * Can we have colors on the data chocie wrappers in the properties dialog
     *
     * @return can do wrapper color
     */
    public boolean canDoWrapperColor() {
        return true;
    }



    /**
     * Set the Bins property.
     *
     * @param value The new value for Bins
     */
    public void setBins(int value) {
        bins = value;
    }

    /**
     * Get the Bins property.
     *
     * @return The Bins
     */
    public int getBins() {
        return bins;
    }


    /**
     * Set the Stacked property.
     *
     * @param value The new value for Stacked
     */
    public void setStacked(boolean value) {
        stacked = value;
    }

    /**
     * Get the Stacked property.
     *
     * @return The Stacked
     */
    public boolean getStacked() {
        return stacked;
    }





}

