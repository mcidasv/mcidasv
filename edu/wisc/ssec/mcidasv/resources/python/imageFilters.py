def cloudFilter(sdataset1,sdataset2,user_replace='Default',user_constant=0,user_stretchval='Contrast'):
  """ 
      cloud filter from McIDAS-X - requires 2 source datasets
      user_replace: replacement value  (default=minimum value in either sdataset1 or sdataset2)
      user_constant: additive constant (default=0)
  """
  
  data1=sdataset1.clone()
  data2=sdataset2.clone() 
  replace=user_replace
  constant=int(user_constant)
  if (replace != 'Default'):
     replace=int(replace)
  stretch=user_stretchval 

  for t in range(data1.getDomainSet().getLength()):
     range1 = data1.getSample(t)
     vals1 = range1.getFloats(0)
     min1= min(vals1[0])
     range2 = data2.getSample(t)
     vals2 = range2.getFloats(0)
     min2= min(vals2[0]) 
     if (replace == 'Default'):
        replace=min([min1, min2])
     domain=GridUtil.getSpatialDomain(range1)
     [element_size,line_size]=domain.getLengths()
     
     for i in xrange(line_size):
        for j in xrange(element_size):
           line1 = vals1[0][i*element_size+j]
           line2 = vals2[0][i*element_size+j] 

           if (line1 <= line2 + constant):
             vals1[0][i*element_size+j] = replace
                
     post_hi = int(max(vals1[0]))
     post_low = int(min(vals1[0])) 
     if (stretch == 'Contrast'):
       lookup=contrast(post_low,post_hi,post_low,post_hi)
     elif (stretch == 'Histogram'):
       h = hist(field(vals1),[0],[post_hi-post_low])
       lookup=histoStretch(post_low,post_hi,h)
     
     vals1=modify(vals1,element_size,line_size,post_low,lookup) 
     range1.setSamples(vals1)

  return data1

def replaceFilter(sdataset,user_replaceVal=0,user_bline='Default',user_eline='Default',user_belem='Default',user_eelem='Default',user_sourceval='Default',user_stretchval='Contrast'):
  """ 
      replace filter from McIDAS-X 
      user_replace : replacement value  (default=0)
      user_bline   : beginning line in the source image region (default=first line)
      user_eline   : ending line in the source image region    (default=last line)
      user_belem   : beginning element in the source image region (default=first element)
      user_eelem   : ending element in the source image region    (default=last element)
      user_sourceval: source image values in the region to replace user_replace; specify values
                      in the list format, e.g. val1 val2 val3 etc., 
                      or a range format, e.g. bval-eval (default=0-255)
  """  
  newData = sdataset.clone()
  replaceVal=int(user_replaceVal)
  bline=user_bline
  eline=user_eline   
  if (bline != 'Default'):
    bline=int(bline)
  else:
    bline=0
  if (eline != 'Default'):
     eline=int(eline)
  belem=user_belem
  eelem=user_eelem   
  if (belem != 'Default'):
    belem=int(belem)
  else:
    belem=0
  if (eelem != 'Default'):
     eelem=int(eelem)
  stretch=user_stretchval
  
  """
     sourceVal can either be specified in list format: val1 val2 val3
                  or in a range format, bval-eval (default = 0-255)
  """
  if (user_sourceval != 'Default'):   
     if '-' in user_sourceval:
         tempVal1 = [ int(m) for m in user_sourceval.split('-')]
         tempVal = range(tempVal1[0],tempVal1[1] + 1)
     else:
         tempVal = user_sourceval.split()
  
  else:
     tempVal = range(0, 256)
  
  sourceVal = [ float(m) for m in tempVal ]
           
  for t in range(newData.getDomainSet().getLength()):
     rangeObject = newData.getSample(t)
     vals = rangeObject.getFloats(0)
     domain=GridUtil.getSpatialDomain(rangeObject)
                      
     [element_size,line_size]=domain.getLengths()
     if (eline == 'Default'):
         eline=line_size
     if (eelem == 'Default'):
         eelem=element_size
             
     for i in range(line_size)[bline:eline]:
        for j in range(element_size)[belem:eelem]:
           line = vals[0][i*element_size+j]
           """ if ((j >= belem and j <= eelem) and (i >= bline and i <= eline) or (line in sourceVal)): """
           if (line in sourceVal):
              line=replaceVal
           vals[0][i*element_size+j] = line
     
     post_hi = int(max(vals[0]))
     post_low = int(min(vals[0])) 
     if (stretch == 'Contrast'):
       lookup=contrast(post_low,post_hi,post_low,post_hi)
     elif (stretch == 'Histogram'):
       h = hist(field(vals),[0],[post_hi-post_low])
       lookup=histoStretch(post_low,post_hi,h)
       
     vals1=modify(vals,element_size,line_size,post_low,lookup) 
     rangeObject.setSamples(vals1)

  return newData

