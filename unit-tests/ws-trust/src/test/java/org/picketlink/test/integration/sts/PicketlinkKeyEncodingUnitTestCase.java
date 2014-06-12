/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.picketlink.test.integration.sts;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.jboss.logging.Logger;
import org.jboss.security.SimplePrincipal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.picketlink.common.constants.WSTrustConstants;
import org.picketlink.common.exceptions.ParsingException;
import org.picketlink.config.federation.STSType;
import org.picketlink.config.federation.parsers.STSConfigParser;
import org.picketlink.identity.federation.core.wstrust.PicketLinkSTSConfiguration;
import org.picketlink.identity.federation.core.wstrust.STSConfiguration;
import org.picketlink.identity.federation.core.wstrust.StandardRequestHandler;
import org.picketlink.identity.federation.core.wstrust.WSTrustRequestHandler;
import org.picketlink.identity.federation.core.wstrust.WSTrustServiceFactory;
import org.picketlink.identity.federation.core.wstrust.WSTrustUtil;
import org.picketlink.identity.federation.core.wstrust.wrappers.RequestSecurityToken;
import org.picketlink.identity.federation.core.wstrust.wrappers.RequestSecurityTokenResponse;
import org.picketlink.identity.federation.core.wstrust.wrappers.RequestSecurityTokenResponseCollection;
import org.picketlink.identity.federation.core.wstrust.writers.WSTrustResponseWriter;
import org.picketlink.identity.federation.web.constants.GeneralConstants;
import org.picketlink.identity.federation.ws.trust.BinarySecretType;
import org.picketlink.identity.federation.ws.trust.EntropyType;
import org.w3c.dom.Document;

/**
 * Regression tests for <a href="https://issues.jboss.org/browse/PLINK-263">PLINK-263</a> feature request (base64 encoding of
 * the secret key configurable for ws-trust).
 *
 * @author Josef Cacek
 */
@RunWith(JUnit4.class)
public class PicketlinkKeyEncodingUnitTestCase {

    private static final Logger LOGGER = Logger.getLogger(PicketlinkKeyEncodingUnitTestCase.class);
    public static final String BASE64_ENCODE_WSTRUST_SECRET_KEY = "picketlink.wstrust.base64_encode_wstrust_secret_key";

    private static final int KEY_SIZE = 16;

    final private XPathExpression entropyXpath;
    final private XPathExpression sharedKeyXpath;
    final byte[] clientSecret = WSTrustUtil.createRandomSecret(KEY_SIZE);

    final RequestSecurityToken request;
    final STSType stsConfig;

    /**
     * Constructor - creates XPath expressions, generates client part of secret key and creates instance of
     * RequestSecurityToken.
     *
     * @throws XPathExpressionException
     * @throws IOException
     * @throws ParsingException
     */
    public PicketlinkKeyEncodingUnitTestCase() throws XPathExpressionException, ParsingException, IOException {
        // configure xpath expressions for parsing keys from STS response
        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();
        HashMap<String, String> prefMap = new HashMap<String, String>();
        prefMap.put("wst", "http://docs.oasis-open.org/ws-sx/ws-trust/200512");
        prefMap.put("xenc", "http://www.w3.org/2001/04/xmlenc#");
        SimpleNamespaceContext namespaces = new SimpleNamespaceContext(prefMap);
        xpath.setNamespaceContext(namespaces);
        entropyXpath = xpath.compile("//wst:BinarySecret[1]");
        sharedKeyXpath = xpath.compile("//xenc:CipherValue[1]");

        // create a WS-Trust request for a SAML assertion.
        Map<String, String> replaceMap = new HashMap<String, String>();
        replaceMap.put("keyStoreUrl", String.valueOf(getClass().getResource("/keystore/sts_keystore.jks")));
        stsConfig = (STSType) new STSConfigParser().parse(IOUtils.toInputStream(StrSubstitutor.replace(
                IOUtils.toString(getClass().getResourceAsStream("/picketlink-test-sts.xml"), "UTF-8"), replaceMap)));

        request = createSAML20RequestWithCombinedSymmetricKey(clientSecret);

    }

