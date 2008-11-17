<?php

error_reporting(E_ALL ^ E_NOTICE);

$START="mcidasv.html";
$END="index.html";

$startdir=getcwd();
$file=$START;
$stop=0;

?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html><!-- InstanceBegin template="/Templates/mcv_guide.dwt" codeOutsideHTMLIsLocked="false" -->
<head>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<title>McIDAS-V User's Guide</title>
<link rel="stylesheet" href="/mcidas/doc/mcv_guide/mcidasv.css" charset="ISO-8859-1" type="text/css">
</head>
<body>
<?php

while (!$stop) {
  $lines=file($file);
  $main=getMain($lines);
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

function getMain($lines) {
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

?>
