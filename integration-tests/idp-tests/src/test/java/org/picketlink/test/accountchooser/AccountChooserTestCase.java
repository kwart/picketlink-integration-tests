package org.picketlink.test.accountchooser;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketlink.test.integration.util.JBoss7Util;
import org.picketlink.test.integration.util.PicketLinkIntegrationTests;
import org.picketlink.test.integration.util.TargetContainers;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.SubmitButton;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import org.apache.http.HttpStatus;
import static org.junit.Assert.assertThat;
import org.junit.matchers.JUnitMatchers;

/**
 * Test case for Account Chooser functionality in PicketLink Service Provider.
 * 
 * @author olukas
 * 
 */
@TargetContainers({ "jbas7", "eap6" })
@RunWith(PicketLinkIntegrationTests.class)
@RunAsClient
public class AccountChooserTestCase {

    private static Logger LOGGER = Logger.getLogger(AccountChooserTestCase.class);

    public static final String USERA = "userA";
    public static final String BADUSER = "badUser";
    public static final String USERB = "userB";
    public static final String USERS = USERA + "=" + USERA + "\n" + BADUSER + "=" + BADUSER;
    public static final String ROLES = USERA + "=" + "gooduser" + "\n" + BADUSER + "=baduser";
    public static final String USERS2 = USERB + "=" + USERB + "\n" + BADUSER + "=" + BADUSER;
    public static final String ROLES2 = USERB + "=" + "gooduser" + "\n" + BADUSER + "=" + BADUSER;

    private static final String IDP1 = "idp1";
    private static final String IDP2 = "idp2";
    private static final String SP1 = "sp1";
    private static final String SP2 = "sp2";

    private static final String HELLO_FROM = "Hello from ";
    private static final String HELLO_FROM_LOGIN_PAGE = HELLO_FROM + "idp";
    private static final String HELLO_FROM_ERROR_LOGIN_PAGE = HELLO_FROM + "error login page";
    private static final String HELLO_FROM_LOGOUT_PAGE = HELLO_FROM + "logout page";
    private static final String HELLO_FROM_IDP1 = HELLO_FROM + IDP1;
    private static final String HELLO_FROM_IDP2 = HELLO_FROM + IDP2;
    private static final String HELLO_FROM_SP1 = HELLO_FROM + SP1;
    private static final String HELLO_FROM_SP2 = HELLO_FROM + SP2;
    private static final String HELLO_FROM_AC1 = HELLO_FROM + "accountchooser " + SP1;
    private static final String HELLO_FROM_AC2 = HELLO_FROM + "accountchooser " + SP2;

    private static final String DOMAINA_URL = "?idp=DomainA";
    private static final String DOMAINB_URL = "?idp=DomainB";
    private static final String LOGOUT = "?GLO=true";

    private static final String REALM = "TestRealm";

    @ArquillianResource
    @OperateOnDeployment(IDP1)
    private URL idp1Url;

    @ArquillianResource
    @OperateOnDeployment(IDP2)
    private URL idp2Url;

    @ArquillianResource
    @OperateOnDeployment(SP1)
    private URL sp1Url;

    @ArquillianResource
    @OperateOnDeployment(SP2)
    private URL sp2Url;