    /**
     * Check if Base64 encoding is used during storing encrypted secret key to RequestSecurityTokenResponse when
     * {@value #BASE64_ENCODE_WSTRUST_SECRET_KEY} system property is "true".
     */
    @Test
    public void testBase64EnabledWithCombinedSymmetricKey() throws Exception {
        try {
            System.setProperty(GeneralConstants.BASE64_ENCODE_WSTRUST_SECRET_KEY, "true");
            checkBase64(true, clientSecret, stsResponse(request, stsConfig));
        } finally {
            System.clearProperty(GeneralConstants.BASE64_ENCODE_WSTRUST_SECRET_KEY);
        }
    }

    /**
     * Check if Base64 encoding is not used during storing encrypted secret key to RequestSecurityTokenResponse when
     * {@value #BASE64_ENCODE_WSTRUST_SECRET_KEY} system property is "false".
     */
    @Test
    public void testBase64DisabledWithCombinedSymmetricKey() throws Exception {
        try {
            System.setProperty(GeneralConstants.BASE64_ENCODE_WSTRUST_SECRET_KEY, "false");
            checkBase64(false, clientSecret, stsResponse(request, stsConfig));
        } finally {
            System.clearProperty(GeneralConstants.BASE64_ENCODE_WSTRUST_SECRET_KEY);
        }
    }

    /**
     * Check if Base64 encoding is not used during storing encrypted secret key to RequestSecurityTokenResponse when
     * {@value #BASE64_ENCODE_WSTRUST_SECRET_KEY} system property is not set.
     */
    @Test
    public void testBase64DefaultWithCombinedSymmetricKey() throws Exception {
        System.clearProperty(GeneralConstants.BASE64_ENCODE_WSTRUST_SECRET_KEY);
        checkBase64(false, clientSecret, stsResponse(request, stsConfig));
    }

    /**
     * Checks if provided RequestSecurityTokenResponseCollection {@link Document} contains expected value of the shared key.
     *
     * @param base64 flag which says if Base64 encoded value of encrypted shared key is expected in RequestSecurityTokenResponse
     * @param clientSecret client part of the shared secret
     * @param doc RequestSecurityTokenResponseCollection document
     * @throws Exception
     */
    private void checkBase64(boolean base64, byte[] clientSecret, Document doc) throws Exception {
        String base64Entropy = (String) entropyXpath.evaluate(doc, XPathConstants.STRING);
        String base64Key = (String) sharedKeyXpath.evaluate(doc, XPathConstants.STRING);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Response contains entropy=" + base64Entropy + " and secret=" + base64Key);
        byte[] entropy = Base64.decodeBase64(base64Entropy.getBytes("UTF-8"));
        byte[] encryptedKey = Base64.decodeBase64(base64Key.getBytes("UTF-8"));

        KeyStore ks = KeyStore.getInstance("JKS");
        final char[] keyPass = "keypass".toCharArray();
        ks.load(getClass().getResourceAsStream("/keystore/service1.jks"), keyPass);
        PrivateKey privateKey = (PrivateKey) ks.getKey("service1", keyPass);

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding"); // , "SunJCE"
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedKey = cipher.doFinal(encryptedKey);
        byte[] computedKey = createKey(clientSecret, entropy);
        byte[] base64ComputedKey = Base64.encodeBase64(computedKey, false);

        assertArrayEquals("Computed and decrypted keys are not equals.", decryptedKey, base64 ? base64ComputedKey : computedKey);
    }

