<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="//object[@class='ucar.unidata.idv.ui.WindowInfo']/property">
		<property name="PersistentComponents">
		<object class="java.util.Hashtable">
		<xsl:apply-templates />
		</object>
		</property>
	</xsl:template>
</xsl:stylesheet>