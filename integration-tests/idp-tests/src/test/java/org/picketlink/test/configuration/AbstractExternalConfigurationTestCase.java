/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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
package org.picketlink.test.configuration;

import static org.picketlink.test.integration.util.PicketLinkConfigurationUtil.getPicketLinkConfiguration;
import static org.picketlink.test.integration.util.TestUtil.getTargetURL;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.tools.ant.filters.StringInputStream;
import org.codehaus.plexus.util.IOUtil;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.picketlink.common.exceptions.ConfigurationException;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.test.integration.util.PicketLinkIntegrationTests;

/**
 * Abstract class for tests of user-defined configuration file picketlink.xml
 * Provides useful constants and methods.
 * 
 * @author Filip Bogyai
 */

@RunWith(PicketLinkIntegrationTests.class)
public abstract class AbstractExternalConfigurationTestCase {

    String IDP_URL = getTargetURL("/idp/");
    static String IDP_SIG_URL = getTargetURL("/idp-sig/");

    String SALES_POST_URL = getTargetURL("/sales-post/");
    String SALES_POST_SIG_URL = getTargetURL("/sales-post-sig/");
    String SALES_POST_VALVE_URL = getTargetURL("/sales-post-valve/");

    String EMPLOYEE_REDIRECT_URL = getTargetURL("/employee/");
    String EMPLOYEE_REDIRECT_SIG_URL = getTargetURL("/employee-sig/");
    String EMPLOYEE_REDIRECT_VALVE_URL = getTargetURL("/employee-redirect-valve/");

    String LOGOUT_URL = "?GLO=true";

    protected String getEmployeeURL() {
        return getTargetURL("/employee-sig/");
    }

    protected String getSalesURL() {
        return getTargetURL("/sales-post-sig/");
    }

    protected static void createPicketLinkConfigFile(WebArchive webArchive, File configFile) throws ProcessingException,
            ConfigurationException, IOException {

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(configFile);
            IOUtil.copy(new StringInputStream(getPicketLinkConfiguration(webArchive)), fos);
        } finally {
            IOUtil.close(fos);
        }

    }

}