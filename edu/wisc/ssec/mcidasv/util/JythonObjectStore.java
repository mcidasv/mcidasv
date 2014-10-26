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

package edu.wisc.ssec.mcidasv.util;

import static java.util.Objects.requireNonNull;

import ucar.unidata.idv.IdvObjectStore;

import edu.wisc.ssec.mcidasv.McIDASV;

/**
 * Wraps the application's {@link ucar.unidata.idv.IdvObjectStore} object and
 * provides methods that are safe to use from Jython scripts.
 *
 * <p>A secondary aim of this class is to be largely API-compatible with
 * {@link java.util.prefs.Preferences}.</p>
 */
public class JythonObjectStore {

    private final IdvObjectStore idvStore;

    public static JythonObjectStore newInstance(final McIDASV mcidasv) {
        return new JythonObjectStore(requireNonNull(mcidasv.getStore()));
    }

    private JythonObjectStore(IdvObjectStore store) {
        idvStore = requireNonNull(store);
    }

    public <T> T getObject(String key, T defaultValue) {
        T storedValue = (T)idvStore.get(key);
        if (storedValue == null) {
            storedValue = defaultValue;
        }
        return storedValue;
    }

    public short getShort(String key, short defaultValue) {
        return idvStore.get(key, defaultValue);
    }

    public char getChar(String key, char defaultValue) {
        return idvStore.get(key, defaultValue);
    }

    public String getString(String key, String defaultValue) {
        return idvStore.get(key, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return idvStore.get(key, defaultValue);
    }

    public double getDouble(String key, double defaultValue) {
        return idvStore.get(key, defaultValue);
    }

    public float getFloat(String key, float defaultValue) {
        return idvStore.get(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return idvStore.get(key, defaultValue);
    }

    public long getLong(String key, long defaultValue) {
        return idvStore.get(key, defaultValue);
    }

    public <T> void putObject(String key, T value) {
        idvStore.put(key, value);
    }

    public void putShort(String key, short value) {
        idvStore.put(key, value);
    }

    public void putChar(String key, char value) {
        idvStore.put(key, value);
    }

    public void putString(String key, String value) {
        idvStore.put(key, value);
    }

    public void putBoolean(String key, boolean value) {
        idvStore.put(key, value);
    }

    public void putDouble(String key, double value) {
        idvStore.put(key, value);
    }

    public void putFloat(String key, float value) {
        idvStore.put(key, value);
    }

    public void putInt(String key, int value) {
        idvStore.put(key, value);
    }

    public void putLong(String key, long value) {
        idvStore.put(key, value);
    }

//    public byte[] getByteArray(String key, byte[] defaultValue)
//    public void putByteArray(String key, byte[] value)
//    remove(String key)
}
