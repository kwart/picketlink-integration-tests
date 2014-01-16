package org.jboss.aerogear.jaxrs.rest.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LDAPTestUtil extends org.picketbox.test.ldap.LDAPTestUtil {

    public LDAPTestUtil(int port) {
        this.port = Integer.toString(port);
    }

    @Override
    public void importLDIF(String fileName) throws Exception {
        long current = System.currentTimeMillis();
        System.out.println("Going to import LDIF:" + fileName);

        InputStream is = null;
        try {
            is = new FileInputStream(new File(fileName));
            ds.importLdif(is);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load LDAP database", e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load LDAP database", e);
            }
        }
        System.out.println("Time taken = " + (System.currentTimeMillis() - current) + "milisec");
    }
}
