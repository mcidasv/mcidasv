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

package edu.wisc.ssec.mcidasv.util;

import static java.util.Objects.requireNonNull;

import ucar.unidata.idv.IdvObjectStore;

import edu.wisc.ssec.mcidasv.McIDASV;

import java.util.Set;
import java.util.TreeSet;

/**
 * Wraps the application's {@link ucar.unidata.idv.IdvObjectStore} object and
 * provides methods that are safe to use from Jython scripts.
 *
 * <p>A secondary aim of this class is to be largely API-compatible with
 * {@code java.util.prefs.Preferences}.</p>
 */
public class JythonObjectStore {

    /** {@code IdvObjectStore} used by the current McIDAS-V session. */
    private final IdvObjectStore idvStore;

    /**
     * Return a new {@code JythonObjectStore} instance.
     *
     * <p>Use this method rather than the constructor.</p>
     *
     * @param mcidasv McIDAS-V instance that represents current session. Cannot
     * be {@code null}.
     *
     * @return New instance of the {@code JythonObjectStore} class.
     *
     * @throws NullPointerException if {@code mcidasv} is {@code null}.
     */
    public static JythonObjectStore newInstance(final McIDASV mcidasv) {
        return new JythonObjectStore(requireNonNull(mcidasv.getStore()));
    }

    /**
     * Create a new {@code JythonObjectStore} wrapper object.
     *
     * @param store McIDAS-V object store. Cannot be {@code null}.
     *
     * @throws NullPointerException if {@code store} is {@code null}.
     */
    private JythonObjectStore(IdvObjectStore store) {
        idvStore = requireNonNull(store);
    }

    /**
     * Removes the value associated with the given {@code key} (if any).
     *
     * @param key Key whose associated value is to be removed. Cannot be
     * {@code null}.
     *
     * @throws NullPointerException if {@code key} is {@code null}.
     */
    public void remove(String key) {
        idvStore.remove(requireNonNull(key));
    }

    /**
     * Returns the object associated with the given {@code key}. If {@code key}
     * does not exist, {@code defaultValue} is returned.
     *
     * @param key Key whose associated object is to be returned.
     * Cannot be {@code null}.
     * @param defaultValue Value to be returned if {@code key} is not valid.
     * {@code null} is allowed.
     *
     * @return Object associated with {@code key} or {@code defaultValue} if
     * {@code key} is not valid.
     *
     * @throws NullPointerException if {@code key} is {@code null}.
     */
    public <T> T getObject(String key, T defaultValue) {
        T storedValue = (T)idvStore.get(requireNonNull(key));
        if (storedValue == null) {
            storedValue = defaultValue;
        }
        return storedValue;
    }

    /**
     * Returns the {@code short} value associated with the given {@code key}.
     * If {@code key} does not exist, {@code defaultValue} is returned.
     *
     * @param key Key whose associated {@code short} value is to be returned.
     * Cannot be {@code null}.
     * @param defaultValue Value to be returned if {@code key} is not valid.
     *
     * @return {@code short} value associated with {@code key} or
     * {@code defaultValue} if {@code key} is not valid.
     *
     * @throws NullPointerException if {@code key} is {@code null}.
     */
    public short getShort(String key, short defaultValue) {
        return idvStore.get(requireNonNull(key), defaultValue);
    }

    /**
     * Returns the {@code char} value associated with the given {@code key}.
     * If {@code key} does not exist, {@code defaultValue} is returned.
     *
     * @param key Key whose associated {@code char} value is to be returned.
     * Cannot be {@code null}.
     * @param defaultValue Value to be returned if {@code key} is not valid.
     *
     * @return {@code char} value associated with {@code key} or
     * {@code defaultValue} if {@code key} is not valid.
     *
     * @throws NullPointerException if {@code key} is {@code null}.
     */
    public char getChar(String key, char defaultValue) {
        return idvStore.get(requireNonNull(key), defaultValue);
    }

