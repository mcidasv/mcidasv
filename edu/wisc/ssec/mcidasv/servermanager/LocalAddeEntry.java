/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
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
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryType;

/**
 * 
 * 
 *
 */
public class LocalAddeEntry implements AddeEntry {

    static final Logger logger = LoggerFactory.getLogger(LocalAddeEntry.class);

    /** Represents a {@literal "bad"} local ADDE entry. */
    // seriously, don't use null unless you REALLY need it.
    public static final LocalAddeEntry INVALID_ENTRY = new Builder("INVALID", "INVALID", "/dev/null", AddeFormat.INVALID).build();

    public static final List<LocalAddeEntry> INVALID_ENTRIES = Collections.singletonList(INVALID_ENTRY);

    /** */
    private static final String CYGWIN_PREFIX = "/cygdrive/";

    /** */
    private static final int CYGWIN_PREFIX_LEN = CYGWIN_PREFIX.length();

    /** */
    private EntryStatus entryStatus = EntryStatus.INVALID;

    /** N1 */
    private final String group;

    /** N2 */
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
    
    
    private String asStringId;

    public enum ServerName {
        AREA, AMSR, AMRR, GINI, FSDX, OMTP, LV1B, MODS, MODX, MOD4, MOD8, 
        MODR, MSGT, MTST, SMIN, TMIN, INVALID;
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
        INVALID(ServerName.INVALID, "", "", EntryType.INVALID),
        MCIDAS_AREA(ServerName.AREA, "McIDAS AREA"),
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
        TRMM(ServerName.TMIN, "TRMM", "Terrascan netCDF (TMIN)");

        /** Name of the server (should be four characters). */
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
         * @param type Only use {@link EntryType#IMAGE} for the time being?
         */
        private AddeFormat(final ServerName servName, final String friendlyName, final String tooltip, final EntryType type) {
            this.servName = servName;
            this.friendlyName = friendlyName;
            this.tooltip = tooltip;
            this.type = type;
            this.fileFilter = (servName != ServerName.MSGT) ? "*" : "*PRO*";
        }

        /**
         * Builds an {@literal "ADDE Format"} <b>without</b> a tooltip.
         */
        private AddeFormat(final ServerName servName, final String friendlyName) {
            this(servName, friendlyName, "", EntryType.IMAGE);
        }

        /**
         * Builds an {@literal "ADDE Format"} <b>with</b> a tooltip.
         */
        private AddeFormat(final ServerName servName, final String friendlyName, final String tooltip) {
            this(servName, friendlyName, tooltip, EntryType.IMAGE);
        }

        public ServerName getServerName() { return servName; }
        public String getTooltip() { return tooltip; }
        public EntryType getType() { return type; }
        public String getFileFilter() { return fileFilter; }
        @Override public String toString() { return friendlyName; }
    }

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
        return "";
    }

    // TODO(jon): fix this noop
    @Override public void setEntryAlias(final String newAlias) {
        if (newAlias == null) {
            throw new NullPointerException("Null aliases are not allowable.");
        }
    }

    @Override public void setEntryStatus(EntryStatus newStatus) {
        entryStatus = newStatus;
    }

    @Override public String getGroup() {
        return group;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public AddeFormat getFormat() {
        return format;
    }

    public String getMask() {
        return fileMask;
    }

    public String getFileMask() {
        return fileMask;
    }

    @Override public String getName() {
        return name;
    }

    public boolean getRealtime() {
        return realtime;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }

    public boolean isValid() {
        if ((group.length() == 0) || (descriptor.length() == 0) || (name.length() == 0)) {
            return false;
        }
        return true;
    }

    public String getRealtimeAsString() {
        if (realtime) {
            return "Y";
        }
        return "N";
    }

    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((fileMask == null) ? 0 : fileMask.hashCode());
        result = prime * result + ((format == null) ? 0 : format.hashCode());
        result = prime * result + ((group == null) ? 0 : group.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

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
        } else if (!format.equals(other.format)) {
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
            "[LocalAddeEntry@%x: name=%s, group=%s, fileMask=\"%s\", descriptor=%s, serverName=%s, format=%s, description=%s, type=%s, status=%s]", 
            hashCode(), name, group, fileMask, descriptor, format.getServerName().name(), format.name(), format.getTooltip(), format.getType(), entryStatus.name());
        
    }

    public static class Builder {
        private static final Random random = new Random();

        // required
        private final String group;
        private final String name;
        private final AddeFormat format;
        private final String mask;

        // optional
        private String descriptor = "ENTRY"+random.nextInt(999999);
        private boolean realtime = false;
        private String start = "1";
        private String end = "999999";
        private EntryStatus status = EntryStatus.INVALID;

        private EntryType type = EntryType.IMAGE;
        private String kind = "NOT_SET";
        private ServerName safeKind = ServerName.INVALID;

        public Builder(final Map<String, String> map) {
            if (!map.containsKey("C") || !map.containsKey("N1") || !map.containsKey("MASK") || !map.containsKey("MCV")) {
                throw new IllegalArgumentException("");
            }

            this.name = map.get("C");
            this.group = map.get("N1");
            this.mask = map.get("MASK");
            this.format = EntryTransforms.strToAddeFormat(map.get("MCV"));

            descriptor(map.get("N2"));
            type(EntryTransforms.strToEntryType(map.get("TYPE")));
            kind(map.get("K").toUpperCase());
            realtime(map.get("RT"));
            start(map.get("R1"));
            end(map.get("R2"));
        }

        
        public Builder(final String name, final String group, final String mask, final AddeFormat format) {
            this.name = name;
            this.group = group;
            this.mask = mask;
            this.format = format;
        }

        public Builder descriptor(final String descriptor) {
            if (descriptor != null) {
                this.descriptor = descriptor;
            }
            return this;
        }

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

        public Builder realtime(final boolean realtime) {
            this.realtime = realtime;
            return this;
        }

        // my assumption is that if "format" is known, you can infer "type"
        public Builder type(final EntryType type) {
            if (type != null) {
                this.type = type;
            }
            return this;
        }

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

        public Builder start(final String start) {
            if (start != null) {
                this.start = start;
            }
            return this;
        }

        public Builder end(final String end) {
            if (end != null) {
                this.end = end;
            }
            return this;
        }

        public Builder range(final String start, final String end) {
            if (start != null && end != null) {
                this.start = start;
                this.end = end;
            }
            return this;
        }

        public Builder status(final String status) {
            if (status != null && status.length() > 0) {
                this.status = EntryTransforms.strToEntryStatus(status);
            }
            return this;
        }

        public Builder status(final EntryStatus status) {
            if (status != null) {
                this.status = status;
            }
            return this;
        }

        public LocalAddeEntry build() {
//            if (format.getType() != type || format.getServerName() != safeKind || safeKind == ServerName.INVALID)
//                System.err.println("oddity: name="+name+" mask="+mask+" group="+group+" descriptor="+descriptor+" realtime="+realtime+" format="+format+" type="+type+" kind="+kind+" safeKind="+safeKind);

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
            }
            return new LocalAddeEntry(this);
        }
    }

}
