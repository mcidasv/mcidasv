/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
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

package edu.wisc.ssec.mcidasv.control;

import java.awt.Color;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.Range;
import org.jfree.data.statistics.HistogramType;

import visad.FlatField;
import visad.Unit;
import visad.VisADException;

import ucar.unidata.data.DataChoice;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.chart.DataChoiceWrapper;
import ucar.unidata.idv.control.chart.HistogramWrapper;
import ucar.unidata.idv.control.chart.MyHistogramDataset;
import ucar.unidata.util.LogUtil;

/**
 * Wraps a JFreeChart histograms to ease working with VisAD data.
 */
public class McIDASVHistogramWrapper extends HistogramWrapper {

    /** The plot */
    private XYPlot plot;

    /** How many bins in the histgram */
    private int bins = 100;

    /** Is the histogram stacked bars. Does not work right now */
    private boolean stacked = false;


    /** for properties dialog */
    private JTextField binFld;

    /** for properties dialog */
    private JCheckBox stackedCbx;

    private DisplayControlImpl myControl;

    private double low;

    private double high;

    /**
     * Default ctor
     */
    public McIDASVHistogramWrapper() {}

    /**
     * Ctor
     *
     * @param name The name.
     * @param dataChoices List of data choices.
     * @param control {@literal "Parent"} control.
     */
    public McIDASVHistogramWrapper(String name, List dataChoices, DisplayControlImpl control) {
        super(name, dataChoices);
        this.myControl = control;
    }

    /**
     * Create the chart
     */
    private void createChart() {
        if (chartPanel != null) {
            return;
        }

        MyHistogramDataset dataset = new MyHistogramDataset();
        chart = ChartFactory.createHistogram("Histogram", null, null,
                                             dataset,
                                             PlotOrientation.VERTICAL, false,
                                             false, false);
        chart.getXYPlot().setForegroundAlpha(0.75f);
        plot = (XYPlot) chart.getPlot();
        initXYPlot(plot);
        chartPanel = doMakeChartPanel(chart);
    }

    public JComponent doMakeContents() {
        return super.doMakeContents();
    }

    /**
     * Create the charts
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void loadData(FlatField data) throws VisADException, RemoteException {
        createChart();
        List dataChoiceWrappers = getDataChoiceWrappers();
        try {
            for (int dataSetIdx = 0; dataSetIdx < plot.getDatasetCount(); dataSetIdx++) {
                MyHistogramDataset dataset = (MyHistogramDataset)plot.getDataset(dataSetIdx);
                dataset.removeAllSeries();
            }

            Hashtable props = new Hashtable();
            for (int paramIdx = 0; paramIdx < dataChoiceWrappers.size(); paramIdx++) {
                DataChoiceWrapper wrapper = (DataChoiceWrapper)dataChoiceWrappers.get(paramIdx);

                DataChoice dataChoice = wrapper.getDataChoice();
                props = dataChoice.getProperties();
                Unit unit = ucar.visad.Util.getDefaultRangeUnits((FlatField) data)[0];
                double[][] samples = data.getValues(false);
                double[] actualValues = filterData(samples[0], getTimeValues(samples, data))[0];
                final NumberAxis domainAxis = new NumberAxis(wrapper.getLabel(unit));

                domainAxis.setAutoRangeIncludesZero(false);

                XYItemRenderer renderer;
                if (stacked) {
                    renderer = new StackedXYBarRenderer();
                } else {
                    renderer = new XYBarRenderer();
                }
                plot.setRenderer(paramIdx, renderer);
                Color c = wrapper.getColor(paramIdx);
                domainAxis.setLabelPaint(c);
                renderer.setSeriesPaint(0, c);

                MyHistogramDataset dataset = new MyHistogramDataset();
                dataset.setType(HistogramType.FREQUENCY);
                dataset.addSeries(dataChoice.getName() + " [" + unit + "]",
                                  actualValues, bins);
                plot.setDomainAxis(paramIdx, domainAxis, false);
                plot.mapDatasetToDomainAxis(paramIdx, paramIdx);
                plot.setDataset(paramIdx, dataset);

                domainAxis.addChangeListener(new AxisChangeListener() {
                    public void axisChanged(AxisChangeEvent ae) {
                        Range range = domainAxis.getRange();
                        double newLow = Math.floor(range.getLowerBound()+0.5);
                        double newHigh = Math.floor(range.getUpperBound()+0.5);
                        try {
                            ucar.unidata.util.Range newRange = new ucar.unidata.util.Range(newLow, newHigh);
                            myControl.setRange(newRange);
                        } catch (Exception e) {
                            System.out.println("Can't set new range e=" + e);
                        }
                        ValueAxis rangeAxis = plot.getRangeAxis();
                    }
                });

                Range range = domainAxis.getRange();
                low = range.getLowerBound();
                high = range.getUpperBound();
            }

        } catch (Exception exc) {
            System.out.println("Exception exc=" + exc);
            LogUtil.logException("Error creating data set", exc);
            return;
        }
    }

    protected boolean modifyRange(int lowVal, int hiVal) {
        try {
            if (plot == null) {
                return false;
            }
            ValueAxis domainAxis = plot.getDomainAxis();
            domainAxis.setRange((double)lowVal, (double)hiVal); 
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    protected Range getRange() {
        ValueAxis domainAxis = plot.getDomainAxis();
        return domainAxis.getRange();
    }

    protected void doReset() {
        resetPlot();
    }

    /**
     * reset the axis'
     */
    private void resetPlot() {
        if (chart == null) {
            return;
        }
        if ( !(chart.getPlot() instanceof XYPlot)) {
            return;
        }
        XYPlot plot = (XYPlot) chart.getPlot();
        int    rcnt = plot.getRangeAxisCount();
        for (int i = 0; i < rcnt; i++) {
            ValueAxis axis = (ValueAxis) plot.getRangeAxis(i);
            axis.setAutoRange(true);
        }
        int dcnt = plot.getDomainAxisCount();
        for (int i = 0; i < dcnt; i++) {
            ValueAxis axis = (ValueAxis) plot.getDomainAxis(i);
            axis.setRange(low, high);
            axis.setAutoRange(false);
        }
    }

    public double getLow() {
        return low;
    }

    public void setLow(double val) {
        low = val;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double val) {
        high = val;
    }
}

