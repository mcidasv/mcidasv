#
#    Here is an example of a McIDAS-V script that does the following:
#          sets up parameters for an ADDE request 
#          makes an adde request
#          creates a window with one panel
#          displays the data
#          changes the projection
#          applies an enhancement table
#          changes the center point
#          adds a layer label
#          annotates the image with an "L" for a Low pressure symbol
#          saves an output file
#
#     Setting up a variable to specify the location of your final images
#     makes your script easier to read and more portable when you share it
#     with other users.
#

import os
#
#     Setting up a variable to specify the location of your final images
#     makes your script easier to read and more portable when you share it
#     with other users
#

homePath=expandpath('~')
imageDir=os.path.join(homePath,'McIDAS-V')


#
#     The easiest way to make an ADDE request is to create a dictionary
#     that defines your parameters.  Here we have a generic request:
# 
adde_parms = dict(
    debug=True,
    server='pappy.ssec.wisc.edu',
    dataset='BLIZZARD',
    size='ALL',
    mag=(1, 1),
    time=('18:01:00', '18:01:00'),
    day=('1993072'),
    unit='BRIT',
)


#
#     Now make the request using the function loadADDEImage.
#     This returns one object containing the data and metadata.
#
ir_data = loadADDEImage(descriptor='G7-IR-4K',band=8,**adde_parms)

# 
#     Create some strings from the object to make it 
#     easier to build our window and label the image.
#
bw_lines = ir_metadata['lines']/2
bw_eles = ir_metadata['elements']/2
ir_label = '%s %s' % (
    ir_metadata['sensor-type'],
    ir_metadata['nominal-time']
    )


#     
#     Build a window with a single panel 
#
panel = buildWindow(height=bw_lines,width=bw_eles)


#
#     Create a layer from the infrared data object 
#
ir_layer = panel[0].createLayer('Image Display', ir_data)


#
#     When changing attributes, some are panel based and
#     others are layer based.  In the following steps, they are:
# 
#     Change the projection (panel)
#     Turn off the wire frame box (panel)
#     Change the center point (panel)
#     Add an L to pinpoint the Low (panel)
#     Add a layer label (layer)
#     Apply an enhancement (layer)
#     Save the output file (panel)
#
panel[0].setProjection('US>CONUS')
panel[0].setWireframe(False)
panel[0].setCenter(35.5,-75.5, scale=1.0)
panel[0].annotate('<b>L</b>', line=353,element=398, size=24, color='Black')
panel[0].annotate('<b>L</b>', line=351,element=396, size=24, color='Red')
ir_layer.setLayerLabel(label=ir_label, size=14)
ir_layer.setEnhancement('Longwave Infrared Deep Convection')
fileName=os.path.join(imageDir,'IR-Blizzard.jpg')
panel[0].captureImage(fileName)