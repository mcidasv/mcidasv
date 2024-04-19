from edu.wisc.ssec.mcidasv.util import SunRelativePosition

def convertDateTimeToJavaUtilDate(inputDateTimeObject):
    """
      Purpose:
         Convert a visad.DateTime to a java.Util.Date.

          - If a java.Util.Date object is entered, the java.Util.Date object is returned
          - If a string is entered, this code raises an exception.

      Usage:
         myDate = convertDateTimeToJavaUtilDate(inputDateTimeObject)

      Input:
         inputDateTimeObject - a visad.DateTime object
      Return:
         java.Util.Date

      Uses the following classes.
      - visad.DateTime
      - java.util.Date
    """
    import visad.DateTime
    import java.util.Date
    import java.text.SimpleDateFormat
    from java.util import TimeZone

    if (isinstance(inputDateTimeObject, DateTime)):
        timeZone = inputDateTimeObject.getFormatTimeZone()
        dateTimeFormat = inputDateTimeObject.getFormatPattern()

        sdf = SimpleDateFormat(dateTimeFormat)
        sdf.setTimeZone(timeZone)

        myTimeString = inputDateTimeObject.toValueString()
        convertedDateObject = sdf.parse(myTimeString)
    elif (isinstance(inputDateTimeObject, java.util.Date)):
        convertedDateObject = inputDateTimeObject
    else:
        myException = 'TypeError: The input object is type: "' +str(type(inputDateTimeObject))+ '." This is not a java.util.Date or visad.DateTime'
        raise IOError(myException)

    return convertedDateObject

def getCosSolarZenith(field):

    """
    Use this function to access the SunRelativePosition java code which returns cosine of
    solar zenith angle matching the visad_datetime of the field.

    Input:
        field - visad.FlatField of data
    Returns:
        cosine of solar zenith angle for every pixel in the field
    """

    dataType = type(field)
    if str(dataType)=="<type 'visad.meteorology.ImageSequenceImpl'>":
        targetSet = getDomainSet(field[0])
        dataTime = field[0].getStartTime()
        javaDate = convertDateTimeToJavaUtilDate(dataTime)
        solzens = field[0].clone()
        latlons = getLatLons(targetSet)
    else:
        targetSet = getDomainSet(field)
        dataTime = field.getStartTime()
        javaDate = convertDateTimeToJavaUtilDate(dataTime)
        solzens = field.clone()
        latlons = getLatLons(targetSet)
     
    SunRelativePosition.getCosSolarZenith(latlons[1], latlons[0], javaDate, solzens)

    noUnitCosSolZens = noUnit(solzens)
    degreeCosSolZens = createNewUnit(noUnitCosSolZens,'degrees')

    return degreeCosSolZens

def getSolarZenith(field):

    """
    Use this function to access the SunRelativePosition java code which returns
    solar zenith angle matching the visad_datetime of the field.

    Input:
        field - visad.FlatField of data
    Returns:
        solar zenith angle for every pixel in the field
    """

    dataType = type(field)
    if str(dataType)=="<type 'visad.meteorology.ImageSequenceImpl'>":
        targetSet = getDomainSet(field[0])
        dataTime = field[0].getStartTime()
        javaDate = convertDateTimeToJavaUtilDate(dataTime)
        solzens = field[0].clone()
        latlons = getLatLons(targetSet)
    else:
        targetSet = getDomainSet(field)
        dataTime = field.getStartTime()
        javaDate = convertDateTimeToJavaUtilDate(dataTime)
        solzens = field.clone()
        latlons = getLatLons(targetSet)

    SunRelativePosition.getSolarZenith(latlons[1], latlons[0], javaDate, solzens)

    noUnitSolZens = noUnit(solzens)
    degreeSolZens = createNewUnit(noUnitSolZens,'degrees')

    return degreeSolZens

def getElevationAngle(field):
    """
    Calculates the elevation angle of the sun above the horizon at a given location and time

    Parameters:
        field - visad.FlatField of data

    Returns:
        elevation angle of the sun in degrees
    """
    # Obtain the solar zenith angle
    solar_zenith_angle = getSolarZenith(field)

    # Calculate the elevation angle
    elevation_angle = 90 - solar_zenith_angle

    return elevation_angle
