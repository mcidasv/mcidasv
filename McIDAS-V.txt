This is the main trunk for SSEC's McIDAS-V Project.

Directory Structure
-------------------

build.xml                     * Main Ant build file
doc/                          * Project documentation
edu/                          
`-- wisc/
    `-- ssec/
        `-- mcidasv/          * General managers and main application code
            |                   should go here, e.g., ViewManger, McIdasV.java
            |-- chooser/      * Data choosers should go here
            |-- control/      * Display controls should go here
            |-- data/         * Datasources should go here
            |-- images/       * Application images should go here
            |-- resources/    * .RBI, .XML, .py, etc... resource should go here
            `-- ui/           * UI related classes here, e.g., UIManger
release/                      * Files used by Install4J

Building a new release
----------------------
Make sure IDV is up to date, then build the "dist" target:
	ant -Duser.name=<CVS username> -Dstorepass=<storepass> dist
Run Install4J and build the installer packages.

Nightlies
---------
A cron job that builds the "nightly" target runs daily at 4am:
	ant -Duser.name=<CVS username> -Dstorepass=<storepass> nightly
There is a separate script on the webserver that pulls the completed build.
