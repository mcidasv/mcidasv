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

/**
 * 
 * 
 *
 */
public class LocalAddeEntry implements AddeEntry {

    /** */
    public static final LocalAddeEntry INVALID_ENTRY = new Builder().invalidate().build();

    /** */
    private static final String CYGWIN_PREFIX = "/cygdrive/";

    /** */
    private static final int CYGWIN_PREFIX_LEN = CYGWIN_PREFIX.length();

    private EntryStatus entryStatus = EntryStatus.INVALID;
    
    /** N1 */
    private String group = "";

    /** N2 */
    private String descriptor = "ERROR";

    /** RT */
    private boolean realtime = false;

    /** MCV */
    private AddeFormat format;

    /** R1 */
    private String start = "1";

    /** R2 */
    private String end = "999999";

    /** MASK */
    private String fileMask = "";

    /** C */
    private String name = "";

    public enum ServerName {
        AREA, AMSR, AMRR, GINI, FSDX, OMTP, LV1B, MODS, MODX, MOD4, MOD8, 
        MODR, MSGT, MTST, SMIN, TMIN, INVALID;
    }

    // TODO(jon): can i remove the shortName field and just use the enumeration name instead? 
    public enum AddeFormat {
        INVALID(ServerName.INVALID, "", EntryType.INVALID),
        MCIDAS_AREA(ServerName.AREA, "McIDAS AREA"),
        AMSRE_L1B(ServerName.AMSR, "AMSR-E Level 1b"),
        AMSRE_RAIN_PRODUCT(ServerName.AMRR, "AMSR-E Rain Product"),
        GINI(ServerName.GINI, "GINI"),
        LRIT_GOES9(ServerName.FSDX, "EUMETCast LRIT GOES-9"),
        LRIT_GOES10(ServerName.FSDX, "EUMETCast LRIT GOES-10"),
        LRIT_GOES11(ServerName.FSDX, "EUMETCast LRIT GOES-11"),
        LRIT_GOES12(ServerName.FSDX, "EUMETCast LRIT GOES-12"),
        LRIT_MET5(ServerName.FSDX, "EUMETCast LRIT MET-5"),
        LRIT_MET7(ServerName.FSDX, "EUMETCast LRIT MET-7"),
        LRIT_MTSAT1R(ServerName.FSDX, "EUMETCast LRIT MTSAT-1R"),
        METEOSAT_OPENMTP(ServerName.OMTP, "Meteosat OpenMTP"),
        METOP_AVHRR_L1B(ServerName.LV1B, "Metop AVHRR Level 1b"),
        MODIS_L1B_MOD02(ServerName.MODS, "MODIS Level 1b"),
        MODIS_L2_MOD06(ServerName.MODX, "MODIS Level 2 (Cloud Top Properties)"),
        MODIS_L2_MOD07(ServerName.MODX, "MODIS Level 2 (Atmospheric Profile)"),
        MODIS_L2_MOD35(ServerName.MODX, "MODIS Level 2 (Cloud Mask)"),
        MODIS_L2_MOD04(ServerName.MOD4, "MODIS Level 2 (Aerosol)"),
        MODIS_L2_MOD28(ServerName.MOD8, "MODIS Level 2 (Sea Surface Temperature)"),
        MODIS_L2_MODR(ServerName.MODR, "MODIS Level 2 (Corrected Reflectance)"),
        MSG_HRIT_FD(ServerName.MSGT, "MSG HRIT (Full Disk)"),
        MSG_HRIT_HRV(ServerName.MSGT,"MSG HRIT (High Resolution Visible)"),
        MTSAT_HRIT(ServerName.MTST, "MTSAT HRIT"),
        NOAA_AVHRR_L1B(ServerName.LV1B, "NOAA AVHRR Level 1b"),
        SSMI(ServerName.SMIN, "Terrascan netCDF"),
        TRMM(ServerName.TMIN, "Terrascan netCDF");

        /** Name of the server (should be four characters). */
        private final ServerName servName;

        /** Long description. */
        private final String tooltip;

        /** Data type. Corresponds to {@code TYPE} in {@literal "RESOLV.SRV"}. */
        private final EntryType type;

        /** */
        private final String fileFilter;

        AddeFormat(final ServerName servName, final String tooltip) {
            this(servName, tooltip, EntryType.IMAGE);
        }

        AddeFormat(final ServerName servName, final String tooltip, final EntryType type) {
            this.servName = servName;
            this.tooltip = tooltip;
            this.type = type;
            this.fileFilter = (servName != ServerName.MSGT) ? "*" : "*PRO*";
        }

