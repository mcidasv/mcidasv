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
package edu.wisc.ssec.mcidasv.servermanager;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryType;

/**
 * 
 * 
 *
 */
public class LocalAddeEntry implements AddeEntry {

    /** Friendly neighborhood logging object. */
    static final Logger logger = LoggerFactory.getLogger(LocalAddeEntry.class);

    /** Represents a {@literal "bad"} local ADDE entry. */
    // seriously, don't use null unless you REALLY need it.
    public static final LocalAddeEntry INVALID_ENTRY = new Builder("INVALID", "INVALID", "/dev/null", AddeFormat.INVALID).build();

    /** Represents a {@literal "bad"} collection of local ADDE entries. */
    public static final List<LocalAddeEntry> INVALID_ENTRIES = Collections.singletonList(INVALID_ENTRY);

    /** */
    private static final String CYGWIN_PREFIX = "/cygdrive/";

    /** */
    private static final int CYGWIN_PREFIX_LEN = CYGWIN_PREFIX.length();

    /** */
    private EntryStatus entryStatus = EntryStatus.INVALID;

    // RESOLV.SRV FIELDS
    /** N1 */
    private final String group;

    /** N2 */
    // this value is built in a non-obvious way. plz to be dox.
    private final String descriptor;

    /** RT */
    private final boolean realtime;

    /** MCV */
    private final AddeFormat format;

    /** R1 */
    private final String start;

    /** R2 */
    private final String end;

    /** MASK */
    private final String fileMask;

    /** C */
    private final String name;
    // END RESOLV.SRV FIELDS

    private String asStringId;

    /** */
    private final boolean isTemporary;

    /** */
    private String entryAlias;

    public enum ServerName {
        AREA, AMSR, AMRR, GINI, FSDX, OMTP, LV1B, MODS, MODX, MOD4, MOD8, 
        MODR, MSGT, MTST, SMIN, TMIN, MD, INVALID;
    }

    /**
     * The various kinds of local ADDE data understood by McIDAS-V, along with
     * some helpful metadata.
     * 
     * <p><ul>
     * <li>{@literal "Human readable"} format names ({@link #friendlyName}).</li>
     * <li>Optional tooltip description ({@link #tooltip}).</li>
     * <li>Type of data ({@link #type}).</li>
     * <li>File naming pattern {@link #fileFilter}.</li>
     * </ul>
     * 
     * <p>None of {@code AddeFormat}'s fields should contain {@code null}.
     */
    public enum AddeFormat {
        MCIDAS_AREA(ServerName.AREA, "McIDAS AREA"),
        MCIDAS_MD(ServerName.MD, "McIDAS MD", "McIDAS MD", EntryType.POINT),
        AMSRE_L1B(ServerName.AMSR, "AMSR-E L 1b", "AMSR-E Level 1b"),
        AMSRE_RAIN_PRODUCT(ServerName.AMRR, "AMSR-E Rain Product"),
        GINI(ServerName.GINI, "GINI"),
        LRIT_GOES9(ServerName.FSDX, "LRIT GOES-9", "EUMETCast LRIT GOES-9"),
        LRIT_GOES10(ServerName.FSDX, "LRIT GOES-10", "EUMETCast LRIT GOES-10"),
        LRIT_GOES11(ServerName.FSDX, "LRIT GOES-11", "EUMETCast LRIT GOES-11"),
        LRIT_GOES12(ServerName.FSDX, "LRIT GOES-12", "EUMETCast LRIT GOES-12"),
        LRIT_MET5(ServerName.FSDX, "LRIT MET-5", "EUMETCast LRIT MET-5"),
        LRIT_MET7(ServerName.FSDX, "LRIT MET-7", "EUMETCast LRIT MET-7"),
        LRIT_MTSAT1R(ServerName.FSDX, "LRIT MTSAT-1R", "EUMETCast LRIT MTSAT-1R"),
        METEOSAT_OPENMTP(ServerName.OMTP, "Meteosat OpenMTP"),
        METOP_AVHRR_L1B(ServerName.LV1B, "Metop AVHRR L 1b", "Metop AVHRR Level 1b"),
        MODIS_L1B_MOD02(ServerName.MODS, "MODIS MOD 02 - Level-1B Calibrated Geolocated Radiances", "MODIS Level 1b"),
        MODIS_L2_MOD06(ServerName.MODX, "MODIS MOD 06 - Cloud Product", "MODIS Level 2 (Cloud Top Properties)"),
        MODIS_L2_MOD07(ServerName.MODX, "MODIS MOD 07 - Atmospheric Profiles", "MODIS Level 2 (Atmospheric Profile)"),
        MODIS_L2_MOD35(ServerName.MODX, "MODIS MOD 35 - Cloud Mask", "MODIS Level 2 (Cloud Mask)"),
        MODIS_L2_MOD04(ServerName.MOD4, "MODIS MOD 04 - Aerosol Product", "MODIS Level 2 (Aerosol)"),
        MODIS_L2_MOD28(ServerName.MOD8, "MODIS MOD 28 - Sea Surface Temperature", "MODIS Level 2 (Sea Surface Temperature)"),
        MODIS_L2_MODR(ServerName.MODR, "MODIS MOD R - Corrected Reflectance", "MODIS Level 2 (Corrected Reflectance)"),
        MSG_HRIT_FD(ServerName.MSGT, "MSG HRIT FD", "MSG HRIT (Full Disk)"),
        MSG_HRIT_HRV(ServerName.MSGT, "MSG HRIT HRV", "MSG HRIT (High Resolution Visible)"),
        MTSAT_HRIT(ServerName.MTST, "MTSAT HRIT"),
        NOAA_AVHRR_L1B(ServerName.LV1B, "NOAA AVHRR L 1b", "NOAA AVHRR Level 1b"),
        SSMI(ServerName.SMIN, "SSMI", "Terrascan netCDF (SMIN)"),
        TRMM(ServerName.TMIN, "TRMM", "Terrascan netCDF (TMIN)"),
        INVALID(ServerName.INVALID, "", "", EntryType.INVALID);

