package org.picketlink.test.integration.relaystate;


import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.SubmitButton;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.hamcrest.CoreMatchers.*;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import static org.junit.matchers.JUnitMatchers.*;
import org.junit.runner.RunWith;
import org.picketlink.test.integration.util.JBoss7Util;
import org.picketlink.test.integration.util.PicketLinkIntegrationTests;
import org.picketlink.test.integration.util.TargetContainers;

/**
 * Test for PLINK-396: IDPWebBrowserSSOValve and IDPFilter are decoding the relaystate.
 *
 * The AbstractIDPBrowserValve and IDPFilter are decoding the relaystate.
 * Per 5.1.2 of the SAML spec: "If the IdP received a RelayState value
 * from the SP, it must return it <b>unmodified</b> to the SP in a hidden form
 * control named RelayState."
 *
 * @see https://issues.jboss.org/browse/PLINK-396
 * @author Hynek Mlnarik <hmlnarik@redhat.com>
 */
@TargetContainers({ "jbas7", "eap6" })
@RunWith(PicketLinkIntegrationTests.class)
@RunAsClient
@ServerSetup(RelayStateDecodingTestCase.ValveSetup.class)
public class RelayStateDecodingTestCase {

    private static final Logger LOGGER = Logger.getLogger(RelayStateDecodingTestCase.class.getName());

    private static final String LOGOUT = "?GLO=true";

    protected static final String IDP_CONTEXT = "idp";

    protected static final String SERVICE_PROVIDER_NAME = "sp";
    protected static final String IDENTITY_PROVIDER_NAME = IDP_CONTEXT;

    protected static final String SERVICE_PROVIDER_REALM = SERVICE_PROVIDER_NAME;
    protected static final String IDENTITY_PROVIDER_REALM = IDP_CONTEXT;

    protected static final String PICKETLINK_MODULE_NAME = "org.picketlink";

    protected static final String FILE_ROLESPROPERTIES = "roles.properties";
    protected static final String FILE_USERSPROPERTIES = "users.properties";

    private static final String HOSTED_IDP_INDEX_TEXT = "Welcome to hosted IdP";

    static class ValveSetup implements ServerSetupTask {

