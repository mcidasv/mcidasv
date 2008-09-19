from visad.python.JPythonMethods import *
def cloudFilter(sdataset1,sdataset2,replace=0,default=0):
  """ cloud filter from McIDAS-X """
  newData1=sdataset1.clone()
  newData2=sdataset2.clone()
  
  for t in range(newData1.getDomainSet().getLength()):
     rangeObject1 = newData1.getSample(t)
     vals1 = rangeObject1.getFloats(0)
     rangeObject2 = newData2.getSample(t)
     vals2 = rangeObject2.getFloats(0)
     domain=GridUtil.getSpatialDomain(rangeObject1)
     x=domain.getLinear1DComponent(0)
     y=domain.getLinear1DComponent(1)
                  
     [element_size,line_size]=domain.getLengths()
     for i in range(len(y)):
        for j in range(len(x)):
           line1 = vals1[0][j*line_size+i]
           line2 = vals2[0][j*line_size+i]
           if (line1 < line2 + default):
              line1 = replace
           vals1[0][j*line_size+i] = line1
     
     rangeObject1.setSamples(vals1,1)

  return newData1

def replaceFilter(sdataset,user_replaceVal=0,user_bline=0,user_eline=999999,user_belem=0,user_eelem=999999):
  newData = sdataset.clone()
  replaceVal=int(user_replaceVal)
  bline=int(user_bline)
  eline=int(user_eline)
  belem=int(user_belem)
  eelem=int(user_eelem)  
  
  for t in range(newData.getDomainSet().getLength()):
     rangeObject = newData.getSample(t)
     vals = rangeObject.getFloats(0)
     domain=GridUtil.getSpatialDomain(rangeObject)
     x=domain.getLinear1DComponent(0)
     y=domain.getLinear1DComponent(1)
                  
     [element_size,line_size]=domain.getLengths()
     for i in range(len(y)):
        for j in range(len(x)):
           line = vals[0][j*line_size+i]
           if ((j >= belem and j <= eelem) and (i >= bline and i <= eline)):
              line=replaceVal
           vals[0][j*line_size+i] = line
     
     rangeObject.setSamples(vals,1)

  return newData

def cleanFilter(sdataset,user_replace='Average',user_bline=0,user_eline=999999,user_pdiff=15,user_ldiff=15):
   newData=sdataset.clone()
   return newData

def shotFilter(sdataset,bline=0,eline=999999,filter_diff=15):
   """ shot filter from McIDAS-X - also called from clean filter """  
   
   newData = sdataset.clone()
      
   for t in range(newData.getDomainSet().getLength()):
     rangeObject = newData.getSample(t)
     vals = rangeObject.getFloats(0)
     domain=GridUtil.getSpatialDomain(rangeObject)
              
     [element_size,line_size]=domain.getLengths()
     
     for i in range(line_size):
       for j in range(element_size)[1:-2]:
         left = vals[0][i*element_size + j - 1]
         value = vals[0][i*element_size + j]
         right = vals[0][i*element_size + j + 2]
      
         left_diff = value - left
         right_diff = value - right
         sign = left_diff *right_diff
         if (sign < 0):
           continue
         
         left_diff = abs(left_diff)
         if (left_diff < filter_diff):
            continue

         right_diff = abs(right_diff)
         if (right_diff < filter_diff):
           continue
         
         vals[0][i*element_size + j] = (left + right)/2
            
     rangeObject.setSamples(vals,1)
   
   return newData