        /** Name of the McIDAS-X server. */
        private final ServerName servName;

        /** {@literal "Human readable"} format name. This is returned by {@link #toString()}. */
        private final String friendlyName;

        /** Description of the format. */
        private final String tooltip;

        /** Data type. Corresponds to {@code TYPE} in {@literal "RESOLV.SRV"}. */
        private final EntryType type;

        /** 
         * Filename pattern used when listing files in a directory. 
         * If {@link #servName} is {@link ServerName#MSGT} then 
         * {@literal "*PRO*"} is used, otherwise {@literal "*"}. 
         */
        private final String fileFilter;

        /**
         * Builds an {@literal "ADDE format"} and its associated metadata in 
         * a typesafe way.
         * 
         * @param servName {@link ServerName} that McIDAS-X uses for this format. 
         * @param friendlyName {@literal "Human readable"} name of the format; returned by {@link #toString()}.
         * @param tooltip If non-empty, this is used as a tooltip in the local entry editor.
         * @param type {@link EntryType} used by this format.
         */
        AddeFormat(final ServerName servName, final String friendlyName, final String tooltip, final EntryType type) {
            this.servName = servName;
            this.friendlyName = friendlyName;
            this.tooltip = tooltip;
            this.type = type;
            this.fileFilter = (servName != ServerName.MSGT) ? "*" : "*PRO*";
        }

        /**
         * Builds an {@literal "imagery ADDE Format"} <b>without</b> a tooltip.
         *
         * @param servName {@link ServerName} that McIDAS-X uses for this format.
         * @param friendlyName {@literal "Human readable"} name of the format; returned by {@link #toString()}.
         */
        AddeFormat(final ServerName servName, final String friendlyName) {
            this(servName, friendlyName, "", EntryType.IMAGE);
        }

        /**
         * Builds an {@literal "imagery ADDE Format"} <b>with</b> a tooltip.
         *
         * @param servName {@link ServerName} that McIDAS-X uses for this format.
         * @param friendlyName {@literal "Human readable"} name of the format; returned by {@link #toString()}.
         * @param tooltip If non-empty, this is used as a tooltip in the local entry editor.
         */
        AddeFormat(final ServerName servName, final String friendlyName, final String tooltip) {
            this(servName, friendlyName, tooltip, EntryType.IMAGE);
        }

