package edu.wisc.ssec.mcidasv.servermanager;

public class LocalAddeEntry implements AddeEntry {

    private String addeGroup;
    private String addeDescriptor;
    private String addeRt;
    private String addeType;
    private String addeFormat;
    private String addeDescription;
    private String addeStart;
    private String addeEnd;
    private String addeFileMask;
    private String addeName;
    
    // Special cases for MSG HRIT
//    private static String MSG_HRIT_FD = "MSG HRIT FD";
//    private static String MSG_HRIT_HRV = "MSG HRIT HRV";
//
//    // Special cases for LRIT
//    private static String LRIT_GOES9 = "LRIT GOES9";
//    private static String LRIT_GOES10 = "LRIT GOES10";
//    private static String LRIT_GOES11 = "LRIT GOES11";
//    private static String LRIT_GOES12 = "LRIT GOES12";
//    private static String LRIT_MET5 = "LRIT MET5";
//    private static String LRIT_MET7 = "LRIT MET7";
//    private static String LRIT_MTSAT1R = "LRIT MTSAT1R";

    private enum ServerName {
        AREA, AMSR, AMRR, GINI, FSDX, OMTP, LV1B, MODS, MODX, MOD4, MOD8, MODR, MSGT, MTST, SMIN, TMIN
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

        private final ServerName servName;
        private final String shortName;
        private final String description;
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

    /**
     * The full list of possible ADDE servers
     * 
     * The fields are:
     *  4-character server name
     *  Short name (MUST be unique)
     *  Long description
     *  Data type (ie. IMAGE, RADAR, GRID, POINT, etc)
     */
    /* Beta2 (see below for Windows-only changes) */
    private String[][] addeFormats = {
            { "AREA", "McIDAS AREA", "McIDAS AREA", EntryType.IMAGE },
            { "AMSR", "AMSR-E L1b", "AMSR-E Level 1b", EntryType.IMAGE },
            { "AMRR", "AMSR-E Rain Product", "AMSR-E Rain Product", EntryType.IMAGE },
            { "GINI", "GINI", "GINI", EntryType.IMAGE },
            { "FSDX", LRIT_GOES9, "EUMETCast LRIT GOES-9", EntryType.IMAGE },
            { "FSDX", LRIT_GOES10, "EUMETCast LRIT GOES-10", EntryType.IMAGE },
            { "FSDX", LRIT_GOES11, "EUMETCast LRIT GOES-11", EntryType.IMAGE },
            { "FSDX", LRIT_GOES12, "EUMETCast LRIT GOES-12", EntryType.IMAGE },
            { "FSDX", LRIT_MET5, "EUMETCast LRIT MET-5", EntryType.IMAGE },
            { "FSDX", LRIT_MET7, "EUMETCast LRIT MET-7", "IMAGE" },
            { "FSDX", LRIT_MTSAT1R, "EUMETCast LRIT MTSAT-1R", "IMAGE" },
            { "OMTP", "Meteosat OpenMTP", "Meteosat OpenMTP", "IMAGE" },
            { "LV1B", "Metop AVHRR L1b", "Metop AVHRR Level 1b", "IMAGE" },
            { "MODS", "MODIS L1b MOD02", "MODIS Level 1b", "IMAGE" },
            { "MODX", "MODIS L2 MOD06", "MODIS Level 2 (Cloud top properties)", "IMAGE" },
            { "MODX", "MODIS L2 MOD07", "MODIS Level 2 (Atmospheric profile)", "IMAGE" },
            { "MODX", "MODIS L2 MOD35", "MODIS Level 2 (Cloud mask)", "IMAGE" },
            { "MOD4", "MODIS L2 MOD04", "MODIS Level 2 (Aerosol)", "IMAGE" },
            { "MOD8", "MODIS L2 MOD28", "MODIS Level 2 (Sea surface temperature)", "IMAGE" },
            { "MODR", "MODIS L2 MODR", "MODIS Level 2 (Corrected reflectance)", "IMAGE" },
            { "MSGT", MSG_HRIT_FD, "MSG HRIT (Full Disk)", "IMAGE" },
            { "MSGT", MSG_HRIT_HRV, "MSG HRIT (High Resolution Visible)", "IMAGE" },
            { "MTST", "MTSAT HRIT", "MTSAT HRIT", "IMAGE" },
            { "LV1B", "NOAA AVHRR L1b", "NOAA AVHRR Level 1b", "IMAGE" },
            { "SMIN", "SSMI", "Terrascan netCDF", "IMAGE" },
            { "TMIN", "TRMM", "Terrascan netCDF", "IMAGE" }
    };
    
    
    @Override public AddeAccount getAccount() {
        return null;
    }

    @Override public String getAddress() {
        return null;
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

    @Override public String getGroup() {
        return null;
    }

    @Override public void setEntryStatus(EntryStatus newStatus) {
        
    }

}