def shotMain(vals,bline,eline,element_size,line_size,filter_diff):
   """ the actual shot filter code - needs to be separate as clean filter calls it as well """
   for i in range(line_size)[bline:eline]:
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
   
   return vals

def badLineFilter(vals,bline,eline,element_size,line_size,filter_fill,line_diff,min_data,max_data):
    """ 
       The bad line filter used by the clean filter
       Lines are bad if 
       1. all the values in the line are the same
       2. the average difference is greater than the supplied limit (line_diff argument)  
    """
    num_badlines = 0
    
    for i in range(line_size)[bline:eline - 1]:
       cur_line = vals[0][i*element_size:(i+1)*element_size]
       min_line = min(cur_line)
       max_line = max(cur_line)
       if (i == bline):
           good_line = cur_line
           continue
       
       diff=0
       
       for ii in range(element_size):
         diff = diff + abs(cur_line[ii]-good_line[ii])
         
       isbad = 0
       if ( (max_line - min_line) == 0 ):
          print 'Bad Line - same values'
          isbad = 1
          num_badlines = num_badlines + 1
          """ store the last good line """
          if (num_badlines == 1):
              last_good_line = good_line
       else:
           ave_diff = diff/element_size
                         
           if ( ave_diff > line_diff ):
             print 'Bad Line - line_diff exceeded'
             isbad = 1
             num_badlines = num_badlines + 1
             if (num_badlines == 1):
                 last_good_line = good_line
       
       if (isbad == 0):
           good_line = cur_line
           if (num_badlines > 0):
               for j in range(num_badlines):
                   fdiv = (1.0/float(num_badlines+1)) * float(j+1)  
                   new_line=(i - num_badlines) + j
                   
                   for k in range(element_size):
                     if ( filter_fill == 'Min' ):
                        vals[0][new_line*element_size + k] = min_line
                     elif ( filter_fill == 'Average' ):
                        ave_diff = int((last_good_line[k] - good_line[k]) * fdiv)
                        vals[0][new_line*element_size + k] = good_line[k] + ave_diff
                     elif (filter_fill == 'Max' ):
                        vals[0][new_line*element_size + k] = max_line
                   good_line=vals[0][new_line*element_size:(new_line+1)*element_size] 
                   
               num_badlines = 0         
                                       
    return vals

def cleanFilter(sdataset,user_fill='Average',user_bline='Default',user_eline='Default',user_pdiff=15,user_ldiff=15,user_stretchval='Contrast'):
   """ clean filter from McIDAS-X
       user_fill    - 'Average': average of surrounding values (default)
                    - 'Min'    : source dataset minimum value
                    - 'Max'    : source dataset maximum value
       user_bline   - beginning line in the source image to clean (default=first line)
       user_eline   - ending line in the source image to clean (default = last line)
       user_pdiff   - absolute difference between an element's value and value of the element on either side 
       user_ldiff   - percentage difference between a line's average value and the average value of
                      the line above and below
   """   
   newData=sdataset.clone()
   
   filter_fill = user_fill
   bline=user_bline
   eline=user_eline   
   if (bline != 'Default'):
     bline=int(bline)
   else:
     bline=0
   if (eline != 'Default'):
     eline=int(eline)
     
   filter_diff=int(user_pdiff)
   l_diff=int(user_ldiff)
   stretch=user_stretchval
   
   for t in range(newData.getDomainSet().getLength()):
     rangeObject = newData.getSample(t)
     vals = rangeObject.getFloats(0)
     high = max(vals[0])
     low = min(vals[0])
     point_diff = (high - low + 1)*(filter_diff/100.0)
     line_diff = (high - low + 1)*(l_diff/100.0) 
     domain=GridUtil.getSpatialDomain(rangeObject)  
     [element_size,line_size]=domain.getLengths()
     if (eline == 'Default'):
       eline=line_size 
     
     vals = shotMain(vals,bline,eline,element_size,line_size,point_diff)
     vals = badLineFilter(vals,bline,eline,element_size,line_size,filter_fill,line_diff,low,high)
     post_hi = int(max(vals[0]))
     post_low = int(min(vals[0])) 
     if (stretch == 'Contrast'):
       lookup=contrast(post_low,post_hi,post_low,post_hi)
     elif (stretch == 'Histogram'):
       h = hist(field(vals),[0],[post_hi-post_low])
       lookup=histoStretch(post_low,post_hi,h)
       
     vals=modify(vals,element_size,line_size,post_low,lookup)   
     rangeObject.setSamples(vals)
        
   return newData

