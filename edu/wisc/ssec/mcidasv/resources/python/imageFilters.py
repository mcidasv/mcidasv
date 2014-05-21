from visad.python.JPythonMethods import field
from ucar.unidata.data.grid import GridUtil

from decorators import transform_flatfields

def cloudFilter(sdataset1, sdataset2, user_replace='Default', user_constant=0, user_stretchval='Contrast', user_britlo=0, user_brithi=255):
    """
        cloud filter from McIDAS-X - requires 2 source datasets
        user_replace: replacement value  (default=minimum value in either sdataset1 or sdataset2)
        user_constant: additive constant (default=0)
        user_britlo    - minimum brightness value for the calibration
        user_brithi    - maximum brightness value for the calibration
    """
    
    data1 = sdataset1.clone()
    data2 = sdataset2.clone()
    replace = user_replace
    constant = int(user_constant)
    britlo = int(user_britlo)
    brithi = int(user_brithi)
    
    if replace != 'Default':
        replace = int(replace)
    stretch = user_stretchval
    
    for t in range(data1.getDomainSet().getLength()):
        range1 = data1.getSample(t)
        vals1 = range1.getFloats(0)
        min1 = min(vals1[0])
        max1 = max(vals1[0])
        range2 = data2.getSample(t)
        vals2 = range2.getFloats(0)
        min2 = min(vals2[0])
        max2 = max(vals2[0])
        in_low = min([min1, min2])
        in_hi = max([max1, max2])
        if replace == 'Default':
            replace = in_low
        
        domain = GridUtil.getSpatialDomain(range1)
        [element_size, line_size] = domain.getLengths()
        
        for i in range(line_size):
            for j in range(element_size):
                line1 = vals1[0][i * element_size + j]
                line2 = vals2[0][i * element_size + j]
                
                if line1 <= (line2 + constant):
                    vals1[0][i * element_size + j] = replace
        
        for i in range(line_size):
            for j in range(element_size):
                vals1[0][i * element_size + j] = scaleOutsideVal(vals1[0][i * element_size + j], britlo, brithi)
        
        filt_low = int(min([min(vals1[0]), min2]))
        filt_hi = int(max([max(vals1[0]), max2]))
        if stretch == 'Contrast':
            lookup = contrast(filt_low, filt_hi, in_low, in_hi, filt_low, filt_hi)
        elif stretch == 'Histogram':
            """ make a histogram from both datasets """
            v = []
            v.append(vals1[0])
            v.append(vals2[0])
            h = makeHistogram(v, element_size, line_size, filt_low, brithi - britlo)
            lookup = histoStretch(filt_low, filt_hi, in_low, in_hi, h)
        
        vals1 = modify(vals1, element_size, line_size, filt_low, lookup)
        range1.setSamples(vals1)
        
    return data1


def replaceFilter(sdataset, user_replaceVal=0, user_sourceval='Default', user_stretchval='Contrast', user_britlo=0, user_brithi=255):
    """
        replace filter from McIDAS-X
        user_replace : replacement value  (default=0)
        user_sourceval: source image values in the region to replace user_replace; specify values
                        in the list format, e.g. val1 val2 val3 etc.,
                        or a range format, e.g. bval-eval (default=0-255)
        user_britlo    - minimum brightness value for the calibration
        user_brithi    - maximum brightness value for the calibration
    """
    newData = sdataset.clone()
    replaceVal = int(user_replaceVal)
    stretch = user_stretchval
    britlo = int(user_britlo)
    brithi = int(user_brithi)
    
    """ sourceVal can either be specified in list format: val1 val2 val3
        or in a range format, bval-eval (default = 0-255) 
    """
    if user_sourceval != 'Default':
        if '-' in user_sourceval:
            tempVal1 = [int(m) for m in user_sourceval.split('-')]
            tempVal = range(tempVal1[0], tempVal1[1] + 1)
        else:
            tempVal = user_sourceval.split()
    else:
        tempVal = range(0, 256)
    
    sourceVal = [float(m) for m in tempVal]
    
    for t in range(newData.getDomainSet().getLength()):
        rangeObject = newData.getSample(t)
        vals = rangeObject.getFloats(0)
        in_low = int(min(vals[0]))
        in_hi = int(max(vals[0]))
        domain = GridUtil.getSpatialDomain(rangeObject)
        
        [element_size, line_size] = domain.getLengths()
        
        for i in range(line_size):
            for j in range(element_size):
                line = vals[0][i * element_size + j]
                if (line in sourceVal):
                    line = replaceVal
                vals[0][i * element_size + j] = line
        
        for i in range(line_size):
            for j in range(element_size):
                vals[0][i * element_size + j] = scaleOutsideVal(vals[0][i * element_size + j], britlo, brithi)
                
        filt_low = int(min(vals[0]))
        filt_hi = int(max(vals[0]))
        if stretch == 'Contrast':
            lookup = contrast(filt_low, filt_hi, britlo, brithi, filt_low, filt_hi)
        elif stretch == 'Histogram':
            h = makeHistogram(vals, element_size, line_size, filt_low, brithi - britlo)
            lookup = histoStretch(filt_low, filt_hi, in_low, in_hi, h)
                 
        vals = modify(vals, element_size, line_size, filt_low, lookup)
        rangeObject.setSamples(vals)
        
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
       
       """ for some reason, have to do this to floor the value """
       p = (left + right)/2
       a = field((p,))
       b = a.floor().getValues(0)  
       vals[0][i*element_size + j] = b[0]
    
  return vals

