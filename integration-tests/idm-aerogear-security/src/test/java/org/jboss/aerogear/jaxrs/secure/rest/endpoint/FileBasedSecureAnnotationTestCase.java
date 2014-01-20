package org.jboss.aerogear.jaxrs.secure.rest.endpoint;

import org.jboss.aerogear.jaxrs.rest.test.InstallPicketLinkFileBasedSetupTask;
import org.jboss.aerogear.jaxrs.rest.test.InstallPicketLinkLdapBasedSetupTask;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.as.arquillian.api.ServerSetup;
import org.junit.runner.RunWith;
import org.picketlink.test.integration.util.PicketLinkIntegrationTests;
import org.picketlink.test.integration.util.TargetContainers;

/**
 * Created by hmlnarik on 1/17/14.
 */
@ServerSetup(InstallPicketLinkFileBasedSetupTask.class)
@RunWith(PicketLinkIntegrationTests.class)
@RunAsClient
@TargetContainers({ "jbas7", "eap6" })
public class FileBasedSecureAnnotationTestCase extends AbstractSecureAnnotationTest {
}
