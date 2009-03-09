/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.idv.control.ControlWidget;
import ucar.unidata.idv.control.WrapperWidget;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Range;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.DisplayableData;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.FlatField;
import visad.RealTuple;
import visad.VisADException;
import visad.georef.MapProjection;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.data.hydra.HydraRGBDisplayable;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralDataSource;
import edu.wisc.ssec.mcidasv.data.hydra.SpectrumAdapter;
import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;
import edu.wisc.ssec.mcidasv.probes.ProbeEvent;
import edu.wisc.ssec.mcidasv.probes.ProbeListener;
import edu.wisc.ssec.mcidasv.probes.ReadoutProbeDeux;

public class MultiSpectralControl extends HydraControl {

    private String PARAM = "BrightnessTemp";

    private static final int DEFAULT_FLAGS = 
        FLAG_COLORTABLE | FLAG_ZPOSITION;

    private MultiSpectralDisplay display;

    private DisplayMaster displayMaster;

    private final JTextField wavenumbox =  
        new JTextField(Float.toString(0f), 12);

    final JTextField minBox = new JTextField(6);
    final JTextField maxBox = new JTextField(6);

    private final List<Hashtable<String, Object>> spectraProperties = new ArrayList<Hashtable<String, Object>>();
    private final List<NewSpectrum> spectra = new ArrayList<NewSpectrum>();

    private McIDASVHistogramWrapper histoWrapper;

    private final JTable probeTable = new JTable(new ProbeTableModel(this, spectra));
    private final JScrollPane scrollPane = new JScrollPane(probeTable);

    private float rangeMin;
    private float rangeMax;

    public MultiSpectralControl() {
        super();
        setHelpUrl("idv.controls.hydra.multispectraldisplaycontrol");
    }

    @Override public boolean init(final DataChoice choice)
        throws VisADException, RemoteException 
    {
        Hashtable props = choice.getProperties();
        PARAM = (String) props.get(MultiSpectralDataSource.paramKey);

        List<DataChoice> choices = Collections.singletonList(choice);
        histoWrapper = new McIDASVHistogramWrapper("histo", choices, this);

        Float fieldSelectorChannel =
            (Float)getDataSelection().getProperty(Constants.PROP_CHAN);
        if (fieldSelectorChannel == null)
            fieldSelectorChannel = 0f;

        display = new MultiSpectralDisplay(this);
        display.setWaveNumber(fieldSelectorChannel);

        displayMaster = getViewManager().getMaster();

        //- intialize the Displayable with data before adding to DisplayControl
        DisplayableData imageDisplay = display.getImageDisplay();
        FlatField image = display.getImageData();

        float[] rngvals = (image.getFloats(false))[0];
        float[] minmax = minmax(rngvals);
        rangeMin = minmax[0];
        rangeMax = minmax[1];

        imageDisplay.setData(display.getImageData());
        addDisplayable(imageDisplay, DEFAULT_FLAGS);

        // put the multispectral display into the layer controls
        addViewManager(display.getViewManager());

        // tell the idv what options to give the user
        setAttributeFlags(DEFAULT_FLAGS);

        setProjectionInView(true);

        probeTable.setDefaultRenderer(Color.class, new ColorRenderer(true));
        probeTable.setDefaultEditor(Color.class, new ColorEditor());
        probeTable.setPreferredScrollableViewportSize(new Dimension(500, 200));
        probeTable.setUI(new HackyDragDropRowUI());

        return true;
    }

    @Override public void initDone() {
        try {
            display.showChannelSelector();

            // TODO: this is ugly.
            Float fieldSelectorChannel =
                (Float)getDataSelection().getProperty(Constants.PROP_CHAN);
            if (fieldSelectorChannel == null)
                fieldSelectorChannel = 0f;
            handleChannelChange(fieldSelectorChannel, false);

            displayMaster.setDisplayInactive();

            // this if-else block is detecting whether or not a bundle is
            // being loaded; if true, then we'll have a list of spectra props.
            // otherwise just throw two default spectrums/probes on the screen.
            if (!spectraProperties.isEmpty()) {
                for (Hashtable<String, Object> table : spectraProperties) {
                    Color c = (Color)table.get("color");
                    NewSpectrum s = addSpectrum(c);
                    s.setProperties(table);
                }
                spectraProperties.clear();
            } else {
                addSpectra(new Color(153, 204, 255), Color.MAGENTA, Color.RED, Color.YELLOW);
            }
            pokeSpectra();
            displayMaster.setDisplayActive();
        } catch (Exception e) {
            logException("MultiSpectralControl.initDone", e);
        }
    }

