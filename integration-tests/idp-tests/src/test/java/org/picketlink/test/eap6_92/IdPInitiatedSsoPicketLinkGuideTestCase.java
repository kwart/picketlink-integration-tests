/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.picketlink.test.eap6_92;

import java.io.IOException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.picketlink.test.integration.util.JBoss7Util;
import org.picketlink.test.integration.util.MavenArtifactUtil;
import org.picketlink.test.integration.util.PicketLinkIntegrationTests;
import org.picketlink.test.integration.util.TargetContainers;

/**
 * Test case for testing unsolicited IdP response SSO.
 * This testcase uses {@link ServiceProviderAuthenticator} valve to interpret
 * SAML assertions, as described in PicketLink guide.
 *
 * @see https://issues.jboss.org/browse/EAP6-92
 * @see https://issues.jboss.org/browse/PLINK-363
 * @see https://issues.jboss.org/browse/PLINK-364
 * 
 * @author Hynek Mlnarik <hmlnarik@redhat.com>
 */
@TargetContainers ({"jbas7","eap6"})
@RunWith(PicketLinkIntegrationTests.class)
@RunAsClient
public class IdPInitiatedSsoPicketLinkGuideTestCase extends AbstractIdPInitiatedSsoTestCase {

    @Deployment(name = QS_NAME_EMPLOYEE, testable = false)
    @TargetsContainer(value = "jboss")
    public static WebArchive createEmployeeDeployment() throws IOException {
        WebArchive res = MavenArtifactUtil.getQuickstartsMavenArchive(QS_NAME_EMPLOYEE);

        res.delete("WEB-INF/jboss-web.xml");

        res.add(JBoss7Util.getJBossWebXmlAsset("sp", "org.picketlink.identity.federation.bindings.tomcat.sp.ServiceProviderAuthenticator"), "WEB-INF/jboss-web.xml")
          .add(new StringAsset(HELLO_WORLD_FROM_WITHIN_CONTEXT_TEXT), HELLO_WORLD_ADDR);

        return res;
    }

}