def shotFilter2(sdataset,bline=0,eline=999999,filter_diff=15, radius=5):
   """ shot filter #2 from McIDAS-X - called from clean filter with radius argument """  
   
   newData = sdataset.clone()
      
   for t in range(newData.getDomainSet().getLength()):
     rangeObject = newData.getSample(t)
     vals = rangeObject.getFloats(0)
     domain=GridUtil.getSpatialDomain(rangeObject)
     x=domain.getLinear1DComponent(0)
     y=domain.getLinear1DComponent(1)
              
     [element_size,line_size]=domain.getLengths()
     
     for i in range(len(y)):
       print 'bob'
       if (i > 1):
          return newData 
       for j in range(len(x))[1:-1]:
         getNextElement=0
         next = i + 1
         value = vals[0][j*line_size + i]
                  
         if (value == 0):
            for k in range(radius):
               next = i + k
               if (next > element_size):
                 getNextElement=1
                 break
               newVal = vals[0][j*line_size + next]
               if (newVal != 0 ):
                 break
            if (k == radius):
              print 'Here 2'
              getNextElement = 1
         
         if (getNextElement):
            continue
           
         left = vals[0][j*line_size + i - 1]         
         right = vals[0][j*line_size + next]
                                 
         left_diff = value - left
         right_diff = value - right
         
         ave_sum = 0
         ave_div = 0

         sign = left_diff *right_diff
         if (sign < 0):
           continue
         
         left_diff = abs(left_diff)
         if (left_diff > filter_diff):
	   ave_div = ave_div+1
           ave_sum = ave_sum + left

         right_diff = abs(right_diff)
         if (right_diff > filter_diff):
	   ave_div = ave_div+1
           ave_sum = ave_sum + right
         
         print value, left, right, left_diff, right_diff, ave_div, ave_sum
         if (ave_div > 0):
           vals[0][i*line_size + i] = ave_sum / ave_div
            
     rangeObject.setSamples(vals,1)
   
   return newData

def spotFilter(sdataset,omcon=0,oacon=0,imcon=0,iacon=0,cmin=0,cmax=0):
   """ spot filter from McIDAS-X """
   newData = sdataset.clone()
   return newData

def coreFilter(sdataset1,sdataset2,brkpnt1,brkpnt2,replace1,replace2):
   """ core filter from McIDAS-X """
   newData1=sdataset1.clone()
   newData2=sdataset2.clone()
   return newData1

def gradientFilter(sdataset):
   """ gradient filter from McIDAS-X """
   newData=sdataset.clone()
   for t in range(newData.getDomainSet().getLength()):
     rangeObject = newData.getSample(t)
     vals = rangeObject.getFloats(0)
     in_hi = int(max(max(vals)))
     in_low = int(min(min(vals))) 
     print 'min/max=', in_low, in_hi
     domain=GridUtil.getSpatialDomain(rangeObject)
                   
     [element_size,line_size]=domain.getLengths()

     for i in xrange(line_size):
       for j in range(element_size)[:-1]:
          vals[0][i*element_size + j] = int(abs(vals[0][i*element_size + j] - vals[0][i*element_size + j + 1]))
          
       """ set last value to zero """
       vals[0][i*element_size + j + 1] = 0
     
     post_hi = int(max(max(vals)))
     post_low = int(min(min(vals))) 
     print 'post-processed min/max=',post_low,post_hi  
     lookup=contrast(post_low,post_hi,post_low,post_hi)
     vals=modify(vals,element_size,line_size,post_low,lookup) 
     rangeObject.setSamples(vals)

   return newData 

