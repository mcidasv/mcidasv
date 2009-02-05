def _myip():
    """Returns your current IP address.
    
    Returns:
      A string containing your IP.
    """
    from socket import gethostname, gethostbyname
    return gethostbyname(gethostname())

def geolocate(ip=None):
    """Fetches the latitude and longitude of an IP address.
    
    Args:
      ip: string containing the IP address to geolocate. Default is None, 
        which means that this function will use the user's current IP.
    
    Returns:
      A dict containing the location of the given IP. For example:
      
      {'lat': 43.0761, 'lon': -89.4104}
    """
    from urllib2 import urlopen
    ip = ip or _myip()
    url = urlopen('http://dcdbs.ssec.wisc.edu/geoip/latlon.php')
    result = url.read()
    return dict(zip(['lat', 'lon'], map(float, result.split())))