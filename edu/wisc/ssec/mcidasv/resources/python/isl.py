import os
import glob

from java.io import File
from java.lang import IllegalArgumentException

from ucar.unidata.ui import ImageUtils
from ucar.unidata.util import IOUtil
from ucar.unidata.util import Misc
from ucar.visad import Util

from edu.wisc.ssec.mcidasv import McIDASV

"""
Set of available ISL tags:
7.1.8 Tag Index
<append>
Append to a property
<backgroundtransparent>
Set transparency on an image. Use the background color of the view.
<bundle>
Load a bundle
<call>
Call an isl procedure
<center>
Center a display at a lat/lon, or from a display control
<clip>
Clip an image
<colorbar>
Add a color bar to an image
<copy>
Copy a file
<datasource>
Create a data source
<delete>
Delete a file
<display>
Create a display
<displaylist>
Render the display list
<echo>
Print a message
<exec>
Execute a shell command
<fileset>
Specify a set of files for use with other tags
<foreach>
For each loop
<group>
Group a set of tags. Possibly loop.
<html>
Create an image from rendered html
<idvproperty>
Define one of the IDV properties
<if>
An if statement
<image>
Generate and manipulate images
<import>
Import another isl file
<isl>
Top level ISL tag
<jython>
Evaluate some Jython
<kmlcolorbar>
Write a colorbar into the kmz file
<latlonlabels>
Add lat/lon labels to an image
<matte>
Matte an image
<mkdir>
Make a directory
<move>
Move a file
<movie>
Create a Quicktime movie, animated GIF or Google Earth KMZ file
<output>
Write output to file
<overlay>
Annotate an image with text or a logo
<panel>
Create a gridded layout of a set of images. This is just like the movie tag and support all of the above attributes
<pause>
Pause for some time
<procedure>
Define an isl procedure
<property>
Define a property
<reload>
Reload all loaded data
<removeall>
Remove all data and displays
<removedisplays>
Remove all displays
<rename>
Rename a file
<resize>
Resize an image
<setfiles>
Override the files or urls used in a bundle
<show>
Show the current image in a dialog
<split>
Split an image into sub-images
<stop>
Stop all processing
<thumbnail>
Generate a thumbnail of an image
<trace>
Turn on tracing and only show the given pattern
<transparent>
Set transparency on an image
<viewpoint>
Change the viewpoint or aspect ratio of a view
<write>
Write out an image
"""

def pauseSeconds(value):
    Misc.sleep(1000.0 * value)

def pauseMinutes(value):
    Misc.sleep(60.0 * 1000.0 * value)

def pauseHours(value):
    Misc.sleep(60.0 * 60.0 * 1000.0 * value)

def pauseUntilDone():
    mcv = McIDASV.importStaticMcv():
    if mcv:
        mcv.waitUntilDisplaysAreDone()

def pauseEvery(value):
    Misc.pauseEvery(60 * value)

def makeDirectory(newdir):
    os.makedirs(newdir)

def renameFile(oldPath, newPath):
    IOUtil.moveFile(File(oldPath), File(newPath))

def expandFileset(fileset):
    for path in fileset:
        for filePath in glob.glob(os.path.expanduser(path)):
            yield filePath

def deleteFiles(fileset):
    # need to figure out how to do filename globbing in python
    # (pretty easy using fnmatch)
    # fileset can have entries like so:
    #   /path/to/file.ext
    #   /path/to/*.ext
    for path in expandFileset(fileset):
        os.remove(path)

def moveFiles(destinationPath, fileset):
    for path in expandFileset(fileset):
        directory, name = os.path.split(path)
        os.rename(path, os.path.join(destinationPath, name))

def copyFiles(destinationPath, fileset):
"""
<copy dir="destination directory">
    <fileset dir="/some/directory" pattern="*.png"/>
    <fileset name="somefile"/>
</copy>
"""
    for path in expandFileset(fileset):
        IOUtil.copyFile(File(path), destinationPath)

