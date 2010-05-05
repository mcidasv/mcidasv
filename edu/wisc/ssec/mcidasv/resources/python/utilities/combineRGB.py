from visad import FieldImpl, Data

uniqueID = 0

def mycombineRGB(red, green, blue):
  global uniqueID
  uniqueID += 1
  red=GridUtil.setParamType(red,makeRealType("redimage%d" % uniqueID), 0)
  green=GridUtil.setParamType(green,makeRealType("greenimage%d" % uniqueID),0)
  blue=GridUtil.setParamType(blue,makeRealType("blueimage%d" % uniqueID),0)
  return DerivedGridFactory.combineGrids([red,green,blue],1)
