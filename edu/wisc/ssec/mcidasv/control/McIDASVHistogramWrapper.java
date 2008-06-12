package edu.wisc.ssec.mcidasv.control;

import ucar.unidata.idv.control.chart.DataChoiceWrapper;
import ucar.unidata.idv.control.chart.HistogramWrapper;
import ucar.unidata.idv.control.chart.MyHistogramDataset;

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.event.*;
import org.jfree.chart.labels.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.urls.*;
import org.jfree.data.*;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import org.jfree.ui.*;



import ucar.unidata.data.DataChoice;


import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.sounding.TrackDataSource;


import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.util.GuiUtils;


import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;





import ucar.visad.GeoUtils;
import ucar.visad.Util;
import ucar.visad.display.*;

import visad.*;

import visad.georef.*;

import visad.util.BaseRGBMap;

import visad.util.ColorPreview;


import java.awt.*;
import java.awt.event.*;


import java.rmi.RemoteException;




import java.text.SimpleDateFormat;



import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;



/**
 * Provides a time series chart
 *
 *
 * @author IDV Development Team
 * @version $Revision$
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
    //private MultiSpectralControl myControl;

    /**
     * Default ctor
     */
    public McIDASVHistogramWrapper() {}



    /**
     * Ctor
     *
     * @param name The name
     * @param dataChoices List of data choices
     */
    //public McIDASVHistogramWrapper(String name, List dataChoices, MultiSpectralControl control) {
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
                                             PlotOrientation.VERTICAL, true,
                                             false, false);
        chart.getXYPlot().setForegroundAlpha(0.75f);
        plot = (XYPlot) chart.getPlot();
        initXYPlot(plot);
        chartPanel = doMakeChartPanel(chart);
    }

    // hijack the UI from idv-land
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
            for (int dataSetIdx = 0; dataSetIdx < plot.getDatasetCount();
                    dataSetIdx++) {
                MyHistogramDataset dataset =
                    (MyHistogramDataset) plot.getDataset(dataSetIdx);
                dataset.removeAllSeries();
            }

            //            dataset.removeAllSeries();
            Hashtable props = new Hashtable();
/*
            props.put(TrackDataSource.PROP_TRACKTYPE,
                      TrackDataSource.ID_TIMETRACE);
*/
            for (int paramIdx = 0; paramIdx < dataChoiceWrappers.size();
                    paramIdx++) {
                DataChoiceWrapper wrapper =
                    (DataChoiceWrapper) dataChoiceWrappers.get(paramIdx);

                DataChoice dataChoice = wrapper.getDataChoice();
                props = dataChoice.getProperties();
//                FlatField data =
//                    getFlatField((FieldImpl) dataChoice.getData(null, props));
                Unit unit =
                    ucar.visad.Util.getDefaultRangeUnits((FlatField) data)[0];
                double[][] samples = data.getValues(false);
                double[] actualValues = filterData(samples[0],
                                            getTimeValues(samples, data))[0];
                final NumberAxis domainAxis =
                    new NumberAxis(wrapper.getLabel(unit));

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
                        double low = range.getLowerBound();
                        double high = range.getUpperBound();
                        Class myClass = myControl.getClass();
                        if (myClass.isInstance(new MultiSpectralControl())) {
                            MultiSpectralControl msc = (MultiSpectralControl)myControl;
                            msc.contrastStretch(low, high);
                        }
                        else if (myClass.isInstance(new TestImagePlanViewControl())) {
                            TestImagePlanViewControl tipv = (TestImagePlanViewControl)myControl;
                            tipv.contrastStretch(low, high);
                        }
                        ValueAxis rangeAxis = plot.getRangeAxis();
                    }
                });


            }

        } catch (Exception exc) {
            LogUtil.logException("Error creating data set", exc);
            return;
        }
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
            axis.setAutoRange(true);
        }
    }
}