def bundle(bundle, width, height, times, clear, wait):
"""
<bundle> Load a bundle
<bundle 
    file="bundle file or url" 
    width="view window width" 
    height="view window height" 
    times="List of times to use" 
    clear="default is true. remove all existing displays and data" 
    wait="default is true. wait until all displays have been rendered." >
The bundle tag lets you load in a new bundle and takes the form:
<bundle file="some_bundle_file.xidv"/>
By default it will remove all currently loaded data and displays. If you want to not clear out the data and displays then add a clear="false" attribute:
<bundle file="some_bundle_file.xidv" clear="false"/>
The wait attribute, if true (the default), will essentially do a pause, waiting until all displays have been rendered.
The times attribute allows you to override the set of time indices that are used in the data sources in the bundle. Note: this overrides the times for all data sources in the bundle.
The value is a comma separated list of time specifiers where the specifier can be a single time, a time range or a time range with a step. The time indices are 0 based, e.g., the first time index is 0. The time range is of the form "firsttime:lasttime". To specify a step do: "firsttime:lasttime:step"
For example, we have:
   times="0"                    -- Just use the first time
   times="2"                    -- Just use the third time
   times="0,1,2"                -- Just use the first 3 times
   times="0:10"                 -- 0 through 10
   times="0:10:2"               -- 0,2,4,6,8,10
   times="0:10:2, 20:30"        -- 0,2,4,6,8,10 20-30
   times="1,2,3,5:11:2,20,30"   -- 1,2 3, 5,7,9,11, 20, 30 
"""
    pass

def createDatasource(url, type, times):
"""
<datasource> Create a data source
<datasource 
    url="url or file name to load" 
    type="data source type" 
    times="List of times to use" >
The datasource tag lets you create a new data source and takes the form. The url attribute is required and specifies the file or url to load. The type attribute is optional and defines the data source type from the datasources.xml file in the source release. For a text listing see Datasources.html. For now you can only specify a data source that takes a single url (or filename). This will change in the future. The times attribute works the same as in the bundle tag above.
<isl>
  <datasource url="dods://motherlode.ucar.edu/cgi-bin/dods/DODS-3.2.1/nph-dods/dods/casestudies/idvtest/grids/small_ruc_grid.nc">
<!-- Set the name on the data source  -->
    <property
       name="name"
       value="the name"/>
  </datasource>
</isl>
datasource.isl
"""
    pass

def createDisplay(type, param):
"""
<display 
    type="display type" 
    param="parameter name" >
The display tag lets you create a display and can exist on its own:
<display type="somedisplaytype" param="some_parameter"/>
or as a child tag of the datasource tag:
<isl>

<!-- Create   a datasource. Note: you can also have a template attribute
     that points to a bundle xidv file that was saved from a data source:
     bundle="${islpath}/datasource.xidv"
-->

  <datasource 
     url="dods://motherlode.ucar.edu:8080/thredds/dodsC/casestudies/idvtest/grids/small_ruc_grid.nc"
     id="datasource1">
<!-- Set the name on the data source  -->
    <property
       name="name"
       value="Example data source"/>
<!-- Create a display of RH. Here we load in the display from a template.  -->
<!--
    <display
       param="RH" template="${islpath}/template.xidv">
      <property
         name="id"
         value="display1"/>
      <property
         name="displayCategory"
         value="Category 1"/>
      <property
         name="legendLabelTemplate"
         value="%datasourcename% - %shortname%"/>
    </display>
-->


<!-- Create a display of T. Here we create the display from the type -->
    <display
    type="planviewcontour"
      param="T">
      <property
         name="displayCategory"
         value="Category 1"/>
<!--
        The contour info can be set with: interval;base;min;max
              <property name="contourInfoParams" value="10;40;-30;100"/>
         Or it can have names in it:
-->
      <property name="contourInfoParams" value="interval=10;base=40;min=-30;max=100;dashed=true;labels=false"/>

      <property
         name="legendLabelTemplate"
         value="%datasourcename% - %shortname%"/>

<!-- This sets the level to be 500 hectopascals -->
<!-- Note: this can also be of the form #<index>, eg. #4 will select the 5th level (this is zero based) -->

      <property
         name="dataSelectionLevel"
         value="500[hectopascals]"/>


    </display>
  </datasource>


<!-- Set the projection to the data projection  of the display we created above  -->
<!--
  <center
     display="display1"
     useprojection="true"/>
-->

</isl>
display.isl
"""
    pass

def findDisplayControl(displayId):
    mcv = McIDASV.getStaticMcv()
    if mcv:
        for control in mcv.getDisplayControls():
            if displayId.startswith('class:'):
                if StringUtil.stringMatch(control.getClass().getName(), displayId[6:], True, True):
                    return control
            if control.getId():
                if StringUtil.stringMatch(control.getId(), displayId, True, True):
                    return control
    return None

