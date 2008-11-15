#!/usr/bin/php -q
<?php

$DEBUG=0;

$DOC_DIR="../docs/userguide/processed/working";

# $INQUIRY=0 will cause the inquiry parser to ignore the commits
$INQUIRY=0;
$CVS_MESSAGE="[$INQUIRY] Dreamweaver updates synced from web server";

# Change working directory to $DOC_DIR
chdir($DOC_DIR);
$CVS_CLEAN=`find . -name ".#*" |xargs rm -f`;
$CVS_RAW=explode("\n", trim(`cvs diff --brief 2>&1`));

$CVS_PARSED=ParseCVSRaw($CVS_RAW);

if ($DEBUG!=0) print_r($CVS_PARSED);

# Add new directores and files
if (isset($CVS_PARSED["ADD"])) {
  $ADD=$CVS_PARSED["ADD"];

  # Add and commit new directories first
  if (isset($ADD["directories"])) {
    foreach ($ADD["directories"] as $directory) {
      RunCVS("cvs add \"$directory\"");
      RunCVS("cvs commit \"$directory\" -m \"$CVS_MESSAGE (new directory)\"");
    }
  }

  # Add and commit new files
  if (isset($ADD["files"])) {
    foreach ($ADD["files"] as $file) {
      RunCVS("cvs add \"$file\"");
      RunCVS("cvs commit \"$file\" -m \"$CVS_MESSAGE (new file)\"");
    }
  }

}

# Remove deleted files
if (isset($CVS_PARSED["REMOVE"])) {
  $REMOVE=$CVS_PARSED["REMOVE"];

  foreach ($REMOVE as $file) {
    RunCVS("cvs remove \"$file\"");
    RunCVS("cvs commit \"$file\" -m \"$CVS_MESSAGE (deleted file)\"");
  }

}

# Run top-level commit to get all the file changes
RunCVS("cvs commit -m \"$CVS_MESSAGE\"");

################################################################################

# Turn raw CVS diff lines into a meaningful array
function ParseCVSRaw($lines) {

  # Returns:
  # array
  #  ADD
  #   directories
  #    ...
  #   files
  #    ...
  #  REMOVE
  #   ...
  $ADD=array();
  $ADD["directories"]=array();
  $ADD["files"]=array();
  $REMOVE=array();

  foreach ($lines as $line) {

    # Strip out status messages
    if (preg_match("/^cvs diff: Diffing /", $line)) {
      continue;
    }

    # Ignore Dreamweaver .LCK files
    elseif (preg_match("/\.LCK$/", $line)) {
      continue;
    }

    # This is a new file or directory
    elseif (preg_match("/^\? /", $line)) {
      $filename=preg_replace("/^\? /", "", $line);
      if (is_dir($filename)) {
        $processed=ProcessNewDirectory($filename);
        foreach ($processed["directories"] as $directory=>$set) {
          array_push($ADD["directories"], $directory);
        }
        foreach ($processed["files"] as $file=>$set) {
          array_push($ADD["files"], $file);
        }
      }
      else {
        array_push($ADD["files"], $filename);
      }
    }

    # This is a file or directory that has been removed
    elseif (preg_match("/^cvs diff: cannot find /", $line)) {
      $filename=preg_replace("/^cvs diff: cannot find /", "", $line);
      array_push($REMOVE, $filename);
    }

  }

  $RETURN=array();
  $RETURN["ADD"]=$ADD;
  $RETURN["REMOVE"]=$REMOVE;
  return($RETURN);
}

# Descend into a new directory and get subdir and filenames
function ProcessNewDirectory($directory) {
  $RETURN=array();
  $RETURN["directories"]=array();
  $RETURN["files"]=array();
  $files=explode("\n", trim(`cd "$directory" && find . -type f`));
  if (count($files)==1 && $files[0]=="") return($RETURN);
  $RETURN["directories"]["$directory"]=1;
  foreach ($files as $file) {
    $file=preg_replace("/^\.\//", "", $file);
    $dir=dirname($file);
    $file=basename($file);
    $subdirs=explode("/", $dir);
    $directory_tmp=$directory;
    if ($dir!=".") {
      foreach ($subdirs as $subdir) {
        $directory_tmp.="/".$subdir;
        $RETURN["directories"]["$directory_tmp"]=1;
      }
    }
    $file_tmp=$directory_tmp."/".$file;
    $RETURN["files"]["$file_tmp"]=1;
  }

  return($RETURN);
}

# Conditionally print or run the CVS command depending on $DEBUG status
function RunCVS($command) {
  global $DEBUG;

  if ($DEBUG!=0) print "$command\n";
  else `$command`;
}

?>