def badLineFilter(vals, bline, eline, element_size, line_size, filter_fill, line_diff, min_data, max_data):
    """
       The bad line filter used by the clean filter
       Lines are bad if
       1. all the values in the line are the same
       2. the average difference is greater than the supplied limit (line_diff argument)
    """
    num_badlines = 0
    
    for i in range(line_size)[bline:eline - 1]:
        cur_line = vals[0][i * element_size:(i + 1) * element_size]
        min_line = min(cur_line)
        max_line = max(cur_line)
        if i == bline:
            good_line = cur_line
            continue
            
        diff = 0
        
        for ii in range(element_size):
            diff = diff + abs(cur_line[ii] - good_line[ii])
            
        isbad = 0
        if (max_line - min_line) == 0:
            print 'Bad Line - same values'
            isbad = 1
            num_badlines = num_badlines + 1
            """ store the last good line """
            if num_badlines == 1:
                last_good_line = good_line
        else:
            ave_diff = diff / element_size
            
            if ave_diff > line_diff:
                print 'Bad Line - line_diff exceeded'
                isbad = 1
                num_badlines = num_badlines + 1
                if num_badlines == 1:
                    last_good_line = good_line
                    
        if isbad == 0:
            good_line = cur_line
            if num_badlines > 0:
                for j in range(num_badlines):
                    fdiv = (1.0 / float(num_badlines + 1)) * float(j + 1)
                    new_line = (i - num_badlines) + j
                    
                    for k in range(element_size):
                        if filter_fill == 'Min':
                            vals[0][new_line * element_size + k] = min_line
                        elif filter_fill == 'Average':
                            ave_diff = int((last_good_line[k] - good_line[k]) * fdiv)
                            vals[0][new_line * element_size + k] = good_line[k] + ave_diff
                        elif filter_fill == 'Max':
                            vals[0][new_line * element_size + k] = max_line
                    good_line = vals[0][new_line * element_size:(new_line + 1) * element_size]
                    
                num_badlines = 0
                
    return vals

@transform_flatfields
def cleanFilter(sdataset, user_fill='Average', user_bline='Default', user_eline='Default', user_pdiff=15, user_ldiff=15, user_stretchval='Contrast', user_britlo=0, user_brithi=255):
    """ clean filter from McIDAS-X
        user_fill    - 'Average': average of surrounding values (default)
                     - 'Min'    : source dataset minimum value
                     - 'Max'    : source dataset maximum value
        user_bline   - beginning line in the source image to clean (default=first line)
        user_eline   - ending line in the source image to clean (default = last line)
        user_pdiff   - absolute difference between an element's value and value of the element on either side
        user_ldiff   - percentage difference between a line's average value and the average value of
                       the line above and below
        user_britlo    - minimum brightness value for the calibration
        user_brithi    - maximum brightness value for the calibration
    """
    newData = sdataset.clone()
    
    filter_fill = user_fill
    bline = user_bline
    eline = user_eline
    britlo = int(user_britlo)
    brithi = int(user_brithi)
    
    if bline != 'Default':
        bline = int(bline)
    else:
        bline = 0
    if eline != 'Default':
        eline = int(eline)
        
    filter_diff = int(user_pdiff)
    l_diff = int(user_ldiff)
    stretch = user_stretchval
    
    for t in range(newData.getDomainSet().getLength()):
        rangeObject = newData.getSample(t)
        vals = rangeObject.getFloats(0)
        in_hi = int(max(vals[0]))
        in_low = int(min(vals[0]))
        point_diff = (in_hi - in_low + 1) * (filter_diff / 100.0)
        line_diff = (in_hi - in_low + 1) * (l_diff / 100.0)
        domain = GridUtil.getSpatialDomain(rangeObject)
        [element_size, line_size] = domain.getLengths()
        if eline == 'Default':
            eline = line_size
            
        vals = shotMain(vals, bline, eline, element_size, line_size, point_diff)
        for i in range(line_size):
            for j in range(element_size):
                vals[0][i * element_size + j] = scaleOutsideVal(vals[0][i * element_size + j], britlo, brithi)
                
        filt_low = int(min(vals[0]))
        filt_hi = int(max(vals[0]))
        
        vals = badLineFilter(vals, bline, eline, element_size, line_size, filter_fill, line_diff, filt_low, filt_hi)
        
        # update the min/max of the image after the removal of the bad lines
        filt_low = int(min(vals[0]))
        filt_hi = int(max(vals[0]))
        
        if stretch == 'Contrast':
            lookup = contrast(filt_low, filt_hi, britlo, brithi, filt_low, filt_hi)
        elif stretch == 'Histogram':
            h = makeHistogram(vals, element_size, line_size, filt_low, brithi - britlo)
            lookup = histoStretch(filt_low, filt_hi, britlo, brithi, h)
            
        vals = modify(vals, element_size, line_size, filt_low, lookup)
        rangeObject.setSamples(vals)
        
    return newData