def shotFilter(sdataset,user_bline='Default',user_eline='Default',user_pdiff=15,user_stretchval='Contrast'):
   """ shot noise filter from McIDAS-X
       bline - beginning line in the source image to clean (default=first line)
       eline - ending line in the source image to clean (default = last line)
       pdiff - maximum percentage of the product range to allow before a new value for the pixel is derived using the
               average of two adjacent pixels
   """    
   newData = sdataset.clone()
   bline=user_bline
   eline=user_eline   
   if (bline != 'Default'):
     bline=int(bline)
   else:
     bline=0
   if (eline != 'Default'):
     eline=int(eline)
     
   filter_diff=int(user_pdiff)
   stretch=user_stretchval
   
   for t in range(newData.getDomainSet().getLength()):
     rangeObject = newData.getSample(t)  
     vals = rangeObject.getFloats(0)
     high = max(vals[0])
     low = min(vals[0])
     """ print high, low, max(vals[0]), min(vals[0]) """
     point_diff = (high - low + 1)*(filter_diff/100.0)    
     domain=GridUtil.getSpatialDomain(rangeObject)  
     [element_size,line_size]=domain.getLengths()
     if (eline == 'Default'):
       eline=line_size 
     
     vals = shotMain(vals,bline,eline,element_size,line_size,point_diff)
     post_hi = int(max(vals[0]))
     post_low = int(min(vals[0])) 
     if (stretch == 'Contrast'):
       lookup=contrast(post_low,post_hi,post_low,post_hi)
     elif (stretch == 'Histogram'):
       h = hist(field(vals),[0],[post_hi-post_low])
       lookup=histoStretch(post_low,post_hi,h)
       
     vals1=modify(vals,element_size,line_size,post_low,lookup) 
     rangeObject.setSamples(vals1)
   
   return newData

def spotFilter(sdataset,omcon=0,oacon=0,imcon=0,iacon=0,cmin=0,cmax=0,user_stretchval='Contrast'):
   """ spot filter from McIDAS-X """
   newData = sdataset.clone()
   return newData

def coreFilter(sdataset1,sdataset2,user_brkpoint1='Default',user_brkpoint2='Default',user_replace1='Default',user_replace2='Default',user_stretchval='Contrast'):
   """ core filter from McIDAS-X - requires 2 source datasets; resulting image has only 2 values
       user_brkpoint1 - sdataset1 breakpoint value (default=minimum value in either source dataset)
       user_brkpoint2 - sdataset2 breakpoint value (default=maximum value in either source dataset)
       user_replace1  - success condition replacement value (default=maximum value in either source dataset)
       user_replace2  - failure condition replacement value (default=minimum vlaue in either source dataset)
   """

   data1=sdataset1.clone()
   data2=sdataset2.clone()
   brkpoint1=user_brkpoint1
   brkpoint2=user_brkpoint2
   replace1=user_replace1
   replace2=user_replace2
   if (brkpoint1 != 'Default'):
     brkpoint1=int(brkpoint1)
   if (brkpoint2 != 'Default'):
     brkpoint2=int(brkpoint2)
   if (replace1 != 'Default'):
     replace1=int(replace1)
   if (replace2 != 'Default'):
     replace2 = int(replace2)
   stretch=user_stretchval

   for t in range(data1.getDomainSet().getLength()):
      range1=data1.getSample(t)
      range2=data2.getSample(t)
      vals1=range1.getFloats(0)
      max1=max(vals1[0])
      min1=min(vals1[0])
      vals2=range2.getFloats(0)
      max2=max(vals2[0])
      min2=min(vals2[0])
      if (brkpoint1 == 'Default'):
         brkpoint1=min([min1, min2])
      if (brkpoint2 == 'Default'):
         brkpoint2=max([max1, max2])
      if (replace1 == 'Default'):
         replace1=brkpoint2
      if (replace2 == 'Default'):
         replace2=brkpoint1

      domain=GridUtil.getSpatialDomain(range1)
      [element_size,line_size]=domain.getLengths()
      for i in range(line_size):
         for j in range(element_size):
            if (vals1[0][i*element_size+j] > brkpoint1 and vals2[0][i*element_size+j] > brkpoint2):
               vals1[0][i*element_size + j]=replace1
            else:
               vals1[0][i*element_size + j]=replace2
     
      post_hi = int(max(vals1[0]))
      post_low = int(min(vals1[0])) 
      if (stretch == 'Contrast'):
       lookup=contrast(post_low,post_hi,post_low,post_hi)
      elif (stretch == 'Histogram'):
       h = hist(field(vals1),[0],[post_hi-post_low])
       lookup=histoStretch(post_low,post_hi,h)
       
      vals=modify(vals1,element_size,line_size,post_low,lookup)          
      range1.setSamples(vals)

   return data1

