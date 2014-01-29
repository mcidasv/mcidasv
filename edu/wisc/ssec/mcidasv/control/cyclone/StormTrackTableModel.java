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

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import ucar.unidata.data.storm.StormParam;
import ucar.unidata.data.storm.StormTrack;
import ucar.unidata.data.storm.StormTrackPoint;
import visad.Real;
import visad.Unit;
import visad.VisADException;

/**
 * 
 * @author Unidata Development Team
 * @version $Revision$
 */

public class StormTrackTableModel extends AbstractTableModel {

	/**
	 * default
	 */
	private static final long serialVersionUID = 1L;

	/** _more_ */
	private StormDisplayState stormDisplayState;

	/** _more_ */
	private StormTrack track;

	/** _more_ */
	private List<StormTrackPoint> points;

	/** _more_ */
	private List<StormParam> params;

	/**
	 * _more_
	 * 
	 * @param stormDisplayState
	 *            _more_
	 * @param track
	 *            _more_
	 */
	public StormTrackTableModel(StormDisplayState stormDisplayState,
			StormTrack track) {
		this.stormDisplayState = stormDisplayState;
		this.track = track;
		this.points = track.getTrackPoints();
		List<StormParam> tmp = track.getParams();
		this.params = new ArrayList<StormParam>();
		for (StormParam param : tmp) {
			if (!param.getDerived()) {
				this.params.add(param);
			}
		}
	}

	/**
	 * _more_
	 * 
	 * @param rowIndex
	 *            _more_
	 * @param columnIndex
	 *            _more_
	 * 
	 * @return _more_
	 */
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		// if (true) {
		// return false;
		// }
		if (columnIndex == 0) {
			return false;
		}
		// return stormDisplayState.getStormTrackControl().isEditable();
		return stormDisplayState.getStormTrackControl().getEditMode();
	}

	/**
	 * _more_
	 * 
	 * @return _more_
	 */
	public StormTrack getStormTrack() {
		return track;
	}

	/**
	 * _more_
	 * 
	 * @return _more_
	 */
	public int getRowCount() {
		return points.size();
	}

	/**
	 * _more_
	 * 
	 * @return _more_
	 */
	public int getColumnCount() {
		return 3 + params.size();
	}

	/**
	 * _more_
	 * 
	 * @param aValue
	 *            _more_
	 * @param rowIndex
	 *            _more_
	 * @param column
	 *            _more_
	 */
	public void setValueAt(Object aValue, int rowIndex, int column) {
		StormTrackPoint stp = points.get(rowIndex);
		if (column == 0) {
			return;
		} else if (column == 1) {
			// latitude
		} else if (column == 2) {
			// longitude
		} else {
			StormParam param = params.get(column - 3);
			Real r = stp.getAttribute(param);
			double newValue = new Double(aValue.toString()).doubleValue();
			// Set the value
			Real rr = null;
			try {
				rr = r.cloneButValue(newValue);

			} catch (VisADException ep) {
			}

			stp.setAttribute(rr);
		}
		stormDisplayState.markHasBeenEdited();
	}

	/**
	 * _more_
	 * 
	 * @param row
	 *            _more_
	 * @param column
	 *            _more_
	 * 
	 * @return _more_
	 */
	public Object getValueAt(int row, int column) {
		if (row >= points.size()) {
			return "";
		}
		StormTrackPoint stp = points.get(row);
		if (column == 0) {
			if (track.getWay().isObservation()) {
				return stp.getTime();
			}
			return "" + stp.getForecastHour();
		}
		if (column == 1) {
			return stp.getLocation().getLatitude();
		}
		if (column == 2) {
			return stp.getLocation().getLongitude();
		}
		StormParam param = params.get(column - 3);
		Real r = stp.getAttribute(param);
		if (r != null) {
			return r.toString();
		}
		return "";
	}

	/**
	 * _more_
	 * 
	 * @param column
	 *            _more_
	 * 
	 * @return _more_
	 */
	public String getColumnName(int column) {
		if (column == 0) {
			return track.getWay().isObservation() ? "Time" : "Hour";
		}
		if (column == 1) {
			return "Lat";
		}
		if (column == 2) {
			return "Lon";
		}
		StormParam param = params.get(column - 3);
		Unit unit = param.getUnit();
		return param.toString() + ((unit == null) ? "" : "[" + unit + "]");
	}

}
