<table cellspacing=0 style="border-top:1px solid black; border-bottom:1px solid black; width:300px; height:30px; margin:0px;">
<tr>
  <td class=selected id="tab_index" style="border:0px solid black; width:33%;">
    <span class="pointer" onClick="setView('index');">Index</span>
  </td>
  <td class=unselected id="tab_search" style="border:0px solid black; width:33%; border-left:1px solid black;">
    <span class="pointer" onClick="setView('search');">Search</span>
  </td>
  <td class=unselected id="tab_pdf" style="border:0px solid black; width:33%; border-left:1px solid black;">
    <span class="pointer" onClick="setView('pdf');">PDF</span>
  </td>
</tr>
</table>

<p>

<!-- INDEX -->
<div class="small menu" id="div_index">
  <span class="link" onClick="setPage('toc.html');">McIDAS-V User's Guide</span><br>

  <div class="indented" style="display:block;">
    <span class="link" onClick="setPage('mcidasv.html');">What is McIDAS-V?</span><br>
    <span class="toggle" onClick="toggleChildren(this,'overview');">+</span>
    <span class="link" onClick="setPage('page_overview.html');">Overview</span><br>

    <div class="indented" id="overview">
      <span class="link" onClick="setPage('ReleaseNotes.html');">Release Notes</span><br>
      <span class="link" onClick="setPage('Systems.html');">System Requirements</span><br>
      <span class="link" onClick="setPage('Starting.html');">Downloading and Running McIDAS-V</span><br>
      <span class="link" onClick="setPage('data/DataSources.html');">Data Formats and Sources</span><br>
      <span class="link" onClick="setPage('Support.html');">Documentation and Support</span><br>
      <span class="link" onClick="setPage('License.html');">License and Copyright</span><br>
    </div>

    <span class="toggle" onClick="toggleChildren(this,'gettingstarted');">+</span>
    <span class="link" onClick="setPage('quickstart/index.html');">Getting Started</span><br>

    <div class="indented" id="gettingstarted">
      <span class="link" onClick="setPage('quickstart/Satellite.html');">Displaying Satellite Imagery</span><br>
            <span class="link" onClick="setPage('quickstart/Hydra.html');">Displaying Hyperspectral Satellite Imagery Using HYDRA</span><br>
                 <span class="link" onClick="setPage('quickstart/OrbitTracks.html');">Displaying Satellite Orbit Tracks</span><br>
                  <span class="link" onClick="setPage('quickstart/RadarLevelII.html');">Displaying Level II Radar Imagery</span><br>
      <span class="link" onClick="setPage('quickstart/RadarLevelIII.html');">Displaying Level III Radar Imagery</span><br>
      <span class="link" onClick="setPage('quickstart/Point.html');">Displaying
      Surface and Upper Air Point Data</span><br>
      <span class="link" onClick="setPage('quickstart/Sounding.html');">Displaying
RAOB Sounding Data</span><br>
            <span class="link" onClick="setPage('quickstart/Grids.html');">Displaying Gridded Data</span><br>
	  <span class="link" onClick="setPage('quickstart/Fronts.html');">Displaying Fronts</span><br>
      <span class="link" onClick="setPage('quickstart/LocalFiles.html');">Displaying Local Files</span><br>
<span class="link" onClick="setPage('quickstart/UrlFiles.html');">Displaying Files from a URL</span><br>
            <span class="link" onClick="setPage('quickstart/Globe.html');">Using the Globe Display</span><br>
    </div>

    <span class="toggle" onClick="toggleChildren(this,'dataexplorer');">+</span>
    <span class="link" onClick="setPage('ui/DataExplorer.html');">Data Explorer</span><br>

    <div class="indented" id="dataexplorer">
      <span class="toggle" onClick="toggleChildren(this,'choosingdatasources');">+</span>
      <span class="link" onClick="setPage('data/choosers/index.html');">Data Sources</span><br>

      <div class="indented" id="choosingdatasources">
        
                <span class="link" onClick="setPage('data/choosers/ImageChooser.html');">Choosing Satellite Imagery</span><br>
                <span class="link" onClick="setPage('data/choosers/HydraChooser.html');">Choosing Hyperspectral Data</span><br>
                <span class="link" onClick="setPage('data/choosers/OrbitTrackChooser.html');">Choosing Orbit Tracks Data</span><br>
                <span class="link" onClick="setPage('data/choosers/Level2Chooser.html');">Choosing NEXRAD Level II Radar Data</span><br>
                <span class="link" onClick="setPage('data/choosers/RadarChooser.html');">Choosing NEXRAD Level III Radar Data</span><br>
                <span class="link" onClick="setPage('data/choosers/PointChooser.html');">Choosing
        Point Data</span><br>
