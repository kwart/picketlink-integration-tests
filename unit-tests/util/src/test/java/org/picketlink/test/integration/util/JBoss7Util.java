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
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.jboss.as.network.NetworkUtils;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;

/**
 * Class containing utility methods for JBoss 7/EAP 6.
 * @author hmlnarik
 */
public class JBoss7Util {	

    private static final Logger LOGGER = Logger.getLogger(JBoss7Util.class);

    /**
     * Generates content of the jboss-deployment-structure.xml deployment descriptor as an ShrinkWrap asset. It fills the given
     * dependencies (module names) into it.
     *
     * @param dependencies AS module names
     * @return
     */
    public static Asset getJBossDeploymentStructure(String... dependencies) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<jboss-deployment-structure><deployment><dependencies>");
        if (dependencies != null) {
            for (String moduleName : dependencies) {
                sb.append("\n\t<module name='").append(moduleName).append("'/>");
            }
        }
        sb.append("\n</dependencies></deployment></jboss-deployment-structure>");
        return new StringAsset(sb.toString());
    }

    /**
     * Generates content of jboss-web.xml file as an ShrinkWrap asset with the given security domain name and given valve class.
     *
     * @param securityDomain security domain name (not-<code>null</code>)
     * @param valveClassNames valve class (e.g. an Authenticator) which should be added to jboss-web file (may be
     *        <code>null</code>)
     * @return Asset instance
     */
    public static Asset getJBossWebXmlAsset(final String securityDomain, final String... valveClassNames) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<jboss-web>");
        sb.append("\n\t<security-domain>").append(securityDomain).append("</security-domain>");
        if (valveClassNames != null) {
            for (String valveClassName : valveClassNames) {
                if (valveClassName != null && ! valveClassName.isEmpty()) {
                    sb.append("\n\t<valve><class-name>").append(valveClassName).append("</class-name></valve>");
                }
            }
        }
        sb.append("\n</jboss-web>");
        return new StringAsset(sb.toString());
    }

	/**
	 * Replace variables in PicketLink configurations files with given values
	 * and set ${hostname} variable from system property: node0
	 *
	 * @param stream Stream to perform replacement on. The stream is expected to be a text file in UTF-8 encoding
	 * @param deploymentName Value of property <code>deployment</code> in replacement
	 * @param bindingType Value of property <code>bindingType</code> in replacement
	 * @param idpContextPath Value of property <code>idpContextPath</code> in replacement
	 * @return Contents of the input stream with replaced values
	 */
	public static String propertiesReplacer(InputStream stream, String deploymentName, String bindingType, String idpContextPath) {

		String hostname = getHostname();

		final Map<String, String> map = new HashMap<String, String>();
		String content = "";
		map.put("hostname", hostname);
		map.put("deployment", deploymentName);
		map.put("bindingType", bindingType);
		map.put("idpContextPath", idpContextPath);

		try {
			content = StrSubstitutor.replace(IOUtils.toString(stream, "UTF-8"), map);
		} catch (IOException ex) {
			String message = "Cannot find or modify input stream, error: " + ex.getMessage();
			LOGGER.error(message);
			throw new RuntimeException(ex);
		}
		return content;
	}
	
	/**
	 * Set ${hostname} variable from system property: node0
	 * 
	 * @return Value of hostname
	 */
	public static String getHostname() {
		String hostname = System.getProperty("node0");

		//expand possible IPv6 address
		try {
			hostname = NetworkUtils.formatPossibleIpv6Address(InetAddress.getByName(hostname).getHostAddress());
		} catch(UnknownHostException ex) {
			String message = "Cannot resolve host address: "+ hostname + " , error : " + ex.getMessage();
			LOGGER.error(message);
			throw new RuntimeException(ex);
		}
		
		return hostname;
	}

}
