/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the 
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.jaxrs.rest.test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Header;
import org.jboss.aerogear.jaxrs.demo.rest.endpoint.LoginEndpoint;
import org.jboss.aerogear.jaxrs.demo.rest.endpoint.RegistrationEndpoint;
import org.jboss.aerogear.jaxrs.demo.user.SimpleUser;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.extension.rest.client.ArquillianResteasyResource;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.net.URL;


/**
 * @author <a href="smikloso@redhat.com">Stefan Miklosovic</a>
 * 
 */
public class AbstractRestTest {

    @ArquillianResource
    private URL context;

    @Deployment(testable = false)
    public static WebArchive getDeployment() {
        return ShrinkWrap.createFromZipFile(WebArchive.class, new File("../../integration-tests/idm-aerogear-security/target/aerogear-rest-test.war"));
    }

    @Test
    @InSequence(0)
    public void testSanity(@ArquillianResteasyResource("rest") LoginEndpoint loginEndpoint) {
        Assert.assertNotNull("context must not be null", context);
        Assert.assertNotNull("loginEndpoint must not be null", loginEndpoint);
        Assert.assertEquals("Hello", loginEndpoint.hello());
    }

    @Test
    @InSequence(1)
    public void testRegistration(@ArquillianResteasyResource("rest") RegistrationEndpoint registrationEndpoint) {
        Assert.assertNotNull("registrationEndpoint must not be null", registrationEndpoint);
        Response response = registrationEndpoint.register(getUser("someuser", "someuser@email.com", "mypassword"));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @InSequence(2)
    public void testDuplicateRegistration(@ArquillianResteasyResource("rest") RegistrationEndpoint registrationEndpoint) {
        Assert.assertNotNull("registrationEndpoint must not be null", registrationEndpoint);
        Response response = registrationEndpoint.register(getUser("someuser", "someuser@email.com", "mypassword"));
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @InSequence(3)
    public void testNotSpecifiedFields(@ArquillianResteasyResource("rest") RegistrationEndpoint registrationEndpoint) {
        Assert.assertNotNull("registrationEndpoint must not be null", registrationEndpoint);
        Response response = registrationEndpoint.register(getUser("someuser", null, null));
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @InSequence(4)
    public void loginIncompleteUser(@ArquillianResteasyResource("rest") LoginEndpoint loginEndpoint) {
        Assert.assertNotNull("loginEndpoint must not be null", loginEndpoint);
        Response response = loginEndpoint.login(getUser("someuser", null, null));
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @InSequence(5)
    public void loginNotRegisteredUser(@ArquillianResteasyResource("rest") LoginEndpoint loginEndpoint) {
        Assert.assertNotNull("loginEndpoint must not be null", loginEndpoint);
        Response response = loginEndpoint.login(getUser("nonregistered", "someemail@email.com", "whatever"));
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @InSequence(6)
    public void loginUserWithWrongPassword(@ArquillianResteasyResource("rest") LoginEndpoint loginEndpoint) {
        Assert.assertNotNull("loginEndpoint must not be null", loginEndpoint);
        Response response = loginEndpoint.login(getUser("someuser", null, "wrongpassword"));
        Assert.assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    @InSequence(7)
    public void loginAndLogoutUser(@ArquillianResteasyResource("rest/auth/logout") ClientRequest request) throws Exception {
        Assert.assertNotNull("request must not be null", request);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("loginName", "someuser");
        jsonObject.put("password", "mypassword");
        
        com.jayway.restassured.response.Response response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header(new Header("Accept", "application/JSON"))
            .body(jsonObject.toJSONString())
            .post("{root}rest/auth/login", context.toExternalForm());
        
        String cookie = response.getCookie("JSESSIONID");
        
        Assert.assertNotNull("Cookie must not be null", cookie);
        
        request.cookie("JSESSIONID", cookie);
        Assert.assertEquals("Expected HTTP status OK", Status.OK.getStatusCode(), request.post().getStatus());
    }
    
    private SimpleUser getUser(String loginName, String email, String password) {
        SimpleUser user = new SimpleUser();
        user.setLoginName(loginName);
        user.setEmail(email);
        user.setPassword(password);
        user.setCreatedDate(null);
        return user;
    }
}
