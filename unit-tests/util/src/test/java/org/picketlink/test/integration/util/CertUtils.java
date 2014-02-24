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

package org.picketlink.test.integration.util;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * Miscellaneous utility methods for key store/trust store and certificate manipulation.
 *
 * @author Hynek Mlnarik <hmlnarik@redhat.com>
 */
public class CertUtils {

    private static final int DEFAULT_KEYSIZE = 1024;
    private static final long DEFAULT_CERT_VALIDITY = 365 * 24 * 60 * 60;

    private static final String KEY_ALGORITHM = "RSA";
    private static final String SIG_ALGORITHM = "SHA1WithRSA";

    private static final Class<?>[] X500NAME_CONSTRUCTOR_PARAMETERS = new Class<?>[] {
      String.class, String.class, String.class,
      String.class, String.class, String.class
    };
    private static final Class<?>[] CERTANDKEYGEN_CONSTRUCTOR_PARAMETERS = new Class<?>[] {
      String.class, String.class
    };
    private static final Class<?>[] CERTANDKEYGEN_GENERATE_PARAMETERS = new Class<?>[] { int.class };

    public static KeyStore createKeyStore() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        return keyStore;
    }

    /**
     * Generates a self-signed certificate with given field values and
     * adds it into the key store (both public and private keys).
     * <p>
     * The generated key store can be stored into file system using:
     * <p>
     * <code>keyStore.store(new FileOutputStream(keystoreFileName), keystorePassword);</code>
     *
     * @param keyStore Key store
     * @param commonName Certificate "common name" field.
     * @param organizationalUnit Certificate "organizational unit" field.
     * @param organization Certificate "organization" field.
     * @param city Certificate "city" field.
     * @param state Certificate "state" field.
     * @param country Certificate "country" field.
     * @param alias Certificate alias in the key store
     * @param keyPass Private key password
     * @return Key store object
     * @throws IOException
     * @throws GeneralSecurityException
     * @throws ClassNotFoundException 
     * @throws NoSuchMethodException      
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws InstantiationException        
     */
    public static KeyStore generateSelfSignedCertificate(
      KeyStore keyStore,
      String commonName,
      String organizationalUnit,
      String organization,
      String city,
      String state,
      String country,
      String alias,
      char[] keyPass)
 throws IOException,
            GeneralSecurityException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        Class certAndKeyGenClass, x500NameClass;
        Constructor certAndKeyGen_constructor, x500Name_constructor;
        Method certAndKeyGen_generate, certAndKeyGen_getPrivateKey, certAndKeyGen_getSelfCertificate;

        if (isIbmJava()) {
            certAndKeyGenClass = Class.forName("com.ibm.security.x509.CertAndKeyGen");
            x500NameClass = Class.forName("com.ibm.security.x509.X500Name");
        } else {
            certAndKeyGenClass = Class.forName("sun.security.x509.CertAndKeyGen");
            x500NameClass = Class.forName("sun.security.x509.X500Name");
        }
        certAndKeyGen_constructor = certAndKeyGenClass.getConstructor(CERTANDKEYGEN_CONSTRUCTOR_PARAMETERS);
        certAndKeyGen_generate = certAndKeyGenClass.getMethod("generate", CERTANDKEYGEN_GENERATE_PARAMETERS);
        certAndKeyGen_getSelfCertificate = certAndKeyGenClass.getMethod("getSelfCertificate", new Class<?>[] { x500NameClass, long.class });
        certAndKeyGen_getPrivateKey = certAndKeyGenClass.getMethod("getPrivateKey");

        
        /* CertAndKeyGen */ Object keypair = certAndKeyGen_constructor.newInstance(KEY_ALGORITHM, SIG_ALGORITHM);

        certAndKeyGen_generate.invoke(keypair, DEFAULT_KEYSIZE);
        PrivateKey privKey = (PrivateKey) certAndKeyGen_getPrivateKey.invoke(keypair);

        Certificate[] chain = new X509Certificate[1];
        
        x500Name_constructor = x500NameClass.getConstructor(X500NAME_CONSTRUCTOR_PARAMETERS);
        /* X500Name */ Object x500Name = x500Name_constructor.newInstance(commonName, organizationalUnit, organization, city, state, country);

        chain[0] = (Certificate) certAndKeyGen_getSelfCertificate.invoke(keypair, x500Name, DEFAULT_CERT_VALIDITY);

        keyStore.setKeyEntry(alias, privKey, keyPass, chain);

        return keyStore;
    }

    /**
     * Generates a trust store and imports certificates from the given key store.
     *
     * @param keyStoreToImport Key store to import the certificates from
     * @param aliasesToImport Aliases of certificates whose public keys will be
     *    imported to the trust store
     * @return Trust store object.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public static KeyStore generateTrustStoreWithImportedCertificate(KeyStore keyStoreToImport, String... aliasesToImport) throws IOException, GeneralSecurityException {
        KeyStore keyStore = createKeyStore();

        for (String aliasToImport : aliasesToImport) {
            final Certificate importedCertificate = keyStoreToImport.getCertificate(aliasToImport);
            keyStore.setCertificateEntry(aliasToImport, importedCertificate);
        }

        return keyStore;
    }

    private static boolean isIbmJava() {
        final String javaVendor = System.getProperty("java.vendor");
        return javaVendor != null && javaVendor.startsWith("IBM");
    }

}
