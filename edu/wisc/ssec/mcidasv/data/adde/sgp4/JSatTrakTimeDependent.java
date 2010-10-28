/*
 * JSatTrakTimeDependent.java
 * 
 * =====================================================================
 * Copyright (C) 2009 Shawn E. Gano
 * 
 * This file is part of JSatTrak.
 * 
 * JSatTrak is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * JSatTrak is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with JSatTrak.  If not, see <http://www.gnu.org/licenses/>.
 * =====================================================================
 * 
 * abstract class for objects that depend on time and will be updated when time changes
 * 
 */

package edu.wisc.ssec.mcidasv.data.adde.sgp4;

import java.util.Hashtable;
//import jsattrak.objects.AbstractSatellite;
//import jsattrak.objects.GroundStation;
//import name.gano.astro.time.Time;

/**
 *
 * @author Shawn
 */
public interface JSatTrakTimeDependent 
{
    
    public void updateTime(final Time currentJulianDate, final Hashtable<String,AbstractSatellite> satHash, final Hashtable<String,GroundStation> gsHash);

}
