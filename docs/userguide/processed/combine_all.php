<?php

error_reporting(E_ALL ^ E_NOTICE);

$START="mcidasv.html";
$END="toc.html";

$startdir=getcwd();
$file=$START;
$havestart=0;
$stop=0;

$versionfile="/var/apache/www/htdocs/mcidas/software/v/stable/version.txt";
if ($_GET["version"]) {
  $version = $_GET["version"];
}
else {
  $version = file_get_contents($versionfile);
}

?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<title>McIDAS-V User's Guide</title>
<link rel="stylesheet" href="/mcidas/doc/mcv_guide/mcidasv.css" charset="ISO-8859-1" type="text/css">
<style type="text/css">
pre { white-space: normal; }
img { max-width: 900px; }
li { margin-bottom: 0px; }
</style>
</head>
<body style="width:950px;">

<table style="page-break-after:always;
  width:100%; border:0px; margin-top:350px;">
<tr><td style="text-align:center; vertical-align:middle;">
<p>
<img src="../../../images/mcidas-logos/mcidasv_500x250.png">
<h1>McIDAS-V User's Guide</h1>
<h2>Version <?php print $version; ?></h2>
</td></tr>
</table>

<?php

# Add the TOC
$myFile=$END;
$lines=file($myFile);
$main=getMain($myFile,$lines);
$firstTarget=makeTarget($START);
$myTarget=$myFile;
if ($myTarget=="index.html") $myTarget=basename(getcwd()).".html";
print "<a name=\"".makeTarget($myTarget)."\">\n";
print "<div class=\"pagetitle\">Table of Contents</div>\n";
print "<p>\n";
foreach ($main as $line) {
  if (!$havestart) {
    if (preg_match("/$firstTarget/",$line)) $havestart=1;
    else continue;
  }
  print "$line\n";
}
print "<hr style=\"page-break-after: always;\">\n";

# Add all the rest...
while (!$stop) {
  $lines=file($file);
  $myTarget=$file;
  if ($myTarget=="index.html") $myTarget=basename(getcwd()).".html";
  $main=getMain($myTarget,$lines);
  print "<a name=\"".makeTarget($myTarget)."\">\n";
  foreach ($main as $line) {
    print "$line\n";
  }
  print "<hr style=\"page-break-after: always;\">\n";
  $file=getNext($lines);
  if ($file=="") $stop=1;
  if (getcwd()==$startdir && $file==$END) $stop=1;
}

?>
</body>
</html>
<?php

################################################################################

function getNext($lines) {
  foreach ($lines as $line) {
    if (preg_match("/InstanceBeginEditable/",$line) &&
        preg_match("/GoToNext/",$line)) {
      $next=trim($line);
      $next=preg_replace("/.*GoToNext/","",$next);
      $next=preg_replace("/.*href=\"/","",$next);
      $next=preg_replace("/\".*/","",$next);
      $directory=dirname($next);
      if ($directory != "") chdir($directory);
      $next=basename($next);
      return($next);
    }
  }
  return("");
}

function getMain($file,$lines) {
  $haveStart=0;
  $haveEnd=0;
  $depth=0;
  $mainLines=array();
  foreach ($lines as $line) {
    if (preg_match("/InstanceBeginEditable/",$line) &&
        preg_match("/MainContent/",$line)) {
      $haveStart=1;
      continue;
    }
    if (!$haveStart) continue;
    if (preg_match("/InstanceBeginEditable/",$line)) $depth++;
    if (preg_match("/InstanceEndEditable/",$line)) {
      $depth--;
      if ($depth<=0) return($mainLines);
    }
    $line=fixImages($line);
    $line=fixLinks($file,$line);
    $line=fixTargets($file,$line);
    array_push($mainLines,$line);
  }
  return($mainLines);
}

function fixImages($line) {
  global $startdir;
  if (!preg_match("/<img\s+src=\"/",$line)) return($line);
  $line=trim($line);

  $remaining=$line;
  $fixed="";
  while (preg_match("/<img\s+src=\"/",$remaining)) {

    $parts=preg_split("/<img\s+src=\"/",$remaining);
    $pre=array_shift($parts)."<img src=\"";
    $rest=implode("<img src=\"",$parts);

    $parts=preg_split("/\"/",$rest);
    $src=array_shift($parts);
    $remaining="\"".implode("\"",$parts);

    $startdirRegEx=preg_replace("/\//","\\/",$startdir);
    $relative=preg_replace("/$startdirRegEx/","",getcwd());
    if ($relative!="") $src=preg_replace("/^\//","",$relative)."/".$src;

    $fixed.=$pre.$src;
  }
  $fixed.=$remaining;

  return($fixed);
}

function fixLinks($file,$line) {
  if (!preg_match("/\s+href=\"/",$line)) return($line);
  $line=trim($line);

  $remaining=$line;
  $fixed="";
  while (preg_match("/\s+href=\"/",$remaining)) {

    $parts=preg_split("/\s+href=\"/",$remaining);
    $pre=array_shift($parts)." href=\"";
    $rest=implode(" href=\"",$parts);

    $parts=preg_split("/\"/",$rest);
    $href=array_shift($parts);
    $remaining="\"".implode("\"",$parts);

    if (preg_match("/^http/",$href)) {
      $fixed.=$pre.$href;
    }
    else {
      list($targetFile,$targetName)=explode("#",$href);
      if ($targetFile=="") $targetFile=$file;
      $fixed.=$pre."#".makeTarget($targetFile,$targetName);
    }
  }
  $fixed.=$remaining;

  return($fixed);
}

function fixTargets($file,$line) {
  if (!preg_match("/\s+name=\"/",$line)) return($line);
  $line=trim($line);

  $remaining=$line;
  $fixed="";
  while (preg_match("/\s+name=\"/",$remaining)) {

    $parts=preg_split("/\s+name=\"/",$remaining);
    $pre=array_shift($parts)." name=\"";
    $rest=implode(" name=\"",$parts);

    $parts=preg_split("/\"/",$rest);
    $name=array_shift($parts);
    $remaining="\"".implode("\"",$parts);

    $fixed.=$pre.makeTarget($file,$name);
  }
  $fixed.=$remaining;

  return($fixed);
}

function makeTarget($file,$target="") {
  if ($target=="") $target="FILE";
  if (preg_match("/\/index.html$/",$file)) {
    $file=dirname($file).".html";
  }
  if (preg_match("/\//",$file)) {
    $file=basename($file);
  }
  $file=preg_replace("/^\.\//","",$file);
  $file=preg_replace("/\//","_",$file);
  $file=preg_replace("/\.html/","_",$file);
  return("TARGET_".$file."_".$target);
}

?>
