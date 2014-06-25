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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketlink.identity.federation.core.wstrust.STSClient;
import org.picketlink.identity.federation.core.wstrust.STSClientConfig;
import org.picketlink.identity.federation.core.wstrust.STSClientConfig.Builder;
import org.picketlink.identity.federation.core.wstrust.STSClientFactory;
import org.picketlink.test.integration.util.PicketLinkIntegrationTests;
import org.picketlink.test.integration.util.TargetContainers;

/**
 * Tests for STSClient pool feature.
 * 
 * @author Josef Cacek
 */
@RunWith(PicketLinkIntegrationTests.class)
@TargetContainers({ "jbas5", "jbas7", "eap5" })
//@Ignore("bz-1069126")
public class STSClientPoolTestCase extends AbstractWSTrustIntegrationTests {

    private static int CLIENTS_NR = 10000;

    /**
     * Create STSClientFactory with negative pool size<br/>
     * Expected result: an exception is thrown
     */
    @Test
    public void testNegativePoolSize() {
        STSClientFactory.getInstance().resetFactory();
        try {
            STSClientFactory.getInstance(-1);
        } catch (Exception e) {
            // OK
            return;
        }
        fail("Negative pool size should throw exception");
    }

    /**
     * create many STS clients with pooling disabled Expected result: every call of STSClientFactory.create() method returns a
     * new STSClient instance
     */
    @Test
    public void testPoolDisabled() {
        STSClientFactory.getInstance().resetFactory();
        final STSClientConfig stsClientConfig = createSTSClientConfig();
        final STSClientFactory fact = STSClientFactory.getInstance();
        Set<STSClient> stsClientSet = new HashSet<STSClient>();
        for (int i = 0; i < CLIENTS_NR; i++) {
            stsClientSet.add(fact.getClient(stsClientConfig));
        }
        assertEquals("Wrong number of STS clients created", CLIENTS_NR, stsClientSet.size());
    }

    /**
     * request more STS clients than is the pool limit (when pooling is enabled)<br/>
     * Expected result: exception thrown, when calling STSClientFactory.create() and the pool is empty
     */
    @Test
    public void testExceedingPoolLimit() {
        STSClientFactory.getInstance().resetFactory();
        final int poolSize = 10;

        final STSClientConfig stsClientConfig = createSTSClientConfig();
        final STSClientFactory fact = STSClientFactory.getInstance(poolSize);
        Set<STSClient> stsClientSet = new HashSet<STSClient>();
        fact.createPool(stsClientConfig);
        for (int i = 0; i < poolSize; i++) {
            stsClientSet.add(fact.getClient(stsClientConfig));
        }
        assertEquals("Wrong number of STS clients created", poolSize, stsClientSet.size());
        try {
            stsClientSet.add(fact.getClient(stsClientConfig));
        } catch (Exception e) {
            // OK we exceeded pool size
            return;
        }
        fail("Excption was expected. We exceeded STSClient pool size.");
    }

    /**
     * 
     * Expected result:
     */
    @Test
    public void testInitialPoolBiggerThanMaxSize() {
        STSClientFactory.getInstance().resetFactory();
        final int maxPoolSize = 10;
        final int initClientSize = 100;

        final STSClientConfig stsClientConfig = createSTSClientConfig("testInitialPoolBiggerThanMaxSize");
        final STSClientFactory fact = STSClientFactory.getInstance(maxPoolSize);

        Set<STSClient> stsClientSet = new HashSet<STSClient>();
        fact.createPool(initClientSize, stsClientConfig);
        for (int i = 0; i < maxPoolSize; i++) {
            stsClientSet.add(fact.getClient(stsClientConfig));
        }
        assertEquals("Wrong number of STS clients created", maxPoolSize, stsClientSet.size());
        try {
            stsClientSet.add(fact.getClient(stsClientConfig));
        } catch (Exception e) {
            // OK we exceeded pool size
            return;
        }
        fail("Excption was expected. We exceeded STSClient pool size.");
    }

    /**
     * reuse STSClient - repeat requesting STSClient from pool and returning them; check if the clients are reused<br/>
     * Expected result: STSClients are reused
     */
    @Test
    public void testClientsReuse() {
        STSClientFactory.getInstance().resetFactory();
        final int poolSize = 10;

        final STSClientConfig stsClientConfig = createSTSClientConfig();
        final STSClientFactory fact = STSClientFactory.getInstance(poolSize);

        Set<STSClient> overAllSTSClientSet = new HashSet<STSClient>();

        for (int i = 0; i < 10; i++) {
            Set<STSClient> stsClientSet = new HashSet<STSClient>();
            for (int j = 0; j < poolSize; j++) {
                stsClientSet.add(fact.getClient(stsClientConfig));
            }
            overAllSTSClientSet.addAll(stsClientSet);
            for (STSClient stsClient : stsClientSet) {
                fact.returnClient(stsClient);
            }
        }
    }

    /**
     * Create several pools (with different sizes) at once<br/>
     * Expected result: it's possible to have more pools
     */
    @Test
    public void testMorePools() {
        STSClientFactory.getInstance().resetFactory();
        assertNotSame(STSClientFactory.getInstance(10), STSClientFactory.getInstance(20));
        // TODO more tests here
    }

    /**
     * Get STSClient from pool and then try to return it to the pool 2 times<br/>
     * Expected result: Second call of <code>factory.returnClient(stsc);</code> should throw an exception
     */
    @Test
    public void testReturnClientMoreTimes() {
        STSClientFactory.getInstance().resetFactory();
        final STSClientConfig stsClientConfig = createSTSClientConfig();
        final STSClientFactory factory = STSClientFactory.getInstance(2);
        factory.createPool(stsClientConfig);
        final STSClient stsc = factory.getClient(stsClientConfig);

        factory.returnClient(stsc);
        try {
            factory.returnClient(stsc);
        } catch (Exception e) {
            // OK expected
            return;
        }
        fail("Returning STSClient to the pool should fail, because it was already returned");
    }

    /**
     * Create an STSClient with pooling disabled and then try to return it to a pool<br/>
     * Expected result: The call of <code>factory.returnClient(stsc);</code> should throw an exception
     */
    @Test
    public void testReturnNotPooledClient() {
        STSClientFactory.getInstance().resetFactory();
        final STSClientConfig stsClientConfig = createSTSClientConfig();
        // get not pooled instance
        final STSClientFactory fact = STSClientFactory.getInstance(0);
        fact.createPool(stsClientConfig);
        final STSClient stsc = fact.getClient(stsClientConfig);
        // try to return the STSClient instance to a pool
        try {
            STSClientFactory.getInstance(10).returnClient(stsc);
        } catch (Exception e) {
            // OK expected
            return;
        }
        fail("Returning STSClient to the pool should fail, because it was not retrieved from the pool");
    }

    // TODO: Check if there could be synchronization issues in the STSClientPool

    private STSClientConfig createSTSClientConfig() {
        return createSTSClientConfig("PicketLinkSTSPort");
    }

    private STSClientConfig createSTSClientConfig(String portName) {
        return new Builder() //
                .endpointAddress("http://localhost:8080/picketlink-sts/PicketLinkSTS") //
                .portName(portName) //
                .serviceName("PicketLinkSTS") //
                .username("admin") //
                .password("admin") //
                .build();
    }

}
