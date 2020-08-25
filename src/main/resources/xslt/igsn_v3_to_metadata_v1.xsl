<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:igsn="https://igsn.csiro.au/schemas/3.0" version="2.0" exclude-result-prefixes="igsn">
	<xsl:output indent="yes" method="xml"/>
	<xsl:param name="registrantName" select="'registrantName'"/>
	<xsl:param name="nameIdentifier" select="'nameIdentifier'"/>
	<xsl:param name="nameIdentifierScheme" select="'nameIdentifierScheme'"/>
	<xsl:param name="eventType" select="'eventType'"/>
	<xsl:param name="timeStamp" select="'timeStamp'"/>
	<!--
    XSLT transformation to create registration metadata v1.1 from an IGSN CSIRO v3.0 record
    -->

	<xsl:template match="/">
		<xsl:apply-templates select="igsn:resources/igsn:resource"/>
	</xsl:template>

	<xsl:template match="igsn:resource">
		<sample xmlns="http://schema.igsn.org/registration/1.1"
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			xsi:schemaLocation="http://schema.igsn.org/registration/1.1 http://schema.igsn.org/registration/1.1/igsn.xsd">
			<xsl:apply-templates select="igsn:resourceIdentifier"/>

			<xsl:element name="registrant">
				<xsl:element name="registrantName">
					<xsl:value-of select="$registrantName"/>
				</xsl:element>

				<xsl:element name="nameIdentifier">
					<xsl:attribute name="nameIdentifierScheme" select="$nameIdentifierScheme"/>
					<xsl:value-of select="$nameIdentifier"/>
				</xsl:element>

			</xsl:element>

			<xsl:apply-templates select="igsn:relatedResources"/>

			<!-- registered, submitted updated deprecated destroyed -->
			<xsl:element name="log" xmlns="http://schema.igsn.org/registration/1.1">
				<xsl:apply-templates select="igsn:logDate"/>
				<xsl:element name="logElement" xmlns="http://schema.igsn.org/registration/1.1">
					<xsl:attribute name="event" select="$eventType"/>
					<xsl:attribute name="timeStamp" select="$timeStamp"/>
				</xsl:element>
			</xsl:element>

		</sample>
	</xsl:template>

	<xsl:template match="igsn:resourceIdentifier">
		<xsl:element name="sampleNumber" xmlns="http://schema.igsn.org/registration/1.1">
			<xsl:attribute name="identifierType" select="'igsn'"/>
			<xsl:value-of select="."/>
		</xsl:element>
	</xsl:template>

	<xsl:template match="igsn:relatedResources">
		<xsl:element name="relatedResourceIdentifiers"
			xmlns="http://schema.igsn.org/registration/1.1">
			<xsl:apply-templates select="igsn:relatedResource"/>
		</xsl:element>
	</xsl:template>

	<xsl:template match="igsn:relatedResource">
		<xsl:element name="relatedResourceIdentifier"
			xmlns="http://schema.igsn.org/registration/1.1">
			<xsl:attribute name="relatedIdentifierType" select="@relatedResourceIdentifierType"/>
			<xsl:attribute name="relationType" select="@relationType"/>
			<xsl:value-of select="."/>
		</xsl:element>
	</xsl:template>

	<xsl:template match="igsn:logDate">
		<xsl:element name="logElement" xmlns="http://schema.igsn.org/registration/1.1">
			<xsl:attribute name="event" select="@eventType"/>
			<xsl:attribute name="timeStamp" select="."/>
		</xsl:element>
	</xsl:template>

</xsl:stylesheet>