    // this will get called before init() by the IDV's bundle magic.
    public void setSpectraProperties(final List<Hashtable<String, Object>> props) {
        spectraProperties.clear();
        spectraProperties.addAll(props);
    }

    public List<Hashtable<String, Object>> getSpectraProperties() {
        List<Hashtable<String, Object>> props = new ArrayList<Hashtable<String, Object>>();
        for (NewSpectrum s : spectra) {
            props.add(s.getProperties());
        }
        return props;
    }

    protected void updateList(final List<NewSpectrum> updatedSpectra) {
        spectra.clear();

        List<String> dataRefIds = new ArrayList<String>(updatedSpectra.size());
        for (NewSpectrum spectrum : updatedSpectra) {
            dataRefIds.add(spectrum.getSpectrumRefName());
            spectra.add(spectrum);
        }
        display.reorderDataRefsById(dataRefIds);
    }

    protected void addSpectra(final Color... colors) {
        NewSpectrum currentSpectrum = null;
        try {
            for (int i = colors.length-1; i >= 0; i--) {
                Color color = colors[i];
                currentSpectrum = new NewSpectrum(this, color);
                spectra.add(currentSpectrum);
            }
            ((ProbeTableModel)probeTable.getModel()).updateWith(spectra);
        } catch (Exception e) {
            LogUtil.logException("MultiSpectralControl.addSpectra: error while adding spectra", e);
        }
    }

    public NewSpectrum addSpectrum(final Color color) {
        NewSpectrum spectrum = null;
        try {
            spectrum = new NewSpectrum(this, color);
            spectra.add(spectrum);
        } catch (Exception e) {
            LogUtil.logException("MultiSpectralControl.addSpectrum: error creating new spectrum", e);
        }
        ((ProbeTableModel)probeTable.getModel()).updateWith(spectra);
        return spectrum;
    }

    public void removeSpectra() {
        try {
            for (NewSpectrum s : spectra)
                s.removeValueDisplay();
        } catch (Exception e) {
            LogUtil.logException("MultiSpectralControl.removeSpectra: error removing spectrum", e);
        }
    }

    public void pokeSpectra() {
        for (NewSpectrum s : spectra)
            s.pokeValueDisplay();
        try {
            display.refreshDisplay();
        } catch (Exception e) {
            
        }
    }

    @Override public DataSelection getDataSelection() {
        DataSelection selection = super.getDataSelection();
        if (display != null) {
            selection.putProperty(Constants.PROP_CHAN, display.getWaveNumber());
            try {
                selection.putProperty(SpectrumAdapter.channelIndex_name, display.getChannelIndex());
            } catch (Exception e) {
                LogUtil.logException("MultiSpectralControl.getDataSelection", e);
            }
        }
        return selection;
    }

    @Override public void setDataSelection(final DataSelection newSelection) {
        super.setDataSelection(newSelection);
    }

    @Override public MapProjection getDataProjection() {
        MapProjection mp = null;
        Rectangle2D rect =
            MultiSpectralData.getLonLatBoundingBox(display.getImageData());

        try {
            mp = new LambertAEA(rect);
        } catch (Exception e) {
            logException("MultiSpectralControl.getDataProjection", e);
        }

        return mp;
    }

    public static float[] minmax(float[] values) {
      float min =  Float.MAX_VALUE;
      float max = -Float.MAX_VALUE;
      for (int k = 0; k < values.length; k++) {
        float val = values[k];
        if ((val == val) && (val < Float.POSITIVE_INFINITY) && (val > Float.NEGATIVE_INFINITY)) {
          if (val < min) min = val;
          if (val > max) max = val;
        }
      }
      return new float[] {min, max};
    }


    @Override protected Range getInitialRange() throws VisADException,
        RemoteException
    {
        return new Range(rangeMin, rangeMax);
    }

    @Override protected ColorTable getInitialColorTable() {
        return getDisplayConventions().getParamColorTable(PARAM);
    }

