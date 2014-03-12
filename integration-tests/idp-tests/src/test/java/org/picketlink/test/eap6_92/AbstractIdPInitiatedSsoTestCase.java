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

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.internal.matchers.StringContains.containsString;
import static org.picketlink.test.integration.util.PicketLinkConfigurationUtil.addTrustedDomain;
import static org.picketlink.test.integration.util.TestUtil.getServerAddress;
import static org.picketlink.test.integration.util.TestUtil.getTargetURL;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;

import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.picketlink.test.integration.util.MavenArtifactUtil;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.SubmitButton;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

/**
 * Abstract test case for EAP6-92 feature: Configuration of IDP indicated SSO.
 *
 *
 * @see https://issues.jboss.org/browse/EAP6-92
 * @see https://issues.jboss.org/browse/PLINK-363
 * @see https://issues.jboss.org/browse/PLINK-364
 * 
 * @author Hynek Mlnarik <hmlnarik@redhat.com>
 */
public abstract class AbstractIdPInitiatedSsoTestCase {

    public static final String QS_NAME_IDP = "idp";
    public static final String QS_NAME_EMPLOYEE = "employee";
    public static final String HELLO_WORLD_ADDR = "hello/world.html";

    public static final String LINK_NAME_NAME_EMPLOYEE_UNSPECIFIED = QS_NAME_EMPLOYEE;
    public static final String LINK_NAME_NAME_EMPLOYEE_11 = QS_NAME_EMPLOYEE + "-1.1";
    public static final String LINK_NAME_NAME_EMPLOYEE_20 = QS_NAME_EMPLOYEE + "-2.0";

    public static final String LINK_NAME_NAME_EMPLOYEE_HELLO_WORLD_UNSPECIFIED = QS_NAME_EMPLOYEE + "-hw";
    public static final String LINK_NAME_NAME_EMPLOYEE_HELLO_WORLD_11 = QS_NAME_EMPLOYEE + "-hw-1.1";
    public static final String LINK_NAME_NAME_EMPLOYEE_HELLO_WORLD_20 = QS_NAME_EMPLOYEE + "-hw-2.0";

    public static final String LINK_NAME_NAME_EMPLOYEE_NO_TERMINATING_SLASH_UNSPECIFIED = QS_NAME_EMPLOYEE + "-ns";
    public static final String LINK_NAME_NAME_EMPLOYEE_NO_TERMINATING_SLASH_11 = QS_NAME_EMPLOYEE + "-ns-1.1";
    public static final String LINK_NAME_NAME_EMPLOYEE_NO_TERMINATING_SLASH_20 = QS_NAME_EMPLOYEE + "-ns-2.0";

    public static final String HELLO_WORLD_FROM_WITHIN_CONTEXT_TEXT = "Hello World from within context";
    public static final String INDEX_TEXT_IN_EMPLOYEE_ROOT_TEXT = "Employee Tool, <b>tomcat</b>";

    private static String getIdPAnchor(String samlVersion, String relativeUrl, String linkName) throws UnsupportedEncodingException {
        return
          "<a href=\"?"
          + (samlVersion == null ? "" : ("SAML_VERSION=" + samlVersion + "&"))
          + "TARGET="
          + URLEncoder.encode(getTargetURL("/" + relativeUrl), "UTF-8")
          + "\">"
          + linkName
          + "</a>"
          ;
    }

    @Deployment(name = QS_NAME_IDP, testable = false)
    @TargetsContainer(value = "jboss")
    public static WebArchive createIDPSigDeployment() throws GeneralSecurityException, IOException {
        WebArchive idp = MavenArtifactUtil.getQuickstartsMavenArchive(QS_NAME_IDP);
        addTrustedDomain(idp, getServerAddress());
        idp.delete("hosted/index.jsp");
        idp.add(new StringAsset("<html><body>Welcome to IdP hosted"

          + getIdPAnchor(null, QS_NAME_EMPLOYEE, LINK_NAME_NAME_EMPLOYEE_NO_TERMINATING_SLASH_UNSPECIFIED)
          + getIdPAnchor("1.1", QS_NAME_EMPLOYEE, LINK_NAME_NAME_EMPLOYEE_NO_TERMINATING_SLASH_11)
          + getIdPAnchor("2.0", QS_NAME_EMPLOYEE, LINK_NAME_NAME_EMPLOYEE_NO_TERMINATING_SLASH_20)

          + getIdPAnchor(null, QS_NAME_EMPLOYEE + "/", LINK_NAME_NAME_EMPLOYEE_UNSPECIFIED)
          + getIdPAnchor("1.1", QS_NAME_EMPLOYEE + "/", LINK_NAME_NAME_EMPLOYEE_11)
          + getIdPAnchor("2.0", QS_NAME_EMPLOYEE + "/", LINK_NAME_NAME_EMPLOYEE_20)

          + getIdPAnchor(null, QS_NAME_EMPLOYEE + "/" + HELLO_WORLD_ADDR, LINK_NAME_NAME_EMPLOYEE_HELLO_WORLD_UNSPECIFIED)
          + getIdPAnchor("1.1", QS_NAME_EMPLOYEE + "/" + HELLO_WORLD_ADDR, LINK_NAME_NAME_EMPLOYEE_HELLO_WORLD_11)
          + getIdPAnchor("2.0", QS_NAME_EMPLOYEE + "/" + HELLO_WORLD_ADDR, LINK_NAME_NAME_EMPLOYEE_HELLO_WORLD_20)

          + "</body></html>"), "hosted/index.jsp");
        return idp;
    }

