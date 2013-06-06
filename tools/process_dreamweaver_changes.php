#!/usr/bin/php -q
<?php

function ValidateTOC($tocpath) {
    global $EMAIL;
    $notify = "";
    $lines = file($tocpath);
    $valid_link = "[A-Za-z0-9#\.\/\" ]+";
    foreach ($lines as $line) {
        if (!preg_match("/name=.* id=.* href=.*/", $line)) {
            continue;
        }
        $nameidhref = preg_replace("/.*name=($valid_link).* id=($valid_link).* href=($valid_link).*/", "$1|$2|$3", $line);
        $each = explode("|", trim($nameidhref));
        if ($each[0] != $each[1] || $each[0] != $each[2]) {
            $notify .= "toc.html has mismatched name/id/href for $each[0]\n";
        }
    }
    if ($notify !== "") {
        mail("$EMAIL","AUTO: Problems with McV toc.html", "$notify");
    }
}

function ValidateVersions() {
    global $DEBUG, $EMAIL;
    $notify = "";
    $version = "";
    $CUR_DIR = getcwd();
    
    # Initialize with version.properties
    chdir("..");
    $lines = file("edu/wisc/ssec/mcidasv/resources/version.properties");
    foreach ($lines as $line) {
        if (preg_match("/major\s+=/", $line)) {
           $version .= trim(preg_replace("/.*major\s+=/","", $line)).".";
        }
        if (preg_match("/minor\s+=/", $line)) {
            $version .= trim(preg_replace("/.*minor\s+=/","", $line));
        }
        if (preg_match("/release\s+=/", $line)) {
            $version .= trim(preg_replace("/.*release\s+=/","", $line));
        }
    }
    
    # Compare to mcidasv.install4j
    $lines = file("release/mcidasv.install4j");
    $versioncheck = "";
    foreach ($lines as $line) {
        if (preg_match("/<application.*version=/", $line)) {
            $versioncheck .= trim(preg_replace("/.* version=\"([^\"]+)\".*/","\\1", $line));
        }
    }
    if ($version!=$versioncheck || $version=="") {
        $notify.="Inconsistent versions between version.properties and mcidasv.install4j\n";
    }
    
    # Compare to doc
    $files = array("release/README.html",
                   "docs/userguide/processed/License.html",
                   "docs/userguide/processed/TOC.xml",
                   "docs/userguide/processed/toc.html");
    foreach ($files as $file) {
        $versioncheck = "";
        $lines = file($file);
        foreach ($lines as $line) {
            $line = trim($line);
            if (preg_match("/^Version /", $line)) {
                $versioncheck = preg_replace("/^Version ([^ \"]+)[< \"].+$/", "\\1", $line);
                break;
            }
            else if (preg_match("/McIDAS-V.* Version /", $line)) {
                $versioncheck = preg_replace("/.* Version ([^ \"]+)[< \"].+$/", "\\1", $line);
                break;
            }
        }
        if ($version != $versioncheck) {
          $notify .= "Inconsistent versions between version.properties and $file\n";
        }
    }
    
    # Send mail if any inconsistencies were found
    if ($notify != "") {
        if (!$DEBUG) {
            mail("$EMAIL", "AUTO: Problems with McV versions", "$notify");
        }
        else {
            print "AUTO: Problems with McV versions\n\n".$notify."\n";
        }
    }
}

function StageNewFiles($filepaths) {
    foreach ($filepaths as $filepath) {
        // print "git add \"$filepath\"\n";
        `git add "$filepath"`;
    }
}

function StageDeletedFiles($filepaths) {
    foreach ($filepaths as $filepath) {
        // print "git rm \"$filepath\"\n";
        `git rm "$filepath"`;
    }
}

function StageModifiedFiles($filepaths) {
    foreach ($filepaths as $filepath) {
        // print "git add \"$filepath\"\n";
        `git add "$filepath"`;
    }
}

function CommitStagedChanges($inquiry, $message) {
    // print "git commit -m \"[$inquiry] $message\"\n";
    `git commit -m "[$inquiry] $message"`;
}

function PushLocalCommits() {
    `git push --quiet origin master`;
}

function ParseGitStatus($ACTIVITY_DIR) {
    $lines = explode("\n", `git status --porcelain`);
    $untracked_files = array();
    $modified_files = array();
    $removed_files = array();
    foreach ($lines as $line) {
        $status = substr($line, 0, 2);
        $filepath = substr($line, 3);
        
        // print "status: '$status' filepath: '$filepath' line: '$line'\n";
        if (substr($filepath, 0, 1) === "\"") {
            $filepath = substr($filepath, 1);
        }
        
        if (substr($filepath, -1, 1) === "\"") {
            $filepath = substr($filepath, 0, -1);
        }
        
        // DO NOT TRIM STATUS!!!
        // $filepath = "\"$ACTIVITY_DIR/".trim($filepath)."\"";
        
        // git status --porcelain results in file paths that are relative to
        // the root of the repository, *not* the current directory.
        $relativepath = "../" . $filepath;
        if (substr($relativepath, 0, strlen($ACTIVITY_DIR)) !== $ACTIVITY_DIR) {
            // print "not interested in files outside '$ACTIVITY_DIR'; filepath: $filepath\n";
            continue;
        } else {
            print "processing '$filepath'\n";
        }
        
        switch ($status) {
            case "A ":
                // print "added    [tracked  ] filepath: $filepath\n";
                break;
            case "M ":
                // print "modified [tracked  ] filepath: $filepath\n";
                break;
            case " M": case "AM":
                // print "modified [untracked] filepath: $filepath\n";
                array_push($modified_files, $filepath);
                break;
            case "D ":
                // print "deleted  [tracked  ] filepath: $filepath\n";
                break;
            case " D":
                // print "deleted  [untracked] filepath: $filepath\n";
                array_push($removed_files, $filepath);
                break;
            case "??":
                // print "untracked filepath: $filepath\n";
                array_push($untracked_files, $filepath);
                break;
            default:
                // print "status: '$status' filepath: '$filepath'\n";
                break;
        }
    }
    
    // print "Staging untracked files\n";
    StageNewFiles($untracked_files);
    
    // print "Staging removals\n";
    StageDeletedFiles($removed_files);
    
    // print "Staging modifications\n";
    StageModifiedFiles($modified_files);
    
    if (empty($untracked_files) && empty($removed_files) && empty($modified_files)) {
        return false;
    } else {
        return true;
    }
}

$EMAIL = "beckys@ssec.wisc.edu,barryr@ssec.wisc.edu,davep@ssec.wisc.edu,robert.carp@ssec.wisc.edu,jbeavers@ssec.wisc.edu";

$DEBUG = false;

// $DOC_DIR="/home/mcidasv/mc-v/docs/userguide/processed";
$DOC_DIR = "../docs/userguide/processed";

ValidateTOC("../docs/userguide/processed/toc.html");

ValidateVersions();

$status = ParseGitStatus($DOC_DIR);

if ($status === true) {
    CommitStagedChanges(0, "Dreamweaver updates synced from web server.");
    PushLocalCommits();
} else {
    print "nothing to do...\n";
}
?>
