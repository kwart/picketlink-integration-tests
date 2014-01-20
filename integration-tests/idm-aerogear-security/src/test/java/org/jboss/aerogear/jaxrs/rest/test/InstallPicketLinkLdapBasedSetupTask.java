package org.jboss.aerogear.jaxrs.rest.test;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
//import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import java.util.logging.Level;
import java.util.logging.Logger;

public class InstallPicketLinkLdapBasedSetupTask implements ServerSetupTask {

    private static final Logger log = Logger.getLogger(InstallPicketLinkLdapBasedSetupTask.class.getSimpleName());

    private LDAPTestUtil ldapUtil;

    @ArquillianResource
    protected ContainerController container;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        log.log(Level.INFO, "Starting LDAP instance");

        ldapUtil = new LDAPTestUtil(10389);
        ldapUtil.setup();
        ldapUtil.importLDIF("../../integration-tests/idm-aerogear-security/target/test-classes/users.ldif");
        log.log(Level.INFO, "Installing LDAP Based Partition Manager into AS/EAP container");

        ModelNode identityManagement = ModelUtil
                .createOpNode("subsystem=picketlink/identity-management=picketlink-ldap", "add");
        identityManagement.get("alias").set("picketlink-ldap");
        identityManagement.get("jndi-name").set("java:jboss/picketlink/LDAPBasedPartitionManager");

        ModelNode identityConfiguration = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-ldap/identity-configuration=picketlink-ldap-configuration",
                        "add");
        identityConfiguration.get("name").set("picketlink-ldap-configuration");

        ModelNode ldapStore = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-ldap/identity-configuration=picketlink-ldap-configuration/ldap-store=ldap-store",
                        "add");
        ldapStore.get("url").set("ldap://localhost:10389");
        ldapStore.get("bind-dn").set("uid=admin,ou=system");
        ldapStore.get("bind-credential").set("secret");
        ldapStore.get("base-dn-suffix").set("dc=jboss,dc=org");

        ModelNode supportedTypes = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-ldap/identity-configuration=picketlink-ldap-configuration/ldap-store=ldap-store/supportedTypes=supportedTypes",
                        "add");
        supportedTypes.get("supportsAll").set(false);

        ModelNode identityType = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-ldap/identity-configuration=picketlink-ldap-configuration/ldap-store=ldap-store/supportedTypes=supportedTypes/supportedType=org.picketlink.idm.model.IdentityType",
                        "add");
        identityType.get("class").set("org.picketlink.idm.model.IdentityType");

        ModelNode relationshipType = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-ldap/identity-configuration=picketlink-ldap-configuration/ldap-store=ldap-store/supportedTypes=supportedTypes/supportedType=org.picketlink.idm.model.Relationship",
                        "add");
        relationshipType.get("class").set("org.picketlink.idm.model.Relationship");

        ModelNode agentMapping = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-ldap/identity-configuration=picketlink-ldap-configuration/ldap-store=ldap-store/mapping=org.picketlink.idm.model.basic.Agent",
                        "add");
        agentMapping.get("class").set("org.picketlink.idm.model.basic.Agent");
        agentMapping.get("base-dn-suffix").set("ou=Agent,dc=jboss,dc=org");
        agentMapping.get("object-classes").set("account");

        ModelNode agentMappingAttr = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-ldap/identity-configuration=picketlink-ldap-configuration/ldap-store=ldap-store/mapping=org.picketlink.idm.model.basic.Agent/attribute=loginName",
                        "add");
        agentMappingAttr.get("name").set("loginName");
        agentMappingAttr.get("ldap-name").set("uid");
        agentMappingAttr.get("is-identifier").set(true);

        ModelNode userMapping = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-ldap/identity-configuration=picketlink-ldap-configuration/ldap-store=ldap-store/mapping=org.picketlink.idm.model.basic.User",
                        "add");
        userMapping.get("class").set("org.picketlink.idm.model.basic.User");
        userMapping.get("base-dn-suffix").set("ou=People,dc=jboss,dc=org");
        userMapping.get("object-classes").set("inetOrgPerson,organizationalPerson");

        ModelNode userMappingAttr1 = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-ldap/identity-configuration=picketlink-ldap-configuration/ldap-store=ldap-store/mapping=org.picketlink.idm.model.basic.User/attribute=loginName",
                        "add");
        userMappingAttr1.get("name").set("loginName");
        userMappingAttr1.get("ldap-name").set("uid");
        userMappingAttr1.get("is-identifier").set(true);

        ModelNode userMappingAttr2 = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-ldap/identity-configuration=picketlink-ldap-configuration/ldap-store=ldap-store/mapping=org.picketlink.idm.model.basic.User/attribute=firstName",
                        "add");
        userMappingAttr2.get("name").set("firstName");
        userMappingAttr2.get("ldap-name").set("cn");

        ModelNode userMappingAttr3 = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-ldap/identity-configuration=picketlink-ldap-configuration/ldap-store=ldap-store/mapping=org.picketlink.idm.model.basic.User/attribute=lastName",
                        "add");
        userMappingAttr3.get("name").set("lastName");
        userMappingAttr3.get("ldap-name").set("sn");

        ModelNode userMappingAttr4 = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-ldap/identity-configuration=picketlink-ldap-configuration/ldap-store=ldap-store/mapping=org.picketlink.idm.model.basic.User/attribute=email",
                        "add");
        userMappingAttr4.get("name").set("email");
        userMappingAttr4.get("ldap-name").set("mail");

        ModelNode roleMapping = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-ldap/identity-configuration=picketlink-ldap-configuration/ldap-store=ldap-store/mapping=org.picketlink.idm.model.basic.Role",
                        "add");
        roleMapping.get("class").set("org.picketlink.idm.model.basic.Role");
        roleMapping.get("base-dn-suffix").set("ou=Roles,dc=jboss,dc=org");
        roleMapping.get("object-classes").set("groupOfNames");

        ModelNode roleMappingAttr = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-ldap/identity-configuration=picketlink-ldap-configuration/ldap-store=ldap-store/mapping=org.picketlink.idm.model.basic.Role/attribute=name",
                        "add");
        roleMappingAttr.get("name").set("name");
        roleMappingAttr.get("ldap-name").set("cn");
        roleMappingAttr.get("is-identifier").set(true);

        ModelNode groupMapping = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-ldap/identity-configuration=picketlink-ldap-configuration/ldap-store=ldap-store/mapping=org.picketlink.idm.model.basic.Group",
                        "add");
        groupMapping.get("class").set("org.picketlink.idm.model.basic.Group");
        groupMapping.get("base-dn-suffix").set("ou=Groups,dc=jboss,dc=org");
        groupMapping.get("object-classes").set("groupOfNames");
        groupMapping.get("parent-membership-attribute-name").set("member");

        ModelNode groupMappingAttr = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-ldap/identity-configuration=picketlink-ldap-configuration/ldap-store=ldap-store/mapping=org.picketlink.idm.model.basic.Group/attribute=name",
                        "add");
        groupMappingAttr.get("name").set("name");
        groupMappingAttr.get("ldap-name").set("cn");
        groupMappingAttr.get("is-identifier").set(true);

        ModelNode grantMapping = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-ldap/identity-configuration=picketlink-ldap-configuration/ldap-store=ldap-store/mapping=org.picketlink.idm.model.basic.Grant",
                        "add");
        grantMapping.get("class").set("org.picketlink.idm.model.basic.Grant");
        grantMapping.get("relates-to").set("org.picketlink.idm.model.basic.Role");

        ModelNode grantMappingAttr = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-ldap/identity-configuration=picketlink-ldap-configuration/ldap-store=ldap-store/mapping=org.picketlink.idm.model.basic.Grant/attribute=assignee",
                        "add");
        grantMappingAttr.get("name").set("assignee");
        grantMappingAttr.get("ldap-name").set("member");

        ModelNode op = ModelUtil.createCompositeNode(identityManagement, identityConfiguration, ldapStore, supportedTypes,
                identityType, relationshipType, agentMapping, agentMappingAttr, userMapping, userMappingAttr1,
                userMappingAttr2, userMappingAttr3, userMappingAttr4, roleMapping, roleMappingAttr,
                groupMapping, groupMappingAttr, grantMapping, grantMappingAttr);