<span class="link" onClick="setPage('data/choosers/RaobChooser.html');">Choosing Upper Air Sounding Data</span><br>
                        		<span class="link" onClick="setPage('data/choosers/GridChooser.html');">Choosing Gridded Data</span><br>
                                		<span class="link" onClick="setPage('data/choosers/FrontChooser.html');">Choosing Front Positions </span><br>
                <span class="link" onClick="setPage('data/choosers/FileChooser.html');">Choosing Data on Disk</span><br>
                    <span class="link" onClick="setPage('data/choosers/CatalogChooser.html');">Choosing Cataloged Data</span><br>    
        <span class="link" onClick="setPage('data/choosers/UrlChooser.html');">Choosing a URL</span><br>
        <span class="link" onClick="setPage('data/choosers/FlatFileChooser.html');">Choosing Flat File Data</span><br>
      </div>

      <span class="toggle" onClick="toggleChildren(this,'thefieldselector');">+</span>
      <span class="link" onClick="setPage('data/FieldSelector.html');">Field Selector</span><br>

      <div class="indented" id="thefieldselector">
        <span class="link" onClick="setPage('data/DataSource.html');">Data Sources</span><br />
          <span class="link" onClick="setPage('data/Fields.html');">Fields</span><br />
                  <span class="link" onClick="setPage('data/Displays.html');">Displays</span><br />
          <span class="link" onClick="setPage('data/DataSubset.html');">Data Subset</span><br />      
      </div>
            <span class="toggle" onClick="toggleChildren(this,'controls/layercontrols');">+</span>
      <span class="link" onClick="setPage('controls/LayerControls.html');">Layer Controls</span><br>
            <div class="indented" id="controls/layercontrols">
      <span class="toggle" onClick="toggleChildren(this,'layercontrols_2');">+</span>
      <span class="link" onClick="setPage('page_griddeddatadisplays.html');">Gridded Data Displays</span><br>

      <div class="indented" id="layercontrols_2">
        <span class="link" onClick="setPage('controls/PlanViewControl.html');">Plan View Controls</span><br>
        <span class="link" onClick="setPage('controls/FlowPlanViewControl.html');">Flow Display Controls</span><br>
        <span class="link" onClick="setPage('controls/3DFlowPlanViewControl.html');">3D Flow Display Controls</span><br>
                <span class="link" onClick="setPage('controls/ValuePlotControl.html');">Value Plot Controls </span><br>
        <span class="link" onClick="setPage('controls/CrossSectionControl.html');">Vertical Cross Section Controls</span><br>
        <span class="link" onClick="setPage('controls/ThreeDSurfaceControl.html');">3D Surface Controls</span><br>
        <span class="link" onClick="setPage('controls/VolumeRenderControl.html');">Volume Rendering Controls</span><br>
		<span class="link" onClick="setPage('controls/PointVolumeControl.html');">Point Volume Controls</span><br>
        <span class="link" onClick="setPage('controls/SoundingControl.html');">Sounding Controls</span><br>
        <span class="link" onClick="setPage('controls/HovmollerControl.html');">Hovmoller Controls</span><br>
        <span class="link" onClick="setPage('controls/GridTrajectory.html');">Grid Trajectory Controls</span><br>
        <span class="link" onClick="setPage('controls/ThetaSurfaceControl.html');">Theta Surface Controls</span><br>
        <strong></strong>
      </div>

      <span class="toggle" onClick="toggleChildren(this,'layercontrols_2_1');">+</span>
      <span class="link" onClick="setPage('page_satelliteandradardisplays.html');">Satellite and Radar Displays</span><br>

      <div class="indented" id="layercontrols_2_1">
        <span class="link" onClick="setPage('controls/ImagePlanViewControl.html');">Image Controls</span><br>
		
		<span class="toggle" onClick="toggleChildren(this,'layercontrols_2_1_1');">+</span>
		<span class="link" onClick="setPage('controls/hydra/index.html');">HYDRA Layer Controls</span><br>
		
		<div class="indented" id="layercontrols_2_1_1">
			<span class="link" onClick="setPage('controls/hydra/MultispectralDisplayControl.html');">MultiSpectral Display Controls</span><br>
			<span class="link" onClick="setPage('controls/hydra/LinearCombinationControl.html');">Linear Combination Controls</span><br>
			<span class="link" onClick="setPage('controls/hydra/ChannelCombinationControl.html');">4 Channel Combination Controls</span><br>
			<span class="link" onClick="setPage('controls/hydra/ProfileAlongTrackControl.html');">ProfileAlongTrack Controls</span><br>
		</div>
		
        <span class="link" onClick="setPage('controls/OrbitTrackControl.html');">Satellite Orbit Track Controls</span><br>
        <span class="link" onClick="setPage('controls/LevelIIIControl.html');">WSR-88D Level III Controls</span><br>
		
		<span class="toggle" onClick="toggleChildren(this,'layercontrols_2_2');">+</span>
        <span class="link" onClick="setPage('page_level2radardisplaycontrols.html');">Level 2 Radar Layer Controls</span><br>
		
		<div class="indented" id="layercontrols_2_2">
        	<span class="link" onClick="setPage('controls/level2/RadarSweepControl.html');">Radar Sweep View Controls</span><br>
        	<span class="link" onClick="setPage('controls/level2/RadarRhiDisplayControls.html');">Radar RHI Display Controls </span><br>
			<span class="link" onClick="setPage('controls/level2/RadarCrossSectionControl.html');">Radar Cross Section Controls</span><br>
			<span class="link" onClick="setPage('controls/level2/RadarCappiDisplayControls.html');">Radar CAPPI Display Controls</span><br>                         
        	<span class="link" onClick="setPage('controls/level2/RadarVolumeControl.html');">Radar Volume Scan Controls</span><br>
        	<span class="link" onClick="setPage('controls/level2/RadarIsosurfaceControl.html');">Radar Isosurface Controls </span><br>
		</div>
      </div>

      <span class="toggle" onClick="toggleChildren(this,'layercontrols_4');">+</span>
      <span class="link" onClick="setPage('page_probes.html');">Probes</span><br>

      <div class="indented" id="layercontrols_4">
        <span class="link" onClick="setPage('controls/ProbeControl.html');">Data Probe/Time Series</span><br>
        <span class="link" onClick="setPage('controls/TimeHeightControl.html');">Time/Height Controls</span><br>
        <span class="link" onClick="setPage('controls/ProfileControl.html');">Vertical Profile Controls</span><br>
        <span class="link" onClick="setPage('controls/DataTransectControl.html');">Data Transect Controls</span><br>
      </div>

      <span class="toggle" onClick="toggleChildren(this,'layercontrols_5');">+</span>
      <span class="link" onClick="setPage('page_mappingcontrols.html');">Mapping Controls</span><br>

      <div class="indented" id="layercontrols_5">
        <span class="link" onClick="setPage('controls/MapDisplayControl.html');">Map Controls</span><br>
        <span class="link" onClick="setPage('controls/TopographyControl.html');">Topography Controls</span><br>
        <span class="link" onClick="setPage('controls/ShapefileControl.html');">Shapefile Controls</span><br>
      </div>

      <span class="toggle" onClick="toggleChildren(this,'layercontrols_6');">+</span>
      <span class="link" onClick="setPage('page_observationandlocationcontrols.html');">Observation and Location Controls</span><br>

      <div class="indented" id="layercontrols_6">
        <span class="link" onClick="setPage('controls/PointDataPlotControl.html');">Point Data Plot Controls</span><br>
        <span class="link" onClick="setPage('controls/ObsListControl.html');">Point Data List Controls</span><br>
		<span class="link" onClick="setPage('controls/ObjectiveAnalysis.html');">Gridded Point Data</span><br>
        <span class="link" onClick="setPage('controls/AerologicalSoundingControl.html');">Sounding Display Controls</span><br>
		<span class="link" onClick="setPage('controls/FrontControl.html');">Front Controls</span><br>
        <span class="link" onClick="setPage('controls/StationLocationControl.html');">Location Controls</span><br>
        <span class="link" onClick="setPage('controls/TrackControl.html');">Track Controls</span><br>
      </div>

      <span class="toggle" onClick="toggleChildren(this,'layercontrols_7');">+</span>
      <span class="link" onClick="setPage('page_miscellaneouscontrols.html');">Miscellaneous Controls</span><br>

      <div class="indented" id="layercontrols_7">
        <span class="link" onClick="setPage('controls/misc/ScatterAnalysisControl.html');">Scatter Analysis Controls</span><br>
                <span class="link" onClick="setPage('controls/misc/GridTable.html');">Grid Table Controls</span><br>
        <span class="link" onClick="setPage('controls/misc/OmniControl.html');">Omni Controls</span><br>
        <span class="link" onClick="setPage('controls/JythonControl.html');">Jython Controls</span><br>
      </div>              <span class="link" onClick="setPage('controls/misc/ContourDialog.html');">Contour Properties Editor</span><br>
            <span class="link" onClick="setPage('controls/PropertiesDialog.html');">Properties Dialog</span><br>
                  <span class="link" onClick="setPage('controls/ColorScale.html');">Color Scale</span><br>
      <span class="link" onClick="setPage('controls/Chart.html');">Charts</span><br>
      <span class="link" onClick="setPage('controls/EnsembleGridControl.html');">Ensemble Grid Controls</span><br>
    </div>
    </div>
    <span class="toggle" onClick="toggleChildren(this,'mainwindow');">+</span>
    <span class="link" onClick="setPage('ui/index.html');">Main Display Window</span><br>

    <div class="indented" id="mainwindow">
        <span class="link" onClick="setPage('ui/Menus.html');">Menu Bar</span><br>
            <span class="link" onClick="setPage('ui/MainToolBar.html');">Main Toolbar</span><br>
            <span class="link" onClick="setPage('ui/DragAndDrop.html');">Drag and Drop Tabs</span><br>
            <span class="link" onClick="setPage('ui/DisplayMenus.html');">Display Menus </span><br>
            <span class="link" onClick="setPage('ui/PropertiesDialog.html');">Properties Dialog </span><br>
            <span class="link" onClick="setPage('ui/DisplayVisibility.html');">Visibility Animation</span><br>
            <span class="link" onClick="setPage('ui/Animation.html');">Time Animation</span><br>                        
