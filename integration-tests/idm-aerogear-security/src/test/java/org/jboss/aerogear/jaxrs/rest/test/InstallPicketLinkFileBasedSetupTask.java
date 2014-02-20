package org.jboss.aerogear.jaxrs.rest.test;

import org.picketlink.test.integration.util.ModelUtil;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

// This class cannot be used in @ServerSetup annotation
// until Arquillian and EAP are able to perform "reload"
// command cleanly.
// Seems to be related to https://bugzilla.redhat.com/show_bug.cgi?id=900065
public class InstallPicketLinkFileBasedSetupTask implements ServerSetupTask {

    private static final Logger log = Logger.getLogger(InstallPicketLinkFileBasedSetupTask.class.getSimpleName());

    public static final String JNDI_PICKETLINK_FILE_BASED_PARTITION_MANAGER = "java:jboss/picketlink/FileBasedPartitionManager";

    private static final PathAddress PICKETLINK_FILES = PathAddress.pathAddress(
      PathElement.pathElement(SUBSYSTEM, "picketlink"),
      PathElement.pathElement("identity-management", "picketlink-files")
    );
    private static final PathAddress PICKETLINK_FILES_CONF = PICKETLINK_FILES.append(PathElement.pathElement("identity-configuration", "picketlink-files-configuration"));
    private static final PathAddress PICKETLINK_FILES_CONF_STORE = PICKETLINK_FILES_CONF.append(PathElement.pathElement("file-store", "file-store"));
    private static final PathAddress PICKETLINK_FILES_CONF_STORE_SUPPORTED = PICKETLINK_FILES_CONF_STORE.append(PathElement.pathElement("supportedTypes", "supportedTypes"));

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        staticSetup(managementClient);

        // FIXME reload is not working due to https://bugzilla.redhat.com/show_bug.cgi?id=900065
        // managementClient.getControllerClient().execute(ModelUtil.createOpNode(null, "reload"));
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        staticTearDown(managementClient);

        // FIXME reload is not working due to https://bugzilla.redhat.com/show_bug.cgi?id=900065
        // managementClient.getControllerClient().execute(ModelUtil.createOpNode(null, "reload"));
    }

    public static void staticSetup(ManagementClient managementClient) throws Exception {
        File workingDir = File.createTempFile("pl-idm-aerogear-security", "workingdir");
        workingDir.delete();
        if (workingDir.mkdirs()) {
            workingDir.deleteOnExit();
        } else {
            throw new AssertionError("Spoofing malicious security operation");
        }

        log.log(Level.INFO, "Installing File Based Partition Manager into AS/EAP container using working dir {0}",
                workingDir.getAbsolutePath());

        ModelNode step1 = Util.createAddOperation(PICKETLINK_FILES);
        step1.get("alias").set("picketlink-files");
        step1.get("jndi-name").set(JNDI_PICKETLINK_FILE_BASED_PARTITION_MANAGER);
        allowServiceRestart(step1);

        ModelNode step2 = Util.createAddOperation(PICKETLINK_FILES_CONF);
        step2.get("name").set("picketlink-files-configuration");
        allowServiceRestart(step2);

        ModelNode step3 = Util.createAddOperation(PICKETLINK_FILES_CONF_STORE);
        step3.get("always-create-files").set(true);
        step3.get("async-write").set(true);
        step3.get("async-write-thread-pool").set(10);
        step3.get("support-attribute").set(true);
        step3.get("working-dir").set(workingDir.getAbsolutePath());
        allowServiceRestart(step3);

        ModelNode step4 = Util.createAddOperation(PICKETLINK_FILES_CONF_STORE_SUPPORTED);
        step4.get("supportsAll").set(true);
        allowServiceRestart(step4);

        ModelNode op = ModelUtil.createCompositeNode(step1, step2, step3, step4);
//        ModelNode op = ModelUtil.createCompositeNode(step1, step2, step3);

        // add picketlink subsystem
//        ModelControllerClient controllerClient = managementClient.getControllerClient();
//        ModelNode res = controllerClient.execute(op);
//        log.log(Level.INFO, "Installing File Based Partition Manager into AS/EAP container {0}",
//                new Object[] { res.toJSONString(true) });
        boolean success = ModelUtil.execute(managementClient, op);
        log.log(success ? Level.INFO : Level.WARNING, "Installing File Based Partition Manager into AS/EAP container {0}",
                new Object[] { success ? "passed" : "failed" });
    }

    public static void staticTearDown(ManagementClient managementClient) throws Exception {
        log.log(Level.INFO, "Deinstalling File Based Partition Manager from AS/EAP container");

        ModelNode step0 = Util.createRemoveOperation(PICKETLINK_FILES_CONF_STORE_SUPPORTED);
        allowServiceRestart(step0);
        ModelNode step1 = Util.createRemoveOperation(PICKETLINK_FILES_CONF_STORE);
        allowServiceRestart(step1);
        ModelNode step2 = Util.createRemoveOperation(PICKETLINK_FILES_CONF);
        allowServiceRestart(step2);
        ModelNode step3 = Util.createRemoveOperation(PICKETLINK_FILES);
        allowServiceRestart(step3);

        // remove picketlink subsystem
        ModelNode op = ModelUtil.createCompositeNode(step0, step1, step2, step3);

        boolean success = ModelUtil.execute(managementClient, op);
        log.log(success ? Level.INFO : Level.WARNING, "Deinstalling File Based Partition Manager into AS/EAP container {0}",
                new Object[] { success ? "passed" : "failed" });
    }

//*
    private static void allowServiceRestart(ModelNode... ops) {
        for (ModelNode op : ops) {
            if (op != null) {
                op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
                // Don't rollback when the AS detects the war needs the module
                op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            }
        }
    }
//*/
}
