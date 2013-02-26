This is the main trunk for SSEC's McIDAS-V Project.

Directory Structure
-------------------

    build.xml                       * Main Ant build file.
    doc/                            * Project documentation.
    edu/                            
    `-- wisc/                       
        `-- ssec/                   
            `-- mcidasv/            * General managers and main application 
                |                     code should go here, e.g., ViewManger, 
                |                     McIDASV.java.
                |-- chooser/        * Data choosers should go here.
                |-- control/        * Display controls should go here.
                |-- data/           * Datasources should go here.
                |-- display/        * Displays code should go here.
                |-- images/         * Application images should go here.
                |-- jython/         * Linear Combination Jython Interpreter.
                |-- monitors/       * Monitor the state of a McIDAS-V session.
                |-- probes/         * Data probes.
                |-- resources/      * .RBI, .XML, .py, etc... resource should 
                |                     go here.
                |-- servermanager/  * Handles local and remote ADDE datasets.
                |-- startupmanager/ * Manage McIDAS-V startup options.
                |-- supportform/    * Submit McIDAS-V support requests.
                |-- ui/             * UI related classes here, e.g., UIManager.
                `-- util/           * Utility classes can go here.
    release/                        * Files used by install4J.

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

Acknowledgements
----------------
YourKit is kindly supporting open source projects with its full-featured Java 
Profiler. YourKit, LLC is the creator of innovative and intelligent tools for 
profiling Java and .NET applications. Take a look at YourKit's leading software
products: <a href="http://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a> and <a href="http://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>.