    /**
     * Simulates work of PicketLink STS.
     *
     * @param request
     * @param stsConfig
     * @return
     * @throws Exception
     */
    private Document stsResponse(RequestSecurityToken request, STSType stsConfig) throws Exception {

        STSConfiguration configuration = new PicketLinkSTSConfiguration(stsConfig);
        WSTrustRequestHandler reqHandler = WSTrustServiceFactory.getInstance().createRequestHandler(
                StandardRequestHandler.class.getCanonicalName(), configuration);
        RequestSecurityTokenResponse resp = reqHandler.issue(request, new SimplePrincipal("Test"));

        RequestSecurityTokenResponseCollection responseCollection = new RequestSecurityTokenResponseCollection();
        responseCollection.addRequestSecurityTokenResponse(resp);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WSTrustResponseWriter writer = new WSTrustResponseWriter(baos);
        writer.write(responseCollection);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("STS response: " + baos.toString("UTF-8"));
        return db.parse(new ByteArrayInputStream(baos.toByteArray()));
    }

    private RequestSecurityToken createSAML20RequestWithCombinedSymmetricKey(byte[] clientSecret) {
        RequestSecurityToken request = new RequestSecurityToken();
        request.setRequestType(URI.create(WSTrustConstants.ISSUE_REQUEST));
        request.setAppliesTo(WSTrustUtil.createAppliesTo("http://services.testcorp.org/provider1"));

        // add a symmetric key type to the request.
        request.setKeyType(URI.create(WSTrustConstants.KEY_TYPE_SYMMETRIC));

        BinarySecretType clientBinarySecret = new BinarySecretType();
        clientBinarySecret.setType(WSTrustConstants.BS_TYPE_NONCE);
        clientBinarySecret.setValue(Base64.encodeBase64(clientSecret, false));

        // set the client secret in the client entropy.
        EntropyType clientEntropy = new EntropyType();
        clientEntropy.addAny(clientBinarySecret);
        request.setEntropy(clientEntropy);

        // set the context
        request.setContext("testContext");

        return request;
    }

    /**
     * Creates shared key from clients secret and entropy retrieved from the server. It computes P_SHA1 from the given values.
     *
     * @param clientSecret
     * @param serverEntropy
     * @return
     * @throws Exception
     */
    public byte[] createKey(byte[] clientSecret, byte[] serverEntropy) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        byte[] tempBytes = phash(clientSecret, serverEntropy, mac, KEY_SIZE);
        byte[] key = new byte[KEY_SIZE];
        for (int i = 0; i < key.length; i++) {
            key[i] = tempBytes[i];
        }
        return key;
    }

    /**
     * Computes P_SHA1 hash from given values.
     *
     * @param secret
     * @param seed
     * @param mac
     * @param required
     * @return
     * @throws Exception
     */
    private static byte[] phash(byte[] secret, byte[] seed, Mac mac, int required) throws Exception {
        byte[] out = new byte[required];
        int offset = 0, tocpy;
        byte[] A, tmp;
        A = seed;
        final SecretKeySpec key = new SecretKeySpec(secret, "HMACSHA1");
        while (required > 0) {
            mac.init(key);
            mac.update(A);
            A = mac.doFinal();
            mac.reset();
            mac.init(key);
            mac.update(A);
            mac.update(seed);
            tmp = mac.doFinal();
            tocpy = min(required, tmp.length);
            System.arraycopy(tmp, 0, out, offset, tocpy);
            offset += tocpy;
            required -= tocpy;
        }
        return out;
    }

    private static int min(int a, int b) {
        return (a > b) ? b : a;
    }

    public static class SimpleNamespaceContext implements NamespaceContext {

        private final Map<String, String> PREF_MAP = new HashMap<String, String>();

        public SimpleNamespaceContext(final Map<String, String> prefMap) {
            PREF_MAP.putAll(prefMap);
        }

        public String getNamespaceURI(String prefix) {
            return PREF_MAP.get(prefix);
        }

        public String getPrefix(String uri) {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("rawtypes")
        public Iterator getPrefixes(String uri) {
            throw new UnsupportedOperationException();
        }

    }

}