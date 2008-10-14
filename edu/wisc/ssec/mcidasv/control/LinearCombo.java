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
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyJavaInstance;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Range;
import ucar.visad.display.DisplayMaster;
import visad.ConstantMap;
import visad.Data;
import visad.Real;
import visad.VisADException;
import visad.georef.MapProjection;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.data.ComboDataChoice;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralDataSource;
import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;
import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay.DragLine;
import edu.wisc.ssec.mcidasv.jython.Console;
import edu.wisc.ssec.mcidasv.jython.ConsoleCallback;

public class LinearCombo extends HydraControl implements ConsoleCallback {

    private static final String PARAM = "BrightnessTemp";

    private static final int DEFAULT_FLAGS =
        FLAG_COLORTABLE | FLAG_SELECTRANGE | FLAG_ZPOSITION;

    private Console console;

    private MultiSpectralDisplay display;

    private DisplayMaster displayMaster;

    private String sourceFile = "";

    private ComboDataChoice comboChoice;

    private MultiSpectralDataSource source;

    private List<String> jythonHistory = new ArrayList<String>();

    private Map<String, Selector> selectorMap = new HashMap<String, Selector>();
    private Map<String, Selector> jythonMap = new HashMap<String, Selector>();

    public LinearCombo() {
        super();
    }

    @Override public boolean init(final DataChoice choice) throws VisADException, RemoteException {
        List<DataSource> sources = new ArrayList<DataSource>();
        choice.getDataSources(sources);

        source = ((MultiSpectralDataSource)sources.get(0));
        sourceFile = source.getDatasetName();

        Float fieldSelectorChannel = (Float)getDataSelection().getProperty(Constants.PROP_CHAN);
        if (fieldSelectorChannel == null)
            fieldSelectorChannel = MultiSpectralData.init_wavenumber;

        console = new Console();
        console.setCallbackHandler(this);

        console.injectObject("_linearCombo", new PyJavaInstance(this));
        console.injectObject("_jythonConsole", new PyJavaInstance(console));

        console.runFile("__main__", "/edu/wisc/ssec/mcidasv/resources/python/linearcombo/hydra.py");

        display = new MultiSpectralDisplay((DirectDataChoice)choice);
        display.setWaveNumber(fieldSelectorChannel);
        display.setDisplayControl(this);

        addDisplayable(display.getImageDisplay(), DEFAULT_FLAGS);

        addViewManager(display.getViewManager());

        setAttributeFlags(DEFAULT_FLAGS);

        setProjectionInView(true);

        return true;
    }

    @Override public void initDone() {
        getIdv().getIdvUIManager().showDashboard();
        console.queueBatch("history", jythonHistory);
        jythonHistory.clear();
    }

    public List<String> getJythonHistory() {
        return console.getHistory();
    }

    public void setJythonHistory(final List<String> persistedHistory) {
        jythonHistory = persistedHistory;
    }

    @Override public MapProjection getDataProjection() {
        MapProjection mp = null;
        Rectangle2D rect = MultiSpectralData.getLonLatBoundingBox(display.getImageData());
        try {
            mp = new LambertAEA(rect);
        } catch (Exception e) {
            logException("LinearCombo.getDataProjection", e);
        }
        return mp;
    }

    @Override protected Range getInitialRange() throws VisADException, RemoteException {
        return getDisplayConventions().getParamRange(PARAM, null);
    }

    @Override protected ColorTable getInitialColorTable() {
        return getDisplayConventions().getParamColorTable(PARAM);
    }

    @Override public Container doMakeContents() {
        JTabbedPane pane = new JTabbedPane();
        pane.add("Console", GuiUtils.inset(getConsoleTab(), 5));
        GuiUtils.handleHeavyWeightComponentsInTabs(pane);
        return pane;
    }

    private JComponent getConsoleTab() {
        JPanel consolePanel = console.getPanel();
        consolePanel.setPreferredSize(new Dimension(500, 150));
        return GuiUtils.topCenter(display.getDisplayComponent(), consolePanel);
    }

    @Override public void doRemove() throws VisADException, RemoteException {
        removeDisplayables();
    }

    @Override public String toString() {
        return "[LinearCombo@" + Integer.toHexString(hashCode()) + 
            ": sourceFile=" + sourceFile + "]";
    }

