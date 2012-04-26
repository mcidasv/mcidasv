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
#          saves an output file
#
#     Setting up a variable to specify the location of your final images
#     makes your script easier to read and more portable when you share it
#     with other users.
#

myuser='username'

#
#     Windows XP example
#

imageDir=('C:\\Documents and Settings\\'+myuser+'\\McIDAS-V\\')

#
#     UNIX example
#
#imageDir=('/home/'+myuser+'/McIDAS-V/')


#
#     OS X example
#
#imageDir=('/Users/'+myuser+'/McIDAS-V/')


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
#     Now make the request using the function getADDEImage.
#     This returns metadata and data objects.
#
ir_metadata,ir_data = getADDEImage(descriptor='G7-IR-4K',
    band=8,
    **adde_parms)

# 
#     Create some strings from the metadata object to make it 
#     easier to build our window and label the image.
#
ir_lines = ir_metadata['lines']
ir_eles = ir_metadata['elements']
ir_label = '%s %s' % (
    ir_metadata['sensor-type'],
    ir_metadata['nominal-time']
    )


#     
#     Build a window with a single panel 
#
panel = buildWindow(height=ir_lines,width=ir_eles)


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
#     Apply an enhancement (layer)
#     Add a layer label (layer)
#     Save the output file (panel)
#
panel[0].setProjection('US>CONUS')
panel[0].setWireframe(False)
panel[0].setCenter(35.5,-75.5, scale=1.0)
ir_layer.setLayerLabel(label=ir_label, size=16)
ir_layer.setEnhancement('Longwave Infrared Deep Convection')
panel[0].captureImage(imageDir+'IR-Blizzard.jpg',height=ir_lines,width=ir_eles)
