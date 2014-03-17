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
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.wisc.ssec.mcidasv.startupmanager.StartupManager;
import edu.wisc.ssec.mcidasv.startupmanager.Platform;

public class OptionMaster {

    public final static String SET_PREFIX = "SET ";
    public final static String EMPTY_STRING = "";
    public final static String QUOTE_STRING = "\"";
    public final static char QUOTE_CHAR = '"';

    // TODO(jon): write CollectionHelpers.zip() and CollectionHelpers.zipWith()
    public final Object[][] blahblah = {
        { "HEAP_SIZE", "Memory", "512m", Type.MEMORY, OptionPlatform.ALL, Visibility.VISIBLE },
        { "JOGL_TOGL", "Enable JOGL", "1", Type.BOOLEAN, OptionPlatform.UNIXLIKE, Visibility.VISIBLE },
        { "USE_3DSTUFF", "Enable 3D controls", "1", Type.BOOLEAN, OptionPlatform.ALL, Visibility.VISIBLE },
        { "DEFAULT_LAYOUT", "Load default layout", "1", Type.BOOLEAN, OptionPlatform.ALL, Visibility.VISIBLE },
        { "STARTUP_BUNDLE", "Defaults", "", Type.DIRTREE, OptionPlatform.ALL, Visibility.VISIBLE },
        /**
         * TODO: DAVEP: TomW's windows machine needs SET D3DREND= to work properly.
         * Not sure why, but it shouldn't hurt other users.  Investigate after Alpha10
         */
        { "D3DREND", "Enable Direct3D", "", Type.BOOLEAN, OptionPlatform.WINDOWS, Visibility.VISIBLE },
        // mcidasv enables this (the actual property is "visad.java3d.geometryByRef")
        // by default in mcidasv.properties.
        { "USE_GEOBYREF", "Enable access to geometry by reference", "1", Type.BOOLEAN, OptionPlatform.ALL, Visibility.VISIBLE },
        { "USE_IMAGEBYREF", "Enable access to image data by reference", "1", Type.BOOLEAN, OptionPlatform.ALL, Visibility.VISIBLE },
        { "USE_NPOT", "Enable Non-Power of Two (NPOT) textures", "0", Type.BOOLEAN, OptionPlatform.ALL, Visibility.VISIBLE },
        // temp bandaid for people suffering from permgen problems.
        { "USE_CMSGC", "Enable concurrent mark-sweep garbage collector", "0", Type.BOOLEAN, OptionPlatform.ALL, Visibility.VISIBLE },
        { "LOG_LEVEL", "Log Level", "INFO", Type.LOGLEVEL, OptionPlatform.ALL, Visibility.VISIBLE },
        { "JVM_OPTIONS", "Java Virtual Machine Options", "", Type.TEXT, OptionPlatform.ALL, Visibility.VISIBLE },
    };
    
    /**
     * {@link Option}s can be either platform-specific or applicable to all
     * platforms. Options that are platform-specific still appear in the 
     * UI, but their component is not enabled.
     */
    public enum OptionPlatform { ALL, UNIXLIKE, WINDOWS };
    
    /**
     * The different types of {@link Option}s.
     * 
     * @see TextOption
     * @see BooleanOption
     * @see MemoryOption
     * @see DirectoryOption
     * @see SliderOption
     * @see LoggerLevelOption
     */
    public enum Type { TEXT, BOOLEAN, MEMORY, DIRTREE, SLIDER, LOGLEVEL };
    
    /** 
     * Different ways that an {@link Option} might be displayed.
     */
    public enum Visibility { VISIBLE, HIDDEN };
    
    /** Maps an option ID to the corresponding object. */
    private Map<String, ? extends Option> optionMap;
    
    private static OptionMaster instance;
    
    public OptionMaster() {
        normalizeUserDirectory();
        optionMap = buildOptions(blahblah);
//        readStartup();
    }
    