        public ServerName getServerName() { return servName; }
        public String getTooltip() { return tooltip; }
        public EntryType getType() { return type; }
        public String getFileFilter() { return fileFilter; }
        @Override public String toString() { return tooltip; }
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
        return null;
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
        if (newAlias == null)
            throw new NullPointerException("Null aliases are not allowable.");
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
    
    public String getName() {
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
        if ((group.length() == 0) || (descriptor.length() == 0) || (name.length() == 0))
            return false;
        return true;
    }

    public String getRealtimeAsString() {
        if (realtime)
            return "Y";
        return "N";
    }

    // autogenerated and ugly!
    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((descriptor == null) ? 0 : descriptor.hashCode());
        result = prime * result + ((end == null) ? 0 : end.hashCode());
        result = prime * result
            + ((entryStatus == null) ? 0 : entryStatus.hashCode());
        result = prime * result
            + ((fileMask == null) ? 0 : fileMask.hashCode());
        result = prime * result + ((format == null) ? 0 : format.hashCode());
        result = prime * result + ((group == null) ? 0 : group.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (realtime ? 1231 : 1237);
        result = prime * result + ((start == null) ? 0 : start.hashCode());
        return result;
    }

    // autogenerated and ugly!
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
        if (descriptor == null) {
            if (other.descriptor != null) {
                return false;
            }
        } else if (!descriptor.equals(other.descriptor)) {
            return false;
        }
        if (end == null) {
            if (other.end != null) {
                return false;
            }
        } else if (!end.equals(other.end)) {
            return false;
        }
        if (entryStatus == null) {
            if (other.entryStatus != null) {
                return false;
            }
        } else if (!entryStatus.equals(other.entryStatus)) {
            return false;
        }
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
        if (realtime != other.realtime) {
            return false;
        }
        if (start == null) {
            if (other.start != null) {
                return false;
            }
        } else if (!start.equals(other.start)) {
            return false;
        }
        return true;
    }

    @Override public String toString() {
        return String.format("[LocalAddeEntry@%x: group=%s, descriptor=%s, serverName=%s, format=%s, description=%s, mask=%s, type=%s, name=%s]", 
            hashCode(), group, descriptor, format.getServerName().name(), format.name(), format.getTooltip(), fileMask, format.getType(), name);
        
    }

    public static class Builder {

        // TODO(jon): determine which of these are required vs optional
        // TODO(jon): determine default values for optional params.
        private String group = "";
        private String name = "";
        private AddeFormat format = AddeFormat.INVALID;
        private String mask = "";

        private String descriptor = "ENTRY"+Math.random()%99999;
        private boolean realtime = false;
        private String start = "1";
        private String end = "999999";

        private EntryType type = EntryType.IMAGE;
        private String kind = "NOT_SET";
        private ServerName safeKind = ServerName.INVALID;

        public Builder() {
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder mask(final String mask) {
            this.mask = mask;
            return this;
        }

        public Builder group(final String group) {
            this.group = group;
            return this;
        }

        public Builder descriptor(final String descriptor) {
            this.descriptor = descriptor;
            return this;
        }

        // looks like mcidasx understands ("Y"/"N"/"A")
        // should probably ignore case and accept "YES"/"NO"/"ARCHIVE"
        // in addition to the normal boolean conversion from String
        public Builder realtime(final String realtimeAsStr) {
            if ("Y".equalsIgnoreCase(realtimeAsStr) || "YES".equalsIgnoreCase(realtimeAsStr))
                this.realtime = true;
            else
                this.realtime = Boolean.valueOf(realtimeAsStr);
             return this;
        }

        public Builder realtime(final boolean realtime) {
            this.realtime = realtime;
            return this;
        }

        public Builder format(final AddeFormat format) {
            this.format = format;
            return this;
        }

        // my assumption is that if "format" is known, you can infer "type"
        public Builder type(final EntryType type) {
            this.type = type;
            return this;
        }

        // my assumption is that if "format" is known, you can infer "kind"
        public Builder kind(final String kind) {
            this.kind = kind;
            try {
                this.safeKind = ServerName.valueOf(kind);
            } catch (IllegalArgumentException e) { 
                this.safeKind = ServerName.INVALID;
            }
            return this;
        }

        public Builder start(final String start) {
            this.start = start;
            return this;
        }

        public Builder end(final String end) {
            this.end = end;
            return this;
        }

        public Builder range(final String start, final String end) {
            this.start = start;
            this.end = end;
            return this;
        }

        public Builder invalidate() {
            this.name = "INVALID";
            this.mask = "INVALID";
            this.group = "INVALID";
            this.descriptor = "INVALID";
            this.realtime = false;
            this.format = AddeFormat.INVALID;
            this.kind = "INVALID";
            this.safeKind = ServerName.INVALID;
            this.type = EntryType.INVALID;
            this.start = "INVALID";
            this.end = "INVALID";
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
