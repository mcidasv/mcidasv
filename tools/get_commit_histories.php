#!/usr/bin/php
<?php

date_default_timezone_set('America/Chicago');

#cvs checkout mc-v
#svn co http://svn.ssec.wisc.edu/repos/visad
#svn co http://svn.unidata.ucar.edu/repos/idv
#git clone https://github.com/Unidata/IDV.git

$MCV_ROOT = "/home/mcidasv/mc-v";
$VISAD_ROOT = "/home/mcidasv/svn_nightly/visad";
$IDV_ROOT = "/home/mcidasv/git_nightly/IDV";

$DATE = date("Y-m-d", strtotime("yesterday"));
$END = date("Y-m-d", strtotime("yesterday"));
$HTML = false;
$OUT = false;

$options = getopt("s:e:o:wh");
if (isset($options["s"])) {
  $DATE = date("Y-m-d", strtotime($options["s"]));
}
if (isset($options["e"])) {
  $END = date("Y-m-d", strtotime($options["e"]));
}
if (isset($options["o"])) {
  $OUT = $options["o"];
}
if (isset($options["w"])) {
  $HTML = true;
}
if (isset($options["h"])) {
  print "\n";
  print "Usage: $argv[0] [options]\n";
  print "  -s YYYY-MM-DD (start date, default: yesterday)\n";
  print "  -e YYYY-MM-DD (end date, default: yesterday)\n";
  print "  -o DIRECTORY  (write output to this directory)\n";
  print "  -w            (html output for web)\n";
  print "  -h            (this help message)\n";
  print "\n";
  exit(0);
}
if ($OUT && !is_dir($OUT)) {
  print "ERROR: ".$OUT." is not a directory\n";
  exit(1);
}

$allCommits = array();

$output = fopen("php://stdout", "w");
if ($DATE == $END) {
  if ($OUT) {
    fclose($output);
    $output = fopen($OUT."/".$DATE.".html", "w");
  }
  if ($HTML) {
    fwrite($output, "<html><head><title>");
  }
  fwrite($output, "Commits for date ".$DATE);
}
else {
  if ($OUT) {
    fclose($output);
    $output = fopen($OUT."/".$DATE."_".$END.".html", "w");
  }
  if ($HTML) {
    fwrite($output, "<html><head><title>");
  }
  fwrite($output, "Commits for date range ".$DATE." to ".$END);
}
if ($HTML) {
  fwrite($output, "</title>\n");
  fwrite($output, "<style>\n");
  fwrite($output, ".mcidas-v { color: darkblue; }\n");
  fwrite($output, ".visad { color: darkred; }\n");
  fwrite($output, ".idv { color: darkgreen; }\n");
  fwrite($output, "</style>\n");
  fwrite($output, "</head><body><pre>\n");
}
else {
  fwrite($output, "\n");
}

# Get the diffs from McV (CVS)
if (!$HTML) {
  print "Getting commit logs from McIDAS-V CVS...\n";
}
$commits = getCVSCommits($MCV_ROOT, $DATE, $END);
foreach ($commits as $date=>$commitArray) {
  if (!isset($allCommits["$date"])) {
    $allCommits["$date"] = array();
  }
  foreach ($commitArray as $commit) {
    $commit["package"] = "McIDAS-V";
    array_push($allCommits["$date"], $commit);
  }
}

# Get the diffs from VisAD (SVN)
if (!$HTML) {
  print "Getting commit logs from VisAD SVN...\n";
}
$commits = getSVNCommits($VISAD_ROOT, $DATE, $END);
foreach ($commits as $date=>$commitArray) {
  if (!isset($allCommits["$date"])) {
    $allCommits["$date"] = array();
  }
  foreach ($commitArray as $commit) {
    $commit["package"] = "VisAD";
    array_push($allCommits["$date"], $commit);
  }
}

# Get the diffs from IDV (GitHub)
if (!$HTML) {
  print "Getting commit logs from IDV GitHub...\n";
}
$commits = getGitCommits($IDV_ROOT, $DATE, $END);
foreach ($commits as $date=>$commitArray) {
  if (!isset($allCommits["$date"])) {
    $allCommits["$date"] = array();
  }
  foreach ($commitArray as $commit) {
    $commit["package"] = "IDV";
    array_push($allCommits["$date"], $commit);
  }
}

