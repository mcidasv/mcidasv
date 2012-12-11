This is the main trunk for SSEC's McIDAS-V Project.

Directory Structure
-------------------

    build.xml                       * Main Ant build file  
    doc/                            * Project documentation  
    edu/                            
    `-- wisc/                       
        `-- ssec/                   
            `-- mcidasv/            * General managers and main application code
                |                     should go here, e.g., ViewManger, 
                |                     McIDASV.java
                |-- chooser/        * Data choosers should go here
                |-- control/        * Display controls should go here
                |-- data/           * Datasources should go here
                |-- display/        * 
                |-- images/         * Application images should go here
                |-- jython/         * 
                |-- monitors/       * 
                |-- probes/         * 
                |-- resources/      * .RBI, .XML, .py, etc... resource should 
                |                     go here.
                |-- servermanager/  * 
                |-- startupmanager/ * 
                |-- supportform/    * 
                |-- ui/             * UI related classes here, e.g., UIManager
                `-- util/           * Utility classes can go here
    release/                        * Files used by Install4J

Building a new release
----------------------
Make sure IDV is up to date, then build the "dist" target:

    ant dist
    
Run Install4J and build the installer packages.

Nightlies
---------
A cron job that builds the "nightly" target runs daily at 4am:

    ant nightly
    
There is a separate script on the webserver that pulls the completed build.