    /**
     * Returns the {@code String} value associated with the given {@code key}.
     * If {@code key} does not exist, {@code defaultValue} is returned.
     *
     * @param key Key whose associated {@code String} value is to be returned.
     * Cannot be {@code null}.
     * @param defaultValue Value to be returned if {@code key} is not valid.
     *
     * @return {@code String} value associated with {@code key} or
     * {@code defaultValue} if {@code key} is not valid.
     *
     * @throws NullPointerException if {@code key} is {@code null}.
     */
    public String getString(String key, String defaultValue) {
        return idvStore.get(requireNonNull(key), defaultValue);
    }

    /**
     * Returns the {@code boolean} value associated with the given {@code key}.
     * If {@code key} does not exist, {@code defaultValue} is returned.
     *
     * @param key Key whose associated {@code boolean} value is to be returned.
     * Cannot be {@code null}.
     * @param defaultValue Value to be returned if {@code key} is not valid.
     *
     * @return {@code boolean} value associated with {@code key} or
     * {@code defaultValue} if {@code key} is not valid.
     *
     * @throws NullPointerException if {@code key} is {@code null}.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return idvStore.get(requireNonNull(key), defaultValue);
    }

    /**
     * Returns the {@code double} value associated with the given {@code key}.
     * If {@code key} does not exist, {@code defaultValue} is returned.
     *
     * @param key Key whose associated {@code double} value is to be returned.
     * Cannot be {@code null}.
     * @param defaultValue Value to be returned if {@code key} is not valid.
     *
     * @return {@code double} value associated with {@code key} or
     * {@code defaultValue} if {@code key} is not valid.
     *
     * @throws NullPointerException if {@code key} is {@code null}.
     */
    public double getDouble(String key, double defaultValue) {
        return idvStore.get(requireNonNull(key), defaultValue);
    }

    /**
     * Returns the {@code float} value associated with the given {@code key}.
     * If {@code key} does not exist, {@code defaultValue} is returned.
     *
     * @param key Key whose associated {@code float} value is to be returned.
     * Cannot be {@code null}.
     * @param defaultValue Value to be returned if {@code key} is not valid.
     *
     * @return {@code float} value associated with {@code key} or
     * {@code defaultValue} if {@code key} is not valid.
     *
     * @throws NullPointerException if {@code key} is {@code null}.
     */
    public float getFloat(String key, float defaultValue) {
        return idvStore.get(requireNonNull(key), defaultValue);
    }

    /**
     * Returns the {@code int} value associated with the given {@code key}.
     * If {@code key} does not exist, {@code defaultValue} is returned.
     *
     * @param key Key whose associated {@code int} value is to be returned.
     * Cannot be {@code null}.
     * @param defaultValue Value to be returned if {@code key} is not valid.
     *
     * @return {@code int} value associated with {@code key} or
     * {@code defaultValue} if {@code key} is not valid.
     *
     * @throws NullPointerException if {@code key} is {@code null}.
     */
    public int getInt(String key, int defaultValue) {
        return idvStore.get(requireNonNull(key), defaultValue);
    }

    /**
     * Returns the {@code long} value associated with the given {@code key}.
     * If {@code key} does not exist, {@code defaultValue} is returned.
     *
     * @param key Key whose associated {@code long} value is to be returned.
     * Cannot be {@code null}.
     * @param defaultValue Value to be returned if {@code key} is not valid.
     *
     * @return {@code long} value associated with {@code key} or
     * {@code defaultValue} if {@code key} is not valid.
     *
     * @throws NullPointerException if {@code key} is {@code null}.
     */
    public long getLong(String key, long defaultValue) {
        return idvStore.get(requireNonNull(key), defaultValue);
    }

    /**
     * Associates the given {@code key} with the given object.
     *
     * @param key Key to associate with the given {@code value}.
     * Cannot be {@code null}.
     * @param value Object to associate with {@code key}. Cannot be
     * {@code null}.
     *
     * @throws NullPointerException if either {@code key} or {@code value} is
     * {@code null}.
     */
    public <T> void putObject(String key, T value) {
        idvStore.put(requireNonNull(key), requireNonNull(value));
    }

    /**
     * Associates the given {@code key} with the given {@code short} value.
     *
     * @param key Key to associate with the given {@code value}.
     * Cannot be {@code null}.
     * @param value {@code short} value to associate with {@code key}.
     *
     * @throws NullPointerException if either {@code key} or {@code value} is
     * {@code null}.
     */
    public void putShort(String key, short value) {
        idvStore.put(requireNonNull(key), value);
    }