def discriminateFilter(sdataset1,sdataset2,user_brkpoint1='Default',user_brkpoint2='Default',user_brkpoint3='Default',user_brkpoint4='Default',user_replace='Default',user_stretchval='Contrast'):
   """ discriminate filter from McIDAS-X - requires 2 source datasets; used to mask off a portion of the first source image
       user_brkpoint1 - low end breakpoint value for sdataset1 (default=minimum value in either source dataset)
       user_brkpoint2 - high end breakpoint value for sdataset1 (default=maximum value in either source dataset)
       user_brkpoint3 - low end breakpoint value for sdataset2 (default=minimum value in either source dataset)
       user_brkpoint4 - high end breakpoint value for sdataset2 (default=maximum value in either source dataset)
       user_replace   - failure condition replacement value (default=minimum value in either source dataset)
   """
   data1=sdataset1.clone()
   data2=sdataset2.clone()
   brkpoint1=user_brkpoint1
   brkpoint2=user_brkpoint2
   brkpoint3=user_brkpoint3
   brkpoint4=user_brkpoint4
   replace=user_replace
   stretch=user_stretchval
   
   if (brkpoint1 != 'Default'):
     brkpoint1=int(brkpoint1)
   if (brkpoint2 != 'Default'):
     brkpoint2=int(brkpoint2)
   if (brkpoint3 != 'Default'):
     brkpoint3=int(brkpoint3)
   if (brkpoint4 != 'Default'):
     brkpoint4=int(brkpoint4)
   if (replace != 'Default'):
     replace=int(replace)

   for t in range(data1.getDomainSet().getLength()):
      range1=data1.getSample(t)
      range2=data2.getSample(t)
      vals1=range1.getFloats(0)
      max1=max(vals1[0])
      min1=min(vals1[0])
      vals2=range2.getFloats(0)
      max2=max(vals2[0])
      min2=min(vals2[0])
      if (brkpoint1 == 'Default'):
         brkpoint1=min([min1, min2])
      if (brkpoint2 == 'Default'):
         brkpoint2=max([max1, max2])
      if (brkpoint3 == 'Default'):
         brkpoint3=min([min1, min2])
      if (brkpoint4 == 'Default'):
         brkpoint4=max([max1, max2])
      if (replace == 'Default'):
         replace=min([min1, min2])

      domain=GridUtil.getSpatialDomain(range1)
      [element_size,line_size]=domain.getLengths()
      for i in range(line_size):
         for j in range(element_size):
            if (vals1[0][i*element_size+j] < brkpoint1 or vals1[0][i*element_size+j] > brkpoint2 or vals2[0][i*element_size+j] < brkpoint3 or vals2[0][i*element_size+j] > brkpoint4):
               vals1[0][i*element_size + j]=replace
      
      post_hi = int(max(vals1[0]))
      post_low = int(min(vals1[0])) 
      if (stretch == 'Contrast'):
       lookup=contrast(post_low,post_hi,post_low,post_hi)
      elif (stretch == 'Histogram'):
       h = hist(field(vals1),[0],[post_hi-post_low])
       lookup=histoStretch(post_low,post_hi,h)
       
      vals1=modify(vals1,element_size,line_size,post_low,lookup) 
      range1.setSamples(vals1)      
      
   return data1   

