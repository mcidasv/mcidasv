/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2015
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

package edu.wisc.ssec.mcidasv.data.hydra;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.rmi.RemoteException;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.MapProjectionDisplayJ3D;
import ucar.visad.ProjectionCoordinateSystem;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.MapLines;
import ucar.visad.display.RubberBandBox;
import visad.CellImpl;
import visad.FlatField;
import visad.Gridded2DSet;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.UnionSet;
import visad.VisADException;
import visad.data.mcidas.BaseMapAdapter;
import visad.georef.MapProjection;

public class TrackSelection extends DataSelectionComponent {

	private static final Logger logger = LoggerFactory.getLogger(TrackSelection.class);
	
	public static final int DEFAULT_TRACK_STRIDE = 5;
	public static final int DEFAULT_VERTICAL_STRIDE = 2;
	
	DataChoice dataChoice;
	FlatField track;

	double[] x_coords = new double[2];
	double[] y_coords = new double[2];
	boolean hasSubset = true;
	MapProjectionDisplayJ3D mapProjDsp;
	DisplayMaster dspMaster;

	int trackStride;
	int verticalStride;

	JTextField trkStr;
	JTextField vrtStr;
	MultiDimensionDataSource datSrc;

	TrackSelection(DataChoice dataChoice, FlatField track,
			MultiDimensionDataSource datSrc) throws VisADException, RemoteException {
		super("track");
		this.dataChoice = dataChoice;
		this.track = track;
		this.datSrc = datSrc;
		mapProjDsp = new MapProjectionDisplayJ3D(
				MapProjectionDisplay.MODE_2Din3D);
		mapProjDsp.enableRubberBanding(false);
		dspMaster = mapProjDsp;
		mapProjDsp.setMapProjection(getDataProjection());
		LineDrawing trackDsp = new LineDrawing("track");
		trackDsp.setLineWidth(2f);
		trackDsp.setData(track);
		mapProjDsp.addDisplayable(trackDsp);

		MapLines mapLines = new MapLines("maplines");
		URL mapSource = mapProjDsp.getClass().getResource(
				"/auxdata/maps/OUTLSUPU");
		try {
			BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
			mapLines.setMapLines(mapAdapter.getData());
			mapLines.setColor(java.awt.Color.cyan);
			mapProjDsp.addDisplayable(mapLines);
		} catch (Exception excp) {
			logger.error("cannot open map file: " + mapSource, excp);
		}

		mapLines = new MapLines("maplines");
		mapSource = mapProjDsp.getClass().getResource("/auxdata/maps/OUTLSUPW");
		try {
			BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
			mapLines.setMapLines(mapAdapter.getData());
			mapLines.setColor(java.awt.Color.cyan);
			mapProjDsp.addDisplayable(mapLines);
		} catch (Exception excp) {
			logger.error("cannot open map file: " + mapSource, excp);
		}

		mapLines = new MapLines("maplines");
		mapSource = mapProjDsp.getClass().getResource("/auxdata/maps/OUTLHPOL");
		try {
			BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
			mapLines.setMapLines(mapAdapter.getData());
			mapLines.setColor(java.awt.Color.cyan);
			mapProjDsp.addDisplayable(mapLines);
		} catch (Exception excp) {
			logger.error("cannot open map file: " + mapSource, excp);
		}

		final LineDrawing selectBox = new LineDrawing("select");
		selectBox.setColor(Color.green);

		final RubberBandBox rbb = new RubberBandBox(RealType.Longitude,
				RealType.Latitude, 1);
		rbb.setColor(Color.green);
		rbb.addAction(new CellImpl() {
			public void doAction() throws VisADException, RemoteException {
				Gridded2DSet set = rbb.getBounds();
				float[] low = set.getLow();
				float[] hi = set.getHi();
				x_coords[0] = low[0];
				x_coords[1] = hi[0];
				y_coords[0] = low[1];
				y_coords[1] = hi[1];

				SampledSet[] sets = new SampledSet[4];
				sets[0] = new Gridded2DSet(
						RealTupleType.SpatialEarth2DTuple,
						new float[][] { { low[0], hi[0] }, { low[1], low[1] } },
						2);
				sets[1] = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple,
						new float[][] { { hi[0], hi[0] }, { low[1], hi[1] } },
						2);
				sets[2] = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple,
						new float[][] { { hi[0], low[0] }, { hi[1], hi[1] } },
						2);
				sets[3] = new Gridded2DSet(
						RealTupleType.SpatialEarth2DTuple,
						new float[][] { { low[0], low[0] }, { hi[1], low[1] } },
						2);
				UnionSet uset = new UnionSet(sets);
				selectBox.setData(uset);
			}
		});
		dspMaster.addDisplayable(rbb);
		dspMaster.addDisplayable(selectBox);
		dspMaster.draw();
	}

	public MapProjection getDataProjection() {
		MapProjection mp = null;
		try {
			mp = new ProjectionCoordinateSystem(new LatLonProjection());
		} catch (Exception e) {
			logger.error("error getting data projection", e);
		}
		return mp;
	}

	protected JComponent doMakeContents() {
		try {
			JPanel panel = new JPanel(new BorderLayout());
			panel.add("Center", dspMaster.getDisplayComponent());

			JPanel stridePanel = new JPanel(new FlowLayout());
			trkStr = new JTextField(Integer.toString(TrackSelection.DEFAULT_TRACK_STRIDE), 3);
			vrtStr = new JTextField(Integer.toString(TrackSelection.DEFAULT_VERTICAL_STRIDE), 3);
			trkStr.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					setTrackStride(Integer.valueOf(trkStr.getText().trim()));
				}
			});
			vrtStr.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					setVerticalStride(Integer.valueOf(vrtStr.getText().trim()));
				}
			});

			stridePanel.add(new JLabel("Track Stride: "));
			stridePanel.add(trkStr);
			stridePanel.add(new JLabel("Vertical Stride: "));
			stridePanel.add(vrtStr);
			panel.add("South", stridePanel);

			return panel;
		} catch (Exception e) {
			logger.error("error creating contents", e);
		}
		return null;
	}

	public void setTrackStride(int stride) {
		trackStride = stride;
	}

	public void setVerticalStride(int stride) {
		verticalStride = stride;
	}

	/**
	 * Update Track Stride if input text box holds a positive integer.
	 * 
	 * @return true if trackStride was updated
	 */
	
	public boolean setTrackStride() {
		boolean setOk = false;
		try {
			int newStride = Integer.valueOf(trkStr.getText().trim());
			trackStride = newStride;
			setOk = true;
		} catch (NumberFormatException nfe) {
			// do nothing, will return correct result code
		}
		return setOk;
	}

	/**
	 * Update Vertical Stride if input text box holds a positive integer.
	 * 
	 * @return true if verticalStride was updated
	 */
	
	public boolean setVerticalStride() {
		boolean setOk = false;
		try {
			int newStride = Integer.valueOf(vrtStr.getText().trim());
			verticalStride = newStride;
			setOk = true;
		} catch (NumberFormatException nfe) {
			// do nothing, will return correct result code
		}
		return setOk;
	}

	public void applyToDataSelection(DataSelection dataSelection) {
		setTrackStride();
		setVerticalStride();
		if (hasSubset) {
			GeoSelection geoSelect = new GeoSelection(new GeoLocationInfo(
					y_coords[1], x_coords[0], y_coords[0], x_coords[1]));
			geoSelect.setXStride(trackStride);
			geoSelect.setYStride(verticalStride);
			dataSelection.setGeoSelection(geoSelect);

			DataSelection datSel = new DataSelection();
			datSel.setGeoSelection(geoSelect);
			datSrc.setDataSelection(datSel);
			dataChoice.setDataSelection(dataSelection);
		}
	}
}