        private static final String MODULE_NAME = "org.picketlink.test.integration";
        private static final String CLASS_NAME = "org.picketlink.test.integration.relaystate.RelayStatePublishingValve";
        private static final String BASE_MODULE_PATH = "/modules/org/picketlink/test/integration/main";

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            String path = ValveUtil.readASPath(managementClient.getControllerClient());
            File file = new File(path);
            if (file.exists()) {
                file = new File(path + BASE_MODULE_PATH);
                file.mkdirs();
                System.out.println(file.getAbsolutePath());
                file = new File(path + BASE_MODULE_PATH + "/valves.jar");
                if (file.exists())
                    file.delete();
                createJar(file);
                file = new File(path + BASE_MODULE_PATH + "/module.xml");
                if (file.exists())
                    file.delete();
                FileWriter fstream = new FileWriter(path + BASE_MODULE_PATH + "/module.xml");
                BufferedWriter out = new BufferedWriter(fstream);
                out.write("<module xmlns=\"urn:jboss:module:1.1\" name=\"" + MODULE_NAME + "\">\n");
                out.write("    <properties>\n");
                out.write("        <property name=\"jboss.api\" value=\"private\"/>\n");
                out.write("    </properties>\n");

                out.write("    <resources>\n");
                out.write("        <resource-root path=\"valves.jar\"/>\n");
                out.write("    </resources>\n");

                out.write("    <dependencies>\n");
                out.write("        <module name=\"sun.jdk\"/>\n");
                out.write("        <module name=\"javax.servlet.api\"/>\n");
                out.write("        <module name=\"org.jboss.as.web\"/>\n");
                out.write("    </dependencies>\n");
                out.write("</module>");
                out.close();
            }
            ValveUtil.createValveModule(managementClient.getControllerClient(), "myvalve", MODULE_NAME, CLASS_NAME);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            ValveUtil.removeValve(managementClient.getControllerClient(), "myvalve");
        }
    }

    public static void createJar(File file) {

        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "valve.jar")
                              .addClass(RelayStatePublishingValve.class);

        archive.as(ZipExporter.class).exportTo(file);
    }



    @ArquillianResource
    @OperateOnDeployment(SERVICE_PROVIDER_NAME)
    private URL spUrl;

    
    @Deployment(name = IDENTITY_PROVIDER_NAME, testable = false)
    public static WebArchive createIDPSigDeployment() throws Exception {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, IDP_CONTEXT + ".war");

        war.addAsManifestResource(JBoss7Util.getJBossDeploymentStructure(PICKETLINK_MODULE_NAME, ValveSetup.MODULE_NAME), "jboss-deployment-structure.xml");

        war.addAsWebInfResource(RelayStateDecodingTestCase.class.getPackage(), "idp-web.xml", "web.xml");
        war.addAsWebInfResource(
          JBoss7Util.getJBossWebXmlAsset(
            IDENTITY_PROVIDER_REALM,
            "org.picketlink.identity.federation.bindings.tomcat.idp.IDPWebBrowserSSOValve",
            "org.picketlink.test.integration.relaystate.RelayStatePublishingValve"
          ), "jboss-web.xml"
        );
        war.addAsWebInfResource(new StringAsset(JBoss7Util.propertiesReplacer(RelayStateDecodingTestCase.class.getResourceAsStream("idp-picketlink.xml"), SERVICE_PROVIDER_NAME, "POST", IDENTITY_PROVIDER_NAME)), "picketlink.xml");

        war.addAsWebResource(RelayStateDecodingTestCase.class.getPackage(), "login-error.jsp", "login-error.jsp");
        war.addAsWebResource(RelayStateDecodingTestCase.class.getPackage(), "login.jsp", "login.jsp");

        war.addAsResource(RelayStateDecodingTestCase.class.getPackage(), "roles.properties", FILE_ROLESPROPERTIES);
        war.addAsResource(RelayStateDecodingTestCase.class.getPackage(), "users.properties", FILE_USERSPROPERTIES);

        war.add(new StringAsset("Welcome to IdP"), "index.jsp");
        war.add(new StringAsset(HOSTED_IDP_INDEX_TEXT), "hosted/index.jsp");

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(war.toString(true));
        }

        return war;
    }

    /**
     * Creates a {@link WebArchive} for given security domain.
     *
     * @return
     */
    @Deployment(name = SERVICE_PROVIDER_NAME)
    public static WebArchive createSpWar() {
        LOGGER.info("Creating deployment for " + SERVICE_PROVIDER_NAME);

        final WebArchive war = ShrinkWrap.create(WebArchive.class, SERVICE_PROVIDER_NAME + ".war");

        war.addAsManifestResource(JBoss7Util.getJBossDeploymentStructure(PICKETLINK_MODULE_NAME), "jboss-deployment-structure.xml");

        war.addAsWebInfResource(RelayStateDecodingTestCase.class.getPackage(), "sp-web.xml", "web.xml");
        war.addAsWebInfResource(JBoss7Util.getJBossWebXmlAsset(SERVICE_PROVIDER_REALM, "org.picketlink.identity.federation.bindings.tomcat.sp.ServiceProviderAuthenticator"), "jboss-web.xml");
        war.addAsWebInfResource(new StringAsset(JBoss7Util.propertiesReplacer(RelayStateDecodingTestCase.class.getResourceAsStream("sp-picketlink.xml"), SERVICE_PROVIDER_NAME, "POST", IDP_CONTEXT)), "picketlink.xml");

        war.add(new StringAsset("Welcome to SP"), "index.jsp");

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(war.toString(true));
        }

        return war;
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
    private static WebResponse makeCall(String url, WebConversation webConversation, String expectedText) throws Exception {
        WebResponse webResponse = webConversation.getResponse(new GetMethodWebRequest(url));
        assertThat("Unexpected page was displayed (according to expectedText).", webResponse.getText(), JUnitMatchers.containsString(expectedText));
        return webResponse;
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
    private static WebResponse loginToIdp(WebConversation webConversation, WebResponse webResponse, String user, String password)
            throws Exception {

        WebForm loginForm = webResponse.getForms()[0];
        loginForm.setParameter("j_username", user);
        loginForm.setParameter("j_password", password);
        SubmitButton submitButton = loginForm.getSubmitButtons()[0];
        submitButton.click();

        webResponse = webConversation.getCurrentPage();
        return webResponse;
    }


    @Test
    public void testBasicLogin() throws Exception {

        WebConversation webConversation = new WebConversation();
        HttpUnitOptions.setScriptingEnabled(false);

        try {
            WebResponse samlRequestResponse = makeCall(spUrl.toString(), webConversation, "HTTP Post Binding");

            assertThat(samlRequestResponse.getForms(), notNullValue());
            assertEquals(1, samlRequestResponse.getForms().length);

            WebForm samlForm = samlRequestResponse.getForms()[0];

            WebRequest requestWithRelayState = new PostMethodWebRequest(samlForm.getAction());
            for (String paramName : samlForm.getParameterNames()) {
                requestWithRelayState.setParameter(paramName, samlForm.getParameterValues(paramName));
            }
            requestWithRelayState.setParameter("RelayState", "http%3A%2F%2F${hostname}%3A8080%2F${deployment}%2F%2F");
            WebResponse submitResponse = webConversation.getResponse(requestWithRelayState);

            assertThat(submitResponse.getForms(), notNullValue());
            assertEquals(1, submitResponse.getForms().length);
            assertThat("Expected field j_username", Arrays.asList(submitResponse.getForms()[0].getParameterNames()), hasItem("j_username"));
            assertEquals("http%3A%2F%2F${hostname}%3A8080%2F${deployment}%2F%2F", submitResponse.getElementWithID("relaystate").getText());

            WebResponse loginToIdpResponse = loginToIdp(webConversation, submitResponse, "user1", "password1");

            assertThat("Unexpected page was displayed.", loginToIdpResponse.getText(), JUnitMatchers.containsString("HTTP Post Binding"));
            assertThat(loginToIdpResponse.getForms(), notNullValue());
            assertEquals(1, loginToIdpResponse.getForms().length);
            assertThat("Expected field RelayState", Arrays.asList(loginToIdpResponse.getForms()[0].getParameterNames()), hasItem("RelayState"));
            assertThat("Unexpected RelayState parameter value.", loginToIdpResponse.getForms()[0].getParameterValue("RelayState"), equalTo("http%3A%2F%2F${hostname}%3A8080%2F${deployment}%2F%2F"));

            loginToIdpResponse.getForms()[0].submit();

            HttpUnitOptions.setScriptingEnabled(true);
        } finally {
            webConversation.clearContents();
            HttpUnitOptions.setScriptingEnabled(true);
        }

    }
}
