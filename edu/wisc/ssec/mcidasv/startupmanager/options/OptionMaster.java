/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
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

package edu.wisc.ssec.mcidasv.startupmanager.options;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.wisc.ssec.mcidasv.startupmanager.StartupManager;
import edu.wisc.ssec.mcidasv.startupmanager.StartupManager.Platform;

public enum OptionMaster {
    /** The lone OptionMaster instance. */
    INSTANCE;

    // TODO(jon): write CollectionHelpers.zip() and CollectionHelpers.zipWith()
    public final Object[][] blahblah = {
        { "HEAP_SIZE", "Memory", "512m", Type.MEMORY, OptionPlatform.ALL, Visibility.VISIBLE },
        { "JOGL_TOGL", "Enable JOGL", "1", Type.BOOLEAN, OptionPlatform.UNIXLIKE, Visibility.VISIBLE },
        { "USE_3DSTUFF", "Enable 3D controls", "1", Type.BOOLEAN, OptionPlatform.ALL, Visibility.VISIBLE },
        { "DEFAULT_LAYOUT", "Load default layout", "1", Type.BOOLEAN, OptionPlatform.ALL, Visibility.VISIBLE },
        { "STARTUP_BUNDLE", "Defaults", "", Type.DIRTREE, OptionPlatform.ALL, Visibility.VISIBLE },
        { "SLIDER_TEST", "Slider Test", "50P", Type.SLIDER, OptionPlatform.ALL, Visibility.VISIBLE },
        /**
         * TODO: DAVEP: TomW's windows machine needs SET D3DREND= to work properly.
         * Not sure why, but it shouldn't hurt other users.  Investigate after Alpha10
         */
        { "D3DREND", "  Enable Direct3D", "", Type.BOOLEAN, OptionPlatform.WINDOWS, Visibility.VISIBLE },
    };

    /**
     * {@link Option}s can be either platform-specific or applicable to all
     * platforms. Options that are platform-specific still appear in the 
     * UI, but their component is not enabled.
     */
    public enum OptionPlatform { ALL, UNIXLIKE, WINDOWS };

    /**
     * The different types of {@link Option}s.
     * @see TextOption
     * @see BooleanOption
     * @see MemoryOption
     */
    public enum Type { TEXT, BOOLEAN, MEMORY, DIRTREE, SLIDER };

    /** 
     * Different ways that an {@link Option} might be displayed.
     */
    public enum Visibility { VISIBLE, HIDDEN };

    /** Maps an option ID to the corresponding object. */
    private final Map<String, Option> optionMap;

    OptionMaster() {
        normalizeUserDirectory();
        optionMap = buildOptions(blahblah);
//        readStartup();
    }

    /**
     * Creates the specified options and returns a mapping of the option ID
     * to the actual {@link Option} object.
     * 
     * @param options An array specifying the {@code Option}s to be built.
     * 
     * @return Mapping of ID to {@code Option}.
     * 
     * @throws AssertionError if the option array contained an entry that
     * this method cannot build.
     */
    private Map<String, Option> buildOptions(final Object[][] options) {
        // TODO(jon): seriously, get that zip stuff working! this array 
        // stuff is BAD.
        Map<String, Option> optMap = new HashMap<String, Option>();

        for (Object[] arrayOption : options) {
            String id = (String)arrayOption[0];
            String label = (String)arrayOption[1];
            String defaultValue = (String)arrayOption[2];
            Type type = (Type)arrayOption[3];
            OptionPlatform platform = (OptionPlatform)arrayOption[4];
            Visibility visibility = (Visibility)arrayOption[5];

            Option newOption;
            switch (type) {
                case TEXT:
                    newOption = new TextOption(id, label, defaultValue, 
                        platform, visibility);
                    break;
                case BOOLEAN:
                    newOption = new BooleanOption(id, label, defaultValue, 
                        platform, visibility);
                    break;
                case MEMORY:
                    newOption = new MemoryOption(id, label, defaultValue, 
                        platform, visibility);
                    break;
                case DIRTREE:
                    newOption = new DirectoryOption(id, label, defaultValue, platform, visibility);
                    break;
                case SLIDER:
                    newOption = new SliderOption(id, label, defaultValue, platform, visibility);
                    break;
                default:
                     throw new AssertionError(type + 
                         " is not known to OptionMaster.buildOptions()");
            }
            optMap.put(id, newOption);
        }
        return optMap;
    }

