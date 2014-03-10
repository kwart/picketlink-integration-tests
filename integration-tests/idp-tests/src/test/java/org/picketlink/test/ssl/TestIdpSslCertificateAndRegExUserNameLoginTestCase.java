/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.picketlink.test.ssl;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import javax.net.ssl.SSLPeerUnverifiedException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import static org.hamcrest.CoreMatchers.*;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketlink.test.integration.util.PicketLinkIntegrationTests;
import org.picketlink.test.integration.util.TargetContainers;

import org.jboss.as.security.Constants;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.picketlink.test.integration.security.as7.AbstractSecurityDomainsServerSetupTask;
import org.picketlink.test.integration.security.as7.JSSE;
import org.picketlink.test.integration.security.as7.SecureStore;
import org.picketlink.test.integration.security.as7.SecurityDomain;
import org.picketlink.test.integration.security.as7.SecurityModule;
import static org.picketlink.test.ssl.AbstractIdpSslCertificateLoginTestCase.PREPARE_SSL_TASK;

/**
 * Tests a IdP using a certificate and RegExUserNameLoginModule.
 * The username is expected to be modified by the RegExUserNameLoginModule.
 * 
 * @author Hynek Mlnarik <hmlnarik@redhat.com>
 */
@TargetContainers ({"jbas7","eap6"})
@ServerSetup({
    AbstractIdpSslCertificateLoginTestCase.PrepareSsl.class,
    TestIdpSslCertificateAndRegExUserNameLoginTestCase.SecurityDomainsServerSetupTask.class
})
@RunWith(PicketLinkIntegrationTests.class)
@RunAsClient
public class TestIdpSslCertificateAndRegExUserNameLoginTestCase extends AbstractIdpSslCertificateLoginTestCase {

    @Rule
    public ExpectedException expextedException = ExpectedException.none();

    /**
     * Test that it is possible to authenticate using a given client certificate.
     */
    @OperateOnDeployment(SERVICE_PROVIDER_NAME)
    @Test
    public void testTrustedCert(@ArquillianResource URI uri) throws Exception {
        assertThat(uri, notNullValue());
        assertThat(uri.getScheme(), equalTo("http"));

        URI address = getPrintPrincipalUri(uri);

        DefaultHttpClient httpClient = prepareAuthenticatingPermissiveHttpClient(
          PrepareKeyAndTrustStoresServerSetupTask.TRUSTED_CLIENT_KEY_ALIAS,
          PrepareKeyAndTrustStoresServerSetupTask.GENERIC_PASSWORD_CHARS
        );

        testSuccessfulOutput(httpClient, address, "Unexpected principal name.", "\"" + PrepareKeyAndTrustStoresServerSetupTask.COMMON_NAME_TRUSTED_CLIENT + "\"");
    }

    /**
     * Test that it is possible to authenticate using a given client certificate and get expected roles.
     */
    @OperateOnDeployment(SERVICE_PROVIDER_NAME)
    @Test
    public void testTrustedCertRoles(@ArquillianResource URI uri) throws Exception {
        assertThat(uri, notNullValue());
        assertThat(uri.getScheme(), equalTo("http"));

        URI address = getPrintRolesUri(uri);

        DefaultHttpClient httpClient = prepareAuthenticatingPermissiveHttpClient(
          PrepareKeyAndTrustStoresServerSetupTask.TRUSTED_CLIENT_KEY_ALIAS,
          PrepareKeyAndTrustStoresServerSetupTask.GENERIC_PASSWORD_CHARS
        );

        testSuccessfulOutput(httpClient, address, "Unexpected roles.",  ",TheDuke,User,R1,");
    }


    /**
     * Test that it is not possible to authenticate using a non-trusted client certificate.
     */
    @OperateOnDeployment(SERVICE_PROVIDER_NAME)
    @Test
    public void testUntrustedCert(@ArquillianResource URI uri) throws Exception {
        assertThat(PREPARE_SSL_TASK, notNullValue());
        assertThat(uri, notNullValue());
        assertThat(uri.getScheme(), equalTo("http"));

        DefaultHttpClient httpClient = prepareAuthenticatingPermissiveHttpClient(
          PrepareKeyAndTrustStoresServerSetupTask.UNTRUSTED_CLIENT_KEY_ALIAS,
          PrepareKeyAndTrustStoresServerSetupTask.GENERIC_PASSWORD_CHARS
        );

        URI address = getPrintPrincipalUri(uri);
        HttpGet get = new HttpGet(address);

        expextedException.expect(SSLPeerUnverifiedException.class);
        httpClient.execute(get);
    }

    static class SecurityDomainsServerSetupTask extends AbstractSecurityDomainsServerSetupTask {

        @Override
        protected SecurityDomain[] getSecurityDomains() throws Exception {
            List<SecurityDomain> res = new LinkedList<SecurityDomain>();

            // Add host security domain
            res.add(new SecurityDomain.Builder()
              .name(IDP_SSL_SECURITY_DOMAIN)
              .cacheType("default")
              .loginModules(
                new SecurityModule.Builder()
                .name("CertificateRoles")
                .flag(Constants.REQUIRED)
                .putOption("password-stacking", "useFirstPass")
                .putOption("securityDomain", IDP_SSL_SECURITY_DOMAIN)
                .putOption("verifier", "org.jboss.security.auth.certs.AnyCertVerifier")
                .build(),

                new SecurityModule.Builder()
                .name("org.picketlink.identity.federation.bindings.jboss.auth.RegExUserNameLoginModule")
                .flag(Constants.REQUIRED)
                .putOption("password-stacking", "useFirstPass")
                .putOption("regex", "CN=([^\",]+|\"[^\"]*\"),")
                .build(),

                new SecurityModule.Builder()
                .name("UsersRoles")
                .flag(Constants.OPTIONAL)
                .putOption("password-stacking", "useFirstPass")
                .putOption("usersProperties", FILE_USERSPROPERTIES)
                .putOption("rolesProperties", FILE_ROLESPROPERTIES)
                .build()
                )
              .jsse(
                new JSSE.Builder()
                .keyStore(
                  new SecureStore.Builder()
                  .url(PREPARE_SSL_TASK.getServerKeystoreFile().toURI().toURL())
                  .password(PrepareKeyAndTrustStoresServerSetupTask.GENERIC_PASSWORD)
                  .build())
                .trustStore(
                  new SecureStore.Builder()
                  .url(PREPARE_SSL_TASK.getServerTruststoreFile().toURI().toURL())
                  .password(PrepareKeyAndTrustStoresServerSetupTask.GENERIC_PASSWORD)
                  .build()
                )
                .clientAuth(true)
                .build()
              )
              .build()
            );


            // Add SP security domain
            res.add(new SecurityDomain.Builder()
              .name(SERVICE_PROVIDER_REALM)
              .loginModules(
                new SecurityModule.Builder()
                .name("org.picketlink.identity.federation.bindings.jboss.auth.SAML2LoginModule")
                .flag(Constants.REQUIRED)
                .build()
              )
              .build()
            );

            return res.toArray(new SecurityDomain[0]);
        }
    }

}