def removeAll():
    mcv = McIDASV.getStaticMcv()
    if mcv:
        mcv.removeAllData(False)
        mcv.removeAllLayers(False)

def removeDisplay(displayId):
"""
<removedisplays> Remove all displays
<removedisplays 
    display="display id to remove" >
This removes all current displays. If there is the display attribute set then it removes that display. (See below).
<removedisplays/>
"""
    control = findDisplayControl(displayId)

def removeAllDisplays():
"""
<removedisplays> Remove all displays
<removedisplays 
    display="display id to remove" >
This removes all current displays. If there is the display attribute set then it removes that display. (See below).
<removedisplays/>
"""
    mcv = McIDASV.getStaticMcv()
    if mcv:
        mcv.removeAllLayers(False)

def removeAllData():
    mcv = McIDASV.getStaticMcv()
    if mcv:
        mcv.removeAllData(False)

def reload():
    mcv = McIDASV.getStaticMcv()
    if mcv:
        for source in mcv.getDataSources():
            source.reloadData()

def getViewManagers(viewId=None):
    mcv = McIDASV.getStaticMcv()
    if mcv:
        viewManagers = mcv.getVMManager().getViewManagers()
        if not viewId:
            return viewManagers
        
        if viewId.startswith('name:'):
            viewId = viewId.replace('name:', '')
        
        goodOnes = []
        for (index, vm) in enumerate(viewManagers):
            if viewId.startswith('#'):
                if index == int(viewId[1:]):
                    goodOnes.append(vm)
                continue
            if viewId.startswith('class:'):
                if StringUtil.stringMatch(vm.getClass().getName(), viewId[6:], True, True):
                    goodOnes.append(vm)
                continue
            
            name = vm.getName()
            if not name:
                name = ''
            if StringUtil.stringMatch(name, viewId, True, True):
                goodOnes.append(vm)
        
        return goodOnes

def centerOnLatLon(latitude, longitude):
    mcv = McIDASV.getStaticMcv()
    if mcv:
        viewManagers =  getViewManagers()
        earthLoc = Util.makeEarthLocation(latitude, longitude)
        mcv.getVMManager().center(earthLoc, viewManagers)

def centerOnRectangle(north, south, west, east):
    mcv = McIDASV.getStaticMcv()
    if mcv:
        viewManagers = getViewManagers()
        projRect = ProjectionRect(west, north, east, south)
        mcv.getVMManager().center(projRec, viewManagers)

def centerDisplay(viewId, useProjection=False):
    control = findDisplayControl(id)
    if not control:
        raise IllegalArgumentException('Could not find display: %s' % (id))
    
    mcv = McIDASV.getStaticMcv()
    if mcv:
        viewManagers = getViewManagers(viewId)
        if useProjection and control.getDataProjection():
            mapProjection = control.getDataProjection()
            mcv.getVMManager().center(mapProjection, viewManagers)
        elif control.getDisplayCenter():
            centerPoint = Util.makeEarthLocation(control.getDisplayCenter())
            mcv.getVMManager().center(centerPoint, viewManagers)

def center():
"""
center> Center a display at a lat/lon, or from a display control
<center 
    lat="latitude" 
    lon="longitude" 
    north="latitude" 
    south="latitude" 
    east="longitude" 
    west="longitude" 
    display="display id to center at" 
    useprojection="Use the projection from the display" >
The center tag comes in a number of forms, depending on the attributes:
<isl>
    <bundle file="test.xidv"/>
    <pause/>

    <echo message="Set the projection to be the projection of the first display"/>
    <center/>
    <pause seconds="10"/>

    <echo message="Center at a point"/>
    <center lat="15" lon="-65"/>
    <pause seconds="10"/>

    <echo message="Set the projection to be the lat/lon box"/>
    <center north="40.0" south="30" east="-90" west="-100"/>
    <pause seconds="10"/>

    <echo message="Set the projection from the specified display"/>
    <center display="display1" useprojection="true"/>
    <pause seconds="10"/>

    <echo message="Center at the center of the given displays projection"/>
    <center display="display1" useprojection="false"/>
    <pause seconds="10"/>

</isl>
center.isl
Using the display attribute specifies a display control to use. This can take two forms. First, you can specify a simple text pattern that we try to match on the displays id. The id can be set from the display controls property dialog.
display="some name"
The second method is to specify a class name or partial class name:
display="class:ucar.unidata.idv.TrackControl"
or:
display="TrackControl"

"""
    pass