def mergeFilter(sdataset1,sdataset2,user_brkpoint1='Default',user_brkpoint2='Default',user_constant=0,user_stretchval='Contrast'):
   """ merge filter from McIDAS-X - requires 2 source datasets; merges them if the sdataset1 value is between the specified breakpoints,
         otherwise it selects the sdataset2 value minus the specified constant
       user_brkpoint1 - sdataset1 breakpoint value (default=minimum value in either source dataset)
       user_brkpoint2 - sdataset2 breakpoint value (default=maximum value in either source dataset)
       user_constant  - subtractive constant
   """
   data1=sdataset1.clone()
   data2=sdataset2.clone()
   brkpoint1=user_brkpoint1
   brkpoint2=user_brkpoint2
   constant=int(user_constant)
   if (brkpoint1 != 'Default'):
     brkpoint1=int(brkpoint1)
   if (brkpoint2 != 'Default'):
     brkpoint2=int(brkpoint2)
   stretch=user_stretchval
   
   for t in range(data1.getDomainSet().getLength()):
      range1=data1.getSample(t)
      range2=data2.getSample(t)
      vals1=range1.getFloats(0)
      max1=max(vals1[0])
      min1=min(vals1[0])
      vals2=range2.getFloats(0)
      max2=max(vals2[0])
      min2=min(vals2[0])
      if (brkpoint1 == 'Default'):
         brkpoint1=min([min1, min2])
      if (brkpoint2 == 'Default'):
         brkpoint2=max([max1, max2])
   
      domain=GridUtil.getSpatialDomain(range1)
      [element_size,line_size]=domain.getLengths()
      for i in range(line_size):
         for j in range(element_size):
            if (vals1[0][i*element_size+j] < brkpoint1 or vals1[0][i*element_size+j] > brkpoint2):
               vals1[0][i*element_size + j]=vals2[0][i*element_size + j] - constant
               
      post_hi = int(max(vals1[0]))
      post_low = int(min(vals1[0])) 
      
      if (stretch == 'Contrast'):
       lookup=contrast(post_low,post_hi,post_low,post_hi)
      elif (stretch == 'Histogram'):
       h = hist(field(vals1),[0],[post_hi-post_low])
       lookup=histoStretch(post_low,post_hi,h)
      
      vals1=modify(vals1,element_size,line_size,post_low,lookup)       
      range1.setSamples(vals1)
   
   return data1
   
def gradientFilter(sdataset,user_stretchval='Contrast'):
   """ gradient filter from McIDAS-X """
   newData=sdataset.clone()
   stretch=user_stretchval
   
   for t in range(newData.getDomainSet().getLength()):
     rangeObject = newData.getSample(t)
     vals = rangeObject.getFloats(0)
     in_hi = max(vals[0])
     in_low = min(vals[0]) 
     domain=GridUtil.getSpatialDomain(rangeObject)
                   
     [element_size,line_size]=domain.getLengths()

     for i in xrange(line_size):
       for j in range(element_size)[:-1]:
          vals[0][i*element_size + j] = int(abs(vals[0][i*element_size + j] - vals[0][i*element_size + j + 1]))
          
       """ set last value to zero """
       vals[0][i*element_size + j + 1] = 0
     
     post_hi = int(max(vals[0]))
     post_low = int(min(vals[0])) 
     if (stretch == 'Contrast'):
       lookup=contrast(post_low,post_hi,post_low,post_hi)
     elif (stretch == 'Histogram'):
       h = hist(field(vals),[0],[post_hi-post_low])
       lookup=histoStretch(post_low,post_hi,h)
        
     vals=modify(vals,element_size,line_size,post_low,lookup) 
     rangeObject.setSamples(vals)

   return newData 