<span class="link" onClick="setPage('ui/Legend.html');"> Legend</span><br>
            <span class="link" onClick="setPage('ui/ModifyColorInteractively.html');">Modify the Color Bar Interactively</span><br>
             <span class="link" onClick="setPage('ui/StatusBar.html');">Status Bar</span><br>    
            <span class="link" onClick="setPage('ui/Navigation.html');">Zooming, Panning and Rotating</span><br>
            <span class="link" onClick="setPage('ui/TransectViewManager.html');">Transect Views</span><br>
        </div>
             <span class="toggle" onClick="toggleChildren(this,'Display Controls');">+</span>
    <span class="link" onClick="setPage('page_displaycontrols.html');">Display Controls</span><br>
    <div class="indented" id="Display Controls">
                <span class="link" onClick="setPage('display_controls/RadarGridControl.html');">Range Ring Controls</span><br>
                <span class="link" onClick="setPage('display_controls/RangeAndBearingControl.html');">Range and Bearing Controls</span><br>
                                                            <span class="link" onClick="setPage('display_controls/TransectDrawingControl.html');">Transect Drawing Controls</span><br>
                <span class="link" onClick="setPage('display_controls/DrawingControl.html');">Drawing Controls</span><br>
                                                            <span class="link" onClick="setPage('display_controls/LocationIndicatorControl.html');">Location Indicator Controls</span><br>
                <span class="link" onClick="setPage('display_controls/WMSControl.html');">Web Map Server(WMS)/Background Image Controls</span><br>
                <span class="link" onClick="setPage('display_controls/MovieDisplay.html');">QuickTime Movie Controls</span><br>
                <span class="link" onClick="setPage('display_controls/ImageCaptures.html');">Image and Movie Capture Controls</span><br>
                <span class="link" onClick="setPage('display_controls/Timeline.html');">Timeline Controls</span><br>
                </div>
    <span class="toggle" onClick="toggleChildren(this,'tools');">+</span>
    
    <span class="link" onClick="setPage('page_tools.html');">Tools</span><br>

    <div class="indented" id="tools">
      <span class="toggle" onClick="toggleChildren(this,'tools_1');">+</span>
      <span class="link" onClick="setPage('tools/preferences/Preferences.html');">User Preferences</span><br>

      <div class="indented" id="tools_1">
        <span class="link" onClick="setPage('tools/preferences/GeneralPreferences.html');">General Preferences</span><br>
        <span class="link" onClick="setPage('tools/preferences/DisplayWindowPreferences.html');">Display Window Preferences</span><br>
        <span class="link" onClick="setPage('tools/preferences/ToolbarPreferences.html');">Toolbar Options Preferences</span><br>
        <span class="link" onClick="setPage('tools/preferences/DataPreferences.html');">Data Sources Preferences</span><br>
        <span class="link" onClick="setPage('tools/preferences/ServerPreferences.html');">ADDE Servers Preferences</span><br>
        <span class="link" onClick="setPage('tools/preferences/AvailableDisplaysPreferences.html');">Available Displays Preferences</span><br>
        <span class="link" onClick="setPage('tools/preferences/NavigationPreferences.html');">Navigation Controls Preferences</span><br>
        <span class="link" onClick="setPage('tools/preferences/FormatPreferences.html');">Formats and Data Preferences</span><br>
        <span class="link" onClick="setPage('tools/preferences/AdvancedPreferences.html');">Advanced Preferences </span><br>
      </div>

      <span class="link" onClick="setPage('tools/RemoteDataManager.html');">Remote ADDE Data Manager</span><br>
      <span class="link" onClick="setPage('tools/LocalDataManager.html');">Local ADDE Data Manager</span><br>
      <span class="link" onClick="setPage('tools/TextProductControl.html');">Weather Text Product Controls</span><br>
      <span class="link" onClick="setPage('tools/StormTrackControl.html');">Storm Track Controls</span><br>
      <span class="link" onClick="setPage('tools/ColorTableEditor.html');">Color Table Editor</span><br>
      <span class="link" onClick="setPage('tools/LayoutModelEditor.html');">Layout Model Editor</span><br>
      <span class="link" onClick="setPage('tools/AliasEditor.html');">Parameter Alias Editor</span><br>
      <span class="link" onClick="setPage('tools/ParameterDefaultsEditor.html');">Parameter Defaults Editor</span><br>
      <span class="link" onClick="setPage('tools/ParameterGroupsEditor.html');">Parameter Groups Editor</span><br>
      <span class="link" onClick="setPage('tools/ProjectionManager.html');">Projection Manager</span><br>
       <span class="toggle" onClick="toggleChildren(this,'miscellaneous_2');">+</span>
      <span class="link" onClick="setPage('page_dataanalysis.html');">Data Analysis</span><br>
	  
      <div class="indented" id="miscellaneous_2">
            <span class="link" onClick="setPage('tools/Formulas.html');">Formulas</span><br>
                    <span class="link" onClick="setPage('tools/DerivedData.html');">Derived Data</span><br>
        <span class="toggle" onClick="toggleChildren(this,'miscellaneous_2_1');">+</span>
		<span class="link" onClick="setPage('tools/NativeFormulas.html');">Native Formulas</span><br>
		<div class="indented" id="miscellaneous_2_1">
		    <span class="link" onClick="setPage('tools/ExportFormulas.html');">Description of Formulas - Export</span><br>
		    <span class="link" onClick="setPage('tools/GridFormulas.html');">Description of Formulas - Grids</span><br>
		    <span class="link" onClick="setPage('tools/ImageFilterFormulas.html');">Description of Formulas - Image Filters</span><br>
            <span class="link" onClick="setPage('tools/ImageryFormulas.html');">Description of Formulas - Imagery</span><br>
			<span class="link" onClick="setPage('tools/MapFormulas.html');">Description of Formulas - Maps</span><br>
            <span class="link" onClick="setPage('tools/MiscellaneousFormulas.html');">Description of Formulas - Miscellaneous</span><br>
		</div>
        <span class="link" onClick="setPage('tools/Jython.html');">Jython Methods</span><br>
        <span class="link" onClick="setPage('tools/JythonShell.html');">Jython Shell</span><br>
        <span class="link" onClick="setPage('tools/JythonLib.html');">Jython Library</span><br>
      </div>
	  <span class="link" onClick="setPage('tools/DisplaySettings.html')">Display Settings</span><br>
      	  <span class="link" onClick="setPage('tools/PluginCreator.html')">Plugin Creator</span><br>
          	  <span class="link" onClick="setPage('tools/Plugins.html')">Plugin Manager</span>
