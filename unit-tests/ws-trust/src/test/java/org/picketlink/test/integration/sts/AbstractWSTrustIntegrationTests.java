package org.picketlink.test.integration.sts;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.picketlink.test.integration.util.PicketLinkIntegrationTests;
import org.picketlink.test.integration.util.TestUtil;

@RunWith(PicketLinkIntegrationTests.class)
public abstract class AbstractWSTrustIntegrationTests {

    @Deployment(name = "picketlink-sts", testable = false)
    @TargetsContainer("jboss")
    public static WebArchive createSTSDeployment() throws GeneralSecurityException, IOException {
        return TestUtil.createSTSDeployment();
    }

}
