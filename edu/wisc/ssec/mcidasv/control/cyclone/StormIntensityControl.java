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

package edu.wisc.ssec.mcidasv.control.cyclone;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.imagery.AddeImageDataSource;
import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.data.imagery.ImageDataSource;
import ucar.unidata.idv.DisplayInfo;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.visad.Util;
import ucar.visad.display.Animation;
import ucar.visad.display.PointProbe;
import ucar.visad.quantities.AirTemperature;
import visad.CommonUnit;
import visad.DateTime;
import visad.DisplayEvent;
import visad.FieldImpl;
import visad.FlatField;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.Set;
import visad.VisADException;
import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;
import visad.util.DataUtility;
import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidasv.data.cyclone.StormAODT;
import edu.wisc.ssec.mcidasv.data.cyclone.StormAODTInfo;
import edu.wisc.ssec.mcidasv.data.cyclone.StormAODTUtil;

/**
 * Created by IntelliJ IDEA. User: yuanho Date: Mar 10, 2009 Time: 1:03:56 PM To
 * change this template use File | Settings | File Templates.
 */

public class StormIntensityControl extends DisplayControlImpl {

	/** _more_ */
	private LatLonWidget latLonWidget;

	/** _more_ */
	private JEditorPane textComp;

	/** _more_ */
	private LatLonPoint probeLocation;

	/** the probe */
	private PointProbe probe;

	/** _more_ */
	private DataChoice choice;

	/** _more_ */
	private boolean running = false;

	/** _more_ */
	private boolean runOnClick = false;

	/** _more_ */
	private JButton aodtBtn;

	/**
	 * _more_
	 */
	public StormIntensityControl() {
	}

	/**
	 * _more_
	 * 
	 * @param choice
	 *            _more_
	 * 
	 * @return _more_
	 * 
	 * @throws RemoteException
	 *             _more_
	 * @throws VisADException
	 *             _more_
	 */
	public boolean init(DataChoice choice) throws VisADException,
			RemoteException {
		if (!super.init(choice)) {
			return false;
		}
		this.choice = choice;

		// probe = new SelectorPoint ("",new
		// RealTuple(RealTupleType.SpatialEarth3DTuple,
		// new double[] {0,0,0}));

		probe = new PointProbe(new RealTuple(RealTupleType.SpatialEarth3DTuple,
				new double[] { 0, 0, 0 }));

		probe.setManipulable(false);
		probe.setVisible(false);
		probe.setAutoSize(true);

		probe.setPointSize(getDisplayScale());
		addDisplayable(probe, FLAG_COLOR);

		setContents(doMakeContents());
		updateProbeLocation();
		return true;
	}

	/** _more_ */
	private JComboBox domainBox;

	/** _more_ */
	private int domainIndex = 0;

	/** _more_ */
	private JComboBox oceanBox;

	/** _more_ */
	private int oceanIndex = 0;

	/** _more_ */
	private JComboBox sceneBox;

	/** _more_ */
	private int sceneIndex = 0;

	/** _more_ */
	private String[] ocean = { "Atlantic", "Pacific", "Indian" };

	/** _more_ */
	private String text;

