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

import static org.junit.Assert.assertTrue;
import static org.picketlink.test.integration.util.PicketLinkConfigurationUtil.addKeyStoreAlias;
import static org.picketlink.test.integration.util.PicketLinkConfigurationUtil.addValveParameter;
import static org.picketlink.test.integration.util.PicketLinkConfigurationUtil.addTrustedDomain;
import static org.picketlink.test.integration.util.PicketLinkConfigurationUtil.addValidatingAlias;
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
 * Test case for custom user-defined configuration file for Service Provider(SP).  Configuration file can be
 * referenced from jboss-web.xml as valve parameter configFile with timeIterval to reload. Then instead of classic 
 * picketlink.xml, referenced configuration file will be used and periodically reloaded in defined time interval.
 * 
 * @author Filip Bogyai
 */

@TargetContainers({ "jbas7" })
public class ReloadSPConfigurationTestCase extends AbstractExternalConfigurationTestCase {

    private static File configFileEmployee = new File(new File(System.getProperty("java.io.tmpdir")), "picketlink-employee.xml");
    private static File configFileSales = new File(new File(System.getProperty("java.io.tmpdir")), "picketlink-sales.xml");
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
        assertTrue(" Reached the sales index page ", webResponse.getText().contains("SalesTool"));

        // Check Employee Application
        System.out.println("Trying " + getEmployeeURL());
        webResponse = webConversation.getResponse(getEmployeeURL());
        assertTrue(" Reached the employee index page ", webResponse.getText().contains("EmployeeDashboard"));

