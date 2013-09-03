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
"

for FILE in $FILES; do
	vi $FILE
done

echo ""
echo "CTRL+C now to skip GIT commit..."
read CONTYN
git commit $FILES
if [ $? -ne 0 ]; then
	echo "GIT push aborted"
else
	echo "Pushing commits to master..."
	cd .. && git push --quiet origin master && cd -
	echo ""
	echo "Update these files on the webserver:"
	echo ""
	for FILE in $WEB; do
		echo "  $FILE"
	done
fi