	/**
	 * _more_
	 * 
	 * @return _more_
	 */
	public Container doMakeContents() {
		latLonWidget = new LatLonWidget(GuiUtils.makeActionListener(this,
				"latLonWidgetChanged", null));
		JPanel latlonPanel = GuiUtils.hbox(Misc.newList(latLonWidget));

		domainBox = new JComboBox(new Vector(Misc.newList("Ocean", "Land")));
		domainBox.setSelectedIndex(domainIndex);
		domainBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				domainIndex = domainBox.getSelectedIndex();
			}
		});
		domainBox.setToolTipText("Domain selection for Ocean or Land.");

		oceanBox = new JComboBox(new Vector(Misc.newList("Atlantic", "Pacific",
				"Indian")));
		oceanBox.setSelectedIndex(oceanIndex);
		oceanBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				oceanIndex = oceanBox.getSelectedIndex();
			}
		});
		oceanBox.setToolTipText("Ocean Selection Atlantic, Pacific, or Indian");

		sceneBox = new JComboBox(new Vector(Misc.newList(" COMPUTED")));
		sceneBox.setSelectedIndex(sceneIndex);
		sceneBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				sceneIndex = sceneBox.getSelectedIndex();
			}
		});
		sceneBox.setToolTipText("Only computed scene type available");

		aodtBtn = new JButton("Run Analysis");
		aodtBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				doAnalysis();
			}
		});
		JComponent btn = GuiUtils.hbox(aodtBtn, GuiUtils.makeCheckbox(
				"Run On Click", this, "runOnClick"));

		textComp = new JEditorPane();

		textComp.setEditable(false);
		textComp.setContentType("text/html");
		textComp.setPreferredSize(new Dimension(300, 180));
		// GuiUtils.setFixedWidthFont(textComp);
		textComp.setEditable(false);

		JScrollPane textScroller = new JScrollPane(textComp);
		textScroller
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		textScroller.setPreferredSize(new Dimension(300, 180));

		JComponent textHolder = GuiUtils.topCenter(new JLabel("Result:"),
				textScroller);

		GuiUtils.tmpInsets = GuiUtils.INSETS_5;
		JComponent widgets = GuiUtils.formLayout(new Component[] {
				GuiUtils.rLabel("Location:"),
				GuiUtils.left(latlonPanel),
				GuiUtils.rLabel("Domain:"),
				GuiUtils.left(GuiUtils.hbox(new Component[] { domainBox,
						new JLabel("Ocean Selection:"), oceanBox,
						new JLabel("Scene Type:"), sceneBox }, 5)),
				GuiUtils.filler(), GuiUtils.left(btn) });
		// JPanel htmlPanel = GuiUtils.hbox( htmlScroller);

		// JPanel controls =
		// GuiUtils.topCenterBottom(latlonPanel,widgets,textHolder);
		JPanel controls = GuiUtils.topCenter(widgets, textHolder);

		return controls;
	}

	/**
	 * _more_
	 */
	public void latLonWidgetChanged() {
		try {
			System.err.println("widget changed");
			String message = latLonWidget.isValidValues();
			if (message != null) {
				userMessage(message);
				return;
			}

			probeLocation = ucar.visad.Util.makeEarthLocation(
					latLonWidget.getLat(), latLonWidget.getLon())
					.getLatLonPoint();
		} catch (Exception e) {
			logException("Handling LatLonWidget changed", e);
		}

	}

	/**
	 * _more_
	 * 
	 * @return _more_
	 */
	protected boolean shouldAddDisplayListener() {
		return true;
	}

	/**
	 * _more_
	 * 
	 * @return _more_
	 */
	protected boolean shouldAddControlListener() {
		return true;
	}

	/**
	 * Should we handle display events
	 * 
	 * @return Ok to handle events
	 */
	protected boolean canHandleEvents() {
		if (!getHaveInitialized() || (getMakeWindow() && !getWindowVisible())) {
			return false;
		}
		return isGuiShown();
	}

	/**
	 * Listen for DisplayEvents
	 * 
	 * @param event
	 *            The event
	 */
	public void handleDisplayChanged(DisplayEvent event) {
		super.handleDisplayChanged(event);

		if (!canHandleEvents()) {
			return;
		}

		int id = event.getId();
		InputEvent inputEvent = event.getInputEvent();

		try {
			if (id == DisplayEvent.MOUSE_PRESSED) {
				if (!isLeftButtonDown(event)) {
					return;
				}
				probeLocation = toEarth(event).getLatLonPoint();
				updateProbeLocation();
				if (runOnClick) {
					doAnalysis();
				}
			}
		} catch (Exception e) {
			logException("Handling display event changed", e);
		}

	}

	/**
	 * _more_
	 */
	private void updateProbeLocation() {
		try {
			if (probeLocation == null) {
				return;
			}
			double lon = probeLocation.getLongitude().getValue(
					CommonUnit.degree);
			double lat = probeLocation.getLatitude()
					.getValue(CommonUnit.degree);
			probe.setPosition(new RealTuple(RealTupleType.SpatialEarth3DTuple,
					new double[] { lon, lat, 0 }));

			// probe.setPoint(new RealTuple(RealTupleType.SpatialEarth3DTuple,
			// new double[] {lon,
			// lat,0}));

			probe.setVisible(true);

			if (latLonWidget != null) {
				latLonWidget.setLat(getDisplayConventions()
						.formatLatLon(
								probeLocation.getLatitude().getValue(
										CommonUnit.degree)));

				latLonWidget.setLon(getDisplayConventions().formatLatLon(
						probeLocation.getLongitude()
								.getValue(CommonUnit.degree)));

			}
			// Misc.run(this, "doAnalysis");

		} catch (Exception e) {
			logException("Handling probe changed", e);
		}
	}

	/**
	 * _more_
	 */
	private void doAnalysis() {
		if (running) {
			return;
		}
		running = true;
		aodtBtn.setEnabled(false);
		aodtBtn.setText("Running");

		Misc.run(new Runnable() {
			public void run() {
				doAnalysisInner();
				running = false;
				aodtBtn.setEnabled(true);
			}
		});

	}

	/**
	 * _more_
	 */
	private void doAnalysisInner() {
		FlatField ffield = null;
		StormAODT sint = new StormAODT();
		int sid = 0;
		int channel = 0;

		if (probeLocation == null) {
			return;
		}
		// Set timeset = null;
		RealTuple timeTuple = null;
		List sources = new ArrayList();
		Real tt = null;
		DateTime dat = null;
		boolean isTemp = false;
		choice.getDataSources(sources);
		try {
			List infos = getDisplayInfos();
			DataInstance de = getDataInstance();
			DisplayInfo displayInfo = (DisplayInfo) infos.get(0);

			Animation anime = displayInfo.getViewManager().getAnimation();
			Set timeSet = anime.getSet();
			int pos = anime.getCurrent();
			ffield = DataUtil.getFlatField(de.getData());
			DataSourceImpl dsi = (DataSourceImpl) sources.get(0);

			if (dsi instanceof AddeImageDataSource) {
				ImageDataSource dds = (ImageDataSource) sources.get(0);
				List imageLists = dds.getImageList();

				AddeImageDescriptor aid = (AddeImageDescriptor) imageLists
						.get(pos);
				AreaDirectory ad = aid.getDirectory();
				sid = ad.getSensorID();
				int[] bands = ad.getBands();
				channel = bands[0];

				isTemp = Util
						.isCompatible(ffield, AirTemperature.getRealType());
			} else {
				channel = 4;
				sid = 70;
				String name = ffield.getSample(0).getType().prettyString();
				if (!name.contains("IR")) {
					text = "Storm intensity analysis only running over Infrared field";
					textComp.setText(text);
					return;
				}
			}

			timeTuple = DataUtility.getSample(timeSet, pos);
			tt = (Real) timeTuple.getComponent(0);
			dat = new DateTime(tt);
		} catch (VisADException e) {
			logException("Handling data", e);
			return;
		} catch (RemoteException f) {
		}

		String shortName = choice.getName();

		float cenlat = (float) probeLocation.getLatitude().getValue();
		float cenlon = (float) probeLocation.getLongitude().getValue();
		double curtime = dat.getValue();

		String g_domain = ocean[oceanIndex]; // "ATL";
		int cursat = 0;
		int posm = 1;

		if (domainIndex == 1) {
			text = "Storm intensity analysis not available over land";
			textComp.setText(text);
			return;
		}

		StormAODTInfo.IRData result = sint.aodtv72_drive(ffield, cenlat,
				cenlon, posm, curtime, cursat, g_domain, sid, channel, isTemp);
		text = StormAODTUtil.aodtv72_textscreenoutput(result,
				getDisplayConventions().getLatLonFormat());
		textComp.setText(text);
	}

	/**
	 * _more_
	 * 
	 * @param data
	 *            _more_
	 * 
	 * @return _more_
	 * 
	 * @throws RemoteException
	 *             _more_
	 * @throws VisADException
	 *             _more_
	 */
	protected FlatField getFlatField(FieldImpl data) throws VisADException,
			RemoteException {
		FlatField ff = null;
		if (GridUtil.isSequence(data)) {
			ff = (FlatField) data.getSample(0);
		} else {
			ff = (FlatField) data;
		}
		return ff;
	}

	/**
	 * Map the screen x/y of the event to an earth location
	 * 
	 * @param event
	 *            The event
	 * 
	 * @return The earth location
	 * 
	 * @throws java.rmi.RemoteException
	 *             When bad things happen
	 * @throws visad.VisADException
	 *             When bad things happen
	 * 
	 * @throws RemoteException
	 *             _more_
	 * @throws VisADException
	 *             _more_
	 */
	public EarthLocation toEarth(DisplayEvent event) throws VisADException,
			RemoteException {
		NavigatedDisplay d = getNavigatedDisplay();
		return (d == null) ? null : d.getEarthLocation(toBox(event));
	}

	/**
	 * _more_
	 * 
	 * @param el
	 *            _more_
	 */
	public void addAODT(EarthLocation el) {
		final JDialog dialog = GuiUtils.createDialog("RUN AODT", true);
		String question = "Please select storm center";
		String label = "latitude: ";
		String label1 = "longitude: ";
		final JTextField field = new JTextField("", 10);
		final JTextField field1 = new JTextField("", 10);

		ObjectListener listener = new ObjectListener(new Boolean(false)) {
			public void actionPerformed(ActionEvent ae) {
				String cmd = ae.getActionCommand();
				if ((ae.getSource() == field) || cmd.equals(GuiUtils.CMD_OK)) {
					theObject = new Boolean(true);
				} else {
					theObject = new Boolean(false);
				}
				dialog.setVisible(false);
			}
		};
		ObjectListener listener1 = new ObjectListener(new Boolean(false)) {
			public void actionPerformed(ActionEvent ae) {
				String cmd = ae.getActionCommand();
				if ((ae.getSource() == field1) || cmd.equals(GuiUtils.CMD_OK)) {
					theObject = new Boolean(true);
				} else {
					theObject = new Boolean(false);
				}
				dialog.setVisible(false);
			}
		};
		field.addActionListener(listener);
		field.addActionListener(listener1);
		List comps = new ArrayList();

		comps.add(GuiUtils.left(GuiUtils.inset(new JLabel(question), 4)));

		JPanel topb = GuiUtils.doLayout(new Component[] {
				GuiUtils.rLabel(label),
				GuiUtils.hbox(field, GuiUtils.filler()),
				GuiUtils.rLabel(label1),
				GuiUtils.hbox(field1, GuiUtils.filler()) }, 4,
				GuiUtils.WT_NYNY, GuiUtils.WT_N);

		comps.add(topb);

		JComponent contents = GuiUtils.inset(GuiUtils.centerBottom(GuiUtils
				.vbox(comps), GuiUtils.makeOkCancelButtons(listener1)), 4);

		GuiUtils.packDialog(dialog, contents);
		Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();

		Point ctr = new Point(ss.width / 2 - 100, ss.height / 2 - 100);
		dialog.setLocation(ctr);
		dialog.setVisible(true);

	}

	/**
	 * Set the ProbeLocation property.
	 * 
	 * @param value
	 *            The new value for ProbeLocation
	 */
	public void setProbeLocation(LatLonPoint value) {
		probeLocation = value;
	}

	/**
	 * Get the ProbeLocation property.
	 * 
	 * @return The ProbeLocation
	 */
	public LatLonPoint getProbeLocation() {
		return probeLocation;
	}

	/**
	 * Set the RunOnClick property.
	 * 
	 * @param value
	 *            The new value for RunOnClick
	 */
	public void setRunOnClick(boolean value) {
		this.runOnClick = value;
	}

	/**
	 * Get the RunOnClick property.
	 * 
	 * @return The RunOnClick
	 */
	public boolean getRunOnClick() {
		return this.runOnClick;
	}

}
