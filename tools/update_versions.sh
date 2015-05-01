#!/bin/sh

FILES="
../release/mcidasv.install4j
../edu/wisc/ssec/mcidasv/resources/version.properties
../release/README.html
../docs/userguide/processed/License.html
../docs/userguide/processed/TOC.xml
../docs/userguide/processed/toc.html
"

WEB="
/var/apache/www/htdocs/mcidas/doc/mcv_guide/working/License.html
/var/apache/www/htdocs/mcidas/doc/mcv_guide/working/TOC.xml
/var/apache/www/htdocs/mcidas/doc/mcv_guide/working/toc.html
/var/apache/www/htdocs/mcidas/doc/mcv_guide/working/ReleaseNotes.html
"

echo ""
echo "You will be editing files containing the McIDAS-V version string."
echo "In each file, update the version and save the changes (using :wq)."
echo "[Enter to continue]"
read CONTYN

for FILE in $FILES; do
	vi $FILE
done

echo ""
echo "Done editing files."
echo -n "Commit to GIT [y/N]? "
read COMMITYN

echo ""
if [ "$COMMITYN" = "y" ]; then
	git commit $FILES
	if [ $? -ne 0 ]; then
		echo "GIT push aborted."
	else
		echo "Pushing commits to master..."
		cd .. && git push --quiet origin master && cd -
		echo ""
		echo "NOTE: Update these files on the webserver to reflect your version changes:"
		echo ""
			for FILE in $WEB; do
			echo "  $FILE"
		done
	fi
else
	echo "GIT commit skipped.  Any changes will be overwritten with the next GIT update."
fi
echo ""