    /**
     * Locates a link with given text in the given response.
     * 
     * @param webResponse Response to search the link in
     * @param searchedLink Link text
     * @return The web link
     * @throws SAXException
     */
    protected WebLink findLink(WebResponse webResponse, String searchedLink) throws SAXException {
        WebLink[] links = webResponse.getLinks();
        for (WebLink webLink : links) {
            String linkText = webLink.getText();
            if (linkText != null && linkText.equals(searchedLink)) {
                return webLink;
            }
        }

        fail("Cannot find web link to SP " + searchedLink);

        return null;
    }

    /**
     * Logs into identity provider using the form method and tomcat/tomcat
     * username/password pair.
     *
     * @param idpUri URI of IdP
     * @return Web conversation with already authenticated user.
     * @throws IOException
     * @throws SAXException
     */
    protected WebConversation loginToIdP(URI idpUri) throws IOException, SAXException {
        WebRequest serviceRequest1 = new GetMethodWebRequest(idpUri.toString());
        WebConversation webConversation = new WebConversation();
        WebResponse webResponse = webConversation.getResponse(serviceRequest1);

        WebForm loginForm = webResponse.getForms()[0];
        loginForm.setParameter("j_username", "tomcat");
        loginForm.setParameter("j_password", "tomcat");
        SubmitButton submitButton = loginForm.getSubmitButtons()[0];
        submitButton.click();

        return webConversation;
    }

    /**
     * Starts the web conversation via IdP, then continues via "click" to
     * the given link to a requested service provider, and checks whether the
     * reply contains expected string.
     * 
     * @param idpUri URI of IdP
     * @param linkName Text of link pointing to the requested service provider
     * @param stringToTest Text which must appear in the response for this test to pass
     */
    protected void checkIdPFirstConversation(URI idpUri, String linkName, String stringToTest) throws Exception {
        WebConversation webConversation = loginToIdP(idpUri);

        WebResponse webResponse = webConversation.getCurrentPage();
        assertThat("IdP hosted index page not reached", webResponse.getText(), containsString("Welcome to IdP hosted"));

        WebLink webLink = findLink(webResponse, linkName);
        assertThat("Link " + linkName + " not found", webLink, CoreMatchers.notNullValue());

        webResponse = webLink.click();
        assertThat("Requested employee page not reached", webResponse.getText(), containsString(stringToTest));
    }

    /**
     * Starts the web conversation via service provider and checks whether the
     * reply contains expected string.
     * 
     * @param spUri
     * @param stringToTest Text which must appear in the response for this test to pass
     * @throws Exception
     */
    protected void checkSpFirstConversation(URI spUri, String stringToTest) throws Exception {
        WebConversation webConversation = loginToIdP(spUri);
        
        WebResponse webResponse = webConversation.getCurrentPage();

        assertThat("Requested employee page not reached", webResponse.getText(), containsString(stringToTest));
    }


    @Test
    public void testSpInitiatedSso(
      @ArquillianResource @OperateOnDeployment(QS_NAME_EMPLOYEE) URI spUri
    ) throws Exception {
        checkSpFirstConversation(spUri, INDEX_TEXT_IN_EMPLOYEE_ROOT_TEXT);
    }

    @Test
    public void testIdPInitiatedSAMLUnspecifiedVersion(
      @ArquillianResource @OperateOnDeployment(QS_NAME_IDP) URI idpUri
    ) throws Exception {
        checkIdPFirstConversation(idpUri, LINK_NAME_NAME_EMPLOYEE_UNSPECIFIED, INDEX_TEXT_IN_EMPLOYEE_ROOT_TEXT);
    }