        /**
         * Gets the McIDAS-X {@link ServerName} for this format.
         *
         * @return Either the name of this format's McIDAS-X server, or {@link ServerName#INVALID}.
         */
        public ServerName getServerName() {
            return servName;
        }

        /**
         * Gets the tooltip text to use in the server manager GUI for this
         * format.
         *
         * @return Text to use as a GUI tooltip. Cannot be {@code null}, though
         * empty {@code String} values are permitted.
         */
        public String getTooltip() {
            return tooltip;
        }

        /**
         * Gets the type of data used by this format. This value dictates the
         * chooser(s) where this format can appear.
         *
         * @return One of {@link EntryType}, or {@link EntryType#INVALID}.
         */
        public EntryType getType() {
            return type;
        }

        /**
         * Gets the string used to filter out files that match this format.
         *
         * @return Either a specialized {@code String}, like {@literal "*PRO*"} or {@literal "*"}.
         */
        public String getFileFilter() {
            return fileFilter;
        }

        /**
         * Gets the {@code String} representation of this format.
         *
         * @return the value of {@link #friendlyName}.
         */
        @Override public String toString() {
            return friendlyName;
        }
    }

    /**
     *
     *
     * @param builder
     * 
     * @see LocalAddeEntry.Builder
     */
    private LocalAddeEntry(final Builder builder) {
        this.group = builder.group;
        this.descriptor = builder.descriptor;
        this.realtime = builder.realtime;
        this.format = builder.format;
        this.fileMask = builder.mask;
        this.name = builder.name;
        this.start = builder.start;
        this.end = builder.end;
        this.entryStatus = builder.status;
        this.isTemporary = builder.temporary;
        this.entryAlias = builder.alias;
        logger.debug("created local: {}", this);
    }

    @Override public AddeAccount getAccount() {
        return RemoteAddeEntry.DEFAULT_ACCOUNT;
    }

    @Override public String getAddress() {
        return "localhost";
    }

    @Override public EntrySource getEntrySource() {
        return EntrySource.USER;
    }

    @Override public EntryStatus getEntryStatus() {
        return entryStatus;
    }

    @Override public String getEntryText() {
        return "localhost/"+getGroup();
    }

    @Override public EntryType getEntryType() {
        return format.getType();
    }

    @Override public EntryValidity getEntryValidity() {
        return (isValid()) ? EntryValidity.VERIFIED : EntryValidity.INVALID;
    }

    // TODO(jon): fix this noop
    @Override public String getEntryAlias() {
        String tmp = entryAlias;
        if (entryAlias == null) {
            tmp = "";
        }
        return tmp;
    }

    // TODO(jon): fix this noop
    @Override public void setEntryAlias(final String newAlias) {
        if (newAlias == null) {
            throw new NullPointerException("Null aliases are not allowable.");
        }
        this.entryAlias = newAlias;
    }

    @Override public void setEntryStatus(EntryStatus newStatus) {
        entryStatus = newStatus;
    }

    @Override public boolean isEntryTemporary() {
        return isTemporary;
    }

    @Override public String getGroup() {
        return group;
    }

    @Override public String getName() {
        return name;
    }

    /**
     * Gets the ADDE descriptor for the current local ADDE entry.
     * 
     * @return ADDE descriptor (corresponds to the {@literal "N2"} section of
     * a RESOLV.SRV entry).
     */
    public String getDescriptor() {
        return descriptor;
    }

    /**
     * Gets the ADDE dataset format for the current local ADDE entry.
     * 
     * @return ADDE format (corresponds to the {@literal "MCV"} section of a
     * RESOLV.SRV entry).
     */
    public AddeFormat getFormat() {
        return format;
    }

    /**
     * Gets the ADDE file mask for the current local ADDE entry.
     * 
     * @return ADDE file mask (corresponds to the {@literal "MASK"} section
     * of a RESOLV.SRV entry).
     */
    public String getMask() {
        return fileMask;
    }

    /**
     * Gets the ADDE file mask for the current local ADDE entry.
     * 
     * @return ADDE file mask (corresponds to the {@literal "MASK"} section
     * of a RESOLV.SRV entry).
     */
    public String getFileMask() {
        return fileMask;
    }