def viewpoint():
"""
<viewpoint> Change the viewpoint or aspect ratio of a view
<viewpoint 
    rotx="Rotation X" 
    roty="Rotation Y" 
    rotz="Rotation Z" 
    transx="Translation X" 
    transy="Translation Y" 
    transz="Translation Z" 
    scale="Scale" 
    aspectx="Aspect ratio x" 
    aspecty="Aspect ratio y" 
    aspectz="Aspect ratio z" 
    tilt="Tilt degrees" 
    azimuth="Asimuth degrees" >
The viewpoint tag comes in a number of forms, depending on the attributes:
<?xml version="1.0" encoding="ISO-8859-1"?>
<isl debug="true" loop="1" offscreen="false" sleep="60.0minutes">
  <bundle clear="true" file="${islpath}/test.xidv" wait="true"/>
  <pause seconds="5"/>

<!-- Specify a rotation, translation and scale. These values are shown in the Aspect Ratio tab of the View Manager properties dialog  -->
  <viewpoint  rotx="75" roty="62" rotz="-3.3" scale="0.399" transx="0.0" transy="0.0" transz="0.0"/>
  <pause seconds="5"/>

<!-- You can also set the aspect ratio  -->
  <viewpoint  aspectx="2" aspecty="5" aspectz="10"/>
  <pause seconds="5"/>

<!-- You can also specify a tilt and azimuth. This is the same as you can do interactively in the viewpoint dialog -->
  <viewpoint  tilt="45" azimuth="180"/>
  <pause seconds="5"/>

</isl>
viewpoint.isl
The rot/trans/scale values can be viewed in the Aspect Ratio tab of the View Manager properties dialog.
"""
    pass

def image():
"""
<image> Generate and manipulate images
<image 
    file="image file, e.g., gif, png or jpg" 
    quality="image quality, 0.0-1.0" 
    view="view name or names to match" 
    display="display id" 
    what="what part of the display control should be captured" >
The image tag allows you to capture an image from a main display or from the GUI of a display control.
The file attribute defines the image file. For image formats that support it you can also specify a quality attribute that can range from close to 0.0(worst) to 1.0 (best and default).
The view attribute allows you to specify a name of a view it use. This can also be a regular expression pattern to use to match on multiple views. If there are multiple views in existence and there is no view attribute specified or if there multiple views resulting from a view name attribute you should use the viewindex and viewname in your filenames, etc., The viewindex property is the number of the view, e.g., the first view we capture has viewindex of 0, the second viewindex = 1, etc.
The display attribute allows you to specify a display control to use to capture. For its use see here. The what attribute allows you to specify what part of the display control gui should be captured. For now the IDV only supports what="chart" to capture the time series chart of the station model and data probe displays.
"""
    pass

def movie():
"""
<movie> Create a Quicktime movie, animated GIF or Google Earth KMZ file
<movie 
    file="movie file" 
    view="view name or names to match" 
    imagedir="The directory to place the images." 
    imagetemplate="The file name template to use" 
    imagesuffix="Should be jpg if generating a QuickTime movie but can be gif or png as well" >
The movie tag allows you to capture a time animation as a Quicktime movie, Google Earth KMZ file or as an animated gif.
The view attribute is the same as in the image tag.
If the file ends with .mov then a Quicktime movie is created. If the file ends with .gif then an animated gif is created. If the file ends with .kmz then a Google Earth KMZ file is created each being time stamped. Note: The file attribute can be a comma separated list of files, e.g.:
<movie file="test.mov,test.kmz,test.gif"/>
This allows you to capture multiple types of movie products in one call.
The imagetemplate is a template filename that can contain text and three different macros, e.g.:
imagetemplate="image_%time%"    Include the animation time formatted in the default format
imagetemplate="image_%count%"   Include which image
imagetemplate="image_%count%_%time:any date format%"  Include animation time in any date format. e.g.:
imagetemplate="image_%count%_%time:yyyyMMddHHmm%"
The generic date format can contain a specification that is used by the Java SimpleDateFormat and is described in Basic Tags.
KML Attributes
The movie tag also supports a set of KML specific attributes that allow you to configure the generated KML file.
      <movie
         file="test.kmz"
         kml_desc="<a href="${wwwroot}/${bundle}">Run in the IDV</a> (Needs Java Webstart)"
         kml_name="${label}"
         kml_visibility="0"
         kml_open="0">
kml_desc is the description for the KML Folder that holds the images. It can contain html
kml_name is the folder name.
kml_visibility specifies whether the images are shown initially or not.
kml_open specifies whether the Folder is open or not.
Using your own images
You can also use any number of contained fileset tags to define a custom list of images that are used instead of the default images derived from an animation capture.
<movie file="test.mov">
   <fileset file="image1.png"/>
   <fileset file="image2.png"/>
   <fileset file="image3.png"/>
</movie>
"""
    pass

