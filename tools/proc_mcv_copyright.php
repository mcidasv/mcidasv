#!/usr/bin/php -q
<?php

$BASE="/home/mcidasv/mcidasv";

$COPYRIGHT=$BASE."/release/licenses/COPYRIGHT";
$NOTICE=$BASE."/release/licenses/NOTICE";
$VERSION_FILE=$BASE."/edu/wisc/ssec/mcidasv/resources/version.properties";

$README=$BASE."/release/README.html";
$LICENSE=$BASE."/docs/userguide/processed/License.html";

$LICENSE_REMOTE="/var/apache/www/htdocs/mcidas/doc/mcv_guide/working/License.html";

################################################################################

# Make a readable VERSION
$VERSION="";
$YEAR="";
$lines=file($VERSION_FILE);
foreach ($lines as $line) {
  list($var, $val)=explode("=", $line);
  if (trim($var)=="mcidasv.version.major") { $VERSION.=trim($val)."."; }
  if (trim($var)=="mcidasv.version.minor") { $VERSION.=trim($val); }
  if (trim($var)=="mcidasv.version.release") { $VERSION.=trim($val); }
  if (trim($var)=="mcidasv.copyright.year") { $YEAR.=trim($val); }
}

# HTML-ize the VERSION, COPYRIGHT and NOTICE files together
$HTML=array();
array_push($HTML, "<h2>License and Copyright</h2>");
array_push($HTML, "<h3>McIDAS-V</h3>");
array_push($HTML, "Version $VERSION<br>");
array_push($HTML, "<br>");
$HTML=array_merge($HTML, htmlize($COPYRIGHT));
array_push($HTML, "<h2>Other Licenses</h2>");;
$HTML=array_merge($HTML, htmlize($NOTICE));

# Make the README file that is shown in the installer
$README_HTML=array();
array_push($README_HTML, "<html>");
array_push($README_HTML, "<body>");
$README_HTML=array_merge($README_HTML, $HTML);
array_push($README_HTML, "</body>");
array_push($README_HTML, "</html>");
$handle=fopen($README, "w");
foreach($README_HTML as $line) {
  fwrite($handle, "$line\n");
}
fclose($handle);

# Modify the LICENSE file that is shown in the users guide
$LICENSE_HTML=file($LICENSE);
$handle=fopen($LICENSE, "w");
$inMainContent=0;
foreach($LICENSE_HTML as $line) {
  $line=trim($line);
  # Start of the region we are modifying
  if ($line=="<!-- InstanceBeginEditable name=\"MainContent\" -->") {
    $inMainContent=1;
    fwrite($handle, "$line\n");
    foreach ($HTML as $line) {
      $line=preg_replace("/<h2>(.*)<\/h2>/",
        "<div class=\"pagetitle\">$1</div>",
        $line);
      $line=preg_replace("/<h3>(.*)<\/h3>/",
        "<div class=\"pagesubtitle\">$1</div>",
        $line);
      fwrite($handle, "$line\n");
    }
    continue;
  }
  # End of the region we are modifying
  if ($line=="<!-- InstanceEndEditable -->" && $inMainContent) {
    $inMainContent=0;
    fwrite($handle, "$line\n");
    continue;
  }
  # Outside of the region we are modifying
  if (!$inMainContent) {
    fwrite($handle, "$line\n");
  }
}
fclose($handle);

# Required user actions
print "Required actions:\n";
print "  cvs commit $README\n";
print "  scp $LICENSE $LICENSE_REMOTE\n";
$VERSION_OK=`grep "$YEAR" "$COPYRIGHT"`;
if ($VERSION_OK=="")
  print "  $COPYRIGHT and $VERSION_FILE years do not match\n";

################################################################################
# Functions

function htmlize($file) {
  $return=array();
  $lines=file($file);
  $inList=0;
  foreach ($lines as $line) {
    $line=trim($line);
    $line=addLinks($line);
    $line=preg_replace("/Copyright /", "Copyright&copy; ", $line);
    if (trim($line)=="") $line="<br>";
    else $line=$line."<br>";

    # Look for lists
    if (preg_match("/^\* /", $line)) {
      if (!$inList) array_push($return, "<ul>");
      $inList=1;
      $line=preg_replace("/^\* /", "<li> ", $line);
    }
    else {
      if ($inList) array_push($return, "</ul>");
      $inList=0;
    }

    array_push($return, $line);
  }
  return($return);
}

function addLinks($line) {
  $words=preg_split("/\s+/", $line);
  for ($i=0; $i<count($words); $i++) {
    $word=$words[$i];
    $period="";
    if (preg_match("/\.$/", $word)) {
      $period=".";
      $word=preg_replace("/\.$/", "", $word);
    }
    if (preg_match("/^http:\/\//", $word)) {
      $words[$i]="<a href=\"$word\" target=\"_blank\">$word</a>$period";
    }
    if (preg_match("/^\(http:\/\//", $word)) {
      $link=preg_replace("/[\(\)]/", "", $word);
      $words[$i]="(<a href=\"$link\" target=\"_blank\">$link</a>)$period";
    }
  }
  return(implode(" ", $words));
}

?>
