"""IP address geolocation."""

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
        endpoint = "https://mcidas.ssec.wisc.edu/geoip/latlon.php?ip=%s" % ip.strip()
    else:
        endpoint = "https://mcidas.ssec.wisc.edu/geoip/latlon.php"

    try:
        url = URL(endpoint)
        connection = url.openConnection()
        connection.setRequestProperty("User-Agent", "McIDAS-V/Jython")
        connection.setConnectTimeout(5000)
        connection.setReadTimeout(5000)

        reader = BufferedReader(InputStreamReader(connection.getInputStream(), "UTF-8"))
        lines = [line for line in iter(reader.readLine, None)]
        reader.close()

        response_text = "".join(lines).strip()

        # Handle comma-separated ("43.0761,-89.4104") or space-separated values
        if "," in response_text:
            parts = response_text.split(",")
        elif " " in response_text:
            parts = response_text.split()
        else:
            # Handle uncomma'd negative longitudes ("43.0761-89.4104")
            # Split on the minus sign for the longitude, restoring '-' to lon
            idx = response_text.find("-", 1)  # start searching after char 0 in case lat is negative
            if idx != -1:
                parts = [response_text[:idx], response_text[idx:]]
            else:
                parts = []

        if len(parts) == 2:
            return float(parts[0]), float(parts[1])
        else:
            print("Unexpected response format: %s" % response_text)
            return None, None

    except Exception as e:
        print("Geolocation error: %s" % str(e))
        return None, None