def shotFilter(sdataset, user_bline='Default', user_eline='Default', user_pdiff=15, user_stretchval='Contrast', user_britlo=0, user_brithi=255):
    """ shot noise filter from McIDAS-X
        bline - beginning line in the source image to clean (default=first line)
        eline - ending line in the source image to clean (default = last line)
        pdiff - maximum percentage of the product range to allow before a new value for the pixel is derived using the
                average of two adjacent pixels
        user_britlo    - minimum brightness value for the calibration
        user_brithi    - maximum brightness value for the calibration
    """
    newData = sdataset.clone()
    bline = user_bline
    eline = user_eline
    britlo = int(user_britlo)
    brithi = int(user_brithi)
       
    if bline != 'Default':
        bline = int(bline)
    else:
        bline = 0
        
    if eline != 'Default':
        eline = int(eline)
      
    filter_diff = int(user_pdiff)
    stretch = user_stretchval
    
    for t in range(newData.getDomainSet().getLength()):
        rangeObject = newData.getSample(t)
        vals = rangeObject.getFloats(0)
        in_hi = int(max(vals[0]))
        in_low = int(min(vals[0]))
        """ the next four lines are to make sure the point_diff value is floored """
        p = (in_hi - in_low + 1) * (filter_diff / 100.0)
        a = field((p,))
        b = a.floor().getValues(0)
        point_diff = b[0]
        
        domain = GridUtil.getSpatialDomain(rangeObject)
        [element_size, line_size] = domain.getLengths()
        if (eline == 'Default'):
            eline = line_size
            
        vals = shotMain(vals, bline, eline, element_size, line_size, point_diff)
        
        for i in range(line_size):
            for j in range(element_size):
                vals[0][i * element_size + j] = scaleOutsideVal(vals[0][i * element_size + j], britlo, brithi)
                
        filt_low = int(min(vals[0]))
        filt_hi = int(max(vals[0]))
        if stretch == 'Contrast':
            lookup = contrast(filt_low, filt_hi, britlo, brithi, filt_low, filt_hi)
        elif stretch == 'Histogram':
            h = makeHistogram(vals, element_size, line_size, filt_low, brithi - britlo)
            lookup = histoStretch(filt_low, filt_hi, britlo, brithi, h)
            
        vals = modify(vals, element_size, line_size, filt_low, lookup)
        rangeObject.setSamples(vals)
        
    return newData


def spotFilter(sdataset, omcon=0, oacon=0, imcon=0, iacon=0, cmin=0, cmax=0, user_stretchval='Contrast'):
    """ spot filter from McIDAS-X """
    newData = sdataset.clone()
    return newData


def coreFilter(sdataset1, sdataset2, user_brkpoint1='Default', user_brkpoint2='Default', user_replace1='Default', user_replace2='Default', user_stretchval='Contrast', user_britlo=0, user_brithi=255):
    """ core filter from McIDAS-X - requires 2 source datasets; resulting image has only 2 values
        user_brkpoint1 - sdataset1 breakpoint value (default=minimum value in either source dataset)
        user_brkpoint2 - sdataset2 breakpoint value (default=maximum value in either source dataset)
        user_replace1  - success condition replacement value (default=maximum value in either source dataset)
        user_replace2  - failure condition replacement value (default=minimum vlaue in either source dataset)
        user_britlo    - minimum brightness value for the calibration
        user_brithi    - maximum brightness value for the calibration
    """
    
    data1 = sdataset1.clone()
    data2 = sdataset2.clone()
    brkpoint1 = user_brkpoint1
    brkpoint2 = user_brkpoint2
    replace1 = user_replace1
    replace2 = user_replace2
    britlo = int(user_britlo)
    brithi = int(user_brithi)
    
    if brkpoint1 != 'Default':
        brkpoint1=int(brkpoint1)
        
    if brkpoint2 != 'Default':
        brkpoint2=int(brkpoint2)
        
    if replace1 != 'Default':
        replace1=int(replace1)
        
    if replace2 != 'Default':
        replace2 = int(replace2)
        
    stretch = user_stretchval
    
    for t in range(data1.getDomainSet().getLength()):
        range1 = data1.getSample(t)
        range2 = data2.getSample(t)
        vals1 = range1.getFloats(0)
        max1 = max(vals1[0])
        min1 = min(vals1[0])
        vals2 = range2.getFloats(0)
        max2 = max(vals2[0])
        min2 = min(vals2[0])
        in_low = min([min1, min2])
        in_hi = max([max1, max2])
        
        if brkpoint1 == 'Default':
            brkpoint1 = in_low
            
        if brkpoint2 == 'Default':
            brkpoint2 = in_hi
            
        if replace1 == 'Default':
            replace1 = brkpoint2
            
        if replace2 == 'Default':
            replace2 = brkpoint1
            
        domain = GridUtil.getSpatialDomain(range1)
        [element_size, line_size] = domain.getLengths()
        for i in range(line_size):
            for j in range(element_size):
                if (vals1[0][i * element_size + j] > brkpoint1 and vals2[0][i * element_size + j] > brkpoint2):
                    vals1[0][i * element_size + j] = replace1
                else:
                    vals1[0][i * element_size + j]=replace2
                    
        for i in range(line_size):
            for j in range(element_size):
                vals1[0][i * element_size + j] = scaleOutsideVal(vals1[0][i * element_size + j], britlo, brithi)
                
        filt_low = int(min([min(vals1[0]), min2]))
        filt_hi = int(max([max(vals1[0]), max2]))
        if stretch == 'Contrast':
            lookup = contrast(filt_low, filt_hi, in_low, in_hi, filt_low, filt_hi)
        elif stretch == 'Histogram':
            """ make a histogram from both datasets """
            v = []
            v.append(vals1[0])
            v.append(vals2[0])
            h = makeHistogram(v, element_size, line_size, filt_low, brithi - britlo)
            lookup = histoStretch(filt_low, filt_hi, in_low, in_hi, h)
            
        vals1 = modify(vals1, element_size, line_size, filt_low, lookup)
        range1.setSamples(vals1)
        
    return data1

