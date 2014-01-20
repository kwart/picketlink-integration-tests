package org.jboss.aerogear.jaxrs.secure.rest.endpoint;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.specification.RequestSpecification;
import org.jboss.aerogear.jaxrs.demo.rest.endpoint.RegistrationEndpoint;
import org.jboss.aerogear.jaxrs.demo.user.SimpleUser;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.extension.rest.client.ArquillianResteasyResource;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;

/**
 * Created by hmlnarik on 1/17/14.
 */
public class AbstractSecureAnnotationTest {

    public static final String REST_APP_URI_BASE = "rest";

    private static final String USER_NAME_ADMIN = "rest-test-admin";
    private static final String PASSWORD_ADMIN = "123";

    private static final String USER_NAME_DEVELOPER = "rest-test-developer";
    private static final String PASSWORD_DEVELOPER = "123";

    public static final String REST_PROTECTED_URI_BASE = REST_APP_URI_BASE + "/protected";

    @ArquillianResource
    private URL context;

    @Deployment(testable = false)
    public static WebArchive getDeployment() {
        return ShrinkWrap.createFromZipFile(WebArchive.class, new File("../../integration-tests/idm-aerogear-security/target/aerogear-rest-test.war"));
    }

    protected String loginAdmin() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("loginName", USER_NAME_ADMIN);
        jsonObject.put("password", PASSWORD_ADMIN);

        com.jayway.restassured.response.Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("Accept", "application/JSON"))
                .body(jsonObject.toJSONString())
                .post("{root}" + REST_APP_URI_BASE + "/auth/login", context.toExternalForm());

        Assert.assertEquals("login", Response.Status.OK.getStatusCode(), response.getStatusCode());

        return response.getCookie("JSESSIONID");
    }

    protected String loginDeveloper() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("loginName", USER_NAME_DEVELOPER);
        jsonObject.put("password", PASSWORD_DEVELOPER);

        com.jayway.restassured.response.Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("Accept", "application/JSON"))
                .body(jsonObject.toJSONString())
                .post("{root}" + REST_APP_URI_BASE + "/auth/login", context.toExternalForm());

        return response.getCookie("JSESSIONID");
    }

    protected int logout(String cookie) {
        JSONObject jsonObject = new JSONObject();

        com.jayway.restassured.response.Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("Accept", "application/JSON"))
                .body(jsonObject.toJSONString())
                .cookie("JSESSIONID", cookie)
                .post("{root}" + REST_APP_URI_BASE + "/auth/logout", context.toExternalForm());

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

        com.jayway.restassured.response.Response response = spec
                .get("{root}" + REST_PROTECTED_URI_BASE + "/" + method, context.toExternalForm());

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

        com.jayway.restassured.response.Response response = spec
                .get("{root}" + REST_PROTECTED_URI_BASE + "/" + method, context.toExternalForm());

        Assert.assertEquals("Expecting response code 401 (UNAUTHORIZED).", Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatusCode());

        if (cookie != null) {
            logout(cookie);
        }
    }

    @Test
    @InSequence(101)
    public void registerDeveloper(@ArquillianResteasyResource(REST_APP_URI_BASE) RegistrationEndpoint registrationEndpoint) {
        Assert.assertNotNull("registrationEndpoint must not be null", registrationEndpoint);
        Response response = registrationEndpoint.register(getUser(USER_NAME_DEVELOPER, "developer@email.com", PASSWORD_DEVELOPER, "developer"));
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @InSequence(102)
    public void registerAdmin(@ArquillianResteasyResource(REST_APP_URI_BASE) RegistrationEndpoint registrationEndpoint) {
        Assert.assertNotNull("registrationEndpoint must not be null", registrationEndpoint);
        Response response = registrationEndpoint.register(getUser(USER_NAME_ADMIN, "admin@email.com", PASSWORD_ADMIN, "admin"));
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }


    @Test
    @InSequence(111)
    public void testPublicMethod() throws Exception {
        assertAccessAllowed("publicMethod", null);
        assertAccessAllowed("publicMethod", loginAdmin());
        assertAccessAllowed("publicMethod", loginDeveloper());
    }

    @Test
    @InSequence(112)
    public void testAdminRestrictedMethod() throws Exception {
        assertAccessUnauthorized("adminRestrictedMethod", null);
        assertAccessUnauthorized("adminRestrictedMethod", loginDeveloper());

        assertAccessAllowed("adminRestrictedMethod", loginAdmin());
    }

    @Test
    @InSequence(113)
    public void testDeveloperRestrictedMethod() throws Exception {
        assertAccessUnauthorized("developerRestrictedMethod", null);
        assertAccessUnauthorized("developerRestrictedMethod", loginAdmin());

        assertAccessAllowed("developerRestrictedMethod", loginDeveloper());
    }

    @Test
    @InSequence(114)
    public void testAdminOrDeveloperRestrictedMethod() throws Exception {
        assertAccessUnauthorized("adminOrDeveloperRestrictedMethod", null);

        assertAccessAllowed("adminOrDeveloperRestrictedMethod", loginAdmin());
        assertAccessAllowed("adminOrDeveloperRestrictedMethod", loginDeveloper());
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
