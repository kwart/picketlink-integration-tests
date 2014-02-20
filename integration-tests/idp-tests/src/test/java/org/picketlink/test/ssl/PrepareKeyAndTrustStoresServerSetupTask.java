/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.picketlink.test.ssl;

import org.picketlink.test.integration.util.CertUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import static org.picketlink.test.integration.util.TestUtil.getServerAddress;

/**
 * Prepares client and server key/trust stores. 
 * The following will be prepared:
 * <ul>
 * <li>Client key store containing two keys with aliases {@link #TRUSTED_CLIENT_KEY_ALIAS}
 *     and {@link #UNTRUSTED_CLIENT_KEY_ALIAS}</li>
 * <li>Client trust store containing server certificate with alias {@link #SERVER_KEY_ALIAS}</li>
 * <li>Server key store containing one key with alias {@link #SERVER_KEY_ALIAS}</li>
 * <li>Server trust store containing one certificat with alias {@link #TRUSTED_CLIENT_KEY_ALIAS}</li>
 * </ul>
 * The keys are secured using {@link #GENERIC_PASSWORD} password.
 * 
 * @author hmlnarik
 */
public class PrepareKeyAndTrustStoresServerSetupTask implements ServerSetupTask {

    private static final Logger LOG = Logger.getLogger(PrepareKeyAndTrustStoresServerSetupTask.class.getName());

    /**
     * Common name part of the trusted client certificate.
     */
    public static final String COMMON_NAME_TRUSTED_CLIENT = "Java Duke <jduke@duke.java.org>";
    private static final String COMMON_NAME_UNTRUSTED_CLIENT = "Hiding Duke <hduke@duke.java.org>";
    private static final String CERT_ORGANIZATION = "organization";
    private static final String CERT_ORGANIZATIONAL_UNIT = "orgUnit";
    private static final String CERT_CITY = "city";
    private static final String CERT_STATE = "st";
    private static final String CERT_COUNTRY = "CT";
    public static final String GENERIC_PASSWORD = "changeit";
    public static final char[] GENERIC_PASSWORD_CHARS = GENERIC_PASSWORD.toCharArray();

    /**
     * Name presented to the server when the certificate of trusted client is used.
     */
    public static final String TRUSTED_CERT_NAME =
      "CN=\"" + COMMON_NAME_TRUSTED_CLIENT + "\""
      + ", OU=" + CERT_ORGANIZATIONAL_UNIT
      + ", O=" + CERT_ORGANIZATION
      + ", L=" + CERT_CITY
      + ", ST=" + CERT_STATE
      + ", C=" + CERT_COUNTRY;

    public static final String UNTRUSTED_CERT_NAME =
      "CN=\"" + COMMON_NAME_UNTRUSTED_CLIENT + "\""
      + ", OU=" + CERT_ORGANIZATIONAL_UNIT
      + ", O=" + CERT_ORGANIZATION
      + ", L=" + CERT_CITY
      + ", ST=" + CERT_STATE
      + ", C=" + CERT_COUNTRY;

    /**
     * Alias of the server certificate.
     */
    public static final String SERVER_KEY_ALIAS = "server";

    /**
     * Alias of the client certificate that is stored in the server truststore.
     */
    public static final String TRUSTED_CLIENT_KEY_ALIAS = "client";

    /**
     * Alias of the client certificate that is <b>NOT</b> stored in the server truststore.
     */
    public static final String UNTRUSTED_CLIENT_KEY_ALIAS = "untrustedClient";

    private KeyStore serverKeyStore;
    private KeyStore clientKeyStore;
    private KeyStore clientTrustStore;
    private KeyStore serverTrustStore;

    private File workDir;

    public KeyStore getServerKeyStore() {
        return serverKeyStore;
    }

    public KeyStore getClientKeyStore() {
        return clientKeyStore;
    }

    public KeyStore getClientTrustStore() {
        return clientTrustStore;
    }

    public KeyStore getServerTrustStore() {
        return serverTrustStore;
    }

    public File getClientKeystoreFile() {
        return new File(workDir, "client.keystore");
    }

    public File getClientTruststoreFile() {
        return new File(workDir, "client.truststore");
    }

    public File getServerKeystoreFile() {
        return new File(workDir, "server.keystore");
    }

    public File getServerTruststoreFile() {
        return new File(workDir, "server.truststore");
    }

    private static void staticTearDown(File workDir) throws IOException {
        FileUtils.deleteDirectory(workDir);
    }


    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        workDir = File.createTempFile("sslConnector", "dir", new File("."));
        workDir.delete();
        workDir.mkdirs();

        LOG.log(Level.FINE, "Work dir: {0}", workDir.getAbsolutePath());

        serverKeyStore = CertUtils.createKeyStore();
        CertUtils.generateSelfSignedCertificate(
          serverKeyStore,
          getServerAddress(),
          CERT_ORGANIZATIONAL_UNIT, CERT_ORGANIZATION, CERT_CITY, CERT_STATE, CERT_COUNTRY,
          SERVER_KEY_ALIAS,
          GENERIC_PASSWORD_CHARS
        );
        serverKeyStore.store(new FileOutputStream(getServerKeystoreFile()), GENERIC_PASSWORD_CHARS);

        clientKeyStore = CertUtils.createKeyStore();
        CertUtils.generateSelfSignedCertificate(
          clientKeyStore,
          COMMON_NAME_TRUSTED_CLIENT,
          CERT_ORGANIZATIONAL_UNIT, CERT_ORGANIZATION, CERT_CITY, CERT_STATE, CERT_COUNTRY,
          TRUSTED_CLIENT_KEY_ALIAS,
          GENERIC_PASSWORD_CHARS
        );
        CertUtils.generateSelfSignedCertificate(
          clientKeyStore,
          COMMON_NAME_TRUSTED_CLIENT,
          CERT_ORGANIZATIONAL_UNIT, CERT_ORGANIZATION, CERT_CITY, CERT_STATE, CERT_COUNTRY,
          UNTRUSTED_CLIENT_KEY_ALIAS,
          GENERIC_PASSWORD_CHARS
        );
        
        clientKeyStore.store(new FileOutputStream(getClientKeystoreFile()), GENERIC_PASSWORD_CHARS);

        serverTrustStore = CertUtils.generateTrustStoreWithImportedCertificate(clientKeyStore, TRUSTED_CLIENT_KEY_ALIAS);
        serverTrustStore.store(new FileOutputStream(getServerTruststoreFile()), GENERIC_PASSWORD_CHARS);

        clientTrustStore = CertUtils.generateTrustStoreWithImportedCertificate(serverKeyStore, SERVER_KEY_ALIAS);
        clientTrustStore.store(new FileOutputStream(getClientTruststoreFile()), GENERIC_PASSWORD_CHARS);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        staticTearDown(workDir);
    }

}