def shotFilter(sdataset,user_bline='Default',user_eline='Default',user_pdiff=15,user_stretchval='Contrast',user_britlo=0,user_brithi=255):
   """ shot noise filter from McIDAS-X
       bline - beginning line in the source image to clean (default=first line)
       eline - ending line in the source image to clean (default = last line)
       pdiff - maximum percentage of the product range to allow before a new value for the pixel is derived using the
               average of two adjacent pixels
       user_britlo    - minimum brightness value for the calibration
       user_brithi    - maximum brightness value for the calibration
   """    
   newData = sdataset.clone()
   bline=user_bline
   eline=user_eline
   britlo=int(user_britlo)
   brithi=int(user_brithi)
      
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
     in_hi = int(max(vals[0]))
     in_low = int(min(vals[0]))
     """ the next four lines are to make sure the point_diff value is floored """
     p=(in_hi - in_low + 1)*(filter_diff/100.0)    
     a=field((p,))
     b=a.floor().getValues(0)
     point_diff = b[0]   
     
     domain=GridUtil.getSpatialDomain(rangeObject)  
     [element_size,line_size]=domain.getLengths()
     if (eline == 'Default'):
       eline=line_size 
     
     vals = shotMain(vals,bline,eline,element_size,line_size,point_diff)
     
     for i in range(line_size):
        for j in range(element_size):
            vals[0][i*element_size+j]=scaleOutsideVal(vals[0][i*element_size+j],britlo,brithi)
     
     filt_low=int(min(vals[0]))
     filt_hi =int(max(vals[0]))
     if (stretch == 'Contrast'):
       lookup=contrast(filt_low,filt_hi,britlo,brithi,filt_low,filt_hi)
     elif (stretch == 'Histogram'):
       h = makeHistogram(vals,element_size,line_size,filt_low,brithi-britlo)
       lookup=histoStretch(filt_low,filt_hi,britlo,brithi,h)
            
     vals=modify(vals,element_size,line_size,filt_low,lookup)
     rangeObject.setSamples(vals)    
   
   return newData       

def discriminateFilter(sdataset1, sdataset2, user_brkpoint1='Default', user_brkpoint2='Default', user_brkpoint3='Default', user_brkpoint4='Default', user_replace='Default', user_stretchval='Contrast', user_britlo=0, user_brithi=255):
    """ discriminate filter from McIDAS-X - requires 2 source datasets; used to mask off a portion of the first source image
        user_brkpoint1 - low end breakpoint value for sdataset1 (default=minimum value in either source dataset)
        user_brkpoint2 - high end breakpoint value for sdataset1 (default=maximum value in either source dataset)
        user_brkpoint3 - low end breakpoint value for sdataset2 (default=minimum value in either source dataset)
        user_brkpoint4 - high end breakpoint value for sdataset2 (default=maximum value in either source dataset)
        user_replace   - failure condition replacement value (default=minimum value in either source dataset)
        user_britlo    - minimum brightness value for the calibration
        user_brithi    - maximum brightness value for the calibration
    """
    
    data1 = sdataset1.clone()
    data2 = sdataset2.clone()
    brkpoint1 = user_brkpoint1
    brkpoint2 = user_brkpoint2
    brkpoint3 = user_brkpoint3
    brkpoint4 = user_brkpoint4
    replace = user_replace
    stretch = user_stretchval
    britlo = int(user_britlo)
    brithi = int(user_brithi)
    
    if brkpoint1 != 'Default':
        brkpoint1 = int(brkpoint1)
        
    if brkpoint2 != 'Default':
        brkpoint2 = int(brkpoint2)
        
    if brkpoint3 != 'Default':
        brkpoint3 = int(brkpoint3)
        
    if brkpoint4 != 'Default':
        brkpoint4 = int(brkpoint4)
        
    if replace != 'Default':
        replace = int(replace)
        
    for t in range(data1.getDomainSet().getLength()):
        range1 = data1.getSample(t)
        range2 = data2.getSample(t)
        vals1 = range1.getFloats(0)
        max1 = max(vals1[0])
        min1 = min(vals1[0])
        vals2 = range2.getFloats(0)
        max2 = max(vals2[0])
        min2 = min(vals2[0])
        in_low = int(min([min1, min2]))
        in_hi = int(max([max1, max2]))
        
        if brkpoint1 == 'Default':
            brkpoint1=in_low
            
        if brkpoint2 == 'Default':
            brkpoint2=in_hi
            
        if brkpoint3 == 'Default':
            brkpoint3=in_low
            
        if brkpoint4 == 'Default':
            brkpoint4 = in_hi
            
        if replace == 'Default':
            replace = in_low
            
        domain = GridUtil.getSpatialDomain(range1)
        [element_size, line_size] = domain.getLengths()
        for i in range(line_size):
            for j in range(element_size):
                if vals1[0][i * element_size + j] < brkpoint1 or vals1[0][i * element_size + j] > brkpoint2 or vals2[0][i * element_size + j] < brkpoint3 or vals2[0][i * element_size + j] > brkpoint4:
                    vals1[0][i * element_size + j]=replace
                    
        if stretch == 'Contrast':
            lookup = contrast(in_low, in_hi, britlo, brithi, in_low, in_hi)
        elif stretch == 'Histogram':
            """ make a histogram from the first dataset """
            h = makeHistogram(vals1, element_size, line_size, in_low, brithi - britlo)
            lookup = histoStretch(in_low, in_hi, britlo, brithi, h)
            
        vals1 = modify(vals1, element_size, line_size, in_low, lookup)
        range1.setSamples(vals1)
        
    return data1