def html():
"""
<html> Create an image from rendered html
<html 
    file="output image file" 
    fromfile="Optional name of file to read in html" 
    width="Fixed width of image" >
The html tag allows you to specify html that is rendered into an image. It acts just like the image tag in that in can contain image manipulation commands. The html is either from a file (specified by the fromfile attribute) or contained in a CDATA block:
<html file="foo.png">
   <![CDATA[
hello there
<hr>
This is my test
]]>
   <matte bottom="50" background="red"/>
</html>
Images Manipulation
There are a set of tags that can be contained by both the image and movie tags that support processing of the image. Most of these tags work on the initial image and act as a filter pipeline. For example, the ISL:
<image file="test.png">
    <clip north="40" south="30" east="-80" west="-90"/>
    <matte bottom="150"/>
</image>
Will generate an image from the main display. It will then clip the image at the given lat/lon bounding box and then add a matte with spacing of 150 pixels at the bottom of the image. It will then write out the image to the file test.png.
You can modify this behavior in a variety of ways. For example, the ISL:
<image file="test.png">
    <clip north="40" south="30" east="-80" west="-90" copy="true" file="clippedimage.gif"/>
    <matte bottom="150"/>
</image>
will clip the original image but not alter it (the copy="true") and then will write out the clipped image to the given file. The original image will be passed to the matte tag which will matte it.
The image manipulation tags can be nested. e.g. the ISL:
<image file="test.png">
    <clip north="40" south="30" east="-80" west="-90" copy="true" file="clippedimage.gif">
        <matte bottom="150"/>
        <write file="somefile.jpg"/>
        <transparent color="black"/>
    </clip>
    <matte bottom="150"/>
</image>
Will clip a copy of the image, matte the copy, write out the matted image as somefile.jpg, set the color black to be transparent and then write it out as clippedimage.gif. The original image is then matted and written out as test.png.
"""
    pass

def clip():
"""
clip> Clip an image
<clip 
    north="latitude" 
    south="latitude" 
    east="longitude" 
    west="longitude" 
    top="pixels or %" 
    bottom="pixels or %" 
    left="pixels or %" 
    right="pixels or %" 
    display="The id of a display which we use its data's map projection to clip with" 
    space=" when clipping at the box pad outwards this number of pixels" 
    hspace=" when clipping at the box pad horiz." 
    vspace=" when clipping at the box pad vertically" 
    space_left, space_right, space_top, space_bottom="padding" >
The clip tag allows you to clip the image. The clipping can either be defined in lat/lon coordinates, with x/y image coordinates or from the projection used from a display. The image coordinates can be specified as percentages. If no arguments are given then we clip the image at the box.
Clip in image space:
<clip top="10" bottom="0" left="10%" right="0"/>

Clip in lat/lon space:
<clip north="40" south="30" east="-80" west="-90"/>
Note: The view should be in a lat/lon projection and an overhead view for clipping in lat/lon
space to be accurate.


Clip at the 3D box:
<clip/>

Clip at the 3D box with 10 pixel padding
<clip space="10"/>

Clip at the 3D box with horizontal spacing of 10 pixels and vertical spacing of 30
    <clip hspace="10" vspace="30"/>

Clip with left/right/bottom/right spacing
    <clip space_left="5" space_right="40" space_bottom="10" space_top="-10"/>


If there is a display attribute defined then we get the lat/lon bounds of its data to clip with. Use the display control Properties dialog to set the id that you reference in the ISL.
Clip using a display id:
<clip display="displayid"/>
"""
    pass

