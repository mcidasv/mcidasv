def makeNavigatedImage (d,ulLat,ulLon,lrLat,lrLon):
  """This takes a image data object and a lat/lon bounding box
     and adds a lat/lon domain to the data. Use it in conjunction with a formula:
  """
  from visad import Linear2DSet 
  from visad import RealTupleType
  ulLat=float(ulLat)
  ulLon=float(ulLon)
  lrLat=float(lrLat)
  lrLon=float(lrLon)
  domain = d.getDomainSet()
  newDomain = Linear2DSet(RealTupleType.SpatialEarth2DTuple,ulLon,lrLon,domain.getX().getLength(),ulLat,lrLat,domain.getY().getLength())
  return GridUtil.setSpatialDomain(d, newDomain)


def combineRGB(red, green, blue):
  """ combine 3 images as an RGB image """
  red=GridUtil.setParamType(red,makeRealType("redimage"), 0)
  green=GridUtil.setParamType(green,makeRealType("greenimage"), 0)
  blue=GridUtil.setParamType(blue,makeRealType("blueimage"), 0)
  return DerivedGridFactory.combineGrids((red,green,blue),1)


def combineABIRGB(chP64,chP86,chP47):
  """ GOES16/17 combine 3 images as an RGB image """
  green =  0.45*chP64 + 0.45*chP47 + 0.1*chP86
  red = GridUtil.setParamType(chP64,makeRealType("redimage"), 0)
  blue = GridUtil.setParamType(chP47,makeRealType("blueimage"), 0)
  grn = GridUtil.setParamType(green,makeRealType("greenimage"), 0)
  return DerivedGridFactory.combineGrids((red,grn,blue),1)


def CLAVRxCloudTypeRGB(b4R, b2R, b5R):
  # red = band 4: 1.378 um reflectance; 0% to 10% rescaled to 0 to 255; gamma 1.5
  # grn = band 2: 0.64 um reflectance; 0% to 100% rescaled to 0 to 255; gamma 1.0
  # blu = band 5: 1.61 um reflectance; 0% to 60% rescaled to 0 to 255; gamma 1.0
  # CIRRUS: red, OPAQUE ICE: yellow, WATER CLOUDS: cyan,
  # SNOW: green, LOAFTED WATER CLOUD: white

  red_range = [0,10]
  green_range = [0,100]
  blue_range = [0,60]

  r = 255*(rescale(b4R, 0, red_range[1], 0, 1)**(1/1.5))
  g = 255*(rescale(b2R, 0, green_range[1], 0, 1))
  b = 255*(rescale(b5R, 0, blue_range[1], 0, 1))
  
  return combineRGB(r, g, b)