def mergeFilter(sdataset1, sdataset2, user_brkpoint1='Default', user_brkpoint2='Default', user_constant=0, user_stretchval='Contrast', user_britlo=0, user_brithi=255):
    """ merge filter from McIDAS-X - requires 2 source datasets; merges them if the sdataset1 value is between the specified breakpoints,
          otherwise it selects the sdataset2 value minus the specified constant
        user_brkpoint1 - sdataset1 breakpoint value (default=minimum value in either source dataset)
        user_brkpoint2 - sdataset2 breakpoint value (default=maximum value in either source dataset)
        user_constant  - subtractive constant
        user_britlo    - minimum brightness value for the calibration
        user_brithi    - maximum brightness value for the calibration
    """
    data1 = sdataset1.clone()
    data2 = sdataset2.clone()
    brkpoint1 = user_brkpoint1
    brkpoint2 = user_brkpoint2
    constant = int(user_constant)
    britlo = int(user_britlo)
    brithi = int(user_brithi)
    
    if brkpoint1 != 'Default':
        brkpoint1 = int(brkpoint1)
        
    if brkpoint2 != 'Default':
        brkpoint2 = int(brkpoint2)
        
    stretch = user_stretchval
    britlo = int(user_britlo)
    brithi = int(user_brithi)
    
    for t in range(data1.getDomainSet().getLength()):
        range1 = data1.getSample(t)
        range2 = data2.getSample(t)
        vals1 = range1.getFloats(0)
        max1 = max(vals1[0])
        min1 = min(vals1[0])
        vals2 = range2.getFloats(0)
        max2 = max(vals2[0])
        min2 = min(vals2[0])
        in_low = min([min1, min2])
        in_hi = max([max1, max2])
        
        if brkpoint1 == 'Default':
            brkpoint1 = in_low
            
        if brkpoint2 == 'Default':
            brkpoint2 = in_hi
            
        domain = GridUtil.getSpatialDomain(range1)
        [element_size, line_size] = domain.getLengths()
        for i in range(line_size):
            for j in range(element_size):
                if vals1[0][i * element_size + j] < brkpoint1 or vals1[0][i * element_size + j] > brkpoint2:
                    vals1[0][i * element_size + j] = vals2[0][i * element_size + j] - constant
                    
        for i in range(line_size):
            for j in range(element_size):
                vals1[0][i * element_size + j] = scaleOutsideVal(vals1[0][i * element_size + j], britlo, brithi)
                
        filt_low = int(min([min(vals1[0]), min2]))
        filt_hi = int(max([max(vals1[0]), max2]))
        if stretch == 'Contrast':
            lookup = contrast(filt_low, filt_hi, in_low, in_hi, filt_low, filt_hi)
        elif stretch == 'Histogram':
            """ make a histogram from both datasets """
            v = []
            v.append(vals1[0])
            v.append(vals2[0])
            h = makeHistogram(v, element_size, line_size, filt_low, brithi - britlo)
            lookup = histoStretch(filt_low, filt_hi, in_low, in_hi, h)
            
        vals1 = modify(vals1, element_size, line_size, filt_low, lookup)
        range1.setSamples(vals1)
        
    return data1


def gradientFilter(sdataset, user_stretchval='Contrast', user_britlo=0, user_brithi=255):
    """
       gradient filter from McIDAS-X
       user_britlo    - minimum brightness value for the calibration
       user_brithi    - maximum brightness value for the calibration
    """
    
    newData = sdataset.clone()
    stretch = user_stretchval
    britlo = int(user_britlo)
    brithi = int(user_brithi)
    
    for t in range(newData.getDomainSet().getLength()):
        rangeObject = newData.getSample(t)
        vals = rangeObject.getFloats(0)
        in_hi = max(vals[0])
        in_low = min(vals[0])
        domain = GridUtil.getSpatialDomain(rangeObject)
        
        [element_size, line_size] = domain.getLengths()
        
        for i in range(line_size):
            for j in range(element_size)[:-1]:
                vals[0][i * element_size + j] = int(abs(vals[0][i * element_size + j] - vals[0][i * element_size + j + 1]))
                
            """ set last value to zero """
            vals[0][i * element_size + j + 1] = 0
            
        for i in range(line_size):
            for j in range(element_size):
                vals[0][i * element_size + j] = scaleOutsideVal(vals[0][i * element_size + j], britlo, brithi)
                
        filt_low = int(min(vals[0]))
        filt_hi = int(max(vals[0]))
        if stretch == 'Contrast':
            lookup = contrast(filt_low, filt_hi, britlo, brithi, filt_low, filt_hi)
        elif stretch == 'Histogram':
            h = makeHistogram(vals, element_size, line_size, filt_low, brithi - britlo)
            lookup = histoStretch(filt_low, filt_hi, in_low, in_hi, h)
            
        vals = modify(vals, element_size, line_size, filt_low, lookup)
        rangeObject.setSamples(vals)
        
    return newData