def matte():
"""
<matte> Matte an image
<matte 
    background="color name or r,g,b" 
    top="pixel spacing" 
    bottom="pixel spacing" 
    left="pixel spacing" 
    right="pixel spacing" 
    space="pixel spacing" 
    hspace="pixel spacing" 
    vspace="pixel spacing" >
The matte tag allows you to add a space around any of the sides of an image. e.g:
<matte top="100" bottom="20"/>
You can also simply specify a space, hspace (horizontal space), or vspace (vertical space) attributes:
<matte space="10"/>

<matte hspace="20"/>

<matte vspace="20"/>
You can also specify a background color. The color can be a named color or a comma separated list of red/green/blue values:
<matte top="100"  background="black"/>
or:
<matte top="100"  background="red,green,blue values"/>
"""
    pass

def overlay():
"""
<overlay> Annotate an image with text or a logo
<overlay 
    image="file or url to image" 
    text="text to draw" 
    place="rectangle point on base image" 
    anchor="rectangle point on overlay" 
    fontsize="font size for text" 
    fontface="font face for text" 
    color="color for text" 
    background="color for background" 
    transparency="transparency percentage 0-1.0" >
The overlay tag allows you to add an icon or text as an image overlay. You can either specify and image or text. The place and anchor tags specify the location of the overlay. They take the form: "rectpoint,offsetx,offsety" Where rectpoint is a point on a rectangle:
   UL    UM    UR
   ML    MM    MR
   LL    LM    LR
   Where U=upper,M=middle,L=lower
   R=right,L=left
The offsetx and offsety are optional. The idea is you define a point on the base image, e.g., the upper left corner ("ul"). Then you define an anchor point on the overlay that will be placed at the place point. So for example, if you wanted the upper left corner of the image overlay to be drawn 10 pixels right and 20 pixels below the upper left corner of the base image then you would do:
   place="UL,10,20" anchor="UL"
If you wanted some text overlay to be placed so that its bottom center was in the middle of the image, 30 pixels from the bottom of the image you do:
   place="LM,0,-30" anchor="LM"
If you wanted some overlay to be placed so that its upper right was placed at the center of the image you do:
   place="M" anchor="UR"
<image file="test.gif">
   ...
  <show message="Here is the image"/>
  ...
</image>
"""
    pass

def latlonlabels():
"""
<latlonlabels> Add lat/lon labels to an image
The latlonlabels tag allows you to add lat/lon labels to an image. Note: For this to be correct your image needs to be in a geographic projection in an overhead view.
<?xml version="1.0" encoding="ISO-8859-1"?>
<isl debug="true" loop="1" offscreen="true" sleep="60.0minutes">
  <bundle clear="true" file="${islpath}/test.xidv" wait="true"/>
  <image file="${islpath}/test.png">
<!-- 
Note: none of these attributes are required and the "xxx" attributes are just commented out

lonvalues/latvalues - Comma separated list of lonvalues and latvalues.

lonlabels/latlabels - An optional list of  labels to use instead of the values

format - A decimal format for formatting the lat/lons if you don't
specify the lonlabels/latlabels.

labelbackground -  if defined, will be the background  color of the labels.

top/bottom/left/right - This is the matte-ing of the image.
If a value is undefined or 0 then the label is shown on the inside of the map image.
If non-zero then the label is shown on the outside of the map image.

background  - background color of the matted border

showleft/showright/showtop/showbottom - controls what labels are shown

drawlonlines/drawlatlines - draw lines across the map

linewidth/linecolor - line attributes

dashes - comma separated list of line segment lengths. format is:
opaque length1, transparent length1,opaque length2, transparent length2,...
defaults to solid line

lineoffsetleft/lineoffsetright/... - offsets when drawing lat/lon lines
defaults to 0

-->

    <latlonlabels 
       lonvalues="-160,-140,-120,-100,-80,-60,-40,-20,0,20,40,60,80,100,120,140,160" 
       xxxlonlabels="a,b,c,d" 
       latvalues="-80,-60,-40,-20,0,20,40,60,80" 
       xxxlatlabels="a,b,c,d" 

       format="##0"
       xxxlabelbackground="white"

       background="gray"
       top="30"
       bottom="0"
       left="30"
       right="0"

       showleft="true"
       showright="true"
       showtop="true"
       showbottom="true"

       drawlonlines="true" 
       drawlatlines="true"
       linewidth="1"
       linecolor="green"
       dashes="2,10"

       lineoffsetleft="0"
       lineoffsetright="0"
       lineoffsettop="0"
       lineoffsetbottom="0"

       />

  </image>
</isl>
latlonlabels.isl
"""
    pass

