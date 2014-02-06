<?xml version="1.0" encoding="UTF-8"?>
<!-- XSLT file to add the a the PicketLink Extension to the standalone.xml 
	of the JBoss AS7 installation. -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:as="urn:jboss:domain:1.5" xmlns:pl="urn:jboss:domain:picketlink:1.0" version="1.0">

	<xsl:output method="xml" indent="yes" />

	<!-- If the extension is already defined, remove it to configure it again. -->
	<xsl:template match="//as:server/as:profile/pl:subsystem" xmlns="urn:jboss:domain:picketlink:1.0">
        <subsystem>
            <identity-management jndi-name="java:jboss/picketlink/FileBasedPartitionManager" alias="picketlink-files">
                <identity-configuration name="picketlink-files-configuration">
                    <file-store working-dir="/tmp/pl-idm-aerogear-security5222950387877915753workingdir" always-create-files="true" async-write="true" async-write-thread-pool="10" support-attribute="true">
                        <supportedTypes supportsAll="true"/>
                    </file-store>
                </identity-configuration>
            </identity-management>

            <identity-management jndi-name="java:jboss/picketlink/LdapBasedPartitionManager" alias="picketlink-ldap">
                <identity-configuration name="picketlink-ldap-configuration">
                    <ldap-store url="ldap://localhost:10389" bind-dn="uid=admin,ou=system" bind-credential="secret" base-dn-suffix="dc=jboss,dc=org">
                        <supportedTypes supportsAll="false">
                            <supportedType class="org.picketlink.idm.model.IdentityType"/>
                            <supportedType class="org.picketlink.idm.model.Relationship"/>
                        </supportedTypes>
                        <mappings>
                            <mapping class="org.picketlink.idm.model.basic.Agent" base-dn-suffix="ou=Agent,dc=jboss,dc=org" object-classes="account">
                                <attribute name="loginName" ldap-name="uid" is-identifier="true"/>
                            </mapping>
                            <mapping class="org.picketlink.idm.model.basic.User" base-dn-suffix="ou=People,dc=jboss,dc=org" object-classes="inetOrgPerson,organizationalPerson">
                                <attribute name="loginName" ldap-name="uid" is-identifier="true"/>
                                <attribute name="firstName" ldap-name="cn"/>
                                <attribute name="lastName" ldap-name="sn"/>
                                <attribute name="email" ldap-name="mail"/>
                            </mapping>
                            <mapping class="org.picketlink.idm.model.basic.Role" base-dn-suffix="ou=Roles,dc=jboss,dc=org" object-classes="groupOfNames">
                                <attribute name="name" ldap-name="cn" is-identifier="true"/>
                            </mapping>
                            <mapping class="org.picketlink.idm.model.basic.Group" base-dn-suffix="ou=Groups,dc=jboss,dc=org" object-classes="groupOfNames" parent-membership-attribute-name="member">
                                <attribute name="name" ldap-name="cn" is-identifier="true"/>
                            </mapping>
                            <mapping class="org.picketlink.idm.model.basic.Grant" relates-to="org.picketlink.idm.model.basic.Role">
                                <attribute name="assignee" ldap-name="member"/>
                            </mapping>
                        </mappings>
                    </ldap-store>
                </identity-configuration>
            </identity-management>
        </subsystem>
	</xsl:template>

	<!-- Copy everything else. -->
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>