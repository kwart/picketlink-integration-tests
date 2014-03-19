/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.File;
import java.security.KeyStore;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.picketlink.test.integration.util.ModelUtil;

/**
 * {@code ServerSetupTask} that adds HTTPS connector to server configuration.
 * Used in the certificate login tests.
 */
public class AddHttpsConnectorServerSetupTask implements ServerSetupTask {

    /**
     * Port number where the SSL connector will be listening.
     */
    public static final int TEST_HTTPS_PORT = 18443;

    public static final String SOCKET_BINDING_NAME_HTTPSTEST = "https-test";

    private final PrepareKeyAndTrustStoresServerSetupTask keyStoreTask = new PrepareKeyAndTrustStoresServerSetupTask();

    private static final PathAddress SOCKET_BINDING_HTTPSTEST = PathAddress.pathAddress(
      PathElement.pathElement(SOCKET_BINDING_GROUP, "standard-sockets"),
      PathElement.pathElement(SOCKET_BINDING, SOCKET_BINDING_NAME_HTTPSTEST)
    );
    private static final PathAddress WEB_CONNECTOR = PathAddress.pathAddress(
      PathElement.pathElement(SUBSYSTEM, "web"),
      PathElement.pathElement("connector", "testConnector")
    );
    private static final PathAddress WEB_CONNECTOR_SSL = WEB_CONNECTOR.append(PathElement.pathElement("ssl", "configuration"));

    private static final Logger LOG = Logger.getLogger(AddHttpsConnectorServerSetupTask.class.getName());

    public KeyStore getClientKeyStore() {
        return keyStoreTask.getClientKeyStore();
    }

    public KeyStore getClientTrustStore() {
        return keyStoreTask.getClientTrustStore();
    }

    public File getServerKeystoreFile() {
        return keyStoreTask.getServerKeystoreFile();
    }

    public File getServerTruststoreFile() {
        return keyStoreTask.getServerTruststoreFile();
    }

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        keyStoreTask.setup(managementClient, containerId);

        LOG.fine("start of the https connector creation");

        // Add the HTTPS socket binding.
        ModelNode binding = Util.createAddOperation(SOCKET_BINDING_HTTPSTEST);
        binding.get("interface").set("public");
        binding.get("port").set(TEST_HTTPS_PORT);

        // Add the HTTPS connector.
        ModelNode httpsConnector = Util.createAddOperation(WEB_CONNECTOR);
        httpsConnector.get(PROTOCOL).set("HTTP/1.1");
        httpsConnector.get("scheme").set("https");
        httpsConnector.get(SOCKET_BINDING).set(SOCKET_BINDING_NAME_HTTPSTEST);
        httpsConnector.get(ENABLED).set(true);
        httpsConnector.get("enable-lookups").set(false);
        httpsConnector.get("secure").set(true);

        ModelNode ssl = Util.createAddOperation(WEB_CONNECTOR_SSL);
        ssl.get("key-alias").set(PrepareKeyAndTrustStoresServerSetupTask.SERVER_KEY_ALIAS);
        ssl.get("password").set(PrepareKeyAndTrustStoresServerSetupTask.GENERIC_PASSWORD);
        ssl.get("certificate-key-file").set(keyStoreTask.getServerKeystoreFile().getAbsolutePath());
        ssl.get("protocol").set("TLSv1");
        ssl.get("ca-certificate-file").set(keyStoreTask.getServerTruststoreFile().getAbsolutePath());
        ssl.get("verify-client").set("true");

        ModelNode ops = ModelUtil.createCompositeNode(
          allowServiceRestart(binding),
          allowServiceRestart(httpsConnector),
          allowServiceRestart(ssl)
        );

        boolean success = ModelUtil.execute(managementClient, allowServiceRestart(ops));
        LOG.log(success ? Level.FINE : Level.WARNING, "Installing SSL connector into AS/EAP container {0}", new Object[] { success ? "passed" : "FAILED" });
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        Exception e = null;

        try {
            boolean success;
            
            ModelNode ops = ModelUtil.createCompositeNode(
              allowServiceRestart(Util.createRemoveOperation(WEB_CONNECTOR_SSL)),
              allowServiceRestart(Util.createRemoveOperation(WEB_CONNECTOR))
            );

            success = ModelUtil.execute(managementClient, allowServiceRestart(ops));
            LOG.log(success ? Level.FINE : Level.WARNING, "Uninstalling SSL connector from AS/EAP container {0}", new Object[] { success ? "passed" : "FAILED" });
        } catch (Exception ex) {
            e = ex;
        }

        try {
            boolean success;

            ModelNode ops = ModelUtil.createCompositeNode(
              allowServiceRestart(Util.createRemoveOperation(SOCKET_BINDING_HTTPSTEST))
            );

            success = ModelUtil.execute(managementClient, allowServiceRestart(ops));
            LOG.log(success ? Level.FINE : Level.WARNING, "Uninstalling socket binding from AS/EAP container {0}", new Object[] { success ? "passed" : "FAILED" });
        } catch (Exception ex) {
            e = ex;
        }

        keyStoreTask.tearDown(managementClient, containerId);

        if (e != null) {
            throw new Exception("tearDown() failed: ", e);
        }
    }

    private static ModelNode allowServiceRestart(ModelNode op) {
        op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);

        return op;
    }
}
