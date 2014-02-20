/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.picketlink.test.ssl;

import org.picketlink.test.integration.util.CertUtils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import static org.hamcrest.CoreMatchers.*;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.*;
import org.junit.Test;
import org.picketlink.test.integration.util.JBoss7Util;
import static org.picketlink.test.ssl.RolePrintingServlet.PARAM_ROLE_NAME;

/**
 *
 * @author hmlnarik
 */
public abstract class AbstractIdpSslCertificateLoginTestCase {

    private static final Logger LOGGER = Logger.getLogger(AbstractIdpSslCertificateLoginTestCase.class);

    protected static final String IDP_CONTEXT = "idp-ssl";

    protected static final String SERVICE_PROVIDER_NAME = "SP_DEPLOYMENT";
    protected static final String IDENTITY_PROVIDER_NAME = "IDP_DEPLOYMENT";

    protected static final String SERVICE_PROVIDER_REALM = "spRealm";
    protected static final String IDENTITY_PROVIDER_REALM = "idp-ssl";
    protected static final String IDP_SSL_SECURITY_DOMAIN = "idp-ssl";

    protected static final String PICKETLINK_MODULE_NAME = "org.picketlink";

    protected static final String FILE_ROLESPROPERTIES = "roles.properties";
    protected static final String FILE_USERSPROPERTIES = "users.properties";

    protected static PrepareSsl PREPARE_SSL_TASK;

    protected static final Pattern POST_BINDING_PATTERN = Pattern.compile("<html>.*"
      + "<form[^>]+ action=\"([^\"]+)\".*?>"
      + ".*"
      + "<input[^>]+\\s+NAME=\"(SAMLResponse)\"\\s+VALUE=\"(.*?)\""
      + "/?>", Pattern.CASE_INSENSITIVE);

    @Deployment(name = IDENTITY_PROVIDER_NAME, testable = false)
    @TargetsContainer(value = "jboss")
    public static WebArchive createIDPSigDeployment() throws Exception {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, IDP_CONTEXT + ".war");

        war.addAsManifestResource(JBoss7Util.getJBossDeploymentStructure(PICKETLINK_MODULE_NAME), "jboss-deployment-structure.xml");

        war.addAsWebInfResource(JBoss7Util.getJBossWebXmlAsset(IDENTITY_PROVIDER_REALM, "org.picketlink.identity.federation.bindings.tomcat.idp.IDPWebBrowserSSOValve"), "jboss-web.xml");
        war.addAsWebInfResource(TestIdpSslCertificateLoginTestCase.class.getPackage(), TestIdpSslCertificateLoginTestCase.class.getSimpleName() + "-idp-web.xml", "web.xml");
        war.addAsWebInfResource(new StringAsset(JBoss7Util.propertiesReplacer(TestIdpSslCertificateLoginTestCase.class.getResourceAsStream("picketlink-idp.xml"), SERVICE_PROVIDER_NAME, "REDIRECT", IDENTITY_PROVIDER_NAME)), "picketlink.xml");

        war.addAsWebResource(TestIdpSslCertificateLoginTestCase.class.getPackage(), "login-error.jsp", "login-error.jsp");
        war.addAsWebResource(TestIdpSslCertificateLoginTestCase.class.getPackage(), "login.jsp", "login.jsp");

        war.addAsResource(TestIdpSslCertificateLoginTestCase.class.getPackage(), "roles.properties", FILE_ROLESPROPERTIES);
        war.addAsResource(TestIdpSslCertificateLoginTestCase.class.getPackage(), "users.properties", FILE_USERSPROPERTIES);

