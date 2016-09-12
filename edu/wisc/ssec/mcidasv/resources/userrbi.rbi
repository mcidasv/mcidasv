<?xml version="1.0" encoding="UTF-8"?>
<!--
  This is an example RBI (Resource Bundle for the IDV) file.

  McIDAS-V uses RBI files to find out where its various configuration files
  are located.

  Each "resources" tag can hold a set of "resource" tags. The resource
  tag has a location attribute that can be a file, url or jar resource,
  that points to some resource to load (e.g., the color tables).

  This location can also use macros that McIDAS-V will replace with the
  appropriate path:

    %USERPATH%    Directory that contains the user's specific setting.
                  While this path may be modified by the -userpath command line
                  argument, the default location varies by platform:

                      Windows: C:\Users\USERNAME\McIDAS-V

                      Linux:   /home/USERNAME/McIDAS-V

                      macOS:   /Users/USERNAME/Documents/McIDAS-V

    %SITEPATH%    This path is used to allow administrators to specify
                  site-specific configurations. The value is controlled via the
                  -sitepath command line argument.

    %IDVPATH%     Path used for configuration files that McIDAS-V inherits from
                  Unidata's IDV. Note: these files are primarily located within
                  idv.jar, but in some situations may also be found in
                  local-idv.jar. The path within both JAR files is
                  /ucar/unidata/idv/resources.

    %APPPATH%     Path used for McIDAS-V configuration files. These files will
                  be inside mcidasv.jar and in the
                  /edu/wisc/ssec/mcidasv/resources path.

                  Note: the path is specified by the idv.resourcepath property
                  found in /edu/wisc/ssec/mcidasv/resources/mcidasv.properties.

  If a resources tag has a loadmore="false", McIDAS-V will not load any more
  resources of this type (e.g. this allows you to not load the default system
  resources). The default is that loadmore="true".

  If you want to add your own resources and/or overwrite the defaults then
  add the appropriate <resource location="..."/> tag. For your convenience
  we have included commented out resource tags below.

  Note: For resource lists that hold "writable" resources (e.g. color tables),
  the first resource in the list is the "writable" resource (i.e. where we
  write new content).
-->
<resourcebundle name="Default">
    <!-- Location(s) of our various color tables. -->
    <resources name="idv.resource.colortables" loadmore="true">
        <!--
            Examples:
            <resource location="%USERPATH%/colortables.xml"/>
            <resource location="%SITEPATH%/colortables.xml"/>
            <resource location="http://www.somewebsite.edu/somepath/colortables.xml"/>
        -->
    </resources>

    <!-- Location(s) of data chooser XML files. -->
    <resources name="idv.resource.choosers" loadmore="true">
    </resources>

    <!-- Where we find the color pairs for the foreground/background colors -->
    <resources name="idv.resource.colorpairs" loadmore="true">
    </resources>

    <!-- Automatically create displays from the data. -->
    <resources name="idv.resource.autodisplays" loadmore="true">
    </resources>

    <!-- The different user interfaces available. -->
    <resources name="idv.resource.skin" loadmore="true">
    </resources>

    <!-- Defines the toolbar icons. -->
    <resources name="idv.resource.toolbar" loadmore="true">
    </resources>

    <!-- Defines the background image wms info. -->
    <resources name="idv.resource.backgroundwms" loadmore="true">
    </resources>

    <!-- Defines the image set XML. -->
    <resources name="idv.resource.imagesets" loadmore="true">
    </resources>

    <!-- Defines the actions for the toolbar, etc. -->
    <resources name="idv.resource.actions" loadmore="true">
    </resources>

    <!-- Where to find the parameter group files. -->
    <resources name="idv.resource.paramgroups" loadmore="true">
    </resources>

    <!-- Where to find the specification of the derived quantities. -->
    <resources name="idv.resource.derived" loadmore="true">
    </resources>

    <!-- Where to find the parameter to color table files. -->
    <resources name="idv.resource.paramdefaults" loadmore="true">
    </resources>

    <!-- The list of station table XML files. -->
    <resources name="idv.resource.locations" loadmore="true">
    </resources>

    <!-- The list of help tip XML files. -->
    <resources name="idv.resource.helptips" loadmore="true">
    </resources>

    <!-- The list of projection table XML files. -->
    <resources name="idv.resource.projections" loadmore="true">
    </resources>

    <!-- Location(s) of predefined transects. -->
    <resources name="idv.resource.transects">
    </resources>

    <!-- Where to find the data source specifications. -->
    <resources name="idv.resource.datasource" loadmore="true">
    </resources>

    <!-- Where to find the adde server specifications. -->
    <resources name="idv.resource.addserver" loadmore="true">
    </resources>

    <!-- Where to find the specification of the display controls used. -->
    <resources name="idv.resource.controls" loadmore="true">
    </resources>

    <!-- Where to find the parameter aliases. -->
    <resources name="idv.resource.aliases" loadmore="true">
    </resources>

    <!-- Location(s) of default bundles (ones used at start up). -->
    <resources name="idv.resource.bundles" loadmore="true">
    </resources>

    <!-- Where do we find the XML definition of the 'favorites' bundles. -->
    <resources name="idv.resource.bundlexml" loadmore="true">
    </resources>

    <!-- This points to the XML document(s) that hold the user defined chooser
         panels. -->
    <resources name="idv.resource.userchooser" loadmore="true">
    </resources>

    <!-- Jython libraries -->
    <resources name="idv.resource.jython" loadmore="true">
    </resources>

    <!-- Deprecated, do not use! Python libraries -->
    <resources name="idv.resource.jythontocopy" loadmore="true">
    </resources>

    <!-- Holds an XML specification of the menu bar used in the GUIs. -->
    <resources name="idv.resource.menubar" loadmore="true">
    </resources>

    <!-- Defines the set of system maps. -->
    <resources name="idv.resource.maps" loadmore="true">
    </resources>

    <!-- Where we find station models. -->
    <resources name="idv.resource.stationmodels" loadmore="true">
    </resources>

    <!-- What goes into the station model editor. -->
    <resources name="idv.resource.stationsymbols" loadmore="true">
    </resources>

    <!-- Defaults for ADDE based imagery. -->
    <resources name="idv.resource.imagedefaults" loadmore="true">
    </resources>

    <!-- We don't use this now but it will hold message catalogs. -->
    <resources name="idv.resource.messages" loadmore="true">
    </resources>

    <!-- Where to find extra GRIB 1 lookup tables. -->
    <resources name="idv.resource.grib1lookuptables" loadmore="true">
    </resources>

    <!-- Where to find extra GRIB 2 lookup tables. -->
    <resources name="idv.resource.grib2lookuptables" loadmore="true">
    </resources>

    <!-- Where to look for plugins. -->
    <resources name="idv.resource.plugins" loadmore="true">
    </resources>

    <!-- Where to look for prototypes. -->
    <resources name="idv.resource.prototypes">
    </resources>
</resourcebundle>