def displaylist():
"""
<displaylist> Render the display list
<displaylist 
    valign="'bottom' or 'top'" 
    mattebg=" If defined then we matte the image with the given color the size that the display list takes up" 
    fontsize="optional font size" 
    fontface="optional font face" >
The displaylist tag renders the display list (normally shown at the bottom of the view) directly into the image. The text rendering is a bit higher quality because we are using direct Java drawing code and not the 3D rendering. Also, you can matte an image and then render the list of displays in the matted area.
You will want to turn off the visibility of the display list for the view (under View->Show->Display List menu) when you save the bundle.
"""
    pass

def show():
"""
<show> Show the current image in a dialog
<show 
    message="optional message to show" >
You can use the show tag inside an image or movie tag to show the current image in a dialog. This allows you to see what is going on and debug your isl image generation.
"""
    pass

def resize():
"""
<resize> Resize an image
<resize 
    width="pixels or percent" 
    height="pixels or percent" >
The resize tag allows you to resize an image. You specify either a width or a height:
<resize width="200"/>

<resize height="150"/>
The width or height can also be a percentage:
<resize width="50%"/>

<resize height="10%"/>
"""
    pass

def thumbnail():
"""
<thumbnail> Generate a thumbnail of an image
<thumbnail 
    file="image file name" 
    width="pixels or percent" 
    height="pixels or percent" >
The thumbnail tag is just like the resize tag except that it will also write out the image. e.g:
<thumbnail width="50%" file="thumbnail.png"/>
"""
    pass

def write():
"""
<write> Write out an image
<write 
    file="file to write to" >
The write tag allows you to write out an intermediate image file at any time.
    <write file="somefile.png"/>
"""
    pass

def colorbar():
"""
<colorbar> Add a color bar to an image
<colorbar 
    display="optional id of display to use. If not defined then this will use all colorbars found" 
    width="bar width" 
    height="bar height" 
    orientation="tick mark location, 'right', 'left', 'bottom', 'top'" 
    tickmarks="number of tick marks" 
    interval="interval value" 
    values="comma separated list of values" 
    place="rectangle location on image" 
    anchor="rectangle location on colorbar" 
    showlines="'true' or 'false', draw tick lines" 
    suffix="text to add to tick labels" 
    suffixfrequency="'all', 'first', 'last' - frequeny of suffix plotting" 
    showunit="'true' or 'false', use unit as suffix" 
    linecolor="line color" >
The colorbar tag allows you to add a color bar from the color tables in the display controls. Currently, it does not do a perfect job when there are more than one color tables present.
The orientation attribute specifies where the tick marks are drawn, e.g., to the left, right, top or bottom of the color bar. This also implicitly specifies the horizontal (top, bottom) or vertical (right, left) orientation of the color bar. Note, placing a vertical color bar is a bit tricky.
You can specify how ticks are drawn. You can give a number of tickmarks, a value interval or a specific list of values.
The location of the color bar is defined using the anchor and place points as described for the overlay tag.
Here is an example that loads a bundle, creates an image, mattes the image and then shows the color bar of a display with id "planview".
<?xml version="1.0" encoding="ISO-8859-1"?>
<isl debug="true" loop="1" offscreen="true" sleep="60.0minutes">
  <bundle clear="true" file="${islpath}/colorbar.xidv" wait="true"/>
  <image file="${islpath}/colorbar.png">
     <matte space="100" background="gray"/>
     <colorbar display="planview" orientation="top" tickmarks="3" width="400" showlines="true"  anchor="LM" place="UM,0,100" showunit="true"/>
     <colorbar display="planview" orientation="bottom" tickmarks="3" width="400" showlines="true"  anchor="UM" place="LM,0,-100" showunit="true"/>
     <colorbar display="planview" orientation="top" tickmarks="3" width="400" showlines="true"  anchor="LM" place="LM" showunit="true"/>
     <colorbar display="planview" orientation="left" tickmarks="3" width="20" height="400" showlines="true"  anchor="MR" place="ML,100,0" showunit="true"/>
     <colorbar display="planview" orientation="left" tickmarks="3" width="20" height="400" showlines="true"  anchor="MR" place="MR" showunit="true"/>
  </image>
</isl>
colorbar.isl

"""
    pass