    @Override public Container doMakeContents() {
        try {
            JTabbedPane pane = new JTabbedPane();
            pane.add("Display", GuiUtils.inset(getDisplayTab(), 5));
            pane.add("Settings", 
                     GuiUtils.inset(GuiUtils.top(doMakeWidgetComponent()), 5));
            pane.add("Histogram", GuiUtils.inset(GuiUtils.top(getHistogramTabComponent()), 5));
            GuiUtils.handleHeavyWeightComponentsInTabs(pane);
            return pane;
        } catch (Exception e) {
            logException("MultiSpectralControl.doMakeContents", e);
        }
        return null;
    }

    @Override public void doRemove() throws VisADException, RemoteException {
        // forcibly clear the value displays when the user has elected to kill
        // the display. the readouts will persist otherwise.
        removeSpectra();
        super.doRemove();
    }

    @SuppressWarnings("unchecked")
    @Override protected JComponent doMakeWidgetComponent() {
        List<Component> widgetComponents;
        try {
            List<ControlWidget> controlWidgets = new ArrayList<ControlWidget>();
            getControlWidgets(controlWidgets);
            controlWidgets.add(new WrapperWidget(this, GuiUtils.rLabel("Readout Probes:"), scrollPane));
            widgetComponents = ControlWidget.fillList(controlWidgets);
        } catch (Exception e) {
            LogUtil.logException("Problem building the MultiSpectralControl settings", e);
            widgetComponents = new ArrayList<Component>();
            widgetComponents.add(new JLabel("Error building component..."));
        }

        GuiUtils.tmpInsets = new Insets(4, 8, 4, 8);
        GuiUtils.tmpFill = GridBagConstraints.HORIZONTAL;
        return GuiUtils.doLayout(widgetComponents, 2, GuiUtils.WT_NY, GuiUtils.WT_N);
    }

    protected MultiSpectralDisplay getMultiSpectralDisplay() {
        return display;
    }

    public boolean updateImage(final float newChan) {
        if (!display.setWaveNumber(newChan))
            return false;

        DisplayableData imageDisplay = display.getImageDisplay();

        // mark the color map as needing an auto scale, these calls
        // are needed because a setRange could have been called which 
        // locks out auto scaling.
        ((HydraRGBDisplayable)imageDisplay).getColorMap().resetAutoScale();
        displayMaster.reScale();

        try {
            FlatField image = display.getImageData();
            displayMaster.setDisplayInactive(); //- try to consolidate display transforms
            imageDisplay.setData(image);
            updateHistogramTab();
            pokeSpectra();
            displayMaster.setDisplayActive();
        } catch (Exception e) {
            LogUtil.logException("MultiSpectralControl.updateImage", e);
            return false;
        }

        return true;
    }

    // be sure to update the displayed image even if a channel change 
    // originates from the msd itself.
    @Override public void handleChannelChange(final float newChan) {
        handleChannelChange(newChan, true);
    }

    public void handleChannelChange(final float newChan, boolean update) {
        if (update) {
            if (updateImage(newChan)) {
                wavenumbox.setText(Float.toString(newChan));
            }
        } else {
            wavenumbox.setText(Float.toString(newChan));
        }
    }

