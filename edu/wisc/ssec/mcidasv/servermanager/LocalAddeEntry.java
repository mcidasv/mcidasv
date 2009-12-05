package edu.wisc.ssec.mcidasv.servermanager;

public class LocalAddeEntry implements AddeEntry {

    private static final String CYGWIN_PREFIX = "/cygdrive/";
    private static final int CYGWIN_PREFIX_LEN = CYGWIN_PREFIX.length();

    private String group = "";

    private String descriptor = "ERROR";

    private String addeRt = "N";

    private String type;

    private String format;

    private String description;

    private String start = "1";

    private String end = "99999";

    private String fileMask = "";

    private String name = "";

    private enum ServerName {
        AREA, AMSR, AMRR, GINI, FSDX, OMTP, LV1B, MODS, MODX, MOD4, MOD8, 
        MODR, MSGT, MTST, SMIN, TMIN
    }

    public enum AddeFormats {
        MCIDAS_AREA(ServerName.AREA, "McIDAS AREA", "McIDAS-AREA", EntryType.IMAGE),
        AMSRE_L1B(ServerName.AMSR, "AMSR-E L1b", "AMSR-E Level 1b", EntryType.IMAGE),
        AMSRE_RAIN(ServerName.AMRR, "AMSR-E Rain Product", "AMSR-E Rain Product", EntryType.IMAGE),
        GINI(ServerName.GINI, "GINI", "GINI", EntryType.IMAGE),
        LRIT_GOES9(ServerName.FSDX, "LRIT GOES9", "EUMETCast LRIT GOES-9", EntryType.IMAGE),
        LRIT_GOES10(ServerName.FSDX, "LRIT GOES10", "EUMETCast LRIT GOES-10", EntryType.IMAGE),
        LRIT_GOES11(ServerName.FSDX, "LRIT GOES11", "EUMETCast LRIT GOES-11", EntryType.IMAGE),
        LRIT_GOES12(ServerName.FSDX, "LRIT GOES12", "EUMETCast LRIT GOES-12", EntryType.IMAGE),
        LRIT_MET5(ServerName.FSDX, "LRIT MET5", "EUMETCast LRIT MET-5", EntryType.IMAGE),
        LRIT_MET7(ServerName.FSDX, "LRIT MET7", "EUMETCast LRIT MET-7", EntryType.IMAGE),
        LRIT_MTSAT1R(ServerName.FSDX, "LRIT MTSAT1R", "EUMETCast LRIT MTSAT-1R", EntryType.IMAGE),
        METEOSAT_OPENMTP(ServerName.OMTP, "Meteosat OpenMTP", "Meteosat OpenMTP", EntryType.IMAGE),
        METOP_AVHRR(ServerName.LV1B, "Metop AVHRR L1b", "Metop AVHRR Level 1b", EntryType.IMAGE),
        MODIS_L1B_MOD02(ServerName.MODS, "MODIS L1b MOD02", "MODIS_Level 1b", EntryType.IMAGE),
        MODIS_L2_MOD06(ServerName.MODX, "MODIS L2 MOD06", "MODIS_Level 2 (Cloud top properties)", EntryType.IMAGE),
        MODIS_L2_MOD07(ServerName.MODX, "MODIS L2 MOD07", "MODIS_Level 2 (Atmospheric profile)", EntryType.IMAGE),
        MODIS_L2_MOD35(ServerName.MODX, "MODIS L2 MOD35", "MODIS_Level 2 (Cloud mask)", EntryType.IMAGE),
        MODIS_L2_MOD04(ServerName.MOD4, "MODIS L2 MOD04", "MODIS_Level 2 (Aerosol)", EntryType.IMAGE),
        MODIS_L2_MOD28(ServerName.MOD8, "MODIS L2 MOD28", "MODIS_Level 2 (Sea surface temperature)", EntryType.IMAGE),
        MODIS_L2_MODR(ServerName.MODR, "MODIS_L2 MODR", "MODIS_Level 2 (Corrected reflectance)", EntryType.IMAGE),
        MSG_HRIT_FD(ServerName.MSGT, "MSG HRIT FD", "MSG HRIT (Full Disk)", EntryType.IMAGE),
        MSG_HRIT_HRV(ServerName.MSGT, "MSG HRIT HRV", "MSG HRIT (High Resolution Visible)", EntryType.IMAGE),
        MTSAT_HRIT(ServerName.MTST, "MTSAT HRIT", "MTSAT HRIT", EntryType.IMAGE),
        NOAA_AVHRR_L1B(ServerName.LV1B, "NOAA AVHRR L1b", "NOAA AVHRR Level 1b", EntryType.IMAGE),
        SSMI(ServerName.SMIN, "SSMI", "Terrascan netCDF", EntryType.IMAGE),
        TRMM(ServerName.TMIN, "TRMM", "Terrascan netCDF", EntryType.IMAGE);

        /** Name of the server (should be four characters). */
        private final ServerName servName;

        /** <i>Unique</i> short name. */
        private final String shortName;

        /** Long description. */
        private final String description;

        /** Data type. */
        private final EntryType type;

        AddeFormats(final ServerName servName, final String shortName, final String desc, final EntryType type) {
            this.servName = servName;
            this.shortName = shortName;
            this.description = desc;
            this.type = type;
        }

        public ServerName getServerName() { return servName; }
        public String getShortName() { return shortName; }
        public String getDescription() { return description; }
        public EntryType getType() { return type; }
    }

//    public LocalAddeEntry() {
//        group = "";
//        descriptor = "ERROR";
//        addeRt = "N";
//        start = "1";
//        end = "99999";
//        fileMask = "";
//        name = "";
//    }

    public LocalAddeEntry(final String group, final String name, final String description, final String mask) {
//        this();
        this.group = group;
        this.name = name;
        this.description = description;
        this.fileMask = mask;
    }

    public LocalAddeEntry(final String resolvLine) {
//        this();
    }

    @Override public AddeAccount getAccount() {
        return null;
    }

    @Override public String getAddress() {
        return "localhost";
    }

    @Override public EntrySource getEntrySource() {
        return null;
    }

    @Override public EntryStatus getEntryStatus() {
        return null;
    }

    @Override public String getEntryText() {
        return null;
    }

    @Override public EntryType getEntryType() {
        return null;
    }

    @Override public EntryValidity getEntryValidity() {
        return null;
    }

    // TODO(jon): fix this noop
    @Override public String getEntryAlias() {
        return "";
    }

    // TODO(jon): fix this noop
    @Override public void setEntryAlias(final String newAlias) {
        if (newAlias == null)
            throw new NullPointerException("Null alises are not allowable.");
    }

    @Override public void setEntryStatus(EntryStatus newStatus) {
        
    }

    @Override public String getGroup() {
        return group;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String getFormat() {
        return format;
    }

    public String getDescription() {
        return description;
    }

    public String getMask() {
        return fileMask;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public boolean isValid() {
        if ((group.length() == 0) || (descriptor.length() == 0) || (name.length() == 0))
            return false;
        return true;
    }

    @Override public String toString() {
        return String.format("[LocalAddeEntry@%x: group=%s, descriptor=%s, format=%s, description=%s, mask=%s, type=%s, name=%s]", 
            hashCode(), group, descriptor, format, description, fileMask, type, name);
        
    }
}
