package org.picketlink.test.accountchooser;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;

import org.picketlink.identity.federation.bindings.tomcat.sp.AbstractAccountChooserValve;

public class NewPropertiesAccountMapProvider implements AbstractAccountChooserValve.AccountIDPMapProvider {
    private ClassLoader classLoader = null;

    private ServletContext servletContext = null;

    public static final String WEB_INF_PROP_FILE_NAME = "/WEB-INF/newidpmap.properties";

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public Map<String, String> getIDPMap() throws IOException {

        Map<String, String> idpmap = new HashMap<String, String>();

        InputStream inputStream = null;

        Properties properties = new Properties();

        if (inputStream == null && servletContext != null) {
            inputStream = servletContext.getResourceAsStream(WEB_INF_PROP_FILE_NAME);
        }

        if (inputStream != null) {
            properties.load(inputStream);
            if (properties != null) {
                Set<Object> keyset = properties.keySet();
                for (Object key : keyset) {
                    idpmap.put((String) key, (String) properties.get(key));
                }
            }
        }
        return idpmap;
    }
}