def passFilter(sdataset, user_passname, user_radius=50, user_leak=100, user_stretchval='Contrast', user_britlo=0, user_brithi=255):
    """ Used by one-dimensional low-pass and high-pass filters from McIDAS-X
        user_passname - either 'High' or 'Low'
        user_radius   - sample length surrounding the source element; used for sample average
        user_leak     - filter efficiency
        user_britlo    - minimum brightness value for the calibration
        user_brithi    - maximum brightness value for the calibration
    """
    
    newData = sdataset.clone()
    radius = int(user_radius)
    leak = int(user_leak)
    stretch = user_stretchval
    britlo = int(user_britlo)
    brithi = int(user_brithi)
    
    ntot = 2 * radius + 1
    nmod = radius + 1
    
    for t in xrange(len(newData)):
        rangeObject = newData.getSample(t)
        vals = rangeObject.getFloats()
        in_hi = max(vals[0])
        in_low = min(vals[0])
        midpoint = (in_hi - in_low) / 2 + in_low
        domain=GridUtil.getSpatialDomain(rangeObject)
        [element_size, line_size] = domain.getLengths()
        radiusArray = range(nmod)
        
        for i in xrange(line_size):
            nr = 1 + radius
            
            if nr > element_size:
                nr = element_size
                
            k = vals[0][i * element_size: i * element_size + nr].tolist()
            
            for p in range(len(k)):
                k[p] = int(k[p])
                
            radiusArray = radius * [int(vals[0][i * element_size])] + k
            nsum = sum(radiusArray)
            
            nright = radius
            for j in xrange(element_size):
                curVal = vals[0][i * element_size + j]
                average = int((leak * nsum) / (100 * ntot))
                
                if user_passname.startswith('High'):
                    vals[0][i * element_size + j] = scaleOutsideVal(curVal - average + midpoint, britlo, brithi)
                    
                if user_passname.startswith('Low'):
                    vals[0][i * element_size + j] = scaleOutsideVal(average, britlo, brithi)
                    
                """ move the radius array one element to the right and recalculate the sum """
                radiusArray.pop(0)
                nright = nright + 1
                mright = nright
                
                if mright > element_size - 1:
                    mright = element_size - 1
                    
                radiusArray.append(int(vals[0][i * element_size + mright]))
                nsum = sum(radiusArray)
                
        filt_low = int(min(vals[0]))
        filt_hi = int(max(vals[0]))
        
        if stretch == 'Contrast':
            lookup = contrast(filt_low, filt_hi, britlo, brithi, filt_low, filt_hi)
        elif stretch == 'Histogram':
            h = makeHistogram(vals, element_size, line_size, filt_low, brithi - britlo)
            lookup = histoStretch(filt_low, filt_hi, in_low, in_hi, h)
            
        vals = modify(vals, element_size, line_size, filt_low, lookup)
        rangeObject.setSamples(vals)
        
    return newData


def lowPass2DFilter(sdataset, user_linecoef=0.5, user_elecoef=0.5, user_stretchval='Contrast', user_britlo=0, user_brithi=255):
    """ 2 dimensional low pass filter from McIDAS-X
        user_linecoef - line coefficient: 0.0 < linecoef < 1.0
        user_elecoef  - element coefficient: 0.0 < elecoef < 1.0
        user_britlo    - minimum brightness value for the calibration
        user_brithi    - maximum brightness value for the calibration
    """
    
    newData = sdataset.clone()
    lcoef = float(user_linecoef)
    ecoef = float(user_elecoef)
    stretch=user_stretchval
    l1 = 1.0 - lcoef
    e1 = 1.0 - ecoef
    britlo = int(user_britlo)
    brithi = int(user_brithi)
    
    for t in xrange(newData.getDomainSet().getLength()):
        rangeObject = newData.getSample(t)
        vals = rangeObject.getFloats(0)
        in_hi = max(vals[0])
        in_low = min(vals[0])
        domain = GridUtil.getSpatialDomain(rangeObject)
        [element_size, line_size] = domain.getLengths()
        
        """ save the first line """
        realLine = vals[0][0:element_size].tolist()
        
        for i in range(line_size):
            """ left to right filter along line """
            val = vals[0][i * element_size]
            for j in range(element_size):
                if vals[0][i * element_size + j] > 0:
                    val = ecoef * val + e1 * vals[0][i * element_size + j]
                vals[0][i * element_size + j] = lowPass2DRound(val+0.0000001)
                
            """ right to left filter along line """
            val = vals[0][i * element_size + (element_size - 1)]
            
            """ second argument of -1 ensures that the 0th element is done """
            for j in xrange(element_size - 1, -1, -1):
                val = ecoef * val + e1 * vals[0][i * element_size + j]
                vals[0][i * element_size + j] = lowPass2DRound(val) 
                
            """ filter along the elements """
            for j in range(element_size):
                val = lcoef * realLine[j] + l1 * vals[0][i * element_size + j]
                vals[0][i * element_size + j] = lowPass2DRound(val) 
                
            realLine = vals[0][i * element_size:i * element_size + element_size].tolist()
            
        """ filter along the lines going through the image up the elements
           save the last line """
        realLine = vals[0][(line_size - 1) * element_size:line_size * element_size].tolist()
        
        """ second argument of -1 ensures that the 0th line is done """
        for i in xrange(line_size - 1, -1, -1):
            for j in range(element_size):
                val = lcoef * realLine[j] + l1 * vals[0][i * element_size + j]
                vals[0][i * element_size + j] = lowPass2DRound(val) 
                
            realLine = vals[0][i * element_size:i * element_size + element_size].tolist()
            
        for i in range(line_size):
            for j in range(element_size):
                vals[0][i * element_size + j] = scaleOutsideVal(vals[0][i * element_size + j], britlo, brithi)
                
        filt_low = int(min(vals[0]))
        filt_hi = int(max(vals[0]))
        
        if stretch == 'Contrast':
            lookup = contrast(filt_low, filt_hi, britlo, brithi, filt_low, filt_hi)
        elif stretch == 'Histogram':
            """ h = hist(field(vals), [0], [post_hi - post_low]) """
            h = makeHistogram(vals, element_size, line_size, filt_low, brithi - britlo)
            lookup = histoStretch(filt_low, filt_hi, in_low, in_hi, h)
            
        vals = modify(vals, element_size, line_size, filt_low, lookup)
        rangeObject.setSamples(vals)
        
    return newData


