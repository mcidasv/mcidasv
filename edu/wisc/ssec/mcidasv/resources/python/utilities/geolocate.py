"""IP address geolocation."""

import json
from java.io import BufferedReader, InputStreamReader
from java.net import URL, InetAddress

def _myip():
    """Return your current IP address.

    Uses Java native networking to avoid Jython 2.7.4 socket bugs.
    
    Returns:
        String containing your IP.
    """
    try:
        return InetAddress.getLocalHost().getHostAddress()
    except Exception as e:
        print("Error fetching local IP: %s" % str(e))
        return "127.0.0.1"

def geolocate(ip=None):
    """Fetch latitude and longitude of an IP address.

    Geolocates an IP address (or the current machine's public IP if unspecified).
    Uses Java native networking to bypass Jython 2.7.4 urllib2/socket bugs.
    
    Args:
        ip: string containing the IP address to geolocate. Default is None, 
            which means that this function will use the user's current IP.
    
    Returns:
        tuple: (latitude, longitude) as floats (e.g., (43.0761, -89.4104)),
               or (None, None) if an error occurs.
    """
    if ip:
        endpoint = "https://ipapi.co/%s/json/" % ip.strip()
    else:
        endpoint = "https://ipapi.co/json/"

    try:
        url = URL(endpoint)
        connection = url.openConnection()
        connection.setRequestProperty("User-Agent", "McIDAS-V/Jython")
        connection.setConnectTimeout(5000)
        connection.setReadTimeout(5000)

        reader = BufferedReader(InputStreamReader(connection.getInputStream(), "UTF-8"))
        lines = [line for line in iter(reader.readLine, None)]
        reader.close()

        data = json.loads("".join(lines))
        return float(data["latitude"]), float(data["longitude"])
    except Exception as e:
        print("Geolocation error: %s" % str(e))
        return None, None