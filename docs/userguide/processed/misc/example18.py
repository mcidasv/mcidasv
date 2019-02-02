#
#    Here is an example of a McIDAS-V script that does the following:
#          sets up parameters for an ADDE request 
#          makes an adde request
#          creates a window with one panel
#          displays the data
#          applies an enhancement table
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
    server='adde.ucar.edu',
    dataset='RTGOESR',
    size='ALL',
    mag=(1, 1),
    unit='TEMP',
)


#
#     Now make the request using the function loadADDEImage.
#     This returns one object containing the data and metadata.
#
ir_data = loadADDEImage(descriptor='M1C14',**adde_parms)

# 
#     Create some strings from the object to make it 
#     easier to build our window and label the image.
#
bw_lines = ir_data['lines']/2
bw_eles = ir_data['elements']/2
ir_label = '%s %s' % (
    ir_data['sensor-type'],
    ir_data['nominal-time']
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
#     Turn off the wire frame box (panel)
#     Apply an enhancement (layer)
#     Save the output file (panel)
#

panel[0].setWireframe(False)
ir_layer.setEnhancement('ABI IR Temperature', range=(220,300))
fileName=os.path.join(imageDir,'G16-IR.jpg')
panel[0].captureImage(fileName)