def highPass2DFilter(sdataset, user_stretchval='Contrast', user_britlo=0, user_brithi=255):
    """ 2 dimensional high pass filter from McIDAS-X
        equation for each sdataset element = (sdataset - (sample average) + (sample midpoint))
        user_britlo    - minimum brightness value for the calibration
        user_brithi    - maximum brightness value for the calibration
    """
    
    newData = sdataset.clone()
    stretch = user_stretchval
    britlo = int(user_britlo)
    brithi = int(user_brithi)
    
    for t in xrange(newData.getDomainSet().getLength()):
        rangeObject = newData.getSample(t)
        vals = rangeObject.getFloats(0)
        in_hi = max(vals[0])
        in_low = min(vals[0])
        domain = GridUtil.getSpatialDomain(rangeObject)
        [element_size, line_size] = domain.getLengths()
        
        """ first and last 2 lines of the image do not change """
        firstLine = vals[0][0:element_size]
        last2Lines = vals[0][(line_size - 2) * element_size:line_size * element_size]
        
        """ do the filter using 3 lines at a time """
        for i in range(line_size)[:-3]:
            for j in range(element_size)[1:-1]:
                midValue = vals[0][(i + 1) * element_size + j]
                
                val = (vals[0][(i + 2) * element_size + j] + vals[0][i * element_size + j] + \
                      vals[0][(i + 1) * element_size + j + 1] + vals[0][(i + 1) * element_size + j - 1]) - \
                      4 * midValue
                
                if (val < midValue):
                    vals[0][i * element_size + j] = scaleOutsideVal(midValue - val, britlo, brithi)
                else:
                    vals[0][i * element_size + j] = 0
                    
        vals[0][0:line_size * element_size] = firstLine + vals[0][0:(line_size - 3) * element_size] + last2Lines
        
        for i in range(line_size):
            for j in range(element_size):
                vals[0][i * element_size + j] = scaleOutsideVal(vals[0][i * element_size + j], britlo, brithi)
                
        filt_low = int(min(vals[0]))
        filt_hi = int(max(vals[0]))
        
        if stretch == 'Contrast':
            lookup = contrast(filt_low, filt_hi, britlo, brithi, filt_low, filt_hi)
        elif stretch == 'Histogram':
            h = makeHistogram(vals, element_size, line_size, filt_low, brithi - britlo)
            lookup = histoStretch(filt_low, filt_hi, in_low, in_hi, h)
            
        vals = modify(vals, element_size, line_size, filt_low, lookup)
        rangeObject.setSamples(vals)
        
    return newData


def holeFilter(sdataset, user_brkpoint1=0, user_brkpoint2=1, user_stretchval='Contrast', user_britlo=0, user_brithi=255):
    """ hole filter from McIDAS-X - searches for missing data and fills the holes
          using the surrounding element values
        brkpoint1 - low end breakpoint value (default = minimum sdataset value)
        brkpoint2 - high end breakpoint value (default = maximum sdataset value)
        user_britlo    - minimum brightness value for the calibration
        user_brithi    - maximum brightness value for the calibration
    """
    
    data = sdataset.clone()
    brkpoint1 = int(user_brkpoint1)
    brkpoint2 = int(user_brkpoint2)
    stretch = user_stretchval
    britlo = int(user_britlo)
    brithi = int(user_brithi)
    
    for t in xrange(data.getDomainSet().getLength()):
        rangeObject = data.getSample(t)
        vals = rangeObject.getFloats(0)
        in_low = min(vals[0])
        in_hi = max(vals[0])
        minVal = min([brkpoint1, brkpoint2])
        maxVal = max([brkpoint1, brkpoint2])
        domain = GridUtil.getSpatialDomain(rangeObject)
        [element_size, line_size] = domain.getLengths()
        
        for i in range(line_size):
            for j in range(element_size)[1:-1]:
                curVal = vals[0][i * element_size + j]
                """ search line for bad values """
                if curVal >= minVal and curVal <= maxVal:
                    """ look for the next good value """
                    doFill = 0
                    for k in range(element_size)[j:]:
                        nextVal = vals[0][i * element_size + k]
                        if nextVal < minVal or nextVal > maxVal:
                            doFill = 1
                            break
                            
                    if doFill == 1:
                        for fill in range(element_size)[j:k]:
                            vals[0][i * element_size + fill] = (vals[0][i * element_size + j - 1] + vals[0][i * element_size + k]) / 2
                            
        for i in range(line_size):
            for j in range(element_size):
                vals[0][i * element_size + j] = scaleOutsideVal(vals[0][i * element_size + j], britlo, brithi)
                   
        filt_low = int(min(vals[0]))
        filt_hi = int(max(vals[0]))
        
        if stretch == 'Contrast':
            lookup = contrast(filt_low, filt_hi, britlo, brithi, filt_low, filt_hi)
        elif stretch == 'Histogram':
            h = makeHistogram(vals, element_size, line_size, filt_low, brithi - britlo)
            lookup = histoStretch(filt_low, filt_hi, in_low, in_hi, h)
            
        vals = modify(vals, element_size, line_size, filt_low, lookup)
        rangeObject.setSamples(vals)
        
    return data