    @Deployment(name = IDP1)
    public static WebArchive deploymentIdP1() {
        LOGGER.info("Start deployment " + IDP1);
        final WebArchive war = ShrinkWrap.create(WebArchive.class, IDP1 + ".war");
        war.addAsResource(new StringAsset(USERS), "users.properties");
        war.addAsResource(new StringAsset(ROLES), "roles.properties");
        war.addAsWebInfResource(AccountChooserTestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsWebInfResource(JBoss7Util.getJBossWebXmlAsset("idp1",
                "org.picketlink.identity.federation.bindings.tomcat.idp.IDPWebBrowserSSOValve"), "jboss-web.xml");
        war.addAsManifestResource(JBoss7Util.getJBossDeploymentStructure("org.picketlink"), "jboss-deployment-structure.xml");
        war.addAsWebInfResource(
                new StringAsset(JBoss7Util.propertiesReplacer(
                        AccountChooserTestCase.class.getResourceAsStream("picketlink-idp.xml"), IDP1, "", IDP1)),
                "picketlink.xml");
        war.add(new StringAsset(HELLO_FROM_IDP1), "index.jsp");
        war.addAsWebResource(AccountChooserTestCase.class.getPackage(), "login.jsp", "login.jsp");
        war.add(new StringAsset(HELLO_FROM_ERROR_LOGIN_PAGE), "login-error.jsp");

        return war;
    }

    @Deployment(name = IDP2)
    public static WebArchive deploymentIdP2() {
        LOGGER.info("Start deployment " + IDP2);
        final WebArchive war = ShrinkWrap.create(WebArchive.class, IDP2 + ".war");
        war.addAsResource(new StringAsset(USERS2), "users.properties");
        war.addAsResource(new StringAsset(ROLES2), "roles.properties");
        war.addAsWebInfResource(AccountChooserTestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsWebInfResource(JBoss7Util.getJBossWebXmlAsset("idp2",
                "org.picketlink.identity.federation.bindings.tomcat.idp.IDPWebBrowserSSOValve"), "jboss-web.xml");
        war.addAsManifestResource(JBoss7Util.getJBossDeploymentStructure("org.picketlink"), "jboss-deployment-structure.xml");
        war.addAsWebInfResource(
                new StringAsset(JBoss7Util.propertiesReplacer(
                        AccountChooserTestCase.class.getResourceAsStream("picketlink-idp.xml"), IDP2, "", IDP2)),
                "picketlink.xml");
        war.add(new StringAsset(HELLO_FROM_IDP2), "index.jsp");
        war.addAsWebResource(AccountChooserTestCase.class.getPackage(), "login.jsp", "login.jsp");
        war.add(new StringAsset(HELLO_FROM_ERROR_LOGIN_PAGE), "login-error.jsp");

        return war;
    }

    @Deployment(name = SP1)
    public static WebArchive deploymentSP1() {
        LOGGER.info("Start deployment " + SP1);
        final WebArchive war = ShrinkWrap.create(WebArchive.class, SP1 + ".war");
        war.addAsWebInfResource(AccountChooserTestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsWebInfResource(JBoss7Util.getJBossWebXmlAsset("sp",
                "org.picketlink.identity.federation.bindings.tomcat.sp.AccountChooserValve",
                "org.picketlink.identity.federation.bindings.tomcat.sp.ServiceProviderAuthenticator"), "jboss-web.xml");
        war.addAsManifestResource(JBoss7Util.getJBossDeploymentStructure("org.picketlink"), "jboss-deployment-structure.xml");
        war.addAsWebInfResource(
                new StringAsset(JBoss7Util.propertiesReplacer(
                        AccountChooserTestCase.class.getResourceAsStream("picketlink-sp.xml"), SP1, "REDIRECT", "whateverHere")),
                "picketlink.xml");
        war.add(new StringAsset(HELLO_FROM_SP1), "index.jsp");
        war.add(new StringAsset(HELLO_FROM_AC1), "accountChooser.html");
        war.add(new StringAsset(HELLO_FROM_LOGOUT_PAGE), "logout.jsp");
        war.addAsWebInfResource(new StringAsset("DomainA=http://" + getHostnameForWar() + ":8080/" + IDP1 + "/\n"
                + "DomainB=http://" + getHostnameForWar() + ":8080/" + IDP2 + "/"), "idpmap.properties");

        return war;
    }

    @Deployment(name = SP2)
    public static WebArchive deploymentSP2() {
        LOGGER.info("Start deployment " + SP2);
        final WebArchive war = ShrinkWrap.create(WebArchive.class, SP2 + ".war");
        war.addAsWebInfResource(AccountChooserTestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsWebInfResource(JBoss7Util.getJBossWebXmlAsset("sp",
                "org.picketlink.identity.federation.bindings.tomcat.sp.AccountChooserValve",
                "org.picketlink.identity.federation.bindings.tomcat.sp.ServiceProviderAuthenticator"), "jboss-web.xml");
        war.addAsManifestResource(JBoss7Util.getJBossDeploymentStructure("org.picketlink"), "jboss-deployment-structure.xml");
        war.addAsWebInfResource(
                new StringAsset(JBoss7Util.propertiesReplacer(
                        AccountChooserTestCase.class.getResourceAsStream("picketlink-sp.xml"), SP2, "REDIRECT", "whateverHere")),
                "picketlink.xml");
        war.add(new StringAsset(HELLO_FROM_SP2), "index.jsp");
        war.add(new StringAsset(HELLO_FROM_AC2), "accountChooser.html");
        war.add(new StringAsset(HELLO_FROM_LOGOUT_PAGE), "logout.jsp");
        war.addAsWebInfResource(new StringAsset("DomainA=http://" + getHostnameForWar() + ":8080/" + IDP1 + "/"),
                "idpmap.properties");

        return war;
    }

    private static String getHostnameForWar() {
        return JBoss7Util.getHostname();
    }

    /**
     * 1. Test access to SP using correct credentials.
     * 
     * 2. Test whether access is granted to another SP using same IDP.
     * 
     * 3. Test correct logout from IDP in both SP.
     * 
     * @throws Exception
     */
    @Test
    public void testBasicLoginAndLogoutThroughOneIDP() throws Exception {

        WebConversation webConversation = new WebConversation();

        try {
            makeCall(sp1Url.toString(), webConversation, HELLO_FROM_AC1);
            makeCallAndTryToAuthenticate(sp1Url.toString() + DOMAINA_URL, webConversation, HELLO_FROM_LOGIN_PAGE, USERA, USERA,
                    HELLO_FROM_SP1);
            makeCall(sp1Url.toString(), webConversation, HELLO_FROM_SP1);

            makeCall(sp2Url.toString(), webConversation, HELLO_FROM_AC2);
            makeCall(sp2Url.toString() + DOMAINA_URL, webConversation, HELLO_FROM_SP2);
            makeCall(sp2Url.toString(), webConversation, HELLO_FROM_SP2);

            makeCall(sp1Url.toString(), webConversation, HELLO_FROM_SP1);

            makeCall(sp2Url.toString() + LOGOUT, webConversation, HELLO_FROM_LOGOUT_PAGE);
            makeCall(sp1Url.toString(), webConversation, HELLO_FROM_LOGIN_PAGE);
            makeCall(sp2Url.toString(), webConversation, HELLO_FROM_LOGIN_PAGE);

        } finally {
            webConversation.clearContents();
        }
    }

    /**
     * 1. Test access to SP using correct credentials.
     * 
     * 2. Test whether access is not granted to another SP which is not using same IDP. Then authenticate.
     * 
     * 3. Test whether logout from one IDP doesn't caused logout from another SP from another different IDP.
     * 
     * @throws Exception
     */
    @Test
    public void testBasicLoginAndLogoutThroughDifferentIDPs() throws Exception {

        WebConversation webConversation = new WebConversation();

        try {
            makeCall(sp1Url.toString(), webConversation, HELLO_FROM_AC1);
            makeCallAndTryToAuthenticate(sp1Url.toString() + DOMAINB_URL, webConversation, HELLO_FROM_LOGIN_PAGE, USERB, USERB,
                    HELLO_FROM_SP1);
            makeCall(sp1Url.toString(), webConversation, HELLO_FROM_SP1);

            makeCall(sp2Url.toString(), webConversation, HELLO_FROM_AC2);
            makeCall(sp2Url.toString() + DOMAINA_URL, webConversation, HELLO_FROM_LOGIN_PAGE);

            // uncomment following line when https://bugzilla.redhat.com/show_bug.cgi?id=1075985 will work fine
            // makeCall(sp2Url.toString(),webConversation,HELLO_FROM_AC2);
            makeCallAndTryToAuthenticate(sp2Url.toString() + DOMAINA_URL, webConversation, HELLO_FROM_LOGIN_PAGE, USERA, USERA,
                    HELLO_FROM_SP2);

            makeCall(sp2Url.toString() + LOGOUT, webConversation, HELLO_FROM_LOGOUT_PAGE);
            makeCall(sp1Url.toString(), webConversation, HELLO_FROM_SP1);
            makeCall(sp2Url.toString(), webConversation, HELLO_FROM_LOGIN_PAGE);

        } finally {
            webConversation.clearContents();
        }

    }

    /**
     * Test whether successful authentication to IDP is propagated to another SP.
     * 
     * @throws Exception
     */
    @Test
    public void testFirstUseWrongCredentialsInOneIDP() throws Exception {

        WebConversation webConversation = new WebConversation();

        try {
            makeCall(sp1Url.toString(), webConversation, HELLO_FROM_AC1);
            makeCallAndTryToAuthenticate(sp1Url.toString() + DOMAINA_URL, webConversation, HELLO_FROM_LOGIN_PAGE, "wrong",
                    "wrong", HELLO_FROM_ERROR_LOGIN_PAGE);

            makeCall(sp2Url.toString(), webConversation, HELLO_FROM_AC2);
            makeCallAndTryToAuthenticate(sp2Url.toString() + DOMAINA_URL, webConversation, HELLO_FROM_LOGIN_PAGE, USERA, USERA,
                    HELLO_FROM_SP2);

            // uncomment following line when https://bugzilla.redhat.com/show_bug.cgi?id=1075985 will work fine
            // makeCall(sp1Url.toString(),webConversation,HELLO_FROM_AC1);
            makeCall(sp1Url.toString() + DOMAINA_URL, webConversation, HELLO_FROM_SP1);

        } finally {
            webConversation.clearContents();
        }

    }

    /**
     * Test whether successful authentication to IDP isn't propagated to another SP which tried to connect to different IDP
     * before.
     * 
     * @throws Exception
     */
    @Test
    public void testFirstUseWrongCredentialsInDifferentIDPs() throws Exception {

        WebConversation webConversation = new WebConversation();

        try {
            makeCall(sp1Url.toString(), webConversation, HELLO_FROM_AC1);
            makeCallAndTryToAuthenticate(sp1Url.toString() + DOMAINB_URL, webConversation, HELLO_FROM_LOGIN_PAGE, "wrong",
                    "wrong", HELLO_FROM_ERROR_LOGIN_PAGE);

            makeCall(sp2Url.toString(), webConversation, HELLO_FROM_AC2);
            makeCallAndTryToAuthenticate(sp2Url.toString() + DOMAINA_URL, webConversation, HELLO_FROM_LOGIN_PAGE, USERA, USERA,
                    HELLO_FROM_SP2);

            // uncomment following line when https://bugzilla.redhat.com/show_bug.cgi?id=1075985 will work fine
            // makeCall(sp1Url.toString(),webConversation,HELLO_FROM_AC1);
            makeCall(sp1Url.toString() + DOMAINB_URL, webConversation, HELLO_FROM_LOGIN_PAGE);

        } finally {
            webConversation.clearContents();
        }

    }

    /**
     * Tests whether account page is displayed only until IDP isn't chosen and user isn't authenticated.
     * 
     * Test access denied for: no credentials, wrong credentials, user without required role.
     * 
     * @throws Exception
     */
    @Test
    public void testWrongAccessAndShowingOfAccountChooserPage() throws Exception {

        WebConversation webConversation = new WebConversation();

        try {
            makeCall(sp1Url.toString(), webConversation, HELLO_FROM_AC1);
            // try it twice
            makeCall(sp1Url.toString(), webConversation, HELLO_FROM_AC1);

            makeCallAndTryToAuthenticate(sp1Url.toString() + DOMAINA_URL, webConversation, HELLO_FROM_LOGIN_PAGE, "", "",
                    HELLO_FROM_ERROR_LOGIN_PAGE);

            // uncomment following line when https://bugzilla.redhat.com/show_bug.cgi?id=1075985 will work fine
            // makeCall(sp1Url.toString(),webConversation,HELLO_FROM_AC1);

            makeCallAndTryToAuthenticate(sp1Url.toString() + DOMAINA_URL, webConversation, HELLO_FROM_LOGIN_PAGE, "wrong",
                    "wrong", HELLO_FROM_ERROR_LOGIN_PAGE);

            // uncomment following line when https://bugzilla.redhat.com/show_bug.cgi?id=1075985 will work fine
            // makeCall(sp1Url.toString(),webConversation,HELLO_FROM_AC1);

            WebResponse webResponse = makeCall(sp1Url.toString() + DOMAINA_URL, webConversation, HELLO_FROM_LOGIN_PAGE);
            WebForm loginForm = webResponse.getForms()[0];
            loginForm.setParameter("j_username", BADUSER);
            loginForm.setParameter("j_password", BADUSER);
            SubmitButton submitButton = loginForm.getSubmitButtons()[0];
            try {
                submitButton.click();
                fail("Access should be denied for user without needed role.");
            } catch (HttpException ex) {
                if (ex.getResponseCode() != HttpStatus.SC_FORBIDDEN) {
                    throw ex;
                }
            }

            // uncomment following line when https://bugzilla.redhat.com/show_bug.cgi?id=1075985 will work fine
            // makeCall(sp1Url.toString(),webConversation,HELLO_FROM_AC1);

            try {
                webConversation.getResponse(new GetMethodWebRequest(sp1Url.toString() + DOMAINA_URL));
                fail("Access should be denied for user without needed role.");
            } catch (HttpException ex) {
                if (ex.getResponseCode() != HttpStatus.SC_FORBIDDEN) {
                    throw ex;
                }
            }

        } finally {
            webConversation.clearContents();
        }
    }

    /**
     * Tests whether account chooser page is displayed if cookies are deleted.
     * 
     * @throws Exception
     */
    @Test
    public void testRemoveCookie() throws Exception {

        WebConversation webConversation = new WebConversation();
        try {
            makeCall(sp1Url.toString(), webConversation, HELLO_FROM_AC1);

            makeCall(sp1Url.toString() + DOMAINA_URL, webConversation, HELLO_FROM_LOGIN_PAGE);

            String[] str = webConversation.getCookieNames();
            for (int i = 0; i < str.length; i++) {
                webConversation.putCookie(str[i], null);
            }

            makeCall(sp1Url.toString(), webConversation, HELLO_FROM_AC1);

        } finally {
            webConversation.clearContents();
        }

    }

    /**
     * Tests whether account chooser page is displayed if cookies are deleted and user was successfully authenticated before.
     * 
     * @throws Exception
     */
    @Test
    public void testRemoveCookieAfterAuthentication() throws Exception {

        WebConversation webConversation = new WebConversation();
        try {
            makeCall(sp1Url.toString(), webConversation, HELLO_FROM_AC1);

            makeCallAndTryToAuthenticate(sp1Url.toString() + DOMAINA_URL, webConversation, HELLO_FROM_LOGIN_PAGE, USERA, USERA,
                    HELLO_FROM_SP1);

            makeCall(sp1Url.toString(), webConversation, HELLO_FROM_SP1);

            String[] str = webConversation.getCookieNames();
            for (int i = 0; i < str.length; i++) {
                webConversation.putCookie(str[i], null);
            }

            makeCall(sp1Url.toString(), webConversation, HELLO_FROM_AC1);

        } finally {
            webConversation.clearContents();
        }

    }

    /**
     * Access selected page.
     * 
     * @param url
     * @param webConversation
     * @param expectedText
     * @return
     * @throws Exception
     */
    private WebResponse makeCall(String url, WebConversation webConversation, String expectedText) throws Exception {
        WebResponse webResponse = webConversation.getResponse(new GetMethodWebRequest(url));
        assertThat("Unexpected page was displayed (according to expectedText).", webResponse.getText(),  JUnitMatchers.containsString(expectedText));
        return webResponse;
    }

    /**
     * Access page which should redirect you to IDP and then try to authenticate on this IDP.
     * 
     * @param url URL of service which redirect you to IDP
     * @param webConversation used webConversation
     * @param expectedText expected text on IDP login page
     * @param user username for authentication
     * @param password password for authentication
     * @param expectedFinalText expected text on page redirected from IDP
     * @throws Exception
     */
    private void makeCallAndTryToAuthenticate(String url, WebConversation webConversation, String expectedText, String user,
            String password, String expectedFinalText) throws Exception {
        WebResponse webResponse = makeCall(url, webConversation, expectedText);
        webResponse = loginToIdp(webConversation, webResponse, user, password);
        assertThat("Unexpected service provider page was displayed.", webResponse.getText(), JUnitMatchers.containsString(expectedFinalText));
    }

    /**
     * Fill and post a form on login page. It assumes successful authentication.
     * 
     * @param webConversation used WebConversation
     * @param webResponse used WebResponse
     * @param user username for authentication
     * @param password password for authentication
     * 
     * @return given WebResponse
     * @throws Exception
     */
    private WebResponse loginToIdp(WebConversation webConversation, WebResponse webResponse, String user, String password)
            throws Exception {

        WebForm loginForm = webResponse.getForms()[0];
        loginForm.setParameter("j_username", user);
        loginForm.setParameter("j_password", password);
        SubmitButton submitButton = loginForm.getSubmitButtons()[0];
        submitButton.click();

        webResponse = webConversation.getCurrentPage();
        return webResponse;
    }

}
