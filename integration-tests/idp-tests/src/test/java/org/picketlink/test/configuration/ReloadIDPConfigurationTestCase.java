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
package org.picketlink.test.configuration;

import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;
import static org.picketlink.test.integration.util.PicketLinkConfigurationUtil.addKeyStoreAlias;
import static org.picketlink.test.integration.util.PicketLinkConfigurationUtil.addTrustedDomain;
import static org.picketlink.test.integration.util.PicketLinkConfigurationUtil.addValidatingAlias;
import static org.picketlink.test.integration.util.PicketLinkConfigurationUtil.addValveParameter;
import static org.picketlink.test.integration.util.TestUtil.getServerAddress;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.codehaus.plexus.util.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.picketlink.test.integration.util.MavenArtifactUtil;
import org.picketlink.test.integration.util.TargetContainers;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.SubmitButton;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

/**
 * Test case for custom user-defined configuration file for Identity Provider(IdP).  Configuration file can be
 * referenced from jboss-web.xml as valve parameter configFile with timeIterval to reload. Then instead of classic
 * picketlink.xml, referenced configuration file will be used and periodically reloaded in defined time interval.
 * 
 * @author Filip Bogyai
 */

@TargetContainers({ "jbas7" })
public class ReloadIDPConfigurationTestCase extends AbstractExternalConfigurationTestCase {

    private static File configFileIDP = new File(new File(System.getProperty("java.io.tmpdir")), "picketlink-idp.xml");
    private static final String TIME_INTERVAL = "1000";
    private static final long WAIT_INTERVAL = 2000;

    @Test
    @InSequence(1)
    public void testCorrectExternalConfiguration() throws Exception {

        System.out.println("Trying " + getSalesURL());
        // Sales post Application Login
        WebRequest serviceRequest1 = new GetMethodWebRequest(getSalesURL());
        WebConversation webConversation = new WebConversation();

        WebResponse webResponse = webConversation.getResponse(serviceRequest1);
        WebForm loginForm = webResponse.getForms()[0];
        loginForm.setParameter("j_username", "tomcat");
        loginForm.setParameter("j_password", "tomcat");
        SubmitButton submitButton = loginForm.getSubmitButtons()[0];
        submitButton.click();

        // Check Sales Application
        webResponse = webConversation.getCurrentPage();
        assertThat(" Not reached the sales index page ", webResponse.getText(), containsString("SalesTool"));

        // Check Employee Application
        System.out.println("Trying " + getEmployeeURL());
        webResponse = webConversation.getResponse(getEmployeeURL());
        assertThat(" Not reached the employee index page ", webResponse.getText(), containsString("EmployeeDashboard"));

        webConversation.clearContents();
    }

    @Test
    @InSequence(2)
    public void changeIDPExternalConfiguration() throws IOException, InterruptedException {

        String content = FileUtils.fileRead(configFileIDP);
        // Change ValidatingAlias values for IDP
        String changedContent = content.replaceAll(getServerAddress(), "example.com");
        FileUtils.fileWrite(configFileIDP.getAbsolutePath(), changedContent);
        Thread.sleep(WAIT_INTERVAL);
    }

    @Test
    @InSequence(4)
    public void testWrongExternalConfiguration() throws IOException, InterruptedException, SAXException {

        System.out.println("Trying " + getSalesURL());
        // Sales post Application Login
        WebRequest serviceRequest1 = new GetMethodWebRequest(getSalesURL());
        WebConversation webConversation = new WebConversation();

        WebResponse webResponse = webConversation.getResponse(serviceRequest1);
        WebForm loginForm = webResponse.getForms()[0];
        loginForm.setParameter("j_username", "tomcat");
        loginForm.setParameter("j_password", "tomcat");
        SubmitButton submitButton = loginForm.getSubmitButtons()[0];
        submitButton.click();

        // Check Sales Application
        webResponse = webConversation.getCurrentPage();
        assertThat(" Reached the sales index page ", webResponse.getText(),
                containsString("The Identity Provider could not process the authentication request"));

        // Check Employee Application
        System.out.println("Trying " + getEmployeeURL());
        webResponse = webConversation.getResponse(getEmployeeURL());
        assertThat(" Reached the employee index page ", webResponse.getText(),
                containsString("The Identity Provider could not process the authentication request"));

        webConversation.clearContents();
    }

    @Deployment(name = "idp-sig", testable = false)
    @TargetsContainer("jboss")
    public static WebArchive createIDPSigDeployment() throws GeneralSecurityException, IOException {
        WebArchive idp = MavenArtifactUtil.getQuickstartsMavenArchive("idp-sig");

        addTrustedDomain(idp, getServerAddress());
        addValidatingAlias(idp, getServerAddress(), getServerAddress());
        addKeyStoreAlias(idp, getServerAddress());

        createPicketLinkConfigFile(idp, configFileIDP);
        addValveParameter(idp, "configFile", configFileIDP.getAbsolutePath());
        addValveParameter(idp, "timerInterval", TIME_INTERVAL);

        idp.delete("/WEB-INF/picketlink.xml");
        return idp;
    }

    @Deployment(name = "sales-post-sig", testable = false)
    @TargetsContainer("jboss")
    public static WebArchive createSalesPostSigDeployment() throws GeneralSecurityException, IOException {
        WebArchive sp = MavenArtifactUtil.getQuickstartsMavenArchive("sales-post-sig");

        addValidatingAlias(sp, getServerAddress(), getServerAddress());
        addKeyStoreAlias(sp, getServerAddress());

        return sp;
    }

    @Deployment(name = "employee-sig", testable = false)
    @TargetsContainer("jboss")
    public static WebArchive createEmployeeSigDeployment() throws KeyStoreException, FileNotFoundException, NoSuchAlgorithmException,
            CertificateException, GeneralSecurityException, IOException {
        WebArchive sp = MavenArtifactUtil.getQuickstartsMavenArchive("employee-sig");

        addValidatingAlias(sp, getServerAddress(), getServerAddress());
        addKeyStoreAlias(sp, getServerAddress());

        return sp;
    }

}
