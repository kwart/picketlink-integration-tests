package org.jboss.aerogear.jaxrs.secure.rest.endpoint;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.specification.RequestSpecification;
import org.jboss.aerogear.jaxrs.demo.rest.endpoint.RegistrationEndpoint;
import org.jboss.aerogear.jaxrs.demo.user.SimpleUser;
import org.jboss.aerogear.jaxrs.demo.user.UserRoles;
import org.jboss.arquillian.extension.rest.client.ArquillianResteasyResource;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Created by hmlnarik on 1/17/14.
 */
public class AbstractSecureAnnotationTest {

    private static final Logger LOG = Logger.getLogger(AbstractSecureAnnotationTest.class.getSimpleName());

    public static final String REST_APP_URI_BASE = "rest";

    private static final String USER_NAME_ADMIN = "rest-test-admin";
    private static final String PASSWORD_ADMIN = "123";

    private static final String USER_NAME_DEVELOPER = "rest-test-developer";
    private static final String PASSWORD_DEVELOPER = "123";

    public static final String REST_PROTECTED_URI_BASE = REST_APP_URI_BASE + "/protected";

    public static final String DEPLOYMENT_NAME = "testSecureAnnotationDeployment";
    public static final String SERVER_QUALIFIER = "server-manual";

    private URL context;

    private void setContext(URL url) {
        Assert.assertNotNull("url must not be null", url);
        this.context = url;
    }

    protected String loginAdmin() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("loginName", USER_NAME_ADMIN);
        jsonObject.put("password", PASSWORD_ADMIN);

        Assert.assertNotNull("context must not be null", this.context);

        com.jayway.restassured.response.Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("Accept", "application/JSON"))
                .body(jsonObject.toJSONString())
                .post("{root}" + REST_APP_URI_BASE + "/auth/login", this.context.toExternalForm());

        Assert.assertNotNull("response must not be null", response);
        Assert.assertEquals("login", Response.Status.OK.getStatusCode(), response.getStatusCode());

        return response.getCookie("JSESSIONID");
    }

    protected String loginUser() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("loginName", USER_NAME_DEVELOPER);
        jsonObject.put("password", PASSWORD_DEVELOPER);

        Assert.assertNotNull("context must not be null", this.context);

        com.jayway.restassured.response.Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("Accept", "application/JSON"))
                .body(jsonObject.toJSONString())
                .post("{root}" + REST_APP_URI_BASE + "/auth/login", this.context.toExternalForm());

        Assert.assertNotNull("response must not be null", response);

        return response.getCookie("JSESSIONID");
    }

    protected int logout(String cookie) {
        JSONObject jsonObject = new JSONObject();

        Assert.assertNotNull("context must not be null", this.context);

        com.jayway.restassured.response.Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("Accept", "application/JSON"))
                .body(jsonObject.toJSONString())
                .cookie("JSESSIONID", cookie)
                .post("{root}" + REST_APP_URI_BASE + "/auth/logout", this.context.toExternalForm());

        Assert.assertNotNull("response must not be null", response);
        Assert.assertEquals("logout", Response.Status.OK.getStatusCode(), response.getStatusCode());

        return response.getStatusCode();
    }

    private void assertAccessAllowed(String method, String cookie) throws Exception {
        RequestSpecification spec = RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("Accept", "application/JSON"));

        if (cookie != null) {
            spec = spec.cookie("JSESSIONID", cookie);
        }

        Assert.assertNotNull("context must not be null", this.context);

        com.jayway.restassured.response.Response response = spec
                .get("{root}" + REST_PROTECTED_URI_BASE + "/" + method, this.context.toExternalForm());

        Assert.assertNotNull("response must not be null", response);
        Assert.assertEquals("Expecting response code 200 (OK).", Response.Status.OK.getStatusCode(), response.getStatusCode());
        Assert.assertEquals(method, response.body().asString());

        if (cookie != null) {
            logout(cookie);
        }
    }

    private void assertAccessUnauthorized(String method, String cookie) throws Exception {
        RequestSpecification spec = RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("Accept", "application/JSON"));

        if (cookie != null) {
            spec = spec.cookie("JSESSIONID", cookie);
        }

        Assert.assertNotNull("context must not be null", this.context);

        com.jayway.restassured.response.Response response = spec
                .get("{root}" + REST_PROTECTED_URI_BASE + "/" + method, this.context.toExternalForm());

        Assert.assertEquals("Expecting response code 401 (UNAUTHORIZED).", Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatusCode());

        if (cookie != null) {
            logout(cookie);
        }
    }

    @Test
    @InSequence(101)
    public void registerUser(@ArquillianResteasyResource(REST_APP_URI_BASE) RegistrationEndpoint registrationEndpoint) {
        Assert.assertNotNull("registrationEndpoint must not be null", registrationEndpoint);
        Response response = registrationEndpoint.register(getUser(USER_NAME_DEVELOPER, "developer@email.com", PASSWORD_DEVELOPER, UserRoles.USER));
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @InSequence(102)
    public void registerAdmin(@ArquillianResteasyResource(REST_APP_URI_BASE) RegistrationEndpoint registrationEndpoint) {
        Assert.assertNotNull("registrationEndpoint must not be null", registrationEndpoint);
        Response response = registrationEndpoint.register(getUser(USER_NAME_ADMIN, "admin@email.com", PASSWORD_ADMIN, UserRoles.ADMIN));
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }


    @Test
    @InSequence(111)
    public void testPublicMethod(@ArquillianResource URL url) throws Exception {
        setContext(url);

        assertAccessAllowed("publicMethod", null);
        assertAccessAllowed("publicMethod", loginAdmin());
        assertAccessAllowed("publicMethod", loginUser());
    }

    @Test
    @InSequence(112)
    public void testAdminRestrictedMethod(@ArquillianResource URL url) throws Exception {
        setContext(url);

        assertAccessUnauthorized("adminRestrictedMethod", null);
        assertAccessUnauthorized("adminRestrictedMethod", loginUser());

        assertAccessAllowed("adminRestrictedMethod", loginAdmin());
    }

    @Test
    @InSequence(113)
    public void testUserRestrictedMethod(@ArquillianResource URL url) throws Exception {
        setContext(url);

        assertAccessUnauthorized("userRestrictedMethod", null);
        assertAccessUnauthorized("userRestrictedMethod", loginAdmin());

        assertAccessAllowed("userRestrictedMethod", loginUser());
    }

    @Test
    @InSequence(114)
    public void testAdminOrUserRestrictedMethod(@ArquillianResource URL url) throws Exception {
        setContext(url);

        assertAccessUnauthorized("adminOrUserRestrictedMethod", null);

        assertAccessAllowed("adminOrUserRestrictedMethod", loginAdmin());
        assertAccessAllowed("adminOrUserRestrictedMethod", loginUser());
    }


    private SimpleUser getUser(String loginName, String email, String password, String roles) {
        SimpleUser user = new SimpleUser();
        user.setLoginName(loginName);
        user.setEmail(email);
        user.setPassword(password);
        user.setCreatedDate(null);
        user.setRoles(roles);
        return user;
    }

}
