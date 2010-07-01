<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes"/>

    <!-- this copies over all XML that *does not* match another template -->
    <!-- apparently known as the "identity template" -->
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- attach the relevant component group stuff to the end of each WindowInfo -->
    <!-- i'm probably being really sloppy (like with the xsl:copy stuff) -->
    <xsl:template match="//object[@class='ucar.unidata.idv.ui.WindowInfo']">
      <xsl:copy>
        <!-- copy over the contents of the WindowInfo -->
        <xsl:apply-templates select="@* | node()"/>
        <xsl:if test="not(descendant::property[@name='PersistentComponents'])">
        <!-- insert the component group-->
        <property name="PersistentComponents">
          <object class="java.util.Hashtable">
            <method name="put">
              <string><![CDATA[nodeid3]]></string>
              <object class="edu.wisc.ssec.mcidasv.ui.McvComponentGroup" id="id8">
                <property name="ActiveComponentHolder">
                  <object class="edu.wisc.ssec.mcidasv.ui.McvComponentHolder" id="id9">
                    <property name="ViewManagers">
                      <object class="java.util.ArrayList">
                        <method name="add">
                          <xsl:copy-of select="property/object/method/object[@class='ucar.unidata.idv.MapViewManager']"/>
                        </method>
                      </object>
                    </property>
                  </object>
                </property>
              </object>
            </method>
          </object>
        </property>
        </xsl:if>
      </xsl:copy>
    </xsl:template>

    <!-- plonk down my stupid debug test wherever there might be a match for the given xpath-->
    <xsl:template match="//object[@class='ucar.unidata.idv.ui.WindowInfo']/property[@name='ViewManagers']/object[@class='java.util.ArrayList']">
        <object class="java.util.ArrayList"/>
    </xsl:template>

    <xsl:template match="//object[@class='ucar.unidata.idv.ui.IdvComponentGroup']">
        <xsl:element name="dumb">
            <xsl:attribute name="class">
                <xsl:value-of select="edu.wisc.ssec.mcidasv.ui.McvComponentGroup"/>
            </xsl:attribute>
            <xsl:when test="@id">
                <xsl:attribute name="id">
                    <xsl:value-of select="@id"/>
                </xsl:attribute>
            </xsl:when>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>