# Print all commits ordered by date
ksort($allCommits);
foreach ($allCommits as $date=>$commits) {
  foreach ($commits as $commit) {
    $date = $commit["date"];
    $revision = $commit["revision"];
    $author = $commit["author"];
    $file = $commit["file"];
    $package = $commit["package"];
    $description = $commit["description"];
    if ($HTML) {
      fwrite($output, "<div class=\"".strtolower($package)."\">");
    }
    fwrite($output, "$date\t$package\t$file\t$revision\t$author\n");
    fwrite($output, $description."\n\n");
    if ($HTML) {
      fwrite($output, "</div>\n");
    }
  }
}

if ($HTML) {
  fwrite($output, "</pre></body></html>\n");
}

################################################################################
# Functions

function getCVSCommits($DIR, $DATE, $END=false) {
  if (!@chdir($DIR)) {
    print "ERROR: Failed to change to directory: ".$DIR."\n";
    exit(1);
  }
  # Don't explicitly update.  Assume it is current.
  # This is looking at the build dir, we shouldn't mess with that here.
  $allLogs = array();
  $filelist = `cvs diff --brief -D $DATE 2>/dev/null |grep "^Index" |sed -e 's/^Index: //'`;
  $endstamp = strtotime($END);
  foreach (explode("\n", $filelist) as $file) {
    $file = trim($file);
    if (!$file) {
      continue;
    }
    $logs = getCVSLog($file, $DATE, $END);
    foreach ($logs as $date=>$logArray) {
      if ($END && strtotime($date) > $endstamp) {
        continue;
      }
      if (!isset($allLogs["$date"])) {
        $allLogs["$date"] = array();
      }
      foreach ($logArray as $log) {
        array_push($allLogs["$date"], $log);
      }
    }
  }
  return $allLogs;
}

function getCVSLog($FILE, $DATE, $END=false) {
  $linelist = `cvs log -d">=$DATE" $FILE 2>/dev/null`;
  $in_description = false;
  $revision = 0;
  $date = 0;
  $author = "";
  $description = "";
  $log = array();
  $logs = array();
  foreach (explode("\n", $linelist) as $line) {
    $line = trim($line);
    if ($line == "") {
      continue;
    }
    if (!$in_description && preg_match("/^description:/", $line)) {
      $in_description = true;
    }
    if (!$in_description) {
      continue;
    }

    # Now we look for each revision commit
    if (preg_match("/^revision /", $line)) {
      $revision = preg_replace("/^revision /", "", $line);
      continue;
    }
    if (preg_match("/^date: /", $line)) {
      $words = preg_split("/\s+/", $line);
      $date = preg_replace("/\//", "-", $words[1]);
      $author = preg_replace("/;/", "", $words[4]);
      continue;
    }
    if (preg_match("/^----/", $line) ||
        preg_match("/^====/", $line)) {

      # We should have a complete record now
      if ($revision) {
        $description = preg_replace("/\[\d+\]/", "", $description);
        $description = trim($description);
        $log["date"] = $date;
        $log["revision"] = $revision;
        $log["author"] = $author;
        $log["file"] = $FILE;
        $log["description"] = $description;
        if (!isset($logs["$date"])) {
          $logs["$date"] = array();
        }
        array_push($logs["$date"], $log);
      }

      $revision = 0;
      $date = 0;
      $author = "";
      $description = "";
      $log = array();
      continue;
    }

    # Description line
    $description .= $line."\n";
  }

  return $logs;
}

function getSVNCommits($DIR, $DATE, $END=false) {
  if (!@chdir($DIR)) {
    print "ERROR: Failed to change to directory: ".$DIR."\n";
    exit(1);
  }
  `svn update -r HEAD 2>/dev/null`;
  $allLogs = array();

  # Always add one day--needs to be a range to work properly
  if (!$END) {
    $END = date("Y-m-d", strtotime($DATE) + (60*60*24));
  }

  if (!$END) {
    $datespec = "\{$DATE\}";
  }
  else {
    # Add one day to be inclusive on the end
    $END = date("Y-m-d", strtotime($END) + (60*60*24));
    $datespec = "\{$DATE\}:\{$END\}";
  }
  $filelist = `svn diff -r $datespec --diff-cmd "diff" -x "-q" . 2>/dev/null | grep Index | cut -d " " -f 2`;
  foreach (explode("\n", $filelist) as $file) {
    $file = trim($file);
    if (!$file) {
      continue;
    }
    $logs = getSVNLog($file, $DATE, $END);
    foreach ($logs as $date=>$logArray) {
      if (!isset($allLogs["$date"])) {
        $allLogs["$date"] = array();
      }
      foreach ($logArray as $log) {
        array_push($allLogs["$date"], $log);
      }
    }
  }
  return $allLogs;
}