</div>

    <span class="toggle" onClick="toggleChildren(this,'miscellaneous');">+</span>
    <span class="link" onClick="setPage('page_miscellaneous.html');">Miscellaneous</span><br>

    <div class="indented" id="miscellaneous">
      <span class="link" onClick="setPage('Bundles.html');">Bundles</span><br>
      <span class="link" onClick="setPage('misc/TimeMatching.html');">Time Matching</span><br>
      <span class="link" onClick="setPage('misc/AdaptiveResolution.html');">Adaptive Resolution</span><br>
      <span class="toggle" onClick="toggleChildren(this,'scripting');">+</span>
      <span class="link" onClick="setPage('misc/Scripting/Scripting.html');">Scripting</span><br>
      <div class="indented" id="scripting">
      <span class="link" onClick="setPage('misc/Scripting/CreateObject.html');">Create an Object</span><br>
      <span class="link" onClick="setPage('misc/Scripting/CreateObjectImagery.html');">Create an Object - Imagery</span><br>
      <span class="link" onClick="setPage('misc/Scripting/CreateObjectGrid.html');">Create an Object - Grids</span><br>
      <span class="link" onClick="setPage('misc/Scripting/ModifyDisplayObject.html');">Modify the Display Object</span><br>
      <span class="link" onClick="setPage('misc/Scripting/ModifyLayerObject.html');">Modify the Layer Object</span><br>
      <span class="link" onClick="setPage('misc/Scripting/ModifyPointLayerObject.html');">Modify the Layer Object - Point</span><br>
      <span class="link" onClick="setPage('misc/Scripting/DataAnalysis.html');">Data Analysis</span><br>
      <span class="link" onClick="setPage('misc/Scripting/SaveDisplay.html');">Save the Display</span><br>
      <span class="link" onClick="setPage('misc/Scripting/Environment.html');">Environment</span><br>
      </div>
      <span class="link" onClick="setPage('collab/Sharing.html');">Sharing</span><br>
      <span class="link" onClick="setPage('misc/SiteConfiguration.html');">Site Configuration</span><br>
      <span class="link" onClick="setPage('misc/PluginJarFiles.html');">Plugin Jar Files</span><br>
      <span class="link" onClick="setPage('data/GribTables.html');">Adding in New GRIB Tables</span><br>
      <span class="toggle" onClick="toggleChildren(this,'miscellaneous_4');">+</span>
      <span class="link" onClick="setPage('page_mcvspecialdataformats.html');">McIDAS-V Special Data Formats</span><br>

      <div class="indented" id="miscellaneous_4">
        <span class="link" onClick="setPage('data/TextPointData.html');">Text (ASCII) Point Data Format</span><br>
        <span class="link" onClick="setPage('data/LocationXml.html');">Location XML Files</span><br>
        <span class="link" onClick="setPage('data/ImageXml.html');">Image XML Files</span><br>
        <span class="link" onClick="setPage('data/ImageMovie.html');">Image Movie Files</span><br>
        <span class="link" onClick="setPage('misc/Xgrf.html');">XGRF Symbols</span><br>
      </div>

      <span class="link" onClick="setPage('misc/Actions.html');">Actions</span><br>
      <span class="link" onClick="setPage('misc/CommandLineArguments.html');">Command Line Arguments</span><br>
      <span class="link" onClick="setPage('misc/DataSourceTypes.html');">Data Source Types</span><br>
      <span class="link" onClick="setPage('misc/PerformanceTuning.html');">Performance Tuning</span><br>
      <span class="link" onClick="setPage('misc/SourceBuild.html');">Building McIDAS-V from Source</span><br>
      <span class="link" onClick="setPage('misc/Console.html');">Message Console</span><br>
      <span class="link" onClick="setPage('misc/SupportRequestForm.html');">Support Request Form</span><br>
    </div>

    <span class="toggle" onClick="toggleChildren(this,'appendix');">+</span>
    <span class="link" onClick="setPage('page_appendix.html');">Appendices</span><br>
