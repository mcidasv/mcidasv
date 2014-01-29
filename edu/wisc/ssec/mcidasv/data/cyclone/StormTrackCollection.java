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

package edu.wisc.ssec.mcidasv.data.cyclone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import visad.DateTime;

/**
 * Created by IntelliJ IDEA. User: yuanho Date: Apr 9, 2008 Time: 5:00:40 PM To
 * change this template use File | Settings | File Templates.
 */

public class StormTrackCollection {

	/** _more_ */
	// private HashMap forecastWayMapStartDates;

	/** _more_ */
	// private StormTrack obsTrack;

	/** _more_ */
	private HashMap<Way, List> wayToTracksHashMap;

	/** _more_ */
	List<StormTrack> tracks = new ArrayList<StormTrack>();

	/**
	 * _more_
	 */
	public StormTrackCollection() {
		wayToTracksHashMap = new HashMap<Way, List>();
		// forecastWayMapStartDates = new HashMap();
		// obsTrack = null;
	}

	/**
	 * _more_
	 * 
	 * 
	 * @return _more_
	 */
	public List<Way> getWayList() {
		Set ss = wayToTracksHashMap.keySet();
		ArrayList ways = new ArrayList();
		ways.addAll(ss);
		return ways;
	}

	/**
	 * _more_
	 * 
	 * @param tracks
	 *            _more_
	 */
	public void addTrackList(List<StormTrack> tracks) {
		for (StormTrack track : tracks) {
			addTrack(track);
		}
	}

	/**
	 * _more_
	 * 
	 * @param track
	 *            _more_
	 */
	public void addTrack(StormTrack track) {
		List list = wayToTracksHashMap.get(track.getWay());
		if (list == null) {
			wayToTracksHashMap.put(track.getWay(),
					list = new ArrayList<StormTrack>());

		}
		list.add(track);
		tracks.add(track);
	}

	/**
	 * _more_
	 * 
	 * @return _more_
	 */
	public List<StormTrack> getTracks() {
		return new ArrayList<StormTrack>(tracks);
	}

	/**
	 * _more_
	 * 
	 * 
	 * @param way
	 *            _more_
	 * @return _more_
	 */
	public List<StormTrack> getTrackList(Way way) {
		return (List<StormTrack>) wayToTracksHashMap.get(way);
	}

	/**
	 * _more_
	 * 
	 * @return _more_
	 */
	public HashMap<Way, List> getWayToTracksHashMap() {
		return wayToTracksHashMap;
	}

	/**
	 * _more_
	 * 
	 * @return _more_
	 */
	public HashMap getWayToStartDatesHashMap() {
		HashMap wayToStartDatesHashMap = new HashMap();
		int size = wayToTracksHashMap.size();
		Set ways = wayToTracksHashMap.keySet();
		Iterator itr = ways.iterator();
		for (int i = 0; i < size; i++) {
			Way way = (Way) itr.next();
			List tracks = getTrackList(way);
			List startTimes = new ArrayList();
			if (tracks != null) {
				Iterator its = tracks.iterator();
				while (its.hasNext()) {
					StormTrack track = (StormTrack) its.next();
					DateTime st = track.getStartTime();
					startTimes.add(st);
				}
				if (startTimes.size() > 0) {
					wayToStartDatesHashMap.put(way, startTimes);
				}
			}

		}
		return wayToStartDatesHashMap;
	}

	/**
	 * _more_
	 * 
	 * @return _more_
	 */
	public StormTrack getObsTrack() {
		List tracks = getTrackList(Way.OBSERVATION);
		if ((tracks == null) || (tracks.size() == 0)) {
			return null;
		}
		return (StormTrack) tracks.get(0);
	}

	/**
	 * _more_
	 * 
	 * 
	 * @param way
	 *            _more_
	 */
	public void setObsTrack(Way way) {
		// first remove the obs track
		List<StormTrack> obtracks = wayToTracksHashMap.get(Way.OBSERVATION);
		StormTrack obtrack = obtracks.get(0);
		StormInfo sInfo = obtrack.getStormInfo();
		List<StormParam> obParams = obtrack.getParams();
		int size = obParams.size();
		StormParam[] obParam = new StormParam[size];
		int i = 0;
		for (StormParam sp : obParams) {
			obParam[i] = sp;
			i++;
		}
		wayToTracksHashMap.remove(Way.OBSERVATION);

		// now construct the obs track
		List<StormTrackPoint> newObsPoints = new ArrayList();
		List<StormTrack> tracks = getTrackList(way);

		for (StormTrack stk : tracks) {
			List<StormTrackPoint> stkPoints = stk.getTrackPoints();
			newObsPoints.add(stkPoints.get(0));
		}

		StormTrack stk = new StormTrack(sInfo, Way.OBSERVATION, newObsPoints,
				obParam);

		addTrack(stk);
	}

}
