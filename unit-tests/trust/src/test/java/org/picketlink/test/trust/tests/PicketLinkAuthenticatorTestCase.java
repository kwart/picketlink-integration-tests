/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.picketlink.test.trust.tests;

import java.io.File;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.picketlink.identity.federation.bindings.tomcat.PicketLinkAuthenticator;
import org.picketlink.common.exceptions.ConfigurationException;
import org.picketlink.common.exceptions.ParsingException;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.test.integration.util.PicketLinkIntegrationTests;
import org.picketlink.test.integration.util.TargetContainers;

/**
 * Test the {@link PicketLinkAuthenticator}
 * 
 * @author Anil.Saldhana@redhat.com
 * @since Sep 13, 2011
 */
@RunWith(PicketLinkIntegrationTests.class)
@TargetContainers ({"jbas5", "eap5"})
public class PicketLinkAuthenticatorTestCase extends AbstractPicketLinkAuthenticatorTestCase {
    
    @Deployment(name = "authenticator", testable = false)
    @TargetsContainer("jboss")
    public static WebArchive createAuthenticatorDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class);
        
        archive.addAsWebInfResource(getTestFile("as5/WEB-INF/web.xml"));
        archive.addAsWebInfResource(getTestFile("as5/WEB-INF/context.xml"));
        archive.addAsWebInfResource(getTestFile("as5/WEB-INF/jboss-web.xml"));
        
        archive.addAsWebResource(getTestFile("index.jsp"));
        
        return archive;
    }
    
    @Deployment(name = "picketlink-wstest-tests", testable = false)
    @TargetsContainer("jboss")
    public static JavaArchive createWSTestDeployment() throws ConfigurationException, ProcessingException, ParsingException,
            InterruptedException {
        return ShrinkWrap.createFromZipFile(JavaArchive.class, new File("../../unit-tests/trust/target/picketlink-wstest-tests.jar"));
    }

}