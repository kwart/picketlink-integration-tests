package org.jboss.aerogear.jaxrs.rest.test;

import org.picketlink.test.integration.util.ModelUtil;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

// This class cannot be used in @ServerSetup annotation
// until Arquillian and EAP are able to perform "reload"
// command cleanly.
// Seems to be related to https://bugzilla.redhat.com/show_bug.cgi?id=900065
public class InstallPicketLinkLdapBasedSetupTask implements ServerSetupTask {

    private static final Logger log = Logger.getLogger(InstallPicketLinkLdapBasedSetupTask.class.getSimpleName());

    public static final String JNDI_PICKETLINK_LDAP_BASED_PARTITION_MANAGER = "java:jboss/picketlink/LdapBasedPartitionManager";

    private LDAPTestUtil ldapUtil;

    private static final PathAddress PICKETLINK_LDAP = PathAddress.pathAddress(
      PathElement.pathElement(SUBSYSTEM, "picketlink"),
      PathElement.pathElement("identity-management", "picketlink-ldap")
    );
    private static final PathAddress PICKETLINK_LDAP_CONF = PICKETLINK_LDAP.append(PathElement.pathElement("identity-configuration", "picketlink-ldap-configuration"));
    private static final PathAddress PICKETLINK_LDAP_CONF_STORE = PICKETLINK_LDAP_CONF.append(PathElement.pathElement("ldap-store", "ldap-store"));
    private static final PathAddress PICKETLINK_LDAP_CONF_STORE_SUPPORTEDTYPES = PICKETLINK_LDAP_CONF_STORE.append(PathElement.pathElement("supportedTypes", "supportedTypes"));

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        this.ldapUtil = staticSetup(managementClient);

        // FIXME reload is not working due to https://bugzilla.redhat.com/show_bug.cgi?id=900065
        // managementClient.getControllerClient().execute(ModelUtil.createOpNode(null, "reload"));
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        staticTearDown(managementClient, this.ldapUtil);

