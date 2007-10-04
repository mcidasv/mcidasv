#!/usr/bin/env python
# encoding: utf-8
"""
Replace the content of the listed files with the under construction html content from 
UnderConstruction.html.

$Id$
"""

import sys
import os

# Files in these lists will have their content replaced with the contents of
# the src_page.  As help content is update/generated remove them from the list.
all_pages = {

  '':[
    'index.html',
    'Bundles.html',
    'Bugs.html',
    'ReleaseNotes.html',
    'Starting.html',
    'Support.html',
    'Systems.html',
    'Faq.html',
    'imageindex.html'
  ],

  'collab':[
    'Sharing.html'
  ],

  'controls':[
    'AerologicalSoundingControl.html',
    'Chart.html',
    'CrossSectionControl.html',
    'DataTransectControl.html',
    'FlowPlanViewControl.html',
    'ImagePlanViewControl.html',
    'ImageSequenceControl.html',
    'index.html',
    'JythonControl.html',
    'LevelIIIControl.html',
    'MapDisplayControl.html',
    'ObsListControl.html',
    'PlanViewControl.html',
    'ProbeControl.html',
    'ProfileControl.html',
    'ShapefileControl.html',
    'StationLocationControl.html',
    'StationModelControl.html',
    'ThreeDSurfaceControl.html',
    'TimeHeightControl.html',
    'TopographyControl.html',
    'TrackControl.html',
    'VolumeRenderControl.html',
    'WorldWindControl.html'
  ],

  'controls/level2':[
    'RadarIsosurfaceControl.html',
    'RadarSweepControl.html',
    'RadarVolumeControl.html',
    'RhiControl.html'
  ],

  'controls/misc':[
    'AudioControl.html',
    'DrawingControl.html',
    'ImageMovieControl.html',
    'LocationIndicatorControl.html',
    'MovieDisplay.html',
    'MultiDisplayHolder.html',
    'NoteControl.html',
    'OmniControl.html',
    'RadarGridControl.html',
    'RangeAndBearingControl.html',
    'TextDisplayControl.html',
    'TransectDrawingControl.html',
    'WMSControl.html'
  ],

  'controls/profiler':[
    'ProfilerMultiStationControl3D.html',
    'ProfilerStationPlotControl.html',
    'ProfilerTimeHeightControl.html'
  ],

  'data':[
    'DataSourceProperties.html',
    'DataSources.html',
    'FieldSelector.html',
    'GribTables.html',
    'ImageMovie.html',
    'ImageXml.html',
    'LocationXml.html',
    'TextPointData.html'
  ],

  'data/choosers':[
    'index.html',
    'CatalogChooser.html',
    'DirectoryChooser.html',
    'FileChooser.html',
    'ImageChooser.html',
    'Level2Chooser.html',
    'PointChooser.html',
    'ProfilerChooser.html',
    'RadarChooser.html',
    'RaobChooser.html',
    'UrlChooser.html'
  ],

  'examples':[
    '3DSurface.html',
    'FlowDisplays.html',
    'Imagery.html',
    'index.html',
    'Miscellaneous.html',
    'Observations.html',
    'PlanViews.html',
    'Profiler.html',
    'Radar.html',
    'Soundings.html'
  ],

  'isl':[
    'BasicTags.html',
    'DataAndDisplays.html',
    'FileTags.html',
    'ImagesAndMovies.html',
    'index.html',
    'Isl.html',
    'JythonISL.html',
    'Output.html',
    'Summary.html'
  ],

  'misc':[
    'Actions.html',
    'CommandLineArguments.html',
    'ImageDefaults.html',
    'PerformanceTuning.html',
    'PluginCreator.html',
    'PluginJarFiles.html',
    'Plugins.html',
    'SiteConfiguration.html',
    'SourceBuild.html',
    'Xgrf.html'
  ],

  'tools':[
    'AliasEditor.html',
    'ColorTableEditor.html',
    'Console.html',
    'ContourDialog.html',
    'DerivedData.html',
    'Formulas.html',
    'ImageCaptures.html',
    'Jython.html',
    'JythonLib.html',
    'JythonShell.html',
    'ParameterDefaultsEditor.html',
    'ParameterGroupsEditor.html',
    'Preferences.html',
    'ProjectionManager.html',
    'StationModelEditor.html',
    'SupportRequestForm.html',
    'Timeline.html'
  ],

  'ui':[
    'Animation.html',
    'Dashboard.html',
    'index.html',
    'MainToolBar.html',
    'Menus.html',
    'Navigation.html',
    'TransectViewManager.html'
  ]
}

def main(src_dir, src_file):
  if not os.path.exists(src_file):
    print "source file %s not found" % src_file
    sys.exit(1)
  elif not os.path.exists(src_dir):
    print "source dir %s not found" % src_dir
    sys.exit(1)
    
  for pth in all_pages:
	  for fn in all_pages[pth]:
	    fpth = os.path.join(pth, fn)
	    print fpth


if __name__ == '__main__':
  from optparse import OptionParser
  parser = OptionParser()
  parser.add_option("-f", "--src_file", dest="src_file", default='',
            help="The under construction HTML source file")
  parser.add_option("-d", "--src_dir", dest="src_dir", default="",
            help="The root directory of the users guide")

  opts, args = parser.parse_args()
  
  src_dir = opts.src_dir
  src_file = opts.src_file
  
  if not src_dir or not src_file:
    parser.parse_args(['-h'])

  main(src_dir, src_file)

