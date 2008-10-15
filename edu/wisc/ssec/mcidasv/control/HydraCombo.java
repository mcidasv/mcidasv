/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.control;

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
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

import org.python.core.PyJavaInstance;
import org.python.core.PyObject;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Range;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.visad.display.DisplayMaster;
import visad.ConstantMap;
import visad.Data;
import visad.VisADException;
import visad.georef.MapProjection;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.control.LinearCombo.Combination;
import edu.wisc.ssec.mcidasv.control.LinearCombo.Selector;
import edu.wisc.ssec.mcidasv.data.ComboDataChoice;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralDataSource;
import edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionSubset;
import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;
import edu.wisc.ssec.mcidasv.jython.Console;
import edu.wisc.ssec.mcidasv.jython.ConsoleCallback;
import edu.wisc.ssec.mcidasv.jython.Runner;

public class HydraCombo extends HydraControl {

    private String sourceFile = "";

    private MultiSpectralDisplay display;

    private DisplayMaster displayMaster;

    private DataChoice dataChoice = null;

    private CombinationPanel comboPanel;

    private final Hashtable<String, Object> persistable = new Hashtable<String, Object>();
    
    private MultiSpectralDataSource source;
    
    public HydraCombo() {
        super();
    }

    @Override public boolean init(final DataChoice choice)
        throws VisADException, RemoteException {
        
        dataChoice = choice;
        List<DataSource> sources = new ArrayList<DataSource>();
        choice.getDataSources(sources);
        source = ((MultiSpectralDataSource)sources.get(0));
        sourceFile = ((MultiSpectralDataSource)sources.get(0)).getDatasetName();

        Float fieldSelectorChannel = (Float)getDataSelection().getProperty(Constants.PROP_CHAN);
        if (fieldSelectorChannel == null)
            fieldSelectorChannel = MultiSpectralData.init_wavenumber;

        display = new MultiSpectralDisplay((DirectDataChoice)choice);
        display.setWaveNumber(fieldSelectorChannel);
        display.setDisplayControl(this);

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

    protected MultiSpectralDisplay getMultiSpectralDisplay() {
        return display;
    }

    private JComponent getComboTab() {
//        JButton button = new JButton("MAKE COMPUTER GO NOW");
        JButton button = new JButton("Compute");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                comboPanel.queueCombination();
            }
        });
        JPanel tmp = GuiUtils.topCenterBottom(display.getDisplayComponent(), comboPanel.getPanel(), button);
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

        public CombinationPanel(final HydraCombo control) {
            this.control = control;
            display = control.getMultiSpectralDisplay();
            if (display == null)
                throw new NullPointerException("Display hasn't been initialized");

            this.console = new Console();
            console.setCallbackHandler(this);

            a = makeWrapper("a", Color.RED);
            b = makeWrapper("b", Color.GREEN);
            c = makeWrapper("c", Color.BLUE);
            d = makeWrapper("d", Color.MAGENTA);

            enableSelector(a);

            ab = new OperationXY(this, a, b);
            cd = new OperationXY(this, c, d);

            abcd = new CombineOperations(this, ab, cd);
        }

        public void ranBlock(final String line) {
            PyObject jythonObj = console.getJythonObject("combo");
            if (jythonObj instanceof PyJavaInstance) {
                Object combination = jythonObj.__tojava__(Object.class);
                if (combination instanceof Combination) {
                    ((HydraCombo)control).addCombination((Combination)combination);
                }
            }
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

        protected void disableSelector(final SelectorWrapper wrapper) {
            wrapper.disable();
            try {
                display.removeSelector(wrapper.getSelector().getId());
            } catch (Exception e) {
                LogUtil.logException("HydraCombo.disableSelector", e);
            }
        }

        protected void enableSelector(final SelectorWrapper wrapper) {
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
                SelectorWrapper tmp = new SelectorWrapper(var, mappedColor, control, console);
                addSelector(tmp.getSelector(), false);
                wrapperMap.put(tmp.getSelector().getId(), tmp);
                console.injectObject(var, new PyJavaInstance(tmp.getSelector()));
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
        private static final String[] OPERATIONS = { "+", "-", "/", "*", " "};
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
                        comboPanel.disableSelector(y);
                    } else {
                        comboPanel.enableSelector(y);
                    }
                }
            });
        }

        public void disable() {
            comboPanel.disableSelector(x);
            combo.setSelectedItem(INVALID_OP);
            comboPanel.disableSelector(y);
        }

        public void enable() {
            comboPanel.enableSelector(x);
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

        private CombinationPanel comboPanel;
        private OperationXY x;
        private OperationXY y;

        public CombineOperations(final CombinationPanel comboPanel, final OperationXY x, final OperationXY y) {
            this.comboPanel = comboPanel;
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

    private static class SelectorWrapper {
        private static final String BLANK = "--------";
        private String variable;
        private final ConstantMap[] color;
        private final Selector selector;
        private final JTextField scale = new JTextField(String.format("%3.1f", 1.0), 4);
        private final JTextField wavenumber;

        public SelectorWrapper(final String variable, final ConstantMap[] color, final HydraCombo control, final Console console) {
            this.variable = variable;
            this.color = color;
            this.selector = new Selector(MultiSpectralData.init_wavenumber, color, control, console);
            this.selector.addName(variable);

            float r = new Double(color[0].getConstant()).floatValue();
            float g = new Double(color[1].getConstant()).floatValue();
            float b = new Double(color[2].getConstant()).floatValue();

            wavenumber = new JTextField(BLANK);
            wavenumber.setBorder(new LineBorder(new Color(r, g, b), 2));
        }

        public Selector getSelector() {
            return selector;
        }

        public JPanel getPanel() {
            JPanel panel = new JPanel(new FlowLayout());
            panel.add(scale);
            panel.add(wavenumber);
            return panel;
        }

        public String getJython() {
            if (!isValid())
                return "";
            return "(" + scale.getText() + "*" + variable + ")";
        }

        public boolean isValid() {
            return !wavenumber.getText().equals(BLANK);
        }

        public void enable() {
            String fmt = String.format("%7.3f", selector.getWaveNumber());
            wavenumber.setText(fmt);
        }

        public void disable() {
            wavenumber.setText(BLANK);
        }

        public void update() {
            String fmt = String.format("%7.3f", selector.getWaveNumber());
            wavenumber.setText(fmt);
        }

        public Hashtable<String, String> persistSelectorWrapper() {
            Hashtable<String, String> table = new Hashtable<String, String>(3);
            String scaleText = scale.getText();
            String waveText = wavenumber.getText();
            
            if (scaleText == null || scaleText.length() == 0)
                scaleText = "1.0";
            if (waveText == null || waveText.length() == 0 || !isValid())
                waveText = BLANK;

            table.put("variable", variable);
            table.put("scale", scale.getText());
            table.put("wave", wavenumber.getText());
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
    }
}