<div class="indented" id="appendix">
                     
      <span class="toggle" onClick="toggleChildren(this,'appendix_1');">+</span>
      <span class="link" onClick="setPage('examples/index.html');">Examples of Display Types</span><br>

      <div class="indented" id="appendix_1">
        <span class="link" onClick="setPage('examples/PlanViews.html');">Plan Views</span><br>
        <span class="link" onClick="setPage('examples/3DSurface.html');">3D Surface</span><br>
        <span class="link" onClick="setPage('examples/CrossSections.html');">Cross Sections</span><br />
        <span class="link" onClick="setPage('examples/Probes.html');">Probes</span><br />
                <span class="link" onClick="setPage('examples/Hovmoller.html');">Hovmoller Display</span><br>
        <span class="link" onClick="setPage('examples/Imagery.html');">Imagery</span><br>
        <span class="link" onClick="setPage('examples/Radar.html');">Radar - Level II WSR-88D Data Displays</span><br>
        <span class="link" onClick="setPage('examples/Soundings.html');">Soundings</span><br>
        <span class="link" onClick="setPage('examples/FlowDisplays.html');">Flow Displays</span><br>
        <span class="link" onClick="setPage('examples/Observations.html');">Observations</span><br>
        <span class="link" onClick="setPage('examples/Miscellaneous.html');">Miscellaneous Display Types</span><br>

        <div class="indented" id="appendix_1_1"><br>
        </div>
       		</div>
            <span class="toggle" onClick="toggleChildren(this,'faq');">+</span>