def passFilter(sdataset,user_passname,user_radius=50,user_leak=100):
   newData = sdataset.clone()
   radius = int(user_radius)
   leak = int (user_leak)

   ntot = 2*radius + 1
   nmod = radius + 1
    
   for t in xrange(len(newData)):
     rangeObject = newData.getSample(t)
     vals = rangeObject.getFloats()
     in_hi = int(max(max(vals)))
     in_low = int(min(min(vals))) 
     midpoint = (in_hi - in_low)/2 + in_low
     print 'minimum=',in_low,in_hi
     domain=GridUtil.getSpatialDomain(rangeObject)
            
     [element_size,line_size]=domain.getLengths()
     print element_size,line_size
     radiusArray=range(nmod)
     
     for i in xrange(line_size):
       nr = 1+radius
       if ( nr > element_size):
          nr = element_size
       k=vals[0][i*element_size: i*element_size + nr].tolist()
       for p in range(len(k)):
         k[p]=int(k[p])
       
       radiusArray=radius*[int(vals[0][i*element_size])] + k
       nsum = sum(radiusArray)
       
       nright=radius
       for j in xrange(element_size):
          curVal=vals[0][i*element_size + j]
          average = int((leak * nsum)/(100 * ntot))
          
          if (user_passname.startswith('High')):
             vals[0][i*element_size + j] =  curVal - average + midpoint
 
          if (user_passname.startswith('Low')):
             vals[0][i*element_size + j] = average
          
          """ move the radius array one element to the right and recalculate the sum """
          radiusArray.pop(0)
          nright=nright + 1
          mright=nright
          if (mright > element_size-1):
            mright = element_size-1
          radiusArray.append(int(vals[0][i*element_size + mright]))
          nsum = sum(radiusArray)
            
     post_hi = int(max(max(vals)))
     post_low = int(min(min(vals))) 
     print 'post-processed min/max=',post_low,post_hi  
     lookup=contrast(post_low,post_hi,post_low,post_hi)
     vals=modify(vals,element_size,line_size,post_low,lookup) 
     rangeObject.setSamples(vals)

   return newData

def lowPass2DFilter(sdataset,user_linecoef=0.5,user_elecoef=0.5):
   """ 2 dimensional low pass filter from McIDAS-X """
   newData = sdataset.clone()
   lcoef = float(user_linecoef)
   ecoef = float(user_elecoef)
   l1 = 1.0 - lcoef
   e1 = 1.0 - ecoef 

   for t in xrange(newData.getDomainSet().getLength()):
     rangeObject = newData.getSample(t)
     vals = rangeObject.getFloats(0)
     domain=GridUtil.getSpatialDomain(rangeObject)
     [element_size,line_size]=domain.getLengths()
     print element_size,line_size   

     """ save the first line """
     firstLine = vals[0][0:element_size].tolist()
     
     for i in xrange(line_size):
       """ left to right filter along line """
       val = vals[0][i*element_size]
       for j in range(element_size):
          if (vals[0][i*element_size + j] > 0):
            val = ecoef*val + e1 * vals[0][i*element_size + j]
          vals[0][i*element_size + j] = round(val)
          if ( i == 5):
            print round(val), j
       """ right to left filter along line """
       val = vals[0][i*element_size + (element_size - 1)]
       if (i == 5):
          print 'Initial val=', val
       for j in xrange(element_size - 1, 0, -1):
         val=ecoef*val + e1 * vals[0][i*element_size + j]
         vals[0][i*element_size + j] = round(val)
         if (i == 5):
           print 'Return val=', val, j 
       """ filter along the elements """
       for j in xrange(element_size):
         val = lcoef * firstLine[j] + l1 * vals[0][i*element_size + j]
         if (i == 5):
           print 'Element val=', val, j, firstLine[j], vals[0][i*element_size + j]
         vals[0][i*element_size + j] = round(val) 

     """ filter along the lines going through the image up the elements """
     """ save the last line """
     """ print (line_size -1) *element_size, (line_size -1)*element_size + (element_size -1)
     lastLine = vals[0][(line_size)*element_size : (line_size)* (element_size) + (element_size)].tolist()
     print lastLine
     for i in xrange(line_size , 0, -1):
        for j in range(element_size):
          val = lcoef * lastLine[j] + l1 * vals[0][i*element_size + j]
          vals[0][i*element_size + j] = round(val) """

     post_hi = int(max(max(vals)))
     post_low = int(min(min(vals))) 
     print 'post-processed min/max=',post_low,post_hi  
     lookup=contrast(post_low,post_hi,post_low,post_hi)
     vals=modify(vals,element_size,line_size,post_low,lookup) 
     rangeObject.setSamples(vals)

   return newData