    /**
     * Gets the ADDE realtime status of the current local ADDE entry.
     * 
     * @return Whether or not the current dataset is {@literal "realtime"}.
     * Corresponds to the {@literal "RT"} section of a RESOLV.SRV entry.
     */
    public boolean getRealtime() {
        return realtime;
    }

    /**
     * Gets the starting number of the current local ADDE dataset.
     * 
     * @return Corresponds to the {@literal "R1"} section of a RESOLV.SRV entry.
     */
    public String getStart() {
        return start;
    }

    /**
     * Gets the ending number of the current local ADDE dataset.
     * 
     * @return Corresponds to the {@literal "R2"} section of a RESOLV.SRV entry.
     */
    public String getEnd() {
        return end;
    }

    /**
     * Tests the current local ADDE dataset for validity.
     * 
     * @return {@code true} iff {@link #group} and {@link #name} are not empty.
     */
    public boolean isValid() {
//        return !((group.isEmpty()) || (descriptor.isEmpty()) || (name.isEmpty()));
        return !(group.isEmpty() || name.isEmpty());
    }

    /**
     * Gets the local ADDE dataset's realtime status as a value suitable for
     * RESOLV.SRV (one of {@literal "Y"} or {@literal "N"}).
     * 
     * @return RESOLV.SRV-friendly representation of the current realtime status.
     */
    public String getRealtimeAsString() {
        return realtime ? "Y" : "N";
    }

    /**
     * @see LocalAddeEntry#generateHashCode(String, String, String, String, boolean, AddeFormat)
     */
    @Override public int hashCode() {
        return generateHashCode(name, group, fileMask, entryAlias, isTemporary, format);
    }