<span class="link" onClick="setPage('Faq.html');"> FAQ - Frequently Asked Questions</span>
<br>
<div class="indented" id="faq">
        <span class="link" onClick="setPage('GeneralFaq.html');">General FAQ</span><br>
                <span class="link" onClick="setPage('UsingMcVFaq.html');">Using McIDAS-V FAQ</span><br>
                        <span class="link" onClick="setPage('DataFaq.html');">Data FAQ</span><br>
                        <span class="link" onClick="setPage('VideoCardsFaq.html');">Video Cards FAQ</span><br>
                        <span class="link" onClick="setPage('InstallationFaq.html');">Common Installation Errors FAQ</span><br>
             <span class="link" onClick="setPage('RunTimeFaq.html');">Common Run-Time Errors FAQ</span><br>
                     <span class="link" onClick="setPage('ReportingProblemsFaq.html');">Reporting Problems FAQ</span><br>	
                     </div>	
        <span class="link" onClick="setPage('xequivalent.html');">McIDAS-X Commands in McIDAS-V</span><br> 

      </div>

    </div>

  </div>

</div>

<!-- SEARCH -->
<div class="small menu" id="div_search" style="display:none;">
  <span class="link"
    onClick="doSearch();"
    onMouseOver="try{hilite(this)}catch(err){};"
    onMouseOut="try{unhilite(this)}catch(err){};"
  >Find:</span>
  <input id="input_search" name="input_search"
    onChange="doSearchTerm(this.value);">

  <p>

  <div id="div_results">
  </div>

</div>

<!-- PDF -->
<div class="small menu" id="div_pdf" style="display:none;">
<img src="acrobat.gif" style="border:0px;"> <a href="mcv_guide.pdf">Download PDF</a>
</div>