    @Test
    public void testIdPInitiatedSAML11(
      @ArquillianResource @OperateOnDeployment(QS_NAME_IDP) URI idpUri
    ) throws Exception {
        checkIdPFirstConversation(idpUri, LINK_NAME_NAME_EMPLOYEE_11, INDEX_TEXT_IN_EMPLOYEE_ROOT_TEXT);
    }

    @Test
    public void testIdPInitiatedSAML20(
      @ArquillianResource @OperateOnDeployment(QS_NAME_IDP) URI idpUri
    ) throws Exception {
        // this fails, see https://bugzilla.redhat.com/show_bug.cgi?id=1071288
        checkIdPFirstConversation(idpUri, LINK_NAME_NAME_EMPLOYEE_20, INDEX_TEXT_IN_EMPLOYEE_ROOT_TEXT);
    }

    @Test
    @Ignore("https://bugzilla.redhat.com/show_bug.cgi?id=1072387")
    public void testSpInitiatedSsoHelloWorld(
      @ArquillianResource @OperateOnDeployment(QS_NAME_EMPLOYEE) URI spUri
    ) throws Exception {

        checkSpFirstConversation(spUri.resolve(HELLO_WORLD_ADDR), HELLO_WORLD_FROM_WITHIN_CONTEXT_TEXT);
    }

    @Test
    public void testIdPInitiatedSAMLUnspecifiedVersionHelloWorld(
      @ArquillianResource @OperateOnDeployment(QS_NAME_IDP) URI idpUri
    ) throws Exception {
        checkIdPFirstConversation(idpUri, LINK_NAME_NAME_EMPLOYEE_HELLO_WORLD_UNSPECIFIED, HELLO_WORLD_FROM_WITHIN_CONTEXT_TEXT);
    }

    @Test
    public void testIdPInitiatedSAML11HelloWorld(
      @ArquillianResource @OperateOnDeployment(QS_NAME_IDP) URI idpUri
    ) throws Exception {
        checkIdPFirstConversation(idpUri, LINK_NAME_NAME_EMPLOYEE_HELLO_WORLD_11, HELLO_WORLD_FROM_WITHIN_CONTEXT_TEXT);
    }

    @Test
    @Ignore("https://bugzilla.redhat.com/show_bug.cgi?id=1072387")
    public void testIdPInitiatedSAML20HelloWorld(
      @ArquillianResource @OperateOnDeployment(QS_NAME_IDP) URI idpUri
    ) throws Exception {
        checkIdPFirstConversation(idpUri, LINK_NAME_NAME_EMPLOYEE_HELLO_WORLD_20, HELLO_WORLD_FROM_WITHIN_CONTEXT_TEXT);
    }


    @Test
    @Ignore("httpunit does not properly set cookie on POST binding response - the test hence terminates in login form again")
    public void testIdPInitiatedSAMLUnspecifiedVersionNoTerminatingSlash(
      @ArquillianResource @OperateOnDeployment(QS_NAME_IDP) URI idpUri
    ) throws Exception {
        checkIdPFirstConversation(idpUri, LINK_NAME_NAME_EMPLOYEE_NO_TERMINATING_SLASH_UNSPECIFIED, INDEX_TEXT_IN_EMPLOYEE_ROOT_TEXT);
    }

    @Test
    @Ignore("httpunit does not properly set cookie on POST binding response - the test hence terminates in login form again")
    public void testIdPInitiatedSAML11NoTerminatingSlash(
      @ArquillianResource @OperateOnDeployment(QS_NAME_IDP) URI idpUri
    ) throws Exception {
        checkIdPFirstConversation(idpUri, LINK_NAME_NAME_EMPLOYEE_NO_TERMINATING_SLASH_11, INDEX_TEXT_IN_EMPLOYEE_ROOT_TEXT);
    }

    @Test
    @Ignore("httpunit does not properly set cookie on POST binding response - the test hence terminates in login form again")
    public void testIdPInitiatedSAML20NoTerminatingSlash(
      @ArquillianResource @OperateOnDeployment(QS_NAME_IDP) URI idpUri
    ) throws Exception {
        checkIdPFirstConversation(idpUri, LINK_NAME_NAME_EMPLOYEE_NO_TERMINATING_SLASH_20, INDEX_TEXT_IN_EMPLOYEE_ROOT_TEXT);
    }
}
