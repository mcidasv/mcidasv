Setting up the support request mailer is mostly pretty simple. You need your
webserver to support PHP 4 or later, and edu.wisc.ssec.mcidasv.supportform.Submitter
needs to be told where to look for the mailer.

On the current SSEC webserver, I have everything living in 
/var/apache/dw_www/htdocs/mcidas/misc/mc-v/supportreq

McV looks for the support request form using the following code:
    private final String requestUrl = "http://www.ssec.wisc.edu/mcidas/misc/mc-v/supportreq/support.php";

If the location of the PHP mailer scripts changes, you'll need to modify
requestUrl to point to the new location.