function getSVNLog($FILE, $DATE, $END=false) {
  if (!$END) {
    $TODAY = date("Y-m-d");
    $datespec = "\{$DATE\}:\{$TODAY\}";
  }
  else {
    $datespec = "\{$DATE\}:\{$END\}";
  }
  $linelist = `svn log -r $datespec $FILE 2>/dev/null`;
  $revision = 0;
  $date = 0;
  $author = "";
  $description = "";
  $log = array();
  $logs = array();
  foreach (explode("\n", $linelist) as $line) {
    $line = trim($line);
    if ($line == "") {
      continue;
    }

    # Look for each revision commit
    if (preg_match("/^r\d+\s+\|/", $line)) {
      $words = preg_split("/\|/", $line);
      $revision = preg_replace("/^r/", "", trim($words[0]));
      $author = preg_replace("/;/", "", trim($words[1]));
      $date = preg_replace("/\s.*/", "", trim($words[2]));
      continue;
    }
    if (preg_match("/^----/", $line) ||
        preg_match("/^====/", $line)) {

      # We should have a complete record now
      if ($revision) {
        $description = preg_replace("/\[\d+\]/", "", $description);
        $description = trim($description);
        $log["date"] = $date;
        $log["revision"] = $revision;
        $log["author"] = $author;
        $log["file"] = $FILE;
        $log["description"] = $description;
        if (!isset($logs["$date"])) {
          $logs["$date"] = array();
        }
        array_push($logs["$date"], $log);
      }

      $revision = 0;
      $date = 0;
      $author = "";
      $description = "";
      $log = array();
      continue;
    }

    # Description line
    $description .= $line."\n";
  }

  return $logs;
}

function getGitCommits($DIR, $DATE, $END=false) {
  if (!@chdir($DIR)) {
    print "ERROR: Failed to change to directory: ".$DIR."\n";
    exit(1);
  }
  `git pull`;
  $allLogs = array();
  
  # Always add one day--needs to be a range to work properly
  if (!$END) {
    $END = date("Y-m-d", strtotime($DATE) + (60*60*24));
  }
  
  if (!$END) {
    $datespec = "--since=\"$DATE\"";
  } else {
    # Add one day to be inclusive on the end
    $END = date("Y-m-d", strtotime($END) + (60*60*24));
    $datespec = "--since=\"$DATE\" --until=\"$END\"";
  }
  
  # ex: git log --since="2012-04-01" --until="2012-05-02" --all-match --name-only --pretty="format:" . 
  $filelist = `git log $datespec --all-match --name-only --pretty="format:" .`;
  foreach (array_unique(explode("\n", $filelist)) as $file) {
    $file = trim($file);
    if (!$file) {
      continue;
    }
    $logs = getGitLog($file, $DATE, $END);
    foreach ($logs as $date=>$logArray) {
      if (!isset($allLogs["$date"])) {
        $allLogs["$date"] = array();
      }
      foreach ($logArray as $log) {
        array_push($allLogs["$date"], $log);
      }
    }
  }
  return $allLogs;
}

function getGitLog($FILE, $DATE, $END=false) {
  #git log --since="2012-04-01" --until="2012-05-02" --all-match --pretty=format:"REV %H%%%%DATE %ai%%%%AUTHOR %an%%%%MSG %s"
  if (!$END) {
    $TODAY = date("Y-m-d");
    $datespec = "--since=\"$DATE\"";
  } else {
    $datespec = "--since=\"$DATE\" --until=\"$END\"";
  }
  $linelist = `git log $datespec --all-match --date=short --pretty=format:"%H%%%%%ad%%%%%an%%%%%s" $FILE`;
  $revision = 0;
  $date = 0;
  $author = "";
  $description = "";
  $log = array();
  $logs = array();
  foreach (explode("\n", $linelist) as $line) {
    $line = trim($line);
    if ($line == "") {
      continue;
    }
    list($revision, $date, $author, $description) = explode("%%", $line);
    $log["date"] = $date;
    $log["revision"] = $revision;
    $log["author"] = $author;
    $log["file"] = $FILE;
    $log["description"] = trim($description);
    if (!isset($logs["$date"])) {
      $logs["$date"] = array();
    }
    array_push($logs["$date"], $log);
    $revision = 0;
    $date = 0;
    $author = "";
    $description = "";
    $log = array();
  }
  return $logs;
}