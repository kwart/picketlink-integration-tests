package org.jboss.aerogear.jaxrs.secure.rest.endpoint;

import org.jboss.aerogear.jaxrs.rest.producer.PicketLinkDefaultUsers;
import org.jboss.aerogear.jaxrs.rest.producer.PicketLinkFileIdmUsers;
import org.jboss.aerogear.jaxrs.rest.test.InstallPicketLinkFileBasedSetupTask;
import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketlink.test.integration.util.PicketLinkIntegrationTests;
import org.picketlink.test.integration.util.TargetContainers;

import java.io.File;

/**
 * Created by hmlnarik on 1/17/14.
 */
//@ServerSetup(InstallPicketLinkFileBasedSetupTask.class)
@RunWith(PicketLinkIntegrationTests.class)
@RunAsClient
@TargetContainers({ "jbas7", "eap6" })
public class FileBasedSecureAnnotationManualTestCase extends AbstractSecureAnnotationTest {

    @ArquillianResource
    protected ContainerController controller;

    @Test
    @InSequence(Integer.MIN_VALUE)
    public void startServer() throws Exception {
        controller.start(SERVER_QUALIFIER);
    }

    @Test
    @InSequence(Integer.MIN_VALUE + 1)
    public void doSetupServer(@ArquillianResource ManagementClient managementClient) throws Exception {
        controller.start(SERVER_QUALIFIER);
        InstallPicketLinkFileBasedSetupTask.staticSetup(managementClient);
        controller.stop(SERVER_QUALIFIER);
    }

    @Test
    @InSequence(Integer.MIN_VALUE + 2)
    public void startServerSecond() throws Exception {
        controller.start(SERVER_QUALIFIER);
    }

    @Test
    @InSequence(Integer.MIN_VALUE + 3)
    public void deploy(@ArquillianResource Deployer deployer) throws Exception {
        deployer.deploy(DEPLOYMENT_NAME);
    }


    @Test
    @InSequence(Integer.MAX_VALUE)
    public void tearDownClass(@ArquillianResource Deployer deployer, @ArquillianResource ManagementClient managementClient) throws Exception {
        deployer.undeploy(DEPLOYMENT_NAME);
        InstallPicketLinkFileBasedSetupTask.staticTearDown(managementClient);
        controller.stop(SERVER_QUALIFIER);
    }

    @Deployment(testable = false, managed = false, name = DEPLOYMENT_NAME)
    @TargetsContainer(SERVER_QUALIFIER)
    public static WebArchive getDeployment() {
        return ShrinkWrap
          .createFromZipFile(WebArchive.class, new File("../../integration-tests/idm-aerogear-security/target/aerogear-rest-test.war"))
          .addClass(PicketLinkFileIdmUsers.class)
          .addClass(PicketLinkDefaultUsers.class)
        ;
    }

}