def contrast(in_low, in_hi, out_low, out_hi, minimum, maximum, inc=1):
    """ create a contrast stretch lookup table
        in_low - input low image value
        in_hi  - input high image value
        out_low - output low image value
        out_hi  - output high image value
        minimum - minimum data value
        maximum - maximum data value
        inc     - increment
    """
    
    smallFloat = 0.00000000000001
    
    if in_hi == in_low:
        slope = 0
    else:
        slope = float(out_hi - out_low) / float(in_hi - in_low)
       
    lookup = []
    for input_value in xrange(minimum, maximum + 1, inc):
        out_value = out_low + int(round((input_value - in_low) * slope + smallFloat))
        if slope < 0:
            out_value = max(min(out_value, out_low), out_hi)
        else:
            out_value = min(max(out_value, out_low), out_hi)
          
        indx = input_value - minimum
        
        for i in xrange(inc):
            lookup.insert(indx, out_value)
            indx = indx + 1
            
    return lookup


def histoStretch(in_low, in_hi, out_low, out_hi, histogram, num_bins=16):
    """ create a histogram stretch lookup table
        in_low - input low image value
       in_hi  - input high image value
       out_low - output low image value
       out_hi  - output high image value
       inc     - increment
    """
    
    step = int((out_hi - out_low + 1) / float(num_bins))
    half_step = int(step / 2)
    breakpoints = [out_low + ((m + 1) * step - half_step) for m in xrange(num_bins)]
    
    start = 0
    end = in_hi - in_low + 1
    lookup = []
    
    for i in xrange(num_bins):
        npop = 0
        ntot = 0
        for j in range(start, end):
            if histogram[j] != 0:
                npop = npop + 1
            ntot = ntot + histogram[j]
            
        nleft = num_bins - i
        nvals_bin = round(ntot / nleft)
        
        if npop >= nleft:
            mtot = 0
            for k in range(start, end):
                mtot = mtot + histogram[k]
                if mtot > nvals_bin:
                    break
                lookup.insert(k, breakpoints[i])
                
            start = k + 1
            lookup.insert(k, breakpoints[i])
        else:
            l = i
            for m in range(start, end):
                lookup.insert(m, breakpoints[l])
                if histogram[m] != 0:
                    l = l + 1
            return lookup
            
    return lookup


def modify(vals, element_size, line_size, minimum, lookup):
    """ modifies an image with the lookup table generated by a stretch function """
    for i in range(line_size):
        for j in range(element_size):
            vals[0][i * element_size + j] = lookup[int(vals[0][i * element_size + j] - minimum)]
    return vals


def makeHistogram(vals, element_size, line_size, minimum, nbins):
    """ Initialize a histogram for the image when using histogram stretch option
        minimum is the minimum of the dataset or datasets
    """
    
    hist = []
    for k in range(nbins + 1):
        hist.append(0)
        
    """ len(vals) allows for 2 or more datasets """
    for i in range(len(vals)):
        for j in range(line_size):
            for k in range(element_size):
                index = int(vals[i][j * element_size + k] - minimum)
                hist[index] = hist[index] + 1
                
    return hist


def printValueDiff(sdataset1, sdataset2):
    data1 = sdataset1.clone()
    data2 = sdataset2.clone()
    
    for t in xrange(data1.getDomainSet().getLength()):
        rangeObj1 = data1.getSample(t)
        rangeObj2 = data2.getSample(t)
        vals1 = rangeObj1.getFloats(0)
        """ vals2 = rangeObj1.getFloats(0)  TODO: bug? """
        vals2 = rangeObj2.getFloats(0)
        domain = GridUtil.getSpatialDomain(rangeObj1)
        [element_size, line_size] = domain.getLengths()
        
        for i in xrange(line_size):
            for j in xrange(element_size):
                print i, j, vals1[0][i * element_size + j] - vals2[0][i * element_size + j]


def printVals(sdataset):
    data = sdataset.clone()
    
    for t in xrange(data.getDomainSet().getLength()):
        rangeObj = data.getSample(t)
        vals = rangeObj.getFloats(0)
        domain = GridUtil.getSpatialDomain(rangeObj)
        [element_size, line_size] = domain.getLengths()
        
        for i in range(line_size):
            for j in range(element_size):
                print i, j, vals[0][i * element_size + j]


def scaleOutsideVal(val, brit_lo=0, brit_hi=255):
    """ this appears to be what McIDAS-X IMGFILT does when values outside of
        britlo/brithi range
    """
    numBritVals = brit_hi - brit_lo + 1
    div = abs(int(val) / numBritVals)
    if val > brit_hi:
        val = val - div * numBritVals
    elif val < brit_lo:
        val = val + div * numBritVals
        
    return val

def lowPass2DRound(val):
  """ applies rounding behavior to account for rounding differences between McIDAS-X and Jython 
      sometimes data in V comes in as .499999 when in -X, it comes in as .500000, so rounding different """
  
  fraction=val%1.0
  
  strfrac=str(fraction)
  
  if strfrac[2:8] == '499999':
     roundVal=round(round(val,5))
  elif strfrac[2:7] == '49999': 
     roundVal=round(round(val,4)) 
  else:
     roundVal=round(val)
  

  return roundVal