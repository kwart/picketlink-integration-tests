package org.jboss.aerogear.jaxrs.rest.test;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InstallPicketLinkFileBasedSetupTask implements ServerSetupTask {

    private static final Logger log = Logger.getLogger(InstallPicketLinkFileBasedSetupTask.class.getSimpleName());

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        File workingDir = File.createTempFile("pl-idm", "workingdir");
        workingDir.delete();
        if (workingDir.mkdirs()) {
            workingDir.deleteOnExit();
        } else {
            throw new AssertionError("Spoofing malicious security operation");
        }

        log.log(Level.INFO, "Installing File Based Partition Manager into AS/EAP container using working dir {0}",
                workingDir.getAbsolutePath());

        ModelNode step1 = ModelUtil.createOpNode("subsystem=picketlink/identity-management=picketlink-files", "add");
        step1.get("alias").set("picketlink-files");
        step1.get("jndi-name").set("java:jboss/picketlink/FileBasedPartitionManager");

        ModelNode step2 = ModelUtil.createOpNode(
                "subsystem=picketlink/identity-management=picketlink-files/identity-configuration=picketlink-files", "add");
        step2.get("name").set("picketlink-files");

        ModelNode step3 = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-files/identity-configuration=picketlink-files/file-store=file-store",
                        "add");
        step3.get("always-create-files").set(true);
        step3.get("async-write").set(true);
        step3.get("async-write-thread-pool").set(10);
        step3.get("support-attribute").set(true);
        step3.get("working-dir").set(workingDir.getAbsolutePath());

        ModelNode step4 = ModelUtil
                .createOpNode(
                        "subsystem=picketlink/identity-management=picketlink-files/identity-configuration=picketlink-files/file-store=file-store/supportedTypes=supportedTypes",
                        "add");
        step4.get("supportsAll").set(true);

        ModelNode op = ModelUtil.createCompositeNode(step1, step2, step3, step4);

        // add picketlink subsystem
        boolean success = ModelUtil.execute(managementClient, op);
        log.log(success ? Level.INFO : Level.WARNING, "Installing File Based Partition Manager into AS/EAP container {0}",
                new Object[] { success ? "passed" : "failed" });

        // FIXME reload is not working due to https://bugzilla.redhat.com/show_bug.cgi?id=900065
        // managementClient.getControllerClient().execute(ModelUtil.createOpNode(null, "reload"));
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        log.log(Level.INFO, "Deinstalling File Based Partition Manager from AS/EAP container");

        ModelNode op = ModelUtil.createOpNode("subsystem=picketlink/identity-management=picketlink-files", "remove");

        // remove picketlink subsystem
        boolean success = ModelUtil.execute(managementClient, op);
        log.log(success ? Level.INFO : Level.WARNING, "Deinstalling File Based Partition Manager into AS/EAP container {0}",
                new Object[] { success ? "passed" : "failed" });

        // FIXME reload is not working due to https://bugzilla.redhat.com/show_bug.cgi?id=900065
        // managementClient.getControllerClient().execute(ModelUtil.createOpNode(null, "reload"));
    }
}