        // FIXME reload is not working due to https://bugzilla.redhat.com/show_bug.cgi?id=900065
        // managementClient.getControllerClient().execute(ModelUtil.createOpNode(null, "reload"));
    }

    public static LDAPTestUtil staticSetup(ManagementClient managementClient) throws Exception {
        log.log(Level.INFO, "Starting LDAP instance");

        LDAPTestUtil res = new LDAPTestUtil(10389);
        res.setup();
        res.importLDIF("../../integration-tests/idm-aerogear-security/target/test-classes/users.ldif");
        log.log(Level.INFO, "Installing LDAP Based Partition Manager into AS/EAP container");

        ModelNode identityManagement = Util.createAddOperation(PICKETLINK_LDAP);
        identityManagement.get("alias").set("picketlink-ldap");
        identityManagement.get("jndi-name").set(JNDI_PICKETLINK_LDAP_BASED_PARTITION_MANAGER);
        allowServiceRestart(identityManagement);

        ModelNode identityConfiguration = Util.createAddOperation(PICKETLINK_LDAP_CONF);
        identityConfiguration.get("name").set("picketlink-ldap-configuration");
        allowServiceRestart(identityConfiguration);

        ModelNode ldapStore = Util.createAddOperation(PICKETLINK_LDAP_CONF_STORE);
        ldapStore.get("url").set("ldap://localhost:10389");
        ldapStore.get("bind-dn").set("uid=admin,ou=system");
        ldapStore.get("bind-credential").set("secret");
        ldapStore.get("base-dn-suffix").set("dc=jboss,dc=org");
        allowServiceRestart(ldapStore);

        ModelNode supportedTypes = Util.createAddOperation(PICKETLINK_LDAP_CONF_STORE_SUPPORTEDTYPES);
//        supportedTypes.get("supportsAll").set(false);
        allowServiceRestart(supportedTypes);

        ModelNode identityType = addLdapSupportedType(org.picketlink.idm.model.IdentityType.class);

        ModelNode relationshipType = addLdapSupportedType(org.picketlink.idm.model.Relationship.class);

        ModelNode agentMapping = addLdapMapping(org.picketlink.idm.model.basic.Agent.class);
        agentMapping.get("base-dn-suffix").set("ou=Agent,dc=jboss,dc=org");
        agentMapping.get("object-classes").set("account");

        ModelNode agentMappingAttr = addLdapMappingAttr(org.picketlink.idm.model.basic.Agent.class, "loginName", "uid");
        agentMappingAttr.get("is-identifier").set(true);


        ModelNode userMapping = addLdapMapping(org.picketlink.idm.model.basic.User.class);
        userMapping.get("base-dn-suffix").set("ou=People,dc=jboss,dc=org");
        userMapping.get("object-classes").set("inetOrgPerson,organizationalPerson");

        ModelNode userMappingAttr1 = addLdapMappingAttr(org.picketlink.idm.model.basic.User.class, "loginName", "uid");
        userMappingAttr1.get("is-identifier").set(true);

        ModelNode userMappingAttr2 = addLdapMappingAttr(org.picketlink.idm.model.basic.User.class, "firstName", "cn");
        ModelNode userMappingAttr3 = addLdapMappingAttr(org.picketlink.idm.model.basic.User.class, "lastName", "sn");
        ModelNode userMappingAttr4 = addLdapMappingAttr(org.picketlink.idm.model.basic.User.class, "email", "mail");

        ModelNode roleMapping = addLdapMapping(org.picketlink.idm.model.basic.Role.class);
        roleMapping.get("base-dn-suffix").set("ou=Roles,dc=jboss,dc=org");
        roleMapping.get("object-classes").set("groupOfNames");

        ModelNode roleMappingAttr = addLdapMappingAttr(org.picketlink.idm.model.basic.Role.class, "name", "cn");
        roleMappingAttr.get("is-identifier").set(true);

        ModelNode groupMapping = addLdapMapping(org.picketlink.idm.model.basic.Group.class);
        groupMapping.get("base-dn-suffix").set("ou=Groups,dc=jboss,dc=org");
        groupMapping.get("object-classes").set("groupOfNames");
        groupMapping.get("parent-membership-attribute-name").set("member");

        ModelNode groupMappingAttr = addLdapMappingAttr(org.picketlink.idm.model.basic.Group.class, "name", "cn");
        groupMappingAttr.get("is-identifier").set(true);

        ModelNode grantMapping = addLdapMapping(org.picketlink.idm.model.basic.Grant.class);
        grantMapping.get("relates-to").set("org.picketlink.idm.model.basic.Role");

        ModelNode grantMappingAttr = addLdapMappingAttr(org.picketlink.idm.model.basic.Grant.class, "assignee", "member");
        grantMapping.get("relates-to").set("org.picketlink.idm.model.basic.Role");
        allowServiceRestart(grantMapping);

        ModelNode op = ModelUtil.createCompositeNode(
          identityManagement, identityConfiguration,
          ldapStore, supportedTypes,
          identityType, relationshipType,
          agentMapping, agentMappingAttr,
          userMapping, userMappingAttr1, userMappingAttr2, userMappingAttr3, userMappingAttr4,
          roleMapping, roleMappingAttr,
          groupMapping, groupMappingAttr,
          grantMapping, grantMappingAttr
        );

        // add picketlink subsystem
        boolean success = ModelUtil.execute(managementClient, op);
        log.log(success ? Level.INFO : Level.WARNING, "Installing LDAP Based Partition Manager into AS/EAP container {0}",
                new Object[] { success ? "passed" : "failed" });

        return res;
    }

    private static ModelNode addLdapMapping(Class<?> clazz) {
        String className = clazz.getName();
        PathAddress address = PICKETLINK_LDAP_CONF_STORE.append(PathElement.pathElement("mapping", className));

        ModelNode node = Util.createAddOperation(address);
        node.get("class").set(className);

        return node;
    }

    private static ModelNode addLdapMappingAttr(Class<?> clazz, String modelAttrName, String ldapAttrName) {
        String className = clazz.getName();

        PathAddress address = PICKETLINK_LDAP_CONF_STORE
          .append(PathElement.pathElement("mapping", className))
          .append(PathElement.pathElement("attribute", modelAttrName));
        ModelNode node = Util.createAddOperation(address);

        node.get("name").set(modelAttrName);
        node.get("ldap-name").set(ldapAttrName);

        return node;
    }

    private static ModelNode addLdapSupportedType(Class<?> clazz) {
        String className = clazz.getName();
        PathAddress address = PICKETLINK_LDAP_CONF_STORE_SUPPORTEDTYPES.append(PathElement.pathElement("supportedType", className));

        ModelNode node = Util.createAddOperation(address);
        node.get("class").set(className);
        return node;
    }

    public static void staticTearDown(ManagementClient managementClient, LDAPTestUtil ldapUtil) throws Exception {
        log.log(Level.INFO, "Stopping LDAP instance");

        ldapUtil.tearDown();

        log.log(Level.INFO, "Deinstalling LDAP Based Partition Manager from AS/EAP container");

        ModelNode step0 = Util.createRemoveOperation(PICKETLINK_LDAP_CONF_STORE_SUPPORTEDTYPES);
        allowServiceRestart(step0);
        ModelNode step1 = Util.createRemoveOperation(PICKETLINK_LDAP_CONF_STORE);
        allowServiceRestart(step1);
        ModelNode step2 = Util.createRemoveOperation(PICKETLINK_LDAP_CONF);
        allowServiceRestart(step2);
        ModelNode step3 = Util.createRemoveOperation(PICKETLINK_LDAP);
        allowServiceRestart(step3);

        ModelNode op = ModelUtil.createCompositeNode(step0, step1, step2, step3);
        // remove picketlink subsystem

        boolean success = ModelUtil.execute(managementClient, op);
        log.log(success ? Level.INFO : Level.WARNING, "Deinstalling LDAP Based Partition Manager into AS/EAP container {0}",
                new Object[] { success ? "passed" : "failed" });
    }

    private static ModelNode allowServiceRestart(ModelNode op) {
        op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        return op;
    }
}