def passFilter(sdataset,user_passname,user_radius=50,user_leak=100,user_stretchval='Contrast'):
   """ Used by one-dimensional low-pass and high-pass filters from McIDAS-X 
       user_passname - either 'High' or 'Low'
       user_radius   - sample length surrounding the source element; used for sample average
       user_leak     - filter efficiency
   """   
   newData = sdataset.clone()
   radius = int(user_radius)
   leak = int (user_leak)
   stretch=user_stretchval

   ntot = 2*radius + 1
   nmod = radius + 1
    
   for t in xrange(len(newData)):
     rangeObject = newData.getSample(t)
     vals = rangeObject.getFloats()
     in_hi = max(vals[0])
     in_low = min(vals[0]) 
     midpoint = (in_hi - in_low)/2 + in_low
     domain=GridUtil.getSpatialDomain(rangeObject)
     [element_size,line_size]=domain.getLengths()
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
            
     post_hi = int(max(vals[0]))
     post_low = int(min(vals[0])) 
     if (stretch == 'Contrast'):
       lookup=contrast(post_low,post_hi,post_low,post_hi)
     elif (stretch == 'Histogram'):
       h = hist(field(vals),[0],[post_hi-post_low])
       lookup=histoStretch(post_low,post_hi,h)
     
     vals=modify(vals,element_size,line_size,post_low,lookup) 
     rangeObject.setSamples(vals)

   return newData

def lowPass2DFilter(sdataset,user_linecoef=0.5,user_elecoef=0.5,user_stretchval='Contrast'):
   """ 2 dimensional low pass filter from McIDAS-X 
       user_linecoef - line coefficient: 0.0 < linecoef < 1.0
       user_elecoef  - element coefficient: 0.0 < elecoef < 1.0
   """  
   newData = sdataset.clone()
   lcoef = float(user_linecoef)
   ecoef = float(user_elecoef)
   stretch=user_stretchval
   l1 = 1.0 - lcoef
   e1 = 1.0 - ecoef 

   for t in xrange(newData.getDomainSet().getLength()):
     rangeObject = newData.getSample(t)
     vals = rangeObject.getFloats(0)
     domain=GridUtil.getSpatialDomain(rangeObject)
     [element_size,line_size]=domain.getLengths()
     
     """ save the first line """
     realLine = vals[0][0:element_size].tolist()
     
     for i in xrange(line_size):
       """ left to right filter along line """
       val = vals[0][i*element_size]
       for j in range(element_size):
          if (vals[0][i*element_size + j] > 0):
            val = ecoef*val + e1 * vals[0][i*element_size + j]
          vals[0][i*element_size + j] = round(val)
     
       """ right to left filter along line """
       val = vals[0][i*element_size + (element_size - 1)]
     
       for j in xrange(element_size - 1, 0, -1):
         val=ecoef*val + e1 * vals[0][i*element_size + j]
         vals[0][i*element_size + j] = round(val)
      
       """ filter along the elements """
       for j in xrange(element_size):
         val = lcoef * realLine[j] + l1 * vals[0][i*element_size + j]
         vals[0][i*element_size + j] = round(val) 
     
       realLine=vals[0][i*element_size:i*element_size+element_size].tolist()

     """ filter along the lines going through the image up the elements """
     """ save the last line """
     realLine = vals[0][(line_size-1)*element_size:line_size*element_size].tolist()
     for i in xrange(line_size - 1, 0, -1):
        for j in range(element_size):
          val = lcoef * realLine[j] + l1 * vals[0][i*element_size + j]
          vals[0][i*element_size + j] = round(val) 
        
        realLine=vals[0][i*element_size:i*element_size+element_size].tolist()

     post_hi = int(max(vals[0]))
     post_low = int(min(vals[0])) 
     if (stretch == 'Contrast'):
       lookup=contrast(post_low,post_hi,post_low,post_hi)
     elif (stretch == 'Histogram'):
       h = hist(field(vals),[0],[post_hi-post_low])
       lookup=histoStretch(post_low,post_hi,h)
     
     vals=modify(vals,element_size,line_size,post_low,lookup) 
     rangeObject.setSamples(vals)

   return newData

def highPass2DFilter(sdataset,user_stretchval='Contrast'):
   """ 2 dimensional high pass filter from McIDAS-X 
       equation for each sdataset element = (sdataset - (sample average) + (sample midpoint))
   """
   newData = sdataset.clone()
   stretch=user_stretchval
   
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
     
     vals[0] = firstLine + vals[0][0:(line_size-1)*element_size]
     post_hi = int(max(vals[0]))
     post_low = int(min(vals[0]))
     if (stretch == 'Contrast'):
       lookup=contrast(post_low,post_hi,post_low,post_hi)
     elif (stretch == 'Histogram'):
       h = hist(field(vals),[0],[post_hi-post_low])
       lookup=histoStretch(post_low,post_hi,h) 
     
     vals=modify(vals,element_size,line_size,post_low,lookup)
     rangeObject.setSamples(vals)

   return newData