    /**
     * Converts a {@link Platform} to its corresponding 
     * {@link OptionPlatform} type.
     * 
     * @return The current platform as a {@code OptionPlatform} type.
     * 
     * @throws AssertionError if {@link StartupManager#getPlatform()} 
     * returned something that this method cannot convert.
     */
    // a lame-o hack :(
    protected OptionPlatform convertToOptionPlatform() {
        Platform platform = StartupManager.INSTANCE.getPlatform();
        switch (platform) {
            case WINDOWS: return OptionPlatform.WINDOWS;
            case UNIXLIKE: return OptionPlatform.UNIXLIKE;
            default: 
                throw new AssertionError("Unknown platform: " + platform);
        }
    }

    /**
     * Returns the {@link Option} mapped to {@code id}.
     * 
     * @param id The ID whose associated {@code Option} is to be returned.
     * 
     * @return Either the {@code Option} associated with {@code id}, or 
     * {@code null} if there was no association.
     */
    public Option getOption(final String id) {
        return optionMap.get(id);
    }

    // TODO(jon): getAllOptions and optionsBy* really need some work.
    // I want to eventually do something like:
    // Collection<Option> = getOpts().byPlatform(WINDOWS, ALL).byType(BOOLEAN).byVis(HIDDEN)
    public Collection<Option> getAllOptions() {
        return Collections.unmodifiableCollection(optionMap.values());
    }

    public Collection<Option> optionsByPlatform(
        final Set<OptionPlatform> platforms) 
    {
        if (platforms == null)
            throw new NullPointerException();

        Collection<Option> allOptions = getAllOptions();
        Collection<Option> filteredOptions = 
            new ArrayList<Option>(allOptions.size());
        for (Option option : allOptions)
            if (platforms.contains(option.getOptionPlatform()))
                filteredOptions.add(option);
//      return Collections.unmodifiableCollection(filteredOptions);
        return filteredOptions;
    }

    public Collection<Option> optionsByType(final Set<Type> types) {
        if (types == null)
            throw new NullPointerException();

        Collection<Option> allOptions = getAllOptions();
        Collection<Option> filteredOptions = 
            new ArrayList<Option>(allOptions.size());
        for (Option option : allOptions)
            if (types.contains(option.getOptionType()))
                filteredOptions.add(option);
//      return Collections.unmodifiableCollection(filteredOptions);
        return filteredOptions;
    }

    public Collection<Option> optionsByVisibility(
        final Set<Visibility> visibilities) 
    {
        if (visibilities == null)
            throw new NullPointerException();

        Collection<Option> allOptions = getAllOptions();
        Collection<Option> filteredOptions = 
            new ArrayList<Option>(allOptions.size());
        for (Option option : allOptions)
            if (visibilities.contains(option.getOptionVisibility()))
                filteredOptions.add(option);
//        return Collections.unmodifiableCollection(filteredOptions);
        return filteredOptions;
    }

    private void normalizeUserDirectory() {
        Platform platform = StartupManager.INSTANCE.getPlatform();
        File dir = new File(platform.getUserDirectory());
        File prefs = new File(platform.getUserPrefs());

        if (!dir.exists())
            dir.mkdir();

        if (!prefs.exists()) {
            try {
                File defaultPrefs = new File(platform.getDefaultPrefs());
                StartupManager.INSTANCE.copy(defaultPrefs, prefs);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void readStartup() {
        String contents;
        String line;

        File script = 
            new File(StartupManager.INSTANCE.getPlatform().getUserPrefs());
        if (script.getPath().length() == 0)
            return;

        try {
            BufferedReader br = new BufferedReader(new FileReader(script));
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#"))
                    continue;

                contents = new String(line);
                String[] chunks = contents.replace("SET ", "").split("=");
                if (chunks.length == 2) {
                    Option option = getOption(chunks[0]);
                    if (option != null)
                        option.fromPrefsFormat(line);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeStartup() {
        File script = 
            new File(StartupManager.INSTANCE.getPlatform().getUserPrefs());
        if (script.getPath().length() == 0)
            return;

        // TODO(jon): use filters when you've made 'em less stupid
        String newLine = 
            StartupManager.INSTANCE.getPlatform().getNewLine();
        OptionPlatform currentPlatform = convertToOptionPlatform();
        StringBuilder contents = new StringBuilder();
        for (Object[] arrayOption : blahblah) {
            Option option = getOption((String)arrayOption[0]);
            OptionPlatform platform = option.getOptionPlatform();
            if (platform == OptionPlatform.ALL || platform == currentPlatform)
                contents.append(option.toPrefsFormat() + newLine);
        }

        try {
            BufferedWriter out = 
                new BufferedWriter(new FileWriter(script));
            out.write(contents.toString());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
