<?xml version="1.0" encoding="UTF-8"?>
<!-- XSLT file to add the a the PicketLink Extension to the standalone.xml 
	of the JBoss AS7 installation. -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:as="urn:jboss:domain:1.5" version="1.0">

	<xsl:output method="xml" indent="yes" />

	<!-- If the extension is already defined, remove it to configure it again. -->
	<xsl:template match="//as:server/as:extensions">
		<extensions>
			<xsl:apply-templates select="@* | *" />
            <extension module="org.picketlink.as.extension"/>
        </extensions>
	</xsl:template>

	<xsl:template match="//as:server/as:profile">
		<profile>
			<xsl:apply-templates select="@* | *" />
            <subsystem xmlns="urn:jboss:domain:picketlink:1.0" />
        </profile>
	</xsl:template>

	<!-- Copy everything else. -->
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>