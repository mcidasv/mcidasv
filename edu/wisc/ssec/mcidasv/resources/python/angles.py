import math
from datetime import datetime, timedelta
from math import cos, sin, acos, radians, degrees
def cos_solar_zenith(field, visad_datetime):

    """
    Use this function to access the SunRelativePosition java code which returns cosine of
    solar zenith angle matching the visad_datetime of the field.

    Input:
        field - visad.FlatField of data
        visad_datetime - time matching flat field
    Returns:
        cosine of solar zenith angle for every pixel in the field.
    """

    from edu.wisc.ssec.mcidasv.util import SunRelativePosition

    targetSet = getDomainSet(field)
    javaDate = convertDateTimeToJavaUtilDate(visad_datetime)
    solzens = field.clone()
    latlons = getLatLons(targetSet)

    # javaDate = convertDateTimeToJavaUtilDate(t)
    SunRelativePosition.getCosSolarZenith(latlons[1], latlons[0], javaDate, solzens)

    return solzens

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

def getHourMinuteSecondDayFromJavaDate(java_date):
    """
    Extracts the hour, minute, second, and day of the year from a java.util.Date object.

    Parameters:
        java_date (java.util.Date): The Java date object.

    Returns:
        tuple: (hour, minute, second, day_of_year)
    """
    from java.text import SimpleDateFormat
    from java.util import Calendar, GregorianCalendar

    calendar = GregorianCalendar()
    calendar.setTime(java_date)

    hour = calendar.get(Calendar.HOUR_OF_DAY)  # 24-hour clock
    minute = calendar.get(Calendar.MINUTE)
    second = calendar.get(Calendar.SECOND)
    day_of_year = calendar.get(Calendar.DAY_OF_YEAR)

    return (hour, minute, second, day_of_year)


def getAzimuth(latitude, longitude, visad_datetime, convertDateTimeToJavaUtilDate):
    """
    Calculates the azimuth angle of the sun at a given location and time, using a visad.DateTime.

    Parameters:
        latitude (float): The latitude of the location.
        longitude (float): The longitude of the location.
        visad_datetime (visad.DateTime): The visad date and time for which to calculate the azimuth.
        convertDateTimeToJavaUtilDate (function): Function to convert visad.DateTime to java.util.Date.

    Returns:
        float: The azimuth angle of the sun in degrees.
    """

    # Convert visad.DateTime to java.util.Date
    java_date = convertDateTimeToJavaUtilDate(visad_datetime)

    # Extract hour, minute, second, and day of the year from java.util.Date
    hour, minute, second, day_of_year = getHourMinuteSecondDayFromJavaDate(java_date)

    # Convert latitude and longitude to radians
    latitude_rad = radians(latitude)
    longitude_rad = radians(longitude)

    # Simplified formula to calculate solar noon (this is a placeholder and needs proper calculation)
    solar_noon = 12  # Assuming solar noon at 12:00 local time, this is not accurate

    # Calculate the hour angle (HRA)
    time_difference = hour + minute / 60 - solar_noon
    hra = 15 * time_difference  # 15 degrees per hour

    # Declination of the Sun as a simplified estimate (placeholder, needs actual calculation)
    declination = 23.44 * sin(radians(360 / 365 * (day_of_year + 10)))

    # Calculate the elevation angle (this is also a simplification)
    elevation = degrees(acos(
        cos(radians(declination)) * cos(latitude_rad) * cos(radians(hra)) + sin(radians(declination)) * sin(
            latitude_rad)))

    # Calculate the azimuth angle (simplified, and assumes the sun is always in the southern hemisphere)
    azimuth = degrees(acos((sin(radians(declination)) - sin(latitude_rad) * sin(radians(elevation))) / (
            cos(latitude_rad) * cos(radians(elevation)))))

    # Adjust azimuth based on time of day
    if hra > 0:
        azimuth = 360 - azimuth

    return azimuth




def getSolarZenith(latitude, longitude, visad_datetime, convertDateTimeToJavaUtilDate):
    """
    Calculates the solar zenith angle at a given location and time, using a visad.DateTime.

    Parameters:
        latitude (float): The latitude of the location.
        longitude (float): The longitude of the location.
        visad_datetime (visad.DateTime): The visad date and time for which to calculate the solar zenith angle.
        convertDateTimeToJavaUtilDate (function): Function to convert visad.DateTime to java.util.Date.

    Returns:
        float: The solar zenith angle in degrees.
    """

    # Convert visad.DateTime to java.util.Date
    java_date = convertDateTimeToJavaUtilDate(visad_datetime)

    # Extract hour, minute, second, and day of the year from java.util.Date
    hour, minute, second, day_of_year = getHourMinuteSecondDayFromJavaDate(java_date)

    # Convert latitude to radians
    latitude_rad = math.radians(latitude)

    # Approximate the solar declination as per day of the year
    declination = 23.45 * math.sin(math.radians((360 / 365) * (day_of_year - 81)))
    declination_rad = math.radians(declination)

    # Calculate the hour angle
    solar_noon = 12  # Approximate time of solar noon
    hour_angle = 15 * (hour + minute / 60 + second / 3600 - solar_noon)
    hour_angle_rad = math.radians(hour_angle)

    # Calculate the solar zenith angle
    cos_zenith = math.sin(latitude_rad) * math.sin(declination_rad) + \
                 math.cos(latitude_rad) * math.cos(declination_rad) * math.cos(hour_angle_rad)
    zenith_angle = math.acos(cos_zenith)

    # Convert zenith angle from radians to degrees
    zenith_angle_deg = math.degrees(zenith_angle)

    return zenith_angle_deg


def getElevation(latitude, longitude, visad_datetime, convertDateTimeToJavaUtilDate):
    """
    Calculates the elevation angle of the sun above the horizon at a given location and time, using a visad.DateTime.

    Parameters:
        latitude (float): The latitude of the location.
        longitude (float): The longitude of the location.
        visad_datetime (visad.DateTime): The visad date and time for which to calculate the elevation angle.
        convertDateTimeToJavaUtilDate (function): Function to convert visad.DateTime to java.util.Date.

    Returns:
        float: The elevation angle of the sun in degrees.
    """
    # Ensure getSolarZenith is modified to accept visad.DateTime and the conversion function
    solar_zenith_angle = getSolarZenith(latitude, longitude, visad_datetime, convertDateTimeToJavaUtilDate)

    # Calculate the elevation angle
    elevation_angle = 90 - solar_zenith_angle

    return elevation_angle