//        allowServiceRestart(op);

        // add picketlink subsystem
        boolean success = ModelUtil.execute(managementClient, op);
        log.log(success ? Level.INFO : Level.WARNING, "Installing LDAP Based Partition Manager into AS/EAP container {0}",
                new Object[] { success ? "passed" : "failed" });

        // FIXME reload is not working due to https://bugzilla.redhat.com/show_bug.cgi?id=900065
        // managementClient.getControllerClient().execute(ModelUtil.createOpNode(null, "reload"));
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        log.log(Level.INFO, "Stopping LDAP instance");

        ldapUtil.tearDown();

        log.log(Level.INFO, "Deinstalling LDAP Based Partition Manager from AS/EAP container");

        ModelNode op = ModelUtil.createOpNode("subsystem=picketlink/identity-management=picketlink-ldap", "remove");
//        allowServiceRestart(op);

        // remove picketlink subsystem
        boolean success = ModelUtil.execute(managementClient, op);
        log.log(success ? Level.INFO : Level.WARNING, "Deinstalling LDAP Based Partition Manager into AS/EAP container {0}",
                new Object[] { success ? "passed" : "failed" });

        // FIXME reload is not working due to https://bugzilla.redhat.com/show_bug.cgi?id=900065
        // managementClient.getControllerClient().execute(ModelUtil.createOpNode(null, "reload"));
    }

/*
    private ModelNode allowServiceRestart(ModelNode op) {
        op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        return op;
    }

*/
}