        webConversation.clearContents();
    }

    @Test
    @InSequence(2)
    public void changeSalesExternalConfiguration() throws IOException, InterruptedException {

        String content = FileUtils.fileRead(configFileSales);
        // Change ValidatingAlias values for Sales app
        String changedContent = content.replaceAll(getServerAddress(), "example.com");  
        FileUtils.fileWrite(configFileSales.getAbsolutePath(), changedContent);
        Thread.sleep(WAIT_INTERVAL);
    }

    @Test
    @InSequence(3)
    public void testWrongSalesExternalConfiguration() throws IOException, SAXException {

        System.out.println("Trying " + getEmployeeURL());
        // Employee Application Login
        WebRequest serviceRequest1 = new GetMethodWebRequest(getEmployeeURL());
        WebConversation webConversation = new WebConversation();

        WebResponse webResponse = webConversation.getResponse(serviceRequest1);
        WebForm loginForm = webResponse.getForms()[0];
        loginForm.setParameter("j_username", "tomcat");
        loginForm.setParameter("j_password", "tomcat");
        SubmitButton submitButton = loginForm.getSubmitButtons()[0];
        submitButton.click();

        // Check Employee Application
        webResponse = webConversation.getCurrentPage();
        assertTrue(" Not reached the employee index page ", webResponse.getText().contains("EmployeeDashboard"));

        // Check Sales Application
        System.out.println("Trying " + getSalesURL());
        webResponse = webConversation.getResponse(getSalesURL());
        assertTrue(" Reached the sales index page ",
                webResponse.getText().contains("The Identity Provider could not process the authentication request"));

        webConversation.clearContents();
    }
      
    @Test
    @InSequence(3)
    public void changeEmployeeExternalConfiguration() throws IOException, InterruptedException {

        String content = FileUtils.fileRead(configFileSales);
        // Change back ValidatingAlias values for Sales Application
        String changedContent = content.replaceAll("example.com", getServerAddress());  
        FileUtils.fileWrite(configFileSales.getAbsolutePath(), changedContent);

        content = FileUtils.fileRead(configFileEmployee);
        // Change ValidatingAlias values for Employee Application
        changedContent = content.replaceAll(getServerAddress(), "example.com");  
        FileUtils.fileWrite(configFileEmployee.getAbsolutePath(), changedContent);
        Thread.sleep(WAIT_INTERVAL);
    }
    
    @Test
    @InSequence(4)
    public void testWrongEmployeeExternalConfiguration() throws IOException, InterruptedException, SAXException {

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
        assertTrue(" Reached the sales index page ", webResponse.getText().contains("SalesTool"));
        
        // Check Employee Application
        System.out.println("Trying " + getEmployeeURL());
        webResponse = webConversation.getResponse(getEmployeeURL());
        assertTrue(" Reached the employee index page ",
                webResponse.getText().contains("The Identity Provider could not process the authentication request."));

        webConversation.clearContents();
    }

    public void testSAMLSignatureAuthentization() throws IOException, SAXException {

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

        webResponse = webConversation.getCurrentPage();
        assertTrue(" Reached the sales index page ", webResponse.getText().contains("SalesTool"));

        // Employee post Application Login
        System.out.println("Trying " + getEmployeeURL());
        webResponse = webConversation.getResponse(getEmployeeURL());
        assertTrue(" Reached the employee index page ", webResponse.getText().contains("EmployeeDashboard"));

        // Logout from sales
        System.out.println("Trying " + getSalesURL() + LOGOUT_URL);
        webResponse = webConversation.getResponse(getSalesURL() + LOGOUT_URL);
        assertTrue("Reached logged out page", webResponse.getText().contains("Logout"));

        // Hit the Sales Apps again
        System.out.println("Trying " + getSalesURL());
        webResponse = webConversation.getResponse(getSalesURL());
        assertTrue(" Reached the Login page ", webResponse.getText().contains("Login"));

        // Hit the Employee Apps again
        System.out.println("Trying " + getEmployeeURL());
        webResponse = webConversation.getResponse(getEmployeeURL());
        assertTrue(" Reached the Login page ", webResponse.getText().contains("Login"));

        webConversation.clearContents();

    }

    @Deployment(name = "idp-sig", testable = false)
    @TargetsContainer("jboss")
    public static WebArchive createIDPSigDeployment() throws GeneralSecurityException, IOException {
        WebArchive idp = MavenArtifactUtil.getQuickstartsMavenArchive("idp-sig");

        addTrustedDomain(idp, getServerAddress());
        addValidatingAlias(idp, getServerAddress(), getServerAddress());
        addKeyStoreAlias(idp, getServerAddress());
        
        return idp;
    }

    @Deployment(name = "sales-post-sig", testable = false)
    @TargetsContainer("jboss")
    public static WebArchive createSalesPostSigDeployment() throws GeneralSecurityException, IOException {
        WebArchive sp = MavenArtifactUtil.getQuickstartsMavenArchive("sales-post-sig");

        addValidatingAlias(sp, getServerAddress(), getServerAddress());
        addKeyStoreAlias(sp, getServerAddress());

        createPicketLinkConfigFile(sp, configFileSales);
        addValveParameter(sp, "configFile", configFileSales.getAbsolutePath());
        addValveParameter(sp, "timerInterval", TIME_INTERVAL);

        sp.delete("/WEB-INF/picketlink.xml");
        return sp;
    }

    @Deployment(name = "employee-sig", testable = false)
    @TargetsContainer("jboss")
    public static WebArchive createEmployeeSigDeployment() throws KeyStoreException, FileNotFoundException, NoSuchAlgorithmException,
            CertificateException, GeneralSecurityException, IOException {
        WebArchive sp = MavenArtifactUtil.getQuickstartsMavenArchive("employee-sig");

        addValidatingAlias(sp, getServerAddress(), getServerAddress());
        addKeyStoreAlias(sp, getServerAddress());

        createPicketLinkConfigFile(sp, configFileEmployee);
        addValveParameter(sp, "configFile", configFileEmployee.getAbsolutePath());
        addValveParameter(sp, "timerInterval", TIME_INTERVAL);

        sp.delete("/WEB-INF/picketlink.xml");
        return sp;
    }

}
