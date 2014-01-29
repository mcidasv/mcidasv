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

package edu.wisc.ssec.mcidasv.control;

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

import org.python.core.PyObject;

import visad.ConstantMap;
import visad.Data;
import visad.VisADException;
import visad.georef.MapProjection;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.view.geoloc.MapProjectionDisplay;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.control.LinearCombo.Combination;
import edu.wisc.ssec.mcidasv.control.LinearCombo.Selector;
import edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionSubset;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralDataSource;
import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;
import edu.wisc.ssec.mcidasv.jython.Console;
import edu.wisc.ssec.mcidasv.jython.ConsoleCallback;

public class HydraCombo extends HydraControl {


    private MultiSpectralDisplay display;

    private DataChoice dataChoice = null;

    private CombinationPanel comboPanel;

    private final Hashtable<String, Object> persistable = new Hashtable<String, Object>();
    
    private MultiSpectralDataSource source;
 
    float init_wavenumber;

    private Map<String, Selector> selectorMap = new HashMap<String, Selector>();

    private static final String defaultButtonLabel = "Compute New Field";
    private JButton computeButton = new JButton(defaultButtonLabel);

    public HydraCombo() {
        super();
        setHelpUrl("idv.controls.hydra.channelcombinationcontrol");
    }

    @Override public boolean init(final DataChoice choice)
        throws VisADException, RemoteException {
        ((McIDASV)getIdv()).getMcvDataManager().setHydraControl(choice, this);
        dataChoice = choice;
        List<DataSource> sources = new ArrayList<DataSource>();
        choice.getDataSources(sources);
        source = ((MultiSpectralDataSource)sources.get(0));

        Float fieldSelectorChannel = (Float)getDataSelection().getProperty(Constants.PROP_CHAN);
        if (fieldSelectorChannel == null)
            fieldSelectorChannel = 0f;
        init_wavenumber = fieldSelectorChannel;

        display = new MultiSpectralDisplay((DirectDataChoice)choice);
        display.setWaveNumber(fieldSelectorChannel);
        display.setDisplayControl(this);

        ((McIDASV)getIdv()).getMcvDataManager().setHydraDisplay(choice, display);

        comboPanel = new CombinationPanel(this);
        if (!persistable.isEmpty()) {
            comboPanel.unpersistData(persistable);
            persistable.clear();
        }
        return true;
    }

    @Override public void initDone() {
        MapViewManager viewManager = (MapViewManager) getViewManager();
        MapProjectionDisplay dispMaster =
            (MapProjectionDisplay) viewManager.getMaster();
        try {
          dispMaster.setMapProjection(getDataProjection());
        } catch (Exception e) {
          logException("problem setting MapProjection", e);
        }

        getIdv().getIdvUIManager().showDashboard();
    }

    public Hashtable<String, Object> getPersistable() {
        return comboPanel.persistData();
    }
    
    public void setPersistable(final Hashtable<String, Object> table) {
        persistable.clear();
        persistable.putAll(table);
    }
    
    @Override public MapProjection getDataProjection() {
        MapProjection mp = null;
        HashMap subset = null;
        Hashtable table = dataChoice.getProperties();
        MultiDimensionSubset dataSel =
           (MultiDimensionSubset) table.get(MultiDimensionSubset.key);
        if (dataSel != null) {
          subset = dataSel.getSubset();
        }
        mp = source.getDataProjection(subset);
        return mp;
    }

    @Override public Container doMakeContents() {
        JTabbedPane pane = new JTabbedPane();
        pane.add("Channel Combination Tool", GuiUtils.inset(getComboTab(), 5));
        GuiUtils.handleHeavyWeightComponentsInTabs(pane);
        return pane;
    }

    public void updateComboPanel(final String id, final float value) {
        if (comboPanel == null)
            return;
        comboPanel.updateSelector(id, value);
    }

    protected void enableSelectorForWrapper(final SelectorWrapper wrapper) {
        if (comboPanel == null)
            return;
        comboPanel.enableSelector(wrapper, false);
    }