    private JComponent getDisplayTab() {
        List<JComponent> compList = new ArrayList<JComponent>();

        if (display.getBandSelectComboBox() == null) {
          final JLabel nameLabel = GuiUtils.rLabel("Wavenumber: ");

          wavenumbox.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                  String tmp = wavenumbox.getText().trim();
                  updateImage(Float.valueOf(tmp));
              }
          });
          compList.add(nameLabel);
          compList.add(wavenumbox);
        } else {
          final JComboBox bandBox = display.getBandSelectComboBox();
          bandBox.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                String bandName = (String) bandBox.getSelectedItem();
                Float channel = (Float) display.getMultiSpectralData().getBandNameMap().get(bandName);
                updateImage(channel.floatValue());
             }
          });
          JLabel nameLabel = new JLabel("Band: ");
          compList.add(nameLabel);
          compList.add(bandBox);
        }

        JPanel waveNo = GuiUtils.center(GuiUtils.doLayout(compList, 2, GuiUtils.WT_N, GuiUtils.WT_N));
        return GuiUtils.centerBottom(display.getDisplayComponent(), waveNo);
    }

    private JComponent getHistogramTabComponent() {
        updateHistogramTab();
        JComponent histoComp = histoWrapper.doMakeContents();
        JLabel rangeLabel = GuiUtils.rLabel("Range   ");
        JLabel minLabel = GuiUtils.rLabel("Min");
        JLabel maxLabel = GuiUtils.rLabel("   Max");
        List<JComponent> rangeComps = new ArrayList<JComponent>();
        rangeComps.add(rangeLabel);
        rangeComps.add(minLabel);
        rangeComps.add(minBox);
        rangeComps.add(maxLabel);
        rangeComps.add(maxBox);
        minBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                rangeMin = Float.valueOf(minBox.getText().trim());
                rangeMax = Float.valueOf(maxBox.getText().trim());
                histoWrapper.modifyRange((int)rangeMin, (int)rangeMax);
            }
        });
        maxBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                rangeMin = Float.valueOf(minBox.getText().trim());
                rangeMax = Float.valueOf(maxBox.getText().trim());
                histoWrapper.modifyRange((int)rangeMin, (int)rangeMax);
            }
        });
        JPanel rangePanel =
            GuiUtils.center(GuiUtils.doLayout(rangeComps, 5, GuiUtils.WT_N, GuiUtils.WT_N));
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                resetColorTable();
            }
        });

        JPanel resetPanel = 
            GuiUtils.center(GuiUtils.inset(GuiUtils.wrap(resetButton), 4));

        return GuiUtils.topCenterBottom(histoComp, rangePanel, resetPanel);
    }

    private void updateHistogramTab() {
        try {
            histoWrapper.loadData(display.getImageData());
            org.jfree.data.Range range = histoWrapper.getRange();
            rangeMin = (float)range.getLowerBound();
            rangeMax = (float)range.getUpperBound();
            minBox.setText(Integer.toString((int)rangeMin));
            maxBox.setText(Integer.toString((int)rangeMax));
        } catch (Exception e) {
            logException("MultiSpectralControl.getHistogramTabComponent", e);
        }
    }

    public void resetColorTable() {
        histoWrapper.doReset();
    }

    protected void contrastStretch(final double low, final double high) {
        try {
            org.jfree.data.Range range = histoWrapper.getRange();
            rangeMin = (float)range.getLowerBound();
            rangeMax = (float)range.getUpperBound();
            minBox.setText(Integer.toString((int)rangeMin));
            maxBox.setText(Integer.toString((int)rangeMax));
            setRange(getInitialColorTable().getName(), new Range(low, high));
        } catch (Exception e) {
            logException("MultiSpectralControl.contrastStretch", e);
        }
    }

    private static class NewSpectrum implements ProbeListener {
        private final MultiSpectralControl control;
        private final MultiSpectralDisplay display;
        private final DataReference spectrumRef;
        private ReadoutProbeDeux probe;
        private boolean isVisible = true;

        public NewSpectrum(final MultiSpectralControl control, final Color color) throws VisADException, RemoteException {
            this.control = control;
            this.display = control.getMultiSpectralDisplay();
            spectrumRef = new DataReferenceImpl(hashCode() + "_spectrumRef");
            display.addRef(spectrumRef, color);
            probe = new ReadoutProbeDeux(control.getNavigatedDisplay(), display.getImageData());
            probe.setColor(color);
            probe.addProbeListener(this);
        }

        public void probePositionChanged(final ProbeEvent<RealTuple> e) {
            RealTuple position = e.getNewValue();
            try {
                FlatField spectrum = display.getMultiSpectralData().getSpectrum(position);
                spectrumRef.setData(spectrum);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public String getValue() {
            return probe.getValue();
        }

        public double getLatitude() {
            return probe.getLatitude();
        }

        public double getLongitude() {
            return probe.getLongitude();
        }

        public Color getColor() {
            return probe.getColor();
        }

        final String[] tmp = new String[] { "probulator", "probot", "probocop", "probie", "probo", "probicon", "probolina", "probulon" };
        final Random r = new Random();
        final int myIndex = r.nextInt(tmp.length);
        public String getId() {
            return tmp[myIndex];
        }

        public DataReference getSpectrumRef() {
            return spectrumRef;
        }

        public String getSpectrumRefName() {
            return hashCode() + "_spectrumRef";
        }

        public void setColor(final Color color) {
            if (color == null)
                throw new NullPointerException("Can't use a null color");

            try {
                display.updateRef(spectrumRef, color);
                probe.quietlySetColor(color);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void setVisible(final boolean visible) {
            isVisible = visible;
            Color c = probe.getColor();
            int alpha = (visible) ? 255 : 0;
            c = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            try {
                display.updateRef(spectrumRef, c);
                probe.quietlySetVisible(visible);
            } catch (Exception e) {
                LogUtil.logException("There was a problem setting the visibility of probe \""+spectrumRef+"\" to "+visible, e);
            }
        }

        public boolean isVisible() {
            return isVisible;
        }

        protected ReadoutProbeDeux getProbe() {
            return probe;
        }

        public void probeColorChanged(final ProbeEvent<Color> e) {
            System.err.println(e);
        }

        public void probeVisibilityChanged(final ProbeEvent<Boolean> e) {
            System.err.println(e);
            Boolean newVal = e.getNewValue();
            if (newVal != null)
                isVisible = newVal;
        }

        public Hashtable<String, Object> getProperties() {
            Hashtable<String, Object> table = new Hashtable<String, Object>();
            table.put("color", probe.getColor());
            table.put("visibility", isVisible);
            table.put("lat", probe.getLatitude());
            table.put("lon", probe.getLongitude());
            return table;
        }

        public void setProperties(final Hashtable<String, Object> table) {
            if (table == null)
                throw new NullPointerException("properties table cannot be null");

            Color color = (Color)table.get("color");
            Double lat = (Double)table.get("lat");
            Double lon = (Double)table.get("lon");
            Boolean visibility = (Boolean)table.get("visibility");
            probe.setLatLon(lat, lon);
            probe.setColor(color);
            setVisible(visibility);
        }

        public void pokeValueDisplay() {
            probe.setField(display.getImageData());
            probe.handleProbeUpdate();
            try {
                FlatField spectrum = display.getMultiSpectralData().getSpectrum(probe.getEarthPosition());
                spectrumRef.setData(spectrum);
            } catch (Exception e) { }
        }

        public void removeValueDisplay() throws VisADException, RemoteException {
            probe.handleProbeRemoval();
            display.removeRef(spectrumRef);
        }
    }

    // TODO(jon): MultiSpectralControl should become the table model.
    private static class ProbeTableModel extends AbstractTableModel implements ProbeListener {
        private static final String[] COLUMNS = { 
            "Visibility", "Probe ID", "Value", "Spectrum", "Latitude", "Longitude", "Color" 
        };

        private final Map<ReadoutProbeDeux, Integer> probeToIndex = new LinkedHashMap<ReadoutProbeDeux, Integer>();
        private final Map<Integer, NewSpectrum> indexToSpectrum = new LinkedHashMap<Integer, NewSpectrum>();
        private final MultiSpectralControl control;

        public ProbeTableModel(final MultiSpectralControl control, final List<NewSpectrum> probes) {
            if (control == null)
                throw new NullPointerException("");
            if (probes == null)
                throw new NullPointerException("");

            this.control = control;
            updateWith(probes);
        }

        public void probeColorChanged(final ProbeEvent<Color> e) {
            ReadoutProbeDeux probe = e.getProbe();
            if (!probeToIndex.containsKey(probe))
                return;

            int index = probeToIndex.get(probe);
            fireTableCellUpdated(index, 6);
        }

        public void probeVisibilityChanged(final ProbeEvent<Boolean> e) {
            ReadoutProbeDeux probe = e.getProbe();
            if (!probeToIndex.containsKey(probe))
                return;

            int index = probeToIndex.get(probe);
            fireTableCellUpdated(index, 0);
        }

        public void probePositionChanged(final ProbeEvent<RealTuple> e) {
            ReadoutProbeDeux probe = e.getProbe();
            if (!probeToIndex.containsKey(probe))
                return;

            int index = probeToIndex.get(probe);
            fireTableRowsUpdated(index, index);
        }

        public void updateWith(final List<NewSpectrum> updatedSpectra) {
            if (updatedSpectra == null)
                throw new NullPointerException("");

            probeToIndex.clear();
            indexToSpectrum.clear();

            for (int i = 0, j = updatedSpectra.size()-1; i < updatedSpectra.size(); i++, j--) {
                NewSpectrum spectrum = updatedSpectra.get(j);
                ReadoutProbeDeux probe = spectrum.getProbe();
                if (!probe.hasListener(this))
                    probe.addProbeListener(this);

                probeToIndex.put(spectrum.getProbe(), i);
                indexToSpectrum.put(i, spectrum);
            }
        }

        public int getColumnCount() {
            return COLUMNS.length;
        }

        public int getRowCount() {
            if (probeToIndex.size() != indexToSpectrum.size())
                throw new AssertionError("");

            return probeToIndex.size();
        }

        // TODO(jon): verify that the given indices are correct!!!!
        public Object getValueAt(final int row, final int column) {
            NewSpectrum spectrum = indexToSpectrum.get(row);
            switch (column) {
                case 0: return spectrum.isVisible();
                case 1: return spectrum.getId();
                case 2: return spectrum.getValue();
                case 3: return "notyet";
                case 4: return formatPosition(spectrum.getLatitude());
                case 5: return formatPosition(spectrum.getLongitude());
                case 6: return spectrum.getColor();
                default: throw new AssertionError("uh oh");
            }
        }

        public boolean isCellEditable(final int row, final int column) {
            switch (column) {
                case 0: return true;
                case 6: return true;
                default: return false;
            }
        }

        public void setValueAt(final Object value, final int row, final int column) {
            NewSpectrum spectrum = indexToSpectrum.get(row);
            boolean didUpdate = true;
            switch (column) {
                case 0: spectrum.setVisible((Boolean)value); break;
                case 6: spectrum.setColor((Color)value); break;
                default: didUpdate = false; break;
            }

            if (didUpdate)
                fireTableCellUpdated(row, column);
        }

        public void moveRow(final int origin, final int destination) {
            // get the dragged spectrum (and probe)
            NewSpectrum dragged = indexToSpectrum.get(origin);
            ReadoutProbeDeux draggedProbe = dragged.getProbe();

            // get the current spectrum (and probe)
            NewSpectrum current = indexToSpectrum.get(destination);
            ReadoutProbeDeux currentProbe = current.getProbe();

            // update references in indexToSpetrum
            indexToSpectrum.put(destination, dragged);
            indexToSpectrum.put(origin, current);

            // update references in probeToIndex
            probeToIndex.put(draggedProbe, destination);
            probeToIndex.put(currentProbe, origin);

            // build a list of the spectra, ordered by index
            List<NewSpectrum> updated = new ArrayList<NewSpectrum>();
            for (int i = indexToSpectrum.size()-1; i >= 0; i--)
                updated.add(indexToSpectrum.get(i));

            // send it to control.
            control.updateList(updated);
        }

        public String getColumnName(final int column) {
            return COLUMNS[column];
        }

        public Class<?> getColumnClass(final int column) {
            return getValueAt(0, column).getClass();
        }

        private static String formatPosition(final double position) {
            McIDASV mcv = McIDASV.getStaticMcv();
            if (mcv == null)
                return "NaN";

            DecimalFormat format = new DecimalFormat(mcv.getStore().get(Constants.PREF_LATLON_FORMAT, "##0.0"));
            return format.format(position);
        }
    }

    public class ColorEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private Color currentColor = Color.CYAN;
        private final JButton button = new JButton();
        private final JColorChooser colorChooser = new JColorChooser();
        private JDialog dialog;
        protected static final String EDIT = "edit";

//        private final JComboBox combobox = new JComboBox(GuiUtils.COLORS); 

        public ColorEditor() {
            button.setActionCommand(EDIT);
            button.addActionListener(this);
            button.setBorderPainted(false);
            
//            combobox.setActionCommand(EDIT);
//            combobox.addActionListener(this);
//            combobox.setBorder(new EmptyBorder(0, 0, 0, 0));
//            combobox.setOpaque(true);
//            ColorRenderer whut = new ColorRenderer(true);
//            combobox.setRenderer(whut);
//            
//            dialog = JColorChooser.createDialog(combobox, "pick a color", true, colorChooser, this, null);
            dialog = JColorChooser.createDialog(button, "pick a color", true, colorChooser, this, null);
        }
        public void actionPerformed(ActionEvent e) {
            if (EDIT.equals(e.getActionCommand())) {
                //The user has clicked the cell, so
                //bring up the dialog.
//                button.setBackground(currentColor);
                colorChooser.setColor(currentColor);
                dialog.setVisible(true);

                //Make the renderer reappear.
                fireEditingStopped();

            } else { //User pressed dialog's "OK" button.
                currentColor = colorChooser.getColor();
            }
        }

        //Implement the one CellEditor method that AbstractCellEditor doesn't.
        public Object getCellEditorValue() {
            return currentColor;
        }

        //Implement the one method defined by TableCellEditor.
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentColor = (Color)value;
            return button;
//            return combobox;
        }
    }

    public class ColorRenderer extends JLabel implements TableCellRenderer, ListCellRenderer {
        Border unselectedBorder = null;
        Border selectedBorder = null;
        boolean isBordered = true;

        public ColorRenderer(boolean isBordered) {
            this.isBordered = isBordered;
            setHorizontalAlignment(CENTER);
            setVerticalAlignment(CENTER);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object color, boolean isSelected, boolean hasFocus, int row, int column) {
            Color newColor = (Color)color;
            setBackground(newColor);
            if (isBordered) {
                if (isSelected) {
                    if (selectedBorder == null)
                        selectedBorder = BorderFactory.createMatteBorder(2,5,2,5, table.getSelectionBackground());
                    setBorder(selectedBorder);
                } else {
                    if (unselectedBorder == null)
                        unselectedBorder = BorderFactory.createMatteBorder(2,5,2,5, table.getBackground());
                    setBorder(unselectedBorder);
                }
            }

            setToolTipText(String.format("RGB: red=%d, green=%d, blue=%d", newColor.getRed(), newColor.getGreen(), newColor.getBlue()));
            return this;
        }

        public Component getListCellRendererComponent(JList list, Object color, int index, boolean isSelected, boolean cellHasFocus) {
            Color newColor = (Color)color;
            setBackground(newColor);
            if (isBordered) {
                if (isSelected) {
                    if (selectedBorder == null)
                        selectedBorder = BorderFactory.createMatteBorder(2,5,2,5, list.getSelectionBackground());
                    setBorder(selectedBorder);
                } else {
                    if (unselectedBorder == null)
                        unselectedBorder = BorderFactory.createMatteBorder(2,5,2,5, list.getBackground());
                    setBorder(unselectedBorder);
                }
            }
            setToolTipText(String.format("RGB: red=%d, green=%d, blue=%d", newColor.getRed(), newColor.getGreen(), newColor.getBlue()));
            return this;
        }
    }

    public class HackyDragDropRowUI extends BasicTableUI {

        private boolean inDrag = false;
        private int start;
        private int offset;

        protected MouseInputListener createMouseInputListener() {
            return new HackyMouseInputHandler();
        }

        public void paint(Graphics g, JComponent c) {
            super.paint(g, c);

            if (!inDrag)
                return;

            int width = table.getWidth();
            int height = table.getRowHeight();
            g.setColor(table.getParent().getBackground());
            Rectangle rect = table.getCellRect(table.getSelectedRow(), 0, false);
            g.copyArea(rect.x, rect.y, width, height, rect.x, offset);

            if (offset < 0)
                g.fillRect(rect.x, rect.y + (height + offset), width, (offset * -1));
            else
                g.fillRect(rect.x, rect.y, width, offset);
        }

        class HackyMouseInputHandler extends MouseInputHandler {

            public void mouseDragged(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row < 0)
                    return;

                inDrag = true;

                int height = table.getRowHeight();
                int middleOfSelectedRow = (height * row) + (height / 2);

                int toRow = -1;
                int yLoc = (int)e.getPoint().getY();

                // goin' up?
                if (yLoc < (middleOfSelectedRow - height))
                    toRow = row - 1;
                else if (yLoc > (middleOfSelectedRow + height))
                    toRow = row + 1;

                ProbeTableModel model = (ProbeTableModel)table.getModel();
                if (toRow >= 0 && toRow < table.getRowCount()) {
                    model.moveRow(row, toRow);
                    table.setRowSelectionInterval(toRow, toRow);
                    start = yLoc;
                }

                offset = (start - yLoc) * -1;
                table.repaint();
            }

            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                start = (int)e.getPoint().getY();
            }

            public void mouseReleased(MouseEvent e){
                super.mouseReleased(e);
                inDrag = false;
                table.repaint();
            }
        }
    }
    
