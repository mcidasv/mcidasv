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
import java.util.Hashtable;
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
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
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
import edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionSubset;
import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;
import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay.DragLine;
import edu.wisc.ssec.mcidasv.jython.Console;
import edu.wisc.ssec.mcidasv.jython.ConsoleCallback;

public class LinearCombo extends HydraControl implements ConsoleCallback {

    private Console console;

    private MultiSpectralDisplay display;

    private DisplayMaster displayMaster;

    private String sourceFile = "";

    private ComboDataChoice comboChoice;

    private MultiSpectralDataSource source;

    private List<String> jythonHistory = new ArrayList<String>();

    private Map<String, Selector> selectorMap = new HashMap<String, Selector>();
    private Map<String, Selector> jythonMap = new HashMap<String, Selector>();

    private DataChoice dataChoice = null;

    public LinearCombo() {
        super();
    }

    @Override public boolean init(final DataChoice choice) throws VisADException, RemoteException {
        List<DataSource> sources = new ArrayList<DataSource>();
        choice.getDataSources(sources);
        dataChoice = choice;

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

    public void moveSelector(final String id, final float wavenum) {
        if (!selectorMap.containsKey(id))
            return;
        display.updateControlSelector(id, wavenum);
    }

    public void updateSelector(final String id, final float wavenum) {
        if (!selectorMap.containsKey(id))
            return;

        selectorMap.get(id).setWaveNumber(wavenum);
        String cmd = 
            String.format("_linearCombo.moveSelector('%s', %f)", id, wavenum);
        console.addPretendHistory(cmd);
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

    protected int getSelectorCount() {
        return selectorMap.size();
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
                return ((JythonThing)other).getName();
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
            return new AddCombination(this, other);
        }
        public Combination __sub__(final Object other) throws VisADException, RemoteException {
            return new SubtractCombination(this, other);
        }
        public Combination __mul__(final Object other) throws VisADException, RemoteException {
            return new MultiplyCombination(this, other);
        }
        public Combination __div__(final Object other) throws VisADException, RemoteException {
            return new DivideCombination(this, other);
        }
        public Combination __pow__(final Object other) throws VisADException, RemoteException {
            return new ExponentCombination(this, other);
        }
        public Combination __mod__(final Object other) throws VisADException, RemoteException {
            return new ModuloCombination(this, other);
        }
        public Combination __radd__(final Object other) throws VisADException, RemoteException {
            return new AddCombination(other, this);
        }
        public Combination __rsub__(final Object other) throws VisADException, RemoteException {
            return new SubtractCombination(other, this);
        }
        public Combination __rmul__(final Object other) throws VisADException, RemoteException {
            return new MultiplyCombination(other, this);
        }
        public Combination __rdiv__(final Object other) throws VisADException, RemoteException {
            return new DivideCombination(other, this);
        }
        public Combination __rpow__(final Object other) throws VisADException, RemoteException {
            return new ExponentCombination(other, this);
        }
        public Combination __rmod__(final Object other) throws VisADException, RemoteException {
            return new ModuloCombination(other, this);
        }
        public Combination __neg__() throws VisADException, RemoteException {
            return new NegateCombination(this);
        }
    }

    public static class Selector extends JythonThing {
        private final String ID;
        private float waveNumber = MultiSpectralData.init_wavenumber;
        private ConstantMap[] color;
        private Console console;
        private HydraControl control;
        private Data data;
        private MultiSpectralDisplay display;

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
                LinearCombo lc = (LinearCombo)control;
                this.ID = lc.getSelectorCount()+"_jython";
                try {
                    lc.addSelector(this);
                } catch (Exception e) {
                    System.err.println("Could not create selector: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                this.ID = hashCode()+"_jython";
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

    public static abstract class Combination extends JythonThing {
        private final Object left;
        private final Object right;

        private final String leftName;
        private final String rightName;

        private final Data leftData;
        private final Data rightData;

        private Data operationData;

        public Combination(final Object lhs, final Object rhs) throws VisADException, RemoteException {
            left = lhs;
            right = rhs;

            leftName = extractName(left);
            rightName = extractName(right);

            leftData = extractData(left);
            rightData = extractData(right);
        }

        private static Data extractData(final Object obj) throws VisADException, RemoteException {
            if (obj instanceof JythonThing)
                return ((JythonThing)obj).getData();
            if (obj instanceof PyFloat)
                return new Real(((PyFloat)obj).getValue());
            if (obj instanceof PyInteger)
                return new Real(((PyInteger)obj).getValue());
            if (obj instanceof Double)
                return new Real((Double)obj);
            if (obj instanceof Integer)
                return new Real((Integer)obj);
            if (obj instanceof Data)
                return (Data)obj;
            throw new IllegalArgumentException("Can't figure out what to do with " + obj);
        }

        protected static String extractName(final Object obj) {
            if (obj instanceof JythonThing)
                return ((JythonThing)obj).getName();
            if (obj instanceof PyFloat)
                return ((PyFloat)obj).toString();
            if (obj instanceof PyInteger)
                return ((PyInteger)obj).toString();
            if (obj instanceof Double)
                return ((Double)obj).toString();
            if (obj instanceof Integer)
                return ((Integer)obj).toString();
            throw new IllegalArgumentException("UGH: "+obj);
        }

        protected void setOperationData(final Data opData) {
            operationData = opData;
        }

        protected Data getOperationData() {
            return operationData;
        }

        public Object getLeft() {
            return left;
        }

        public Object getRight() {
            return right;
        }

        public String getLeftName() {
            return leftName;
        }

        public String getRightName() {
            return rightName;
        }

        public Data getLeftData() {
            return leftData;
        }

        public Data getRightData() {
            return rightData;
        }

        public boolean removeName(final String name) {
            return true;
        }

        public boolean addName(final String name) {
            return true;
        }

        public String getName() {
            return getFriendlyString();
        }

        public Data getData() {
            return operationData;
        }

        public Collection<String> getNames() {
            Set<String> set = new LinkedHashSet<String>(1);
            set.add(getFriendlyString());
            return set;
        }

        public abstract String getFriendlyString();
        public abstract String getPersistableString();
        public abstract String toString();
    }

    private static class AddCombination extends Combination {
        public AddCombination(final Object lhs, final Object rhs) throws VisADException, RemoteException {
            super(lhs, rhs);
            setOperationData(getLeftData().add(getRightData()));
        }
        public String getFriendlyString() {
            return String.format("(%s + %s)", getLeftName(), getRightName());
        }
        public String getPersistableString() {
            return getFriendlyString();
        }
        public String toString() {
            return String.format("[AddCombo@%x: leftName=%s, rightName=%s, friendlyString=%s, persistableString=%s]", hashCode(), getLeftName(), getRightName(), getFriendlyString(), getPersistableString());
        }
    }
    private static class SubtractCombination extends Combination {
        public SubtractCombination(final Object lhs, final Object rhs) throws VisADException, RemoteException {
            super(lhs, rhs);
            setOperationData(getLeftData().subtract(getRightData()));
        }
        public String getFriendlyString() {
            return String.format("(%s - %s)", getLeftName(), getRightName());
        }
        public String getPersistableString() {
            return getFriendlyString();
        }
        public String toString() {
            return String.format("[SubtractCombo@%x: leftName=%s, rightName=%s, friendlyString=%s, persistableString=%s]", 
                hashCode(), getLeftName(), getRightName(), getFriendlyString(), getPersistableString());
        }
    }
    private static class MultiplyCombination extends Combination {
        public MultiplyCombination(final Object lhs, final Object rhs) throws VisADException, RemoteException {
            super(lhs, rhs);
            setOperationData(getLeftData().multiply(getRightData()));
        }
        public String getFriendlyString() {
            return String.format("(%s * %s)", getLeftName(), getRightName());
        }
        public String getPersistableString() {
            return getFriendlyString();
        }
        public String toString() {
            return String.format("[MultiplyCombo@%x: leftName=%s, rightName=%s, friendlyString=%s, persistableString=%s]", 
                hashCode(), getLeftName(), getRightName(), getFriendlyString(), getPersistableString());
        }
    }
    private static class DivideCombination extends Combination {
        public DivideCombination(final Object lhs, final Object rhs) throws VisADException, RemoteException {
            super(lhs, rhs);
            setOperationData(getLeftData().divide(getRightData()));
        }
        public String getFriendlyString() {
            return String.format("(%s / %s)", getLeftName(), getRightName());
        }
        public String getPersistableString() {
            return getFriendlyString();
        }
        public String toString() {
            return String.format("[DivideCombo@%x: leftName=%s, rightName=%s, friendlyString=%s, persistableString=%s]", 
                hashCode(), getLeftName(), getRightName(), getFriendlyString(), getPersistableString());
        }
    }
    private static class ExponentCombination extends Combination {
        public ExponentCombination(final Object lhs, final Object rhs) throws VisADException, RemoteException {
            super(lhs, rhs);
            setOperationData(getLeftData().pow(getRightData()));
        }
        public String getFriendlyString() {
            return String.format("(%s**%s)", getLeftName(), getRightName());
        }
        public String getPersistableString() {
            return getFriendlyString();
        }
        public String toString() {
            return String.format("[ExponentCombo@%x: leftName=%s, rightName=%s, friendlyString=%s, persistableString=%s]", 
                hashCode(), getLeftName(), getRightName(), getFriendlyString(), getPersistableString());
        }
    }
    private static class ModuloCombination extends Combination {
        public ModuloCombination(final Object lhs, final Object rhs) throws VisADException, RemoteException {
            super(lhs, rhs);
            setOperationData(getLeftData().remainder(getRightData()));
        }
        public String getFriendlyString() {
            return String.format("(%s % %s)", getLeftName(), getRightName());
        }
        public String getPersistableString() {
            return getFriendlyString();
        }
        public String toString() {
            return String.format("[ModuloCombo@%x: leftName=%s, rightName=%s, friendlyString=%s, persistableString=%s]", 
                hashCode(), getLeftName(), getRightName(), getFriendlyString(), getPersistableString());
        }
    }
    private static class NegateCombination extends Combination {
        public NegateCombination(final Object lhs) throws VisADException, RemoteException {
            super(lhs, null);
            setOperationData(getLeftData().negate());
        }
        public String getFriendlyString() {
            return String.format("(-%s)", getLeftName());
        }
        public String getPersistableString() {
            return getFriendlyString();
        }
        public String toString() {
            return String.format("[NegateCombo@%x: leftName=%s, friendlyString=%s, persistableString=%s]", 
                hashCode(), getLeftName(), getFriendlyString(), getPersistableString());
        }
    }
}