def highPass2DFilter(sdataset):
   """ 2 dimensional high pass filter from McIDAS-X """
   newData = sdataset.clone()
   
   for t in xrange(newData.getDomainSet().getLength()):
     rangeObject = newData.getSample(t)
     vals = rangeObject.getFloats(0)
     domain=GridUtil.getSpatialDomain(rangeObject)
     [element_size,line_size]=domain.getLengths()
    
     firstLine=vals[0][0:element_size] 
     """ do the filter using 3 lines at a time """
     for i in range(line_size)[:-2]:
       for j in range(element_size)[1:-1]:
         midValue = vals[0][(i+1)*element_size + j]
         
         val = (vals[0][(i+2)*element_size + j] + vals[0][i*element_size + j] + \
               vals[0][(i+1) * element_size + j + 1] + vals[0][(i+1)*element_size + j - 1]) - \
               4*midValue
         
         if (val < midValue):
            vals[0][i * element_size + j] = midValue - val
         else:
            vals[0][i * element_size + j] = 0
         if (i == 405):
            print 'values are ', i, j, vals[0][i*element_size + j]

     vals[0] = firstLine + vals[0][0:(line_size-1)*element_size]
     post_hi = int(max(max(vals)))
     post_low = int(min(min(vals))) 
     print 'post-processed min/max=',post_low,post_hi  
     """ lookup=contrast(post_low,post_hi,post_low,post_hi)
     vals=modify(vals,element_size,line_size,post_low,lookup) """
     rangeObject.setSamples(vals)

   return newData

def holeFilter(sdataset,brkpoint1=0,brkpoint2=255):
  """ hole filter from McIDAS-X - searches for missing data and fills the holes """
  data = sdataset.clone()
  for t in xrange(data.getDomainSet().getLength()):
     rangeObject=data.getSample(t)
     vals=rangeObject.getFloats(0)
     brkpoint1 = min(min(vals))
     brkpoint2 = max(max(vals))
     domain=GridUtil.getSpatialDomain(rangeObject)
     [element_size,line_size]=domain.getLengths()
  
  return data

def contrast(in_low,in_hi,minimum,maximum,out_low=0,out_hi=255,inc=1):
   """ create a contrast stretch lookup table """
   if (in_hi == in_low):
      slope=0
   else:
      slope=float(out_hi - out_low)/float(in_hi - in_low)
   print 'slope =', out_hi, out_low, in_hi, in_low, slope, maximum, minimum

   lookup = []
   for input_value in xrange(minimum, maximum + 1, inc):
      out_value = out_low + int(round((input_value - in_low)*slope))
      if (slope < 0):
        out_value=max(min(out_value,out_low),out_hi)
      else:
        out_value=min(max(out_value,out_low),out_hi)
      
      indx=input_value - minimum
      print indx, input_value, inc, out_value
      for i in xrange(inc):
         lookup.insert(indx, out_value)
         indx = indx + 1
   
   print lookup
   return lookup

def modify(vals,element_size,line_size,minimum,lookup):
  """ modifies an image with the lookup table generated by a stretch function """
  print line_size, element_size
  for i in xrange(line_size):
     for j in xrange(element_size):
        """ print i, j, minimum, vals[0][i*element_size+j] - minimum + 1, lookup[int(vals[0][i*element_size+j]-minimum -1)], len(lookup) """
        vals[0][i*element_size + j] = lookup[int(vals[0][i*element_size+j] - minimum)]

  return vals

def printValueDiff(sdataset1,sdataset2):
  data1=sdataset1.clone()
  data2=sdataset2.clone()

  for t in xrange(data1.getDomainSet().getLength()):
    rangeObj1=data1.getSample(t)
    rangeObj2=data2.getSample(t)
    vals1=rangeObj1.getFloats(0)
    vals2=rangeObj1.getFloats(0)
    domain=GridUtil.getSpatialDomain(rangeObj1)
    [element_size,line_size]=domain.getLengths()

    for i in xrange(line_size):
      for j in xrange(element_size):
         print i, j, vals1[0][i*element_size + j] - vals2[0][i*element_size+j]