def holeFilter(sdataset,user_brkpoint1=0,user_brkpoint2=1,user_stretchval='Contrast'):
  """ hole filter from McIDAS-X - searches for missing data and fills the holes 
        using the surrounding element values
      brkpoint1 - low end breakpoint value (default = minimum sdataset value)
      brkpoint2 - high end breakpoint value (default = maximum sdataset value)
  """  
  data = sdataset.clone()
  brkpoint1=int(user_brkpoint1)
  brkpoint2=int(user_brkpoint2)
  stretch=user_stretchval
  
  for t in xrange(data.getDomainSet().getLength()):
     rangeObject=data.getSample(t)
     vals=rangeObject.getFloats(0)
     minVal = min([brkpoint1,brkpoint2])
     maxVal = max([brkpoint1,brkpoint2])
     domain=GridUtil.getSpatialDomain(rangeObject)
     [element_size,line_size]=domain.getLengths()
     
     for i in range(line_size):
       for j in range(element_size)[1:-1]:
         curVal = vals[0][i*element_size + j]
         """ search line for bad values """
         if (curVal >= minVal and curVal <= maxVal):
            """ look for the next good value """
            doFill = 0
            for k in range(element_size)[j:]:
               nextVal = vals[0][i*element_size + k]
               if (nextVal < minVal or nextVal > maxVal):
                 doFill = 1
                 break

            if (doFill == 1):
               for fill in range(element_size)[j:k]:
                  vals[0][i*element_size + fill] = (vals[0][i*element_size + j - 1] + vals[0][i*element_size+k])/2
               
     post_hi = int(max(vals[0]))
     post_low = int(min(vals[0])) 
     if (stretch == 'Contrast'):
       lookup=contrast(post_low,post_hi,post_low,post_hi)
     elif (stretch == 'Histogram'):
       h = hist(field(vals),[0],[post_hi-post_low])
       lookup=histoStretch(post_low,post_hi,h)
     
     vals=modify(vals,element_size,line_size,post_low,lookup) 
     rangeObject.setSamples(vals)
    
  return data

def contrast(in_low,in_hi,minimum,maximum,out_low=0,out_hi=255,inc=1):
   """ create a contrast stretch lookup table """
   if (in_hi == in_low):
      slope=0
   else:
      slope=float(out_hi - out_low)/float(in_hi - in_low)
 
   lookup = []
   for input_value in xrange(minimum, maximum + 1, inc):
      out_value = out_low + int(round((input_value - in_low)*slope))
      if (slope < 0):
        out_value=max(min(out_value,out_low),out_hi)
      else:
        out_value=min(max(out_value,out_low),out_hi)
      
      indx=input_value - minimum
      for i in xrange(inc):
         lookup.insert(indx, out_value)
         indx = indx + 1
   
   return lookup

def histoStretch(in_low,in_hi,histogram,out_low=0,out_hi=255,num_bins=16):
    """ create a histogram stretch lookup table """
    step=round((out_hi - out_low)/float(num_bins))
    half_step=round(step/2)
        
    breakpoints = [ out_low + ((m+1)*step-half_step) for m in xrange(num_bins)]
        
    start = 1
    end = in_hi - in_low + 1
    lookup=[]
    
    for i in xrange(num_bins):
        npop=0
        ntot=0
        for j in range(start,end):
            if (histogram[j-1] != 0):
               npop=npop+1
            ntot=ntot + histogram[j-1]
            
        nleft = num_bins - i
        nvals_bin = round(ntot/nleft)
            
        if (npop >= nleft):
            mtot=0
            for k in range(start,end):
                mtot=mtot+histogram[k-1]
                if (mtot > nvals_bin):
                    break
                lookup.insert(k,breakpoints[i])
            
            start = k + 1
            lookup.insert(k,breakpoints[i])
        else:
            l=i
            for m in range(start,end):
                lookup.insert(m,breakpoints[l])
                if (histogram[m] != 0):
                    l=l+1
    
    return lookup    
    
def modify(vals,element_size,line_size,minimum,lookup):
  """ modifies an image with the lookup table generated by a stretch function """
  for i in xrange(line_size):
     for j in xrange(element_size):
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