    public static OptionMaster getInstance() {
        if (instance == null) {
            instance = new OptionMaster();
        }
        return instance;
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
        Map<String, Option> optMap = new HashMap<String, Option>(options.length);
        
        for (Object[] arrayOption : options) {
            String id = (String)arrayOption[0];
            String label = (String)arrayOption[1];
            String defaultValue = (String)arrayOption[2];
            Type type = (Type)arrayOption[3];
            OptionPlatform platform = (OptionPlatform)arrayOption[4];
            Visibility visibility = (Visibility)arrayOption[5];
            
            switch (type) {
                case TEXT:
                    optMap.put(id, new TextOption(id, label, defaultValue, platform, visibility));
                    break;
                case BOOLEAN:
                    optMap.put(id, new BooleanOption(id, label, defaultValue, platform, visibility));
                    break;
                case MEMORY:
                    optMap.put(id, new MemoryOption(id, label, defaultValue, platform, visibility));
                    break;
                case DIRTREE:
                    optMap.put(id, new DirectoryOption(id, label, defaultValue, platform, visibility));
                    break;
                case SLIDER:
                    optMap.put(id, new SliderOption(id, label, defaultValue, platform, visibility));
                    break;
                case LOGLEVEL:
                    optMap.put(id, new LoggerLevelOption(id, label, defaultValue, platform, visibility));
                    break;
                default:
                     throw new AssertionError(type + 
                         " is not known to OptionMaster.buildOptions()");
            }
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
        Platform platform = StartupManager.getInstance().getPlatform();
        switch (platform) {
            case WINDOWS: 
                return OptionPlatform.WINDOWS;
            case UNIXLIKE: 
                return OptionPlatform.UNIXLIKE;
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
     * 
     * 
     * @see #getMemoryOption
     * @see #getBooleanOption
     * @see #getDirectoryOption
     * @see #getSliderOption
     * @see #getTextOption
     * @see #getLoggerLevelOption
     */
    private Option getOption(final String id) {
        return optionMap.get(id);
    }
    
    /**
     * Searches {@link #optionMap} for the {@link MemoryOption} that 
     * corresponds with the given {@code id}.
     * 
     * @param id Identifier for the desired {@code MemoryOption}. 
     * Should not be {@code null}.
     * 
     * @return Either the {@code MemoryOption} that corresponds to {@code id} 
     * or {@code null}.
     */
    public MemoryOption getMemoryOption(final String id) {
        return (MemoryOption)optionMap.get(id);
    }
    
    /**
     * Searches {@link #optionMap} for the {@link BooleanOption} that 
     * corresponds with the given {@code id}.
     * 
     * @param id Identifier for the desired {@code BooleanOption}. 
     * Should not be {@code null}.
     * 
     * @return Either the {@code BooleanOption} that corresponds to {@code id} 
     * or {@code null}.
     */
    public BooleanOption getBooleanOption(final String id) {
        return (BooleanOption)optionMap.get(id);
    }
    
    /**
     * Searches {@link #optionMap} for the {@link DirectoryOption} that 
     * corresponds with the given {@code id}.
     * 
     * @param id Identifier for the desired {@code DirectoryOption}. 
     * Should not be {@code null}.
     * 
     * @return Either the {@code DirectoryOption} that corresponds to 
     * {@code id} or {@code null}.
     */
    public DirectoryOption getDirectoryOption(final String id) {
        return (DirectoryOption)optionMap.get(id);
    }
    
    /**
     * Searches {@link #optionMap} for the {@link SliderOption} that 
     * corresponds with the given {@code id}.
     * 
     * @param id Identifier for the desired {@code SliderOption}. 
     * Should not be {@code null}.
     * 
     * @return Either the {@code SliderOption} that corresponds to {@code id} 
     * or {@code null}.
     */
    public SliderOption getSliderOption(final String id) {
        return (SliderOption)optionMap.get(id);
    }
    
    /**
     * Searches {@link #optionMap} for the {@link TextOption} that 
     * corresponds with the given {@code id}.
     * 
     * @param id Identifier for the desired {@code TextOption}. 
     * Should not be {@code null}.
     * 
     * @return Either the {@code TextOption} that corresponds to {@code id} 
     * or {@code null}.
     */
    public TextOption getTextOption(final String id) {
        return (TextOption)optionMap.get(id);
    }
    
    /**
     * Searches {@link #optionMap} for the {@link LoggerLevelOption} that 
     * corresponds with the given {@code id}.
     * 
     * @param id Identifier for the desired {@code LoggerLevelOption}. 
     * Should not be {@code null}.
     * 
     * @return Either the {@code LoggerLevelOption} that corresponds to {@code id} 
     * or {@code null}.
     */
    public LoggerLevelOption getLoggerLevelOption(final String id) {
        return (LoggerLevelOption)optionMap.get(id);
    }
    
    // TODO(jon): getAllOptions and optionsBy* really need some work.
    // I want to eventually do something like:
    // Collection<Option> = getOpts().byPlatform(WINDOWS, ALL).byType(BOOLEAN).byVis(HIDDEN)
    /**
     * Returns all the available startup manager options.
     * 
     * @return Either all available startup manager options or an empty 
     * {@link Collection}.
     */
    public Collection<Option> getAllOptions() {
        return Collections.unmodifiableCollection(optionMap.values());
    }
    
    /**
     * Returns the {@link Option Options} applicable to the given 
     * {@link OptionPlatform OptionPlatforms}.
     * 
     * @param platforms Desired platforms. Cannot be {@code null}.
     * 
     * @return Either a {@link List} of {code Option}-s applicable to
     * {@code platforms} or an empty {@code List}.
     */
    public List<Option> optionsByPlatform(
        final Collection<OptionPlatform> platforms) 
    {
        if (platforms == null) {
            throw new NullPointerException();
        }
        Collection<Option> allOptions = getAllOptions();
        List<Option> filteredOptions = 
            new ArrayList<Option>(allOptions.size());
        for (Option option : allOptions) {
            if (platforms.contains(option.getOptionPlatform())) {
                filteredOptions.add(option);
            }
        }
        return filteredOptions;
    }
    
    /**
     * Returns the {@link Option Options} that match the given 
     * {@link Type Types}. 
     * 
     * @param types Desired {@code Option} types. Cannot be {@code null}.
     * 
     * @return Either the {@code List} of {@code Option}-s that match the given 
     * types or an empty {@code List}.
     */
    public List<Option> optionsByType(final Collection<Type> types) {
        if (types == null) {
            throw new NullPointerException();
        }
        Collection<Option> allOptions = getAllOptions();
        List<Option> filteredOptions = 
            new ArrayList<Option>(allOptions.size());
        for (Option option : allOptions) {
            if (types.contains(option.getOptionType())) {
                filteredOptions.add(option);
            }
        }
        return filteredOptions;
    }
    
    /**
     * Returns the {@link Option Options} that match the given levels of 
     * {@link Visibility visibility}.
     * 
     * @param visibilities Desired visibility levels. Cannot be {@code null}.
     * 
     * @return Either the {@code List} of {@code Option}-s that match the given 
     * visibility levels or an empty {@code List}. 
     */
    public List<Option> optionsByVisibility(
        final Collection<Visibility> visibilities) 
    {
        if (visibilities == null) {
            throw new NullPointerException();
        }
        Collection<Option> allOptions = getAllOptions();
        List<Option> filteredOptions = 
            new ArrayList<Option>(allOptions.size());
        for (Option option : allOptions) {
            if (visibilities.contains(option.getOptionVisibility())) {
                filteredOptions.add(option);
            }
        }
        return filteredOptions;
    }
    
    private void normalizeUserDirectory() {
        StartupManager startup = StartupManager.getInstance();
        Platform platform = startup.getPlatform();
        File dir = new File(platform.getUserDirectory());
        File prefs = new File(platform.getUserPrefs());
        
        if (!dir.exists()) {
            dir.mkdir();
        }
        if (!prefs.exists()) {
            try {
                File defaultPrefs = new File(platform.getDefaultPrefs());
                startup.copy(defaultPrefs, prefs);
            } catch (IOException e) {
                System.err.println("Non-fatal error copying user preference template: "+e.getMessage());
            }
        }
    }

    public void readStartup() {
        String line;
        
        File script = 
            new File(StartupManager.getInstance().getPlatform().getUserPrefs());
        System.err.println("reading "+script);
        if (script.getPath().isEmpty()) {
            return;
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader(script));
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }

                int splitAt = line.indexOf('=');
                if (splitAt >= 0) {
                    String id = line.substring(0, splitAt).replace(SET_PREFIX, EMPTY_STRING);
                    Option option = getOption(id);
                    if (option != null) {
                        System.err.println("setting '"+id+"' with '"+line+'\'');
                        option.fromPrefsFormat(line);
                    } else {
                        System.err.println("Warning: Unknown ID '"+id+'\'');
                    }
                } else {
                    System.err.println("Warning: Bad line format '"+line+'\'');
                }
            }
            br.close();
        } catch (IOException e) {
            System.err.println("Non-fatal error reading the user preferences: "+e.getMessage());
        }
    }
    
    public void writeStartup() {
        File script = 
            new File(StartupManager.getInstance().getPlatform().getUserPrefs());
        if (script.getPath().isEmpty()) {
            return;
        }
        // TODO(jon): use filters when you've made 'em less stupid
        String newLine = 
                StartupManager.getInstance().getPlatform().getNewLine();
        OptionPlatform currentPlatform = convertToOptionPlatform();
        StringBuilder contents = new StringBuilder(2048);
        for (Object[] arrayOption : blahblah) {
            Option option = getOption((String)arrayOption[0]);
            OptionPlatform platform = option.getOptionPlatform();
            if (platform == OptionPlatform.ALL || platform == currentPlatform) {
                contents.append(option.toPrefsFormat()).append(newLine);
            }
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