def kmlColorbar():
"""
<kmlcolorbar> Write a colorbar into the kmz file
<kmlcolorbar 
    width="width of color bar image" 
    height="height of color bar image" 
    file="file to write color bar image to" 
    space="extra padding around image" 
    suffix="label suffix - can include "%unit%"" 
    kml.name="Name used in kml for this image" 
    kml.overlayXY.x="see below" 
    kml.overlayXY.y="see below" 
    kml.overlayXY.xunits="see below" 
    kml.overlayXY.yunits="see below" 
    kml.screenXY.x="see below" 
    kml.screenXY.y="see below" 
    kml.screenXY.xunits="see below" 
    kml.screenXY.yunits="see below" >
The kmlcolorbar tag acts just like the colorbar tag but is intended to generate a separate color bar image (written to the file specified by the file attribute) and include it into a KMZ file.
All of the kml. attributes are simply passed into the generated KML file and are used to place the image into Google Earth. These attributes are the attributes and tags in the KML. For example, the attribute kml.overlayXY.x ends up being the x attribute of the overlayXY tag in the KML. See here http://code.google.com/apis/kml/documentation/kmlreference.html#screenoverlay for more info on the KML.
Here is an example:
<?xml version="1.0" encoding="ISO-8859-1"?>
<isl debug="true" loop="1" offscreen="true" sleep="60.0minutes">
  <bundle clear="true" file="${islpath}/testtwoview.xidv" wait="true"/>
  <image file="${islpath}/test.kmz">
  <kmlcolorbar  width="400" height="20"
        showlines="true" tickmarks="4"  fontsize="12" background="white" color="black"
        file="${islpath}/testcolorbar.png" space="20" suffix=" %unit%"
        kml.name="Color bar"
        kml.overlayXY.x="0" kml.overlayXY.y="1" kml.overlayXY.xunits="fraction" kml.overlayXY.yunits="fraction"
        kml.screenXY.x="10" kml.screenXY.y="1" kml.screenXY.xunits="pixels" kml.screenXY.yunits="fraction"/>
  </image>
  
</isl>
kmlcolorbar.isl
"""
    pass

def transparent():
"""
<transparent> Set transparency on an image
<transparent 
    color="color to set to transparent" >
The transparent tag allows you to set a particular color in an image to be transparent.
<transparent color="black"/>
or:
<transparent color="red,green,blue"/>
"""
    pass

def backgroundTransparent():
"""
The backgroundtransparent tag allows you to use the background color of the view as the transparent color in the image
<backgrountransparent/>
"""
    pass

def split():
"""
<split> Split an image into sub-images
<split 
    file="base file name" 
    columns="number of columns" 
    rows="number of rows" >
The split tag splits up an image by a given number of rows and columns. The file attribute should contain properties for each image: ${row}, ${column} and ${count}.
"""
    pass

def output():
"""
<output> Write output to file
<output 
    file="the file to write to" 
    template="inline text of 'file:filename' of the template file" 
    fromfile="filename to read contents from" 
    text="text to output" >
The output tag allows you to generate text files. For example, this could be used to generate html pages, xml files, etc.
The output tag is used in two modes. First it is used to define the output file name, and possibly some templates. Then, the output tag is used to write text output into that file.
You can write text a number of ways:
<isl>
    <output file="output.txt">
        <output text="Some text to write"/>
<!-- Read in the contents of the file. Apply properties to the name and the contents -->
        <output fromfile="${islpath}/file.txt"/>

        <output>Some more text</output>
        <output><![CDATA[
Some text in a CDATA Section. Thi allows you to have " and < without 
escaping the in the xml
        >]]></output>
   </output>

</isl>
To be even more complicated the output tag can specify templates to write into. For example, in output1.isl below, we are writing an output1.txt file. Its template has some text and a ${text} macro. The text macro corresponds to the text that gets written into the template:text template. The two following output tags use the template:text template, writing in the values of the thetext attribute.
<isl>
    <output 
        file="output1.txt" 
        template="Here is the text: ${text}"
        template:text="${thetext} ">

        <output template="text" thetext="The text I am writing"/>

        <output template="text" thetext="More text I am writing"/>

   </output>

</isl>
output1.isl
The result of running this is:
Here is the text: The text I am writing More text I am writing 
"""
    pass