    /**
     * Checks a given object for equality with the current {@code LocalAddeEntry}
     * instance.
     * 
     * @param obj Object to check. {@code null} values allowed.
     * 
     * @return {@code true} if {@code obj} is {@literal "equal"} to the current
     * {@code LocalAddeEntry} instance.
     */
    @Override public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof LocalAddeEntry)) {
            return false;
        }
        LocalAddeEntry other = (LocalAddeEntry) obj;
        if (fileMask == null) {
            if (other.fileMask != null) {
                return false;
            }
        } else if (!fileMask.equals(other.fileMask)) {
            return false;
        }
        if (format == null) {
            if (other.format != null) {
                return false;
            }
        } else if (!format.toString().equals(other.format.toString())) {
            return false;
        }
        if (group == null) {
            if (other.group != null) {
                return false;
            }
        } else if (!group.equals(other.group)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (entryAlias == null) {
            if (other.entryAlias != null) {
                return false;
            }
        } else if (!entryAlias.equals(other.entryAlias)) {
            return false;
        }
        if (isTemporary != other.isTemporary) {
            return false;
        }
        return true;
    }

    @Override public String asStringId() {
        if (asStringId == null) {
            asStringId = "localhost!"+group+'!'+EntryType.IMAGE.name()+'!'+name;
        }
        return asStringId;
    }

    @Override public String toString() {
        return String.format(
            "[LocalAddeEntry@%x: name=%s, group=%s, fileMask=\"%s\", descriptor=%s, serverName=%s, format=%s, description=%s, type=%s, status=%s, temporary=%s, alias=%s]", 
            hashCode(), name, group, fileMask, descriptor, format.getServerName().name(), format.name(), format.getTooltip(), format.getType(), entryStatus.name(), isTemporary, entryAlias);
        
    }

    public static int generateHashCode(final LocalAddeEntry entry) {
        return generateHashCode(entry.getName(), entry.getGroup(), entry.getMask(), entry.getEntryAlias(), entry.isEntryTemporary(), entry.getFormat());
    }

    public static int generateHashCode(String name, String group, String fileMask, String entryAlias, boolean isTemporary, AddeFormat format) {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((fileMask == null) ? 0 : fileMask.hashCode());
        result = prime * result + ((format == null) ? 0 : format.toString().hashCode());
        result = prime * result + ((group == null) ? 0 : group.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((entryAlias == null) ? 0 : entryAlias.hashCode());
        result = prime * result + (isTemporary ? 1231 : 1237);
        return result;
    }

    /**
     * A builder of (mostly) immutable {@link LocalAddeEntry} instances.
     * 
     * <p>Usage example: <pre>    {@code
     *     LocalAddeEntry entry = new LocalAddeEntry
     *         .Builder(group, name, format, mask)
     *         .realtime("Y")
     *         .range(start, end)
     *         .type(EntryType.POINT)
     *         .build();}</pre>
     * 
     * Only the values required by the Builder constructor are required.
     */
    public static class Builder {
        // required
        /** Corresponds to RESOLV.SRV's {@literal "N1"} section. */
        private final String group;

        /** Corresponds to RESOLV.SRV's {@literal "C"} section. */
        private final String name;

        /** Corresponds to RESOLV.SRV's {@literal "MCV"} section. */
        private final AddeFormat format;

        /** Corresponds to RESOLV.SRV's {@literal "MASK"} section. */
        private final String mask;

        // generated
        private String descriptor;

        // optional
        /**
         * Corresponds to RESOLV.SRV's {@literal "RT"} section.
         * Defaults to {@code false}.
         */
        private boolean realtime = false;

        /**
         * Corresponds to RESOLV.SRV's {@literal "R1"} section.
         * Defaults to {@literal "1"}.
         */
        private String start = "1";

        /**
         * Corresponds to RESOLV.SRV's {@literal "R2"} section.
         * Defaults to {@literal "999999"}.
         */
        private String end = "999999";

        /**
         * Defaults to {@link edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryStatus#INVALID}.
         */
        private EntryStatus status = EntryStatus.INVALID;

        /**
         * Corresponds to RESOLV.SRV's {@literal "TYPE"} section.
         * Defaults to {@link EntryType#IMAGE}.
         */
        private EntryType type = EntryType.IMAGE;

        /**
         * Corresponds to RESOLV.SRV's {@literal "K"} section.
         * Defaults to {@literal "NOT_SET"}.
         */
        private String kind = "NOT_SET";

        /**
         * Defaults to {@link ServerName#INVALID}.
         */
        private ServerName safeKind = ServerName.INVALID;

        /** */
        private boolean temporary = false;

        /** */
        private String alias = "";

        public Builder(final Map<String, String> map) {
            if (!map.containsKey("C") || !map.containsKey("N1") || !map.containsKey("MASK") || !map.containsKey("MCV")) {
                throw new IllegalArgumentException("Cannot build a LocalAddeEntry without the following keys: C, N1, MASK, and MCV.");
            }

            this.name = map.get("C");
            this.group = map.get("N1");
            this.mask = map.get("MASK");
            this.format = EntryTransforms.strToAddeFormat(map.get("MCV"));

//            descriptor(map.get("N2"));
            type(EntryTransforms.strToEntryType(map.get("TYPE")));
            kind(map.get("K").toUpperCase());
            realtime(map.get("RT"));
            start(map.get("R1"));
            end(map.get("R2"));
            
            if (map.containsKey("TEMPORARY")) {
                temporary(map.get("TEMPORARY"));
            }
        }

        /**
         * Creates a new {@code LocalAddeEntry} {@literal "builder"} with the 
         * required fields for a {@code LocalAddeEntry} object.
         * 
         * @param name 
         * @param group 
         * @param mask 
         * @param format
         */
        public Builder(final String name, final String group, final String mask, final AddeFormat format) {
            this.name = name;
            this.group = group;
            this.mask = mask;
            this.format = format;
        }

        /**
         * This method is currently a no-op.
         *
         * @param descriptor
         *
         * @return {@code LocalAddeEntry.Builder} with ADDE descriptor.
         */
        public Builder descriptor(final String descriptor) {
//            if (descriptor != null) {
//                this.descriptor = descriptor;
//            }
            return this;
        }

        /**
         *
         *
         * @param realtimeAsStr
         *
         * @return {@code LocalAddeEntry.Builder} with ADDE realtime flag.
         */
        // looks like mcidasx understands ("Y"/"N"/"A")
        // should probably ignore case and accept "YES"/"NO"/"ARCHIVE"
        // in addition to the normal boolean conversion from String
        public Builder realtime(final String realtimeAsStr) {
            if (realtimeAsStr == null) {
                return this;
            }

            if ("Y".equalsIgnoreCase(realtimeAsStr) || "YES".equalsIgnoreCase(realtimeAsStr)) {
                this.realtime = true;
            } else {
                this.realtime = Boolean.valueOf(realtimeAsStr);
            }
            return this;
        }

        /**
         *
         *
         * @param realtime
         *
         * @return {@code LocalAddeEntry.Builder} with ADDE realtime flag.
         */
        public Builder realtime(final boolean realtime) {
            this.realtime = realtime;
            return this;
        }

        /**
         *
         *
         * @param type
         *
         * @return {@code LocalAddeEntry.Builder} with ADDE data type.
         */
        // my assumption is that if "format" is known, you can infer "type"
        public Builder type(final EntryType type) {
            if (type != null) {
                this.type = type;
            }
            return this;
        }

        /**
         *
         *
         * @param kind
         *
         * @return {@code LocalAddeEntry.Builder} with ADDE kind.
         */
        // my assumption is that if "format" is known, you can infer "kind"
        public Builder kind(final String kind) {
            if (kind == null) {
                return this;
            }

            this.kind = kind;
            try {
                this.safeKind = ServerName.valueOf(kind);
            } catch (IllegalArgumentException e) { 
                this.safeKind = ServerName.INVALID;
            }
            return this;
        }

        /**
         *
         *
         * @param start
         *
         * @return {@code LocalAddeEntry.Builder} with ADDE dataset {@literal "start"}.
         */
        public Builder start(final String start) {
            if (start != null) {
                this.start = start;
            }
            return this;
        }

        /**
         *
         *
         * @param end
         *
         * @return {@code LocalAddeEntry.Builder} with ADDE dataset {@literal "end"}.
         */
        public Builder end(final String end) {
            if (end != null) {
                this.end = end;
            }
            return this;
        }

        /**
         *
         *
         * @param start
         * @param end
         *
         * @return {@code LocalAddeEntry.Builder} with ADDE dataset {@literal "start" and "end"} values.
         */
        public Builder range(final String start, final String end) {
            if (start != null && end != null) {
                this.start = start;
                this.end = end;
            }
            return this;
        }

        /**
         *
         *
         * @param status
         *
         * @return {@code LocalAddeEntry.Builder} with {@link AddeEntry.EntryStatus}.
         */
        public Builder status(final String status) {
            if (status != null && status.length() > 0) {
                this.status = EntryTransforms.strToEntryStatus(status);
            }
            return this;
        }

        /**
         * 
         * 
         * @param status
         * 
         * @return {@code LocalAddeEntry.Builder} with {@link AddeEntry.EntryStatus}.
         */
        public Builder status(final EntryStatus status) {
            if (status != null) {
                this.status = status;
            }
            return this;
        }

        /**
         * 
         * 
         * @param temporary
         * 
         * @return {@code LocalAddeEntry.Builder} with the specified temporary status.
         */
        public Builder temporary(final boolean temporary) {
            this.temporary = temporary;
            return this;
        }

        public Builder temporary(final String temporary) {
            this.temporary = Boolean.valueOf(temporary);
            return this;
        }

        /**
         * 
         * 
         * @param alias 
         * 
         * @return {@code LocalAddeEntry.Builder} with the specified alias.
         */
        public Builder alias(final String alias) {
            this.alias = alias;
            return this;
        }

        /**
         * 
         * 
         * @return New {@code LocalAddeEntry} instance.
         */
        public LocalAddeEntry build() {
            // apparently need to hack up the descriptor for certain formats
            switch (format) {
                case MSG_HRIT_FD: this.descriptor = "FD"; break;
                case MSG_HRIT_HRV: this.descriptor = "HRV"; break;
                case LRIT_GOES9: this.descriptor = "GOES9"; break;
                case LRIT_GOES10: this.descriptor = "GOES10"; break;
                case LRIT_GOES11: this.descriptor = "GOES11"; break;
                case LRIT_GOES12: this.descriptor = "GOES12"; break;
                case LRIT_MET5: this.descriptor = "MET5"; break;
                case LRIT_MET7: this.descriptor = "MET7"; break;
                case LRIT_MTSAT1R: this.descriptor = "MTSAT1R"; break;
                default:
                    this.descriptor = Integer.toHexString(generateHashCode(name, group, mask, alias, temporary, format));
                    break;
            }
            return new LocalAddeEntry(this);
        }
    }
}
