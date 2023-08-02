This is the main trunk for SSEC's McIDAS-V Project.

Things of Interest
------------------

    .                                   
    ├── build-customized.xml            * User-customizable Ant build file.
    ├── build.xml                       * Main Ant build file.
    ├── docs                            
    │   ├── javadoc                     * API documentation for developers.
    │   └── userguide                   * Project documentation.
    ├── edu                             
    │   └── wisc                        
    │       └── ssec                    
    │           ├── mcidas              
    │           └── mcidasv             * General managers and main application 
    │               │                     code should go here, e.g., ViewManager, 
    │               │                     McIDASV.java.
    │               ├── chooser         * Data choosers should go here.
    │               ├── control         * Display controls should go here.
    │               ├── data            * Datasources should go here.
    │               ├── display         * Displays code should go here.
    │               ├── images          * DEPRECATED: please use appropriate
    │               │                     directory within "resources".
    │               ├── jython          * Linear Combination Jython Interpreter.
    │               ├── monitors        * Monitor the state of a McIDAS-V session.
    │               ├── probes          * Data probes.
    │               ├── resources       * Non-code resources required by 
    │               │   │                 McIDAS-V should reside here. Things
    │               │   │                 like RBI, XML, and images.
    │               │   └── python      * Jython library code.
    │               ├── servermanager   * Handles local and remote ADDE datasets.
    │               ├── startupmanager  * Manage McIDAS-V startup options.
    │               ├── supportform     * Submit McIDAS-V support requests.
    │               ├── ui              * UI related classes here, e.g., UIManager.
    │               └── util            * Utility classes can go here.
    |
    ├── lib                             * McIDAS-V dependencies (other than VisAD/IDV).
    |   |
    |   ├── linux-amd64                 * 64-bit Linux dependencies.
    |   ├── linux-i586                  * 32-bit Linux dependencies.
    |   ├── macosx                      * OS X dependencies
    |   ├── share                       * Platform independent dependencies. This is
    |   |                                 where most JAR files will end up.
    |   ├── windows-amd64               * 64-bit Windows dependencies.
    |   └── windows-i586                * 32-bit Windows dependencies.
    |
    ├── release                         * Files used by install4j.
    ├── tools                           
    │   ├── apidocs                     
    │   └── external                    
    │       ├── orphan_icon_finder      
    │       ├── pluginfeed              
    │       └── supportreq              
    └── ucar                            
                                        
Running McIDAS-V
----------------
Assuming you've cloned the repo, and have installed Java 8+:

    ant jar.runlarge


Building a new release
----------------------
Make sure IDV is up to date, then build the "dist" target:

    ant dist
    
Run Install4J and build the installer packages.

Nightlies
---------
A cron job that builds the "nightly" target runs daily at 4:00 AM:

    ant nightly
    
There is a separate script on the webserver that pulls the completed build.

Acknowledgements
----------------
YourKit is kindly supporting open source projects with its full-featured Java 
Profiler. YourKit, LLC is the creator of innovative and intelligent tools for 
profiling Java and .NET applications. Take a look at YourKit's leading
software products: <a href="http://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a> and <a href="http://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>.