    /**
     * Associates the given {@code key} with the given {@code char} value.
     *
     * @param key Key to associate with the given {@code value}.
     * Cannot be {@code null}.
     * @param value {@code char} value to associate with {@code key}.
     *
     * @throws NullPointerException if either {@code key} or {@code value} is
     * {@code null}.
     */
    public void putChar(String key, char value) {
        idvStore.put(requireNonNull(key), value);
    }

    /**
     * Associates the given {@code key} with the given {@code String} value.
     *
     * @param key Key to associate with the given {@code value}.
     * Cannot be {@code null}.
     * @param value {@code String} value to associate with {@code key}.
     * Cannot be {@code null}.
     *
     * @throws NullPointerException if either {@code key} or {@code value} is
     * {@code null}.
     */
    public void putString(String key, String value) {
        idvStore.put(requireNonNull(key), requireNonNull(value));
    }

    /**
     * Associates the given {@code key} with the given {@code boolean} value.
     *
     * @param key Key to associate with the given {@code value}.
     * Cannot be {@code null}.
     * @param value {@code boolean} value to associate with {@code key}.
     *
     * @throws NullPointerException if either {@code key} or {@code value} is
     * {@code null}.
     */
    public void putBoolean(String key, boolean value) {
        idvStore.put(requireNonNull(key), value);
    }

    /**
     * Associates the given {@code key} with the given {@code double} value.
     *
     * @param key Key to associate with the given {@code value}.
     * Cannot be {@code null}.
     * @param value {@code double} value to associate with {@code key}.
     *
     * @throws NullPointerException if either {@code key} or {@code value} is
     * {@code null}.
     */
    public void putDouble(String key, double value) {
        idvStore.put(requireNonNull(key), value);
    }

    /**
     * Associates the given {@code key} with the given {@code float} value.
     *
     * @param key Key to associate with the given {@code value}.
     * Cannot be {@code null}.
     * @param value {@code float} value to associate with {@code key}.
     *
     * @throws NullPointerException if either {@code key} or {@code value} is
     * {@code null}.
     */
    public void putFloat(String key, float value) {
        idvStore.put(requireNonNull(key), value);
    }

    /**
     * Associates the given {@code key} with the given {@code int} value.
     *
     * @param key Key to associate with the given {@code value}.
     * Cannot be {@code null}.
     * @param value {@code int} value to associate with {@code key}.
     *
     * @throws NullPointerException if either {@code key} or {@code value} is
     * {@code null}.
     */
    public void putInt(String key, int value) {
        idvStore.put(requireNonNull(key), value);
    }

    /**
     * Associates the given {@code key} with the given {@code long} value.
     *
     * @param key Key to associate with the given {@code value}.
     * Cannot be {@code null}.
     * @param value {@code long} value to associate with {@code key}.
     *
     * @throws NullPointerException if either {@code key} or {@code value} is
     * {@code null}.
     */
    public void putLong(String key, long value) {
        idvStore.put(requireNonNull(key), value);
    }


    public Set<String> getKeys() {
        // yeah, i don't like forwarding stuff like this either. but remember,
        // the intent is to eventually move away from the IDV object store
        // altogether (so this kinda redundant call may eventually go away).
        return idvStore.getKeys();
    }


    public Set<String> getMatchingKeys(String substring) {
        Set<String> allKeys = idvStore.getKeys();
        Set<String> matches = new TreeSet<>();
        for (String key : allKeys) {
            if (key.contains(substring)) {
                matches.add(key);
            }
        }
        return matches;
    }

    public String listKeys() {
        Set<String> keys = idvStore.getKeys();
        // 128 is just a guess as to the typical key length
        StringBuilder s = new StringBuilder(keys.size() * 128);
        for (String key : keys) {
            s.append(key).append('\n');
        }
        return s.toString();
    }

    public String listMatchingKeys(String substring) {
        Set<String> keys = idvStore.getKeys();
        StringBuilder s = new StringBuilder(keys.size() * 128);
        for (String key : keys) {
            if (key.contains(substring)) {
                s.append(key).append('\n');
            }
        }
        return s.toString();
    }
}