//    // TODO(jon): maybe this should be in MultiSpectralDisplay?
//    private static class Spectrum implements ProbeListener {
//        private final MultiSpectralControl control;
//        private final MultiSpectralDisplay display;
//        private final DataReference spectrumRef;
//        private ReadoutProbe probe;
//
//        public Spectrum(final MultiSpectralControl control, final Color color) throws VisADException, RemoteException {
//            this.control = control;
//            this.display = control.getMultiSpectralDisplay();
//            spectrumRef = new DataReferenceImpl(hashCode() + "_spectrumRef");
//            display.addRef(spectrumRef, color);
//            createReadoutProbe(color);
//        }
//
//        public void probePositionChanged(final ProbeEvent<RealTuple> e) {
//            RealTuple position = e.getNewValue();
//            getProperties();
//            try {
//                FlatField spectrum = display.getMultiSpectralData().getSpectrum(position);
//                spectrumRef.setData(spectrum);
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        }
//
//        public void probeColorChanged(final ProbeEvent<Color> e) {
//            Color color = e.getNewValue();
//            try {
//                display.updateRef(spectrumRef, color);
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        }
//
//        // alpha value approach taken so that the z-order in the spectrum 
//        // display is maintained.
//        public void probeVisibilityChanged(final ProbeEvent<Boolean> e) {
//            boolean isVisible = e.getNewValue();
//            Color color = probe.getColor();
//            int alpha = (isVisible) ? 255 : 0;
//            color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
//            try {
//                display.updateRef(spectrumRef, color);
//            } catch (Exception ex) {
//                LogUtil.logException("Spectrum.probeVisibilityChanged", ex);
//            }
//        }
//
//        public void pokeValueDisplay() {
//            probe.updateReadoutValue();
//        }
//
//        public void removeValueDisplay() throws VisADException, RemoteException {
//            probe.doRemove();
//            DisplayMaster master = control.getViewManager().getMaster();
//            master.removeDisplayable(probe.getValueDisplay());
//        }
//
//        private void createReadoutProbe(final Color color) {
//            try {
//                IntegratedDataViewer idv = control.getIdv();
//                DataChoice choice = control.getDataChoice();
//                ControlDescriptor descriptor = idv.getControlDescriptor("readout.probe");
//                ReadoutProbe probe = (ReadoutProbe)idv.doMakeControl(Misc.newList(choice), descriptor, (String)null, null, false);
//                probe.doMakeProbe();
//                probe.setDisplay(display);
//                this.probe = probe; // ugh :(
//                probe.addProbeListener(this);
//                probe.setColor(color);
//
//                DisplayMaster displayMaster = control.getViewManager().getMaster();
//                displayMaster.addDisplayable(probe.getValueDisplay());
//            } catch (Exception e) {
//                LogUtil.logException("MultiSpectralControl.createProbe", e);
//            }
//        }
//
//        public Hashtable<String, Object> getProperties() {
//            Hashtable<String, Object> table = new Hashtable<String, Object>();
//            table.put("color", probe.getColor());
//            table.put("visibility", new Boolean(probe.getDisplayVisibility()));
//
//            double[] latLon = probe.getLatLonVals();
//            table.put("lat", new Double(latLon[0]));
//            table.put("lon", new Double(latLon[1]));
//            return table;
//        }
//
//        public void setProperties(final Hashtable<String, Object> table) {
//            Color newColor = (Color)table.get("color");
//            Boolean visibility = (Boolean)table.get("visibility");
//            Double lat = (Double)table.get("lat");
//            Double lon = (Double)table.get("lon");
//
//            try {
//                System.out.println("setProperties: "+newColor);
//                probe.setColor(newColor);
//                display.updateRef(spectrumRef, newColor);
//                double[] xy = probe.getBoxVals(lat, lon);
//                probe.setProbePosition(xy[0], xy[1]);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
}
