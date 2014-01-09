<?xml version="1.0" encoding="UTF-8"?>
<!-- XSLT file to add Picket link common exported dependency. -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:m="urn:jboss:module:1.1"
    >

    <xsl:output method="xml" indent="yes"  />

    <xsl:template match="//m:module/m:dependencies" xmlns="urn:jboss:module:1.1">
        <dependencies>
            <module name="org.picketlink.common" export="true" />

            <xsl:apply-templates select="@* | *" />
        </dependencies>
    </xsl:template>


    <!-- Copy everything else. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>