#!/bin/sh

CURPWD=$PWD
USERID=`/usr/bin/id -u -n`

if [ "$CURPWD" != "/home/mcidasv/mcidasv/tools" -o "$USERID" != "mcidasv" ]; then
	echo "This script MUST be run as user \"mcidasv\" from \"/home/mcidasv/mcidasv/tools\""
	exit 1
fi

echo ""
echo "################################################################################"
echo "This script will prompt you to update McIDAS-V version info, create new"
echo "installers, and archive and upload the new release to the webserver."
echo ""
echo "You can cancel at any time by typing CTRL+C."
echo ""
echo "Shall we begin?"
echo "[Enter to continue]"
echo ""
read CONTYN

echo "################################################################################"
echo "Update version (update_version.sh)"
./update_versions.sh

echo "################################################################################"
echo "Make installers (ant dist packsource installers)"
echo ""
echo "This process will create new installers and place them in ~/install4j/media."
echo "[Enter to continue]"
read CONTYN
rm -f /home/mcidasv/install4j/media/McIDAS-V*
cd ..
/home/mcidasv/bin/ant dist packsource installers
cd $CURPWD
echo ""

echo "################################################################################"
echo "Archive installers (manual)"
echo ""
echo "If these are release candidate installers, please copy them from"
echo "~/install4j/media to ~/archive/[version].  This is NOT done automatically."
echo ""

echo "################################################################################"
echo "Copy installers to webserver (manual)"
echo ""
echo "Log on to the webserver (www.ssec.wisc.edu) and run the script:"
echo "  /var/apache/www/htdocs/mcidas/private/bin/get_mcv_release.bash"
echo ""
echo "This will copy the new installers to the appropriate directory."
echo "NOTE: All directories beginning with \"/var/apache/www\" refer to the webserver."
echo ""

echo "################################################################################"
echo "Create new User's Guide (manual)"
echo ""
echo "Visit this URL, substituting the new version:"
echo "  http://www.ssec.wisc.edu/mcidas/doc/mcv_guide/working/combine_all.php?version=[version]"
echo ""
echo "This creates a new HTML page of the User's Guide with the appropriate version."
echo "You can turn this into a PDF using Adobe tools on Windows or on any Mac:"
echo "  1) File > Print"
echo "  2) PDF > Save as PDF..."
echo ""
echo "The resulting .pdf file sould be uploaded to:"
echo "  /var/apache/www/htdocs/mcidas/doc/mcv_guide/[version]/mcv_guide.pdf"
echo ""  

echo "################################################################################"
echo "Update the version to the next beta1 (update_version.sh)"
echo ""
echo "Increment the version and flag it as \"beta1\".  For example, if you just "
echo "created version 1.4, change all versions to 1.5beta1.  This will prevent"
echo "confusion between the next unstable nightly and the release."
./update_versions.sh

echo "################################################################################"
echo "Commit the release to the webpage (manual)"
echo ""
echo "When you are ready to make the release public, simply update the file:"
echo "  /var/apache/www/htdocs/mcidas/software/v/stable/versiontxt"
echo ""
echo "NOTE: Nightlies are NOT updated on the webserver when the contents of"
echo "version.txt does not match the greatest version directory!  This prevents the"
echo "automatic scripts from updating the unstable nightlies during the release"
echo "process.  See:"
echo "  /var/apache/www/htdocs/mcidas/private/bin/check_stable_release.bash"
echo ""


echo "################################################################################"
echo "$0 complete!"
echo ""