    public void updateSelector(final String id, final float channel) {
        if (!selectorMap.containsKey(id))
            return;

        selectorMap.get(id).setWaveNumber(channel);
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

    protected MultiSpectralDisplay getMultiSpectralDisplay() {
        return display;
    }

    private Set<String> getSelectorIds(final Map<String, Object> objMap) {
        assert objMap != null : objMap;

        Set<String> ids = new HashSet<String>();
        Collection<Object> jython = objMap.values();

        for (Iterator<Object> i = jython.iterator(); i.hasNext();) {
            Object obj = i.next();
            if (!(obj instanceof Selector))
                continue;

            String selectorId = ((Selector)obj).getId();
            ids.add(selectorId);
        }

        return ids;
    }

    private Map<String, Selector> mapNamesToThings(final Map<String, Object> objMap) {
        assert objMap != null : objMap;

        Map<String, Selector> nameMap = new HashMap<String, Selector>(objMap.size());
        for (Map.Entry<String, Object> entry : objMap.entrySet()) {
            Object obj = entry.getValue();
            if (!(obj instanceof Selector))
                continue;

            String name = entry.getKey();
            Selector selector = (Selector)obj;
            nameMap.put(name, selector);
            selector.addName(name);
        }
        return nameMap;
    }

    public void addCombination(final String name, final Data combo) {
        source.addChoice(name, combo);
    }

    public void ranBlock(final String line) {
        List<DragLine> dragLines = display.getSelectors();
        Map<String, Object> javaObjects = console.getJavaInstances();

        Set<String> ids = getSelectorIds(javaObjects);

        for (DragLine dragLine : dragLines) {
            String lineId = dragLine.getControlId();
            if (!ids.contains(lineId)) {
                display.removeSelector(lineId);
                selectorMap.remove(lineId);
            }
        }

        jythonMap = mapNamesToThings(javaObjects);
    }

//    public void saveJythonThings() {
//        // well, only selectors so far...
//        for (Map.Entry<String, Selector> entry : jythonMap.entrySet()) {
//            String cmd = String.format("%s.setWaveNumber(%f)", entry.getKey(), entry.getValue().getWaveNumber());
//            System.err.println("saving: "+cmd);
//            console.addMetaCommand(cmd);
//        }
//    }

    public static abstract class JythonThing {
        protected Set<String> jythonNames = new LinkedHashSet<String>();
        public JythonThing() { }
        public abstract Data getData();

        private static Data extractData(final Object other) throws VisADException, RemoteException {
            if (other instanceof JythonThing)
                return ((JythonThing)other).getData();
            if (other instanceof PyFloat)
                return new Real(((PyFloat)other).getValue());
            if (other instanceof PyInteger)
                return new Real(((PyInteger)other).getValue());
            if (other instanceof Double)
                return new Real((Double)other);
            if (other instanceof Integer)
                return new Real((Integer)other);
            if (other instanceof Data)
                return (Data)other;
            throw new IllegalArgumentException("Can't figure out what to do with " + other);
        }

        private static String extractName(final Object other) {
            if (other instanceof JythonThing)
                return ((Selector)other).getName();
            if (other instanceof PyInteger)
                return ((PyInteger)other).toString();
            if (other instanceof PyFloat)
                return ((PyFloat)other).toString();
            if (other instanceof Double)
                return ((Double)other).toString();
            if (other instanceof Integer)
                return ((Integer)other).toString();
            throw new IllegalArgumentException("UGH: "+other);
        }

        public abstract boolean removeName(final String name);
        public abstract boolean addName(final String name);
        public abstract String getName();
        public abstract Collection<String> getNames();

        public Combination __add__(final Object other) throws VisADException, RemoteException {
            Combination combo = new Combination(getData().add(extractData(other)));
            combo.addName("("+getName()+" + "+extractName(other)+")");
            return combo;
        }

        public Combination __radd__(final Object other) throws VisADException, RemoteException {
            Combination combo = new Combination(getData().add(extractData(other)));
            combo.addName("("+extractName(other)+" + "+getName()+")");
            return combo;
        }
        public Combination __sub__(final Object other) throws VisADException, RemoteException {
            Combination combo = new Combination(getData().subtract(extractData(other)));
            combo.addName("("+getName()+" - "+extractName(other)+")");
            return combo;
        }
        public Combination __rsub__(final Object other) throws VisADException, RemoteException {
            Combination combo = new Combination(extractData(other).subtract(getData()));
            combo.addName("("+extractName(other)+" - "+getName()+")");
            return combo;
        }
        public Combination __mul__(final Object other) throws VisADException, RemoteException {
            Combination combo = new Combination(getData().multiply(extractData(other)));
            combo.addName("("+getName()+" * "+extractName(other)+")");
            return combo;
        }
        public Combination __rmul__(final Object other) throws VisADException, RemoteException {
            Combination combo = new Combination(getData().multiply(extractData(other)));
            combo.addName("("+extractName(other)+" * "+getName()+")");
            return combo;
        }
        public Combination __div__(final Object other) throws VisADException, RemoteException {
            Combination combo = new Combination(getData().divide(extractData(other)));
            combo.addName("("+getName()+" / "+extractName(other)+")");
            return combo;
        }
        public Combination __rdiv__(final Object other) throws VisADException, RemoteException {
            Combination combo = new Combination(extractData(other).divide(getData()));
            combo.addName("("+extractName(other)+" / "+getName()+")");
            return combo;
        }
        public Combination __pow__(final Object power) throws VisADException, RemoteException {
            Combination combo = new Combination(getData().pow(extractData(power)));
            combo.addName("("+getName()+"**"+extractName(power)+")");
            return combo;
        }
        public Combination __rpow__(final Object power) throws VisADException, RemoteException {
            Combination combo = new Combination(extractData(power).pow(getData()));
            combo.addName("("+extractName(power)+"**"+getName()+")");
            return combo;
        }
        public Combination __mod__(final Object other) throws VisADException, RemoteException {
            Combination combo = new Combination(getData().remainder(extractData(other)));
            combo.addName("("+getName()+"%"+extractName(other)+")");
            return combo;
        }
        public Combination __rmod__(final Object other) throws VisADException, RemoteException {
            Combination combo = new Combination(extractData(other).remainder(getData()));
            combo.addName("("+extractName(other)+"%"+getName()+")");
            return combo;
        }
        public Combination __neg__() throws VisADException, RemoteException {
            Combination combo = new Combination(getData().negate());
            combo.addName("(-"+getName()+")");
            return combo;
        }
    }

    public static class Selector extends JythonThing {
        private final String ID = hashCode() + "_jython";
        
        private float waveNumber = MultiSpectralData.init_wavenumber;
        private ConstantMap[] color;
        private Console console;
        private HydraControl control;
        private Data data;
        private MultiSpectralDisplay display;
        

        public Selector(final float waveNumber, final ConstantMap[] color) {
            super();
            this.waveNumber = waveNumber;
            this.color = color;
        }

        public Selector(final float waveNumber, final ConstantMap[] color, final HydraControl control, final Console console) {
            super();
            this.waveNumber = waveNumber;
            this.control = control;
            this.console = console;
            this.display = control.getMultiSpectralDisplay();

            this.color = new ConstantMap[color.length];
            for (int i = 0; i < this.color.length; i++) {
                ConstantMap mappedColor = (ConstantMap)color[i];
                this.color[i] = (ConstantMap)mappedColor.clone();
            }

            // TODO(jon): less dumb!
            if (control instanceof LinearCombo) {
                try {
                    ((LinearCombo)control).addSelector(this);
                } catch (Exception e) {
                    System.err.println("Could not create selector: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        public boolean removeName(final String name) {
            return jythonNames.remove(name);
        }

        public Collection<String> getNames() {
            return new LinkedHashSet<String>(jythonNames);
        }

        public boolean addName(final String name) {
            return jythonNames.add(name);
        }

        public String getName() {
            if (jythonNames.isEmpty())
                return "";
            else
                return jythonNames.iterator().next();
        }

        public void setWaveNumber(final float newChannel) {
            waveNumber = newChannel;
            try {
                display.setSelectorValue(ID, waveNumber);
            } catch (Exception e) {
                LogUtil.logException("Selector.setWaveNumber", e);
            }
        }

        public float getWaveNumber() {
            return waveNumber;
        }

        public ConstantMap[] getColor() {
            return color;
        }

        public Data getData() {
            return control.getMultiSpectralDisplay().getImageDataFrom(waveNumber);
        }

       public String getId() {
            return ID;
        }

       @Override public String toString() {
           return String.format("[Selector@%x: id=%s, waveNumber=%f, color=%s, jythonNames=%s]",
               hashCode(), ID, waveNumber, color, jythonNames);
           
       }
    }

    public static class Combination extends JythonThing {
        private String name = "";
        private Data data;

        public Combination(Data data) {
            this.data = data;
        }

        public boolean addName(final String name) {
            this.name = name;
            return true;
        }

        public String getName() {
            return name;
        }

        public Collection<String> getNames() {
            Set<String> set = new LinkedHashSet<String>();
            if (name.length() > 0)
                set.add(name);
            return set;
        }

        public boolean removeName(final String name) {
            this.name = "";
            return true;
        }

        public Data getData() {
            if (data == null)
                System.err.println("oh no! Combination." + hashCode() + " is null!");
            return data;
        }

        @Override public String toString() {
            return String.format("[Combination@%x: name=\"%s\"]", hashCode(), name);
        }
    }
// this about this a little longer...
//    public enum JythonOp {
//
//        ADD(" + "),
//        SUB(" - "),
//        MUL(" * "),
//        DIV(" / "),
//        POW("**"),
//        MOD(" % "),
//        NEG("-");
//        
//        private final String operationString;
//
//        JythonOp(final String str) {
//            this.operationString = str;
//        }
//
//        public String str() {
//            return operationString;
//        }
//
//        public String toString() {
//            return String.format("[JythonOp@%x: operationString=%s]", 
//                hashCode(), operationString);
//        }
//    }
//
//    public static class Combo2 {
//        private final JythonOp operation;
//        private final Object left;
//        private final Object right;
//        public Combo2(final JythonOp operation, final Object lhs, final Object rhs) {
//            this.operation = operation;
//            this.left = lhs;
//            this.right = rhs;
//        }
//
//        public String getName() {
//            return getHumanFriendlyCombination();
//        }
//
//        public boolean addName(final String name) {
//            return true;
//        }
//
//        public boolean removeName(final String name) {
//            return true;
//        }
//
//        public Collection<String> getNames() {
//            Set<String> set = new LinkedHashSet<String>();
//            set.add(getHumanFriendlyCombination());
//            return set;
//        }
//
//        public String getHumanFriendlyCombination() {
//            
//        }
//
//        public String getMcvFriendlyCombination() {
//            
//        }
//    }
}