        war.add(new StringAsset("Welcome to IdP"), "index.jsp");
        war.add(new StringAsset(HOSTED_IDP_INDEX_TEXT), "hosted/index.jsp");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(war.toString(true));
        }

        return war;
    }
    public static final String HOSTED_IDP_INDEX_TEXT = "Welcome to hosted IdP";

    /**
     * Creates a {@link WebArchive} for given security domain.
     *
     * @return
     */
    @Deployment(name = SERVICE_PROVIDER_NAME)
    public static WebArchive createSpWar() {
        LOGGER.info("Creating deployment for " + SERVICE_PROVIDER_NAME);

        final WebArchive war = ShrinkWrap.create(WebArchive.class, SERVICE_PROVIDER_NAME + ".war");

        war.addClasses(PrincipalPrintingServlet.class);
        war.addClasses(RolePrintingServlet.class);

        war.addAsWebInfResource(TestIdpSslCertificateLoginTestCase.class.getPackage(), TestIdpSslCertificateLoginTestCase.class.getSimpleName() + "-sp-web.xml", "web.xml");
        war.addAsWebInfResource(JBoss7Util.getJBossWebXmlAsset(SERVICE_PROVIDER_REALM, "org.picketlink.identity.federation.bindings.tomcat.sp.ServiceProviderAuthenticator"), "jboss-web.xml");
        war.addAsWebInfResource(new StringAsset(JBoss7Util.propertiesReplacer(TestIdpSslCertificateLoginTestCase.class.getResourceAsStream("picketlink-sp.xml"), SERVICE_PROVIDER_NAME, "REDIRECT", IDP_CONTEXT)), "picketlink.xml");

        war.addAsManifestResource(JBoss7Util.getJBossDeploymentStructure(PICKETLINK_MODULE_NAME), "jboss-deployment-structure.xml");

        war.add(new StringAsset("Welcome to deployment: " + SERVICE_PROVIDER_NAME), "index.jsp");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(war.toString(true));
        }

        return war;
    }

    /**
     * Accesses given SP address and authorizes via IdP using certificate already
     * set in the {@code httpClient}.
     * 
     * @param httpClient HttpClient instance used to access the URI
     * @param address URI to get the document from
     * @param invalidOutputMessage Message to output if the document at URI does not match {@code expectedOutput}
     * @param expectedOutput Expected output
     * @throws IOException 
     */
    protected void testSuccessfulOutput(HttpClient httpClient, URI address,
      String invalidOutputMessage, String expectedOutput) throws IOException {
        assertThat(PREPARE_SSL_TASK, notNullValue());

        HttpGet get = new HttpGet(address);
        HttpResponse response = httpClient.execute(get);

        assertThat("Unexpected status code when accessing SP via IdP.", response.getStatusLine().getStatusCode(), equalTo(HttpServletResponse.SC_OK));

        String responseBody = EntityUtils.toString(response.getEntity());

        response = performPostBinding(responseBody, httpClient);

        Header location = response.getFirstHeader("Location");
        assertThat("Expected redirection", location, notNullValue());
        assertThat("Expected non-empty redirection", location.getValue(), not(equalTo("")));
        EntityUtils.consume(response.getEntity());

        get = new HttpGet(location.getValue());

        response = httpClient.execute(get);
        responseBody = EntityUtils.toString(response.getEntity());

        // This is just plain wrong - SAML POST binding should be received only once. We do
        // this only because it is currently implemented so, it is working yet not efficient

        response = performPostBinding(responseBody, httpClient);
        EntityUtils.consume(response.getEntity());

        // This should have been already performed by the redirection above,
        // however it redirects to the context root.
        get = new HttpGet(address);
        response = httpClient.execute(get);
        responseBody = EntityUtils.toString(response.getEntity());

        assertThat(invalidOutputMessage, responseBody, equalTo(expectedOutput));
    }

    /**
     * Inspects response which should contain POST form containing SAML response
     * and submits the form to the form target.
     * @param responseBody Response body to inspect
     * @param httpClient Client to perform connections with
     * @return Response of the form submission
     * @throws UnsupportedEncodingException
     * @throws ParseException
     * @throws IOException
     */
    protected HttpResponse performPostBinding(String responseBody, HttpClient httpClient) throws UnsupportedEncodingException, ParseException, IOException {
        // Perform HTTP Post binding redirection.
        Matcher postBindingMatcher = POST_BINDING_PATTERN.matcher(responseBody);

        assertThat("Expected POST binding reponse, got: " + responseBody, postBindingMatcher.find(), equalTo(true));

        String formAction = postBindingMatcher.group(1);
        String samlResponseName = postBindingMatcher.group(2);
        String samlResponseValue = postBindingMatcher.group(3);

        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        pairs.add(new BasicNameValuePair(samlResponseName, samlResponseValue));

        HttpPost post = new HttpPost(formAction);
        post.setEntity(new UrlEncodedFormEntity(pairs));

        return httpClient.execute(post);
    }

    /**
     * Prepares a {@link DefaultHttpClient} instance capable of connecting
     * using http (default port: 80) and https protocol
     * (default port: {@link AddHttpsConnectorServerSetupTask#TEST_HTTPS_PORT}),
     * which for a TLS connection:
     * 
     * <ul>
     *  <li>Authenticates itself as a client using the certificate from
     *      {@link PrepareSsl#getClientKeyStore() PREPARE_SSL_TASK.getClientKeyStore()}</li>
     *  <li>Checks server certificate using the certificate from
     *      {@link PrepareSsl#getClientTrustStore() PREPARE_SSL_TASK.getClientTrustStore()}</li>
     *  <li>Is permissive - does not check hostname in server certificate.</li>
     * </ul>
     *
     * @return
     * @throws GeneralSecurityException
     */
    protected DefaultHttpClient prepareAuthenticatingPermissiveHttpClient(String clientAlias, char[] clientKeyPassword) throws GeneralSecurityException, IOException {
        KeyStore globalClientKeyStore = PREPARE_SSL_TASK.getClientKeyStore();

        Certificate[] clientCertificateChain = globalClientKeyStore.getCertificateChain(clientAlias);
        Key clientKey = globalClientKeyStore.getKey(clientAlias, clientKeyPassword);

        KeyStore clientKeyStore = CertUtils.createKeyStore();
        clientKeyStore.setKeyEntry(clientAlias, clientKey, clientKeyPassword, clientCertificateChain);

        SSLSocketFactory sslSocketFactory = new SSLSocketFactory("TLS",
          clientKeyStore,
          PrepareKeyAndTrustStoresServerSetupTask.GENERIC_PASSWORD,
          PREPARE_SSL_TASK.getClientTrustStore(),
          null,
          SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER
        );

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        schemeRegistry.register(new Scheme("https", AddHttpsConnectorServerSetupTask.TEST_HTTPS_PORT, sslSocketFactory));

        final DefaultHttpClient res = new DefaultHttpClient(new BasicClientConnectionManager(schemeRegistry));

        return res;
    }

    /**
     * Returns URI where the protocol is replaced with "https" and port with
     * {@link AddHttpsConnectorServerSetupTask#TEST_HTTPS_PORT}.
     * 
     * @param uri URI to modify
     * @return see description
     * @throws URISyntaxException
     */
    protected URI getHttpsUriFromHttpUri(URI uri) throws URISyntaxException {
        return new URI("https", uri.getUserInfo(), uri.getHost(), AddHttpsConnectorServerSetupTask.TEST_HTTPS_PORT, uri.getPath(), uri.getQuery(), uri.getFragment());
    }

    /**
     * Returns URI of the {@link PrincipalPrintingServlet} servlet.
     * @param baseUri Context path of the service provider application
     * @return see description
     * @throws URISyntaxException
     */
    protected URI getPrintPrincipalUri(URI baseUri) throws URISyntaxException {
        return new URI(baseUri.getScheme(), baseUri.getUserInfo(), baseUri.getHost(), baseUri.getPort(), baseUri.getPath() + PrincipalPrintingServlet.SERVLET_PATH, baseUri.getQuery(), baseUri.getFragment());
    }

    /**
     * Returns URI of the {@link RolePrintingServlet} servlet.
     * @param baseUri Context path of the service provider application
     * @return see description
     * @throws URISyntaxException
     */
    protected URI getPrintRolesUri(URI baseUri) throws URISyntaxException {
        return new URI(baseUri.getScheme(), baseUri.getUserInfo(), baseUri.getHost(), baseUri.getPort(), baseUri.getPath() + RolePrintingServlet.SERVLET_PATH,
          PARAM_ROLE_NAME + "=TheDuke"
          + "&" + PARAM_ROLE_NAME + "=Echo"
          + "&" + PARAM_ROLE_NAME + "=TheDuke2"
          + "&" + PARAM_ROLE_NAME + "=Echo2"
          + "&" + PARAM_ROLE_NAME + "=JBossAdmin"
          + "&" + PARAM_ROLE_NAME + "=jduke"
          + "&" + PARAM_ROLE_NAME + "=jduke2"
          + "&" + PARAM_ROLE_NAME + "=RG1"
          + "&" + PARAM_ROLE_NAME + "=RG/2"
          + "&" + PARAM_ROLE_NAME + "=RG3"
          + "&" + PARAM_ROLE_NAME + "=R1"
          + "&" + PARAM_ROLE_NAME + "=R2"
          + "&" + PARAM_ROLE_NAME + "=R3"
          + "&" + PARAM_ROLE_NAME + "=R4"
          + "&" + PARAM_ROLE_NAME + "=R5"
          + "&" + PARAM_ROLE_NAME + "=Roles"
          + "&" + PARAM_ROLE_NAME + "=User"
          + "&" + PARAM_ROLE_NAME + "=Admin"
          + "&" + PARAM_ROLE_NAME + "=SharedRoles",
          baseUri.getFragment());
    }


    /**
     * Test that IdP deployment is accessible via https protocol.
     */
    @OperateOnDeployment(value = IDENTITY_PROVIDER_NAME)
    @Test
    public void testIdpDeployment(@ArquillianResource URI uri) throws Exception {
        assertThat(PREPARE_SSL_TASK, notNullValue());
        assertThat(uri, notNullValue());

        URI address = getHttpsUriFromHttpUri(uri);
        DefaultHttpClient httpClient = prepareAuthenticatingPermissiveHttpClient(
          PrepareKeyAndTrustStoresServerSetupTask.TRUSTED_CLIENT_KEY_ALIAS,
          PrepareKeyAndTrustStoresServerSetupTask.GENERIC_PASSWORD_CHARS
        );
        HttpGet get = new HttpGet(address);
        HttpResponse response = httpClient.execute(get);

        assertThat("Unexpected status code when accessing IdP via https.", response.getStatusLine().getStatusCode(), equalTo(HttpServletResponse.SC_OK));
        
        // Redirection should work when processing the request.
        assertThat("Unexpected IdP page body when accessing IdP via https.", EntityUtils.toString(response.getEntity()), equalTo(HOSTED_IDP_INDEX_TEXT));
    }

    /**
     * Test that IdP deployment is accessible via http protocol.
     */
    @OperateOnDeployment(value = IDENTITY_PROVIDER_NAME)
    @Test
    public void testIdpDeploymentNonSecured(@ArquillianResource URI uri) throws Exception {
        assertThat(PREPARE_SSL_TASK, notNullValue());
        assertThat(uri, notNullValue());

        DefaultHttpClient httpClient = prepareAuthenticatingPermissiveHttpClient(
          PrepareKeyAndTrustStoresServerSetupTask.TRUSTED_CLIENT_KEY_ALIAS,
          PrepareKeyAndTrustStoresServerSetupTask.GENERIC_PASSWORD_CHARS
        );
        HttpGet get = new HttpGet(uri);
        HttpResponse response = httpClient.execute(get);

        assertThat("Unexpected status code when accessing IdP via http.", response.getStatusLine().getStatusCode(), equalTo(HttpServletResponse.SC_OK));
    }



    /**
     * Prepares the keystores and SSL connector for the test.
     */
    protected static class PrepareSsl extends AddHttpsConnectorServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            super.setup(managementClient, containerId);

            PREPARE_SSL_TASK = this;
        }
    }

}