    protected void disableSelectorForWrapper(final SelectorWrapper wrapper) {
        if (comboPanel == null)
            return;
        comboPanel.disableSelector(wrapper, false);
    }
    
    protected MultiSpectralDisplay getMultiSpectralDisplay() {
        return display;
    }

    protected JButton getComputeButton() {
        return computeButton;
    }

    private JComponent getComboTab() {
        computeButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                comboPanel.queueCombination();
                computeButton.setEnabled(false);
                showWaitCursor();
            }
        });
        // wrap compute button in JPanel to retain preferred size
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(computeButton);
        JPanel tmp = GuiUtils.topCenterBottom(display.getDisplayComponent(), comboPanel.getPanel(), buttonPanel);
        return tmp;
    }

    public void addCombination(final String name, final Data combination) {
        if (combination != null)
            source.addChoice(name, combination);
    }
    
    public void addCombination(final Combination combination) {
        if (combination != null)
            source.addChoice(combination.getName(), combination.getData());
    }

    protected void addSelector(final Selector selector) throws Exception {
        ConstantMap[] mapping = selector.getColor();
        float r = new Double(mapping[0].getConstant()).floatValue();
        float g = new Double(mapping[1].getConstant()).floatValue();
        float b = new Double(mapping[2].getConstant()).floatValue();
        Color javaColor = new Color(r, g, b);
        display.createSelector(selector.getId(), javaColor);
        display.setSelectorValue(selector.getId(), selector.getWaveNumber());
        selectorMap.put(selector.getId(), selector);
    }

    public void moveSelector(final String id, final float wavenum) {
        if (!selectorMap.containsKey(id))
            return;
        display.updateControlSelector(id, wavenum);
    }

    public void updateSelector(final String id, final float wavenum) {
        if (!selectorMap.containsKey(id))
            return;

        selectorMap.get(id).setWaveNumber(wavenum);
    }

    

    public enum DataType { HYPERSPECTRAL, MULTISPECTRAL };

    public enum WrapperState { ENABLED, DISABLED };

    public static class CombinationPanel implements ConsoleCallback {
        private final SelectorWrapper a;
        private final SelectorWrapper b;
        private final SelectorWrapper c;
        private final SelectorWrapper d;

        private final OperationXY ab;
        private final OperationXY cd;

        private final CombineOperations abcd;

        private final MultiSpectralDisplay display;

        private final HydraCombo control;

        private final Console console;

        private final Map<String, Selector> selectorMap = new HashMap<String, Selector>();
        private final Map<String, SelectorWrapper> wrapperMap = new HashMap<String, SelectorWrapper>();

        private final DataType dataType;

        public CombinationPanel(final HydraCombo control) {
            this.control = control;
            display = control.getMultiSpectralDisplay();
            if (display == null)
                throw new NullPointerException("Display hasn't been initialized");

            MultiSpectralData data = display.getMultiSpectralData();
            if (data.hasBandNames())
                dataType = DataType.MULTISPECTRAL;
            else
                dataType = DataType.HYPERSPECTRAL;

            this.console = new Console();
            console.setCallbackHandler(this);

            a = makeWrapper("a", Color.RED);
            b = makeWrapper("b", Color.GREEN);
            c = makeWrapper("c", Color.BLUE);
            d = makeWrapper("d", Color.MAGENTA);

            enableSelector(a, true);

            ab = new OperationXY(this, a, b);
            cd = new OperationXY(this, c, d);

            abcd = new CombineOperations(this, ab, cd);
        }

//        public Console getConsole() {
//            return console;
//        }

        public void ranBlock(final String line) {
            PyObject jythonObj = console.getJythonObject("combo");
//            if (jythonObj instanceof PyJavaInstance) {
                Object combination = jythonObj.__tojava__(Object.class);
                if (combination instanceof Combination) {
                    control.addCombination((Combination)combination);
                    control.getComputeButton().setEnabled(true);
                    control.showNormalCursor();
                }
//            }
        }

        public void updateSelector(final String id, final float channel) {
            if (!selectorMap.containsKey(id))
                return;

            Selector selector = selectorMap.get(id);
            selector.setWaveNumber(channel);
            wrapperMap.get(id).update();
        }

        protected void addSelector(final Selector selector, final boolean enabled) throws Exception {
            String id = selector.getId();
            if (enabled) {
                display.createSelector(id, selector.getColor());
                display.setSelectorValue(id, selector.getWaveNumber());
            }
            selectorMap.put(id, selector);
        }

        protected void disableSelector(final SelectorWrapper wrapper, final boolean disableWrapper) {
            if (disableWrapper)
                wrapper.disable();

            try {
                display.removeSelector(wrapper.getSelector().getId());
            } catch (Exception e) {
                LogUtil.logException("HydraCombo.disableSelector", e);
            }
        }

        protected void enableSelector(final SelectorWrapper wrapper, final boolean enableWrapper) {
            if (enableWrapper)
                wrapper.enable();

            try {
                Selector selector = wrapper.getSelector();
                String id = selector.getId();
                display.createSelector(id, selector.getColor());
                display.setSelectorValue(id, selector.getWaveNumber());
            } catch (Exception e) {
                LogUtil.logException("HydraCombo.disableSelector", e);
            }
        }

        private SelectorWrapper makeWrapper(final String var, final Color color) {
            try {
                ConstantMap[] mappedColor = MultiSpectralDisplay.makeColorMap(color);
                
                SelectorWrapper tmp;
                if (dataType == DataType.HYPERSPECTRAL)
                    tmp = new HyperspectralSelectorWrapper(var, mappedColor, control, console);
                else
                    tmp = new MultispectralSelectorWrapper(var, mappedColor, control, console);
                addSelector(tmp.getSelector(), false);
                wrapperMap.put(tmp.getSelector().getId(), tmp);
//                console.injectObject(var, new PyJavaInstance(tmp.getSelector()));
                console.injectObject(var, tmp.getSelector());
                return tmp;
            } catch (Exception e) { 
                LogUtil.logException("HydraCombo.makeWrapper", e);
            }
            return null;
        }

        public JPanel getPanel() {
            JPanel panel = new JPanel(new FlowLayout());
            panel.add(new JLabel("("));
            panel.add(a.getPanel());
            panel.add(ab.getBox());
            panel.add(b.getPanel());
            panel.add(new JLabel(")"));
            panel.add(abcd.getBox());
            panel.add(new JLabel("("));
            panel.add(c.getPanel());
            panel.add(cd.getBox());
            panel.add(d.getPanel());
            panel.add(new JLabel(")"));
            return panel;
        }

        public void queueCombination() {
            String jy = "combo="+abcd.getJython();
            System.err.println("jython=" + jy);
            console.queueLine(jy);
        }

        public Hashtable<String, Object> persistData() {
            Hashtable<String, Object> table = new Hashtable<String, Object>();
            
            table.put("a", a.persistSelectorWrapper());
            table.put("b", b.persistSelectorWrapper());
            table.put("c", c.persistSelectorWrapper());
            table.put("d", d.persistSelectorWrapper());
            table.put("ab", ab.getOperation());
            table.put("cd", cd.getOperation());
            table.put("abcd", abcd.getOperation());
            return table;
        }

        public void unpersistData(final Hashtable<String, Object> table) {
            Hashtable<String, String> tableA = (Hashtable<String, String>)table.get("a");
            Hashtable<String, String> tableB = (Hashtable<String, String>)table.get("b");
            Hashtable<String, String> tableC = (Hashtable<String, String>)table.get("c");
            Hashtable<String, String> tableD = (Hashtable<String, String>)table.get("d");

            a.unpersistSelectorWrapper(tableA);
            b.unpersistSelectorWrapper(tableB);
            c.unpersistSelectorWrapper(tableC);
            d.unpersistSelectorWrapper(tableD);

            String opAb = (String)table.get("ab");
            String opCd = (String)table.get("cd");
            String opAbcd = (String)table.get("abcd");

            ab.setOperation(opAb);
            cd.setOperation(opCd);
            abcd.setOperation(opAbcd);
        }
    }

    private static class OperationXY {
        private static final String[] OPERATIONS = { "+", "-", "/", "*", " " };
        private static final String INVALID_OP = " ";
        private final JComboBox combo = new JComboBox(OPERATIONS);

        private CombinationPanel comboPanel;
        private SelectorWrapper x;
        private SelectorWrapper y;

        public OperationXY(final CombinationPanel comboPanel, final SelectorWrapper x, final SelectorWrapper y) {
            this.x = x;
            this.y = y;
            this.comboPanel = comboPanel;
            combo.setSelectedItem(" ");
            combo.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    if (getOperation().equals(" ")) {
                        comboPanel.disableSelector(y, true);
                    } else {
                        comboPanel.enableSelector(y, true);
                    }
                }
            });
        }

        public void disable() {
            comboPanel.disableSelector(x, true);
            combo.setSelectedItem(INVALID_OP);
            comboPanel.disableSelector(y, true);
        }

        public void enable() {
            comboPanel.enableSelector(x, true);
        }

        public String getOperation() {
            return (String)combo.getSelectedItem();
        }

        public void setOperation(final String operation) {
            combo.setSelectedItem(operation);
        }

        public JComboBox getBox() {
            return combo;
        }

        public String getJython() {
            String operation = getOperation();
            if (operation.equals(INVALID_OP))
                operation = "";

            String jython = x.getJython() + operation + y.getJython();
            if (x.isValid() && y.isValid())
                return "(" + jython + ")";
            return jython;
        }
    }

    private static class CombineOperations {
        private static final String[] OPERATIONS = { "+", "-", "/", "*", " "};
        private static final String INVALID_OP = " ";
        private final JComboBox combo = new JComboBox(OPERATIONS);

        private OperationXY x;
        private OperationXY y;

        public CombineOperations(final CombinationPanel comboPanel, final OperationXY x, final OperationXY y) {
            this.x = x;
            this.y = y;
            combo.setSelectedItem(INVALID_OP);
            combo.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    if (getOperation().equals(" ")) {
                        y.disable();
                    } else {
                        y.enable();
                    }
                }
            });
        }

        public String getOperation() {
            return (String)combo.getSelectedItem();
        }

        public void setOperation(final String operation) {
            combo.setSelectedItem(operation);
        }

        public JComboBox getBox() {
            return combo;
        }

        public String getJython() {
            String operation = getOperation();
            if (operation.equals(INVALID_OP))
                operation = "";

            String jython = x.getJython() + operation + y.getJython();
            return jython;
        }
    }

    private static abstract class SelectorWrapper {
        protected static final String BLANK = "-----";
        private String variable;
        private final ConstantMap[] color;
        protected final Selector selector;
        protected final MultiSpectralDisplay display;
        protected final MultiSpectralData data;
        private final JTextField scale = new JTextField(String.format("%3.1f", 1.0));
        protected WrapperState currentState = WrapperState.DISABLED;

        public SelectorWrapper(final String variable, final ConstantMap[] color, final HydraCombo control, final Console console) {
            this.display = control.getMultiSpectralDisplay();
            this.data = control.getMultiSpectralDisplay().getMultiSpectralData();
            this.variable = variable;
            this.color = color;
            this.selector = new Selector(control.init_wavenumber, color, control, console);
            this.selector.addName(variable);
        }

        public Selector getSelector() {
            return selector;
        }

        public JPanel getPanel() {
            JPanel panel = new JPanel(new FlowLayout());
            JComponent comp = getWavenumberComponent();
            float r = new Double(color[0].getConstant()).floatValue();
            float g = new Double(color[1].getConstant()).floatValue();
            float b = new Double(color[2].getConstant()).floatValue();
            comp.setBorder(new LineBorder(new Color(r, g, b), 2));
            panel.add(scale);
            panel.add(comp);
            return panel;
        }

        public String getJython() {
            if (!isValid())
                return "";
            return "(" + scale.getText() + "*" + variable + ")";
        }

        public boolean isValid() {
            return !getValue().equals(BLANK);
        }

        public void enable() {
            setValue(Float.toString(selector.getWaveNumber()));
            currentState = WrapperState.ENABLED;
        }

        public void disable() {
            setValue(BLANK);
            currentState = WrapperState.DISABLED;
        }

        public void update() {
            setValue(Float.toString(selector.getWaveNumber()));
        }

        public Hashtable<String, String> persistSelectorWrapper() {
            Hashtable<String, String> table = new Hashtable<String, String>(3);
            String scaleText = scale.getText();
            String waveText = getValue();

            if (scaleText == null || scaleText.length() == 0)
                scaleText = "1.0";
            if (waveText == null || waveText.length() == 0 || !isValid())
                waveText = BLANK;

            table.put("variable", variable);
            table.put("scale", scale.getText());
            table.put("wave", getValue());
            return table;
        }

        public void unpersistSelectorWrapper(final Hashtable<String, String> table) {
            variable = table.get("variable");
            selector.addName(variable);
            scale.setText(String.format("%3.1f", new Float(table.get("scale"))));

            String waveText = table.get("wave");
            if (waveText.equals(BLANK)) {
                disable();
            } else {
                float wave = new Float(table.get("wave"));
                selector.setWaveNumber(wave);
                if (isValid())
                    update();
            }
        }

        public abstract JComponent getWavenumberComponent();

        public abstract void setValue(final String value);

        public abstract String getValue();
    }

    private static class HyperspectralSelectorWrapper extends SelectorWrapper {
        private final JTextField wavenumber;
        public HyperspectralSelectorWrapper(final String variable, final ConstantMap[] color, final HydraCombo control, final Console console) {
            super(variable, color, control, console);
            wavenumber = new JTextField(BLANK, 7);

            wavenumber.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    String textVal = wavenumber.getText();
                    if (textVal.equals(BLANK))
                        return;

                    float wave = Float.parseFloat(textVal.trim());
                    control.updateComboPanel(getSelector().getId(), wave);
                }
            });
        }

        @Override public JComponent getWavenumberComponent() {
            return wavenumber;
        }

        @Override public void setValue(final String value) {
            if (value == null)
                throw new NullPointerException("");

            if (!value.equals(BLANK)) {
                float wave = Float.parseFloat(value);
                String fmt = String.format("%7.2f", wave);
                wavenumber.setText(fmt);
            }
        }

        public String getValue() {
            return wavenumber.getText();
        }
    }

    private static class MultispectralSelectorWrapper extends SelectorWrapper {

        private final JComboBox bands;

        public MultispectralSelectorWrapper(final String variable, final ConstantMap[] color, final HydraCombo control, final Console console) {
            super(variable, color, control, console);
            removeMSDListeners();
            bands = bigBadBox(control);
        }

        private void removeMSDListeners() {
            JComboBox box = display.getBandSelectComboBox();
            for (ActionListener l : box.getActionListeners())
                box.removeActionListener(l);
        }

        private JComboBox bigBadBox(final HydraCombo control) {
            final JComboBox box = new JComboBox();
            box.addItem(BLANK);

            for (String name : data.getBandNames())
                box.addItem(name);

            final SelectorWrapper wrapper = this;
            box.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    String selected = (String)box.getSelectedItem();
                    float wave = Float.NaN;

                    if (!selected.equals(BLANK)) {
                        Map<String, Float> map = data.getBandNameMap();
                        if (map.containsKey(selected))
                            wave = map.get(selected);

                        if (currentState == WrapperState.DISABLED)
                            control.enableSelectorForWrapper(wrapper);
                        control.updateComboPanel(getSelector().getId(), wave);
                    } else {
                        control.disableSelectorForWrapper(wrapper);
                    }
                }
            });
            
            return box;
        }
        
        @Override public JComponent getWavenumberComponent() {
            return bands;
        }

        @Override public void setValue(final String value) {
            if (value == null)
                throw new NullPointerException();

            if (!value.equals(BLANK)) {
                String name = data.getBandNameFromWaveNumber(Float.parseFloat(value));
                bands.setSelectedItem(name);
            } else {     
                bands.setSelectedItem(BLANK);
            }
        }

        @Override public String getValue() {
            return (String)bands.getSelectedItem();
        }
    }
}
