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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketlink.identity.federation.core.wstrust.STSClient;
import org.picketlink.identity.federation.core.wstrust.STSClientConfig;
import org.picketlink.identity.federation.core.wstrust.STSClientConfig.Builder;
import org.picketlink.identity.federation.core.wstrust.STSClientFactory;
import org.picketlink.identity.federation.core.wstrust.STSClientPool;
import org.picketlink.test.integration.util.PicketLinkIntegrationTests;
import org.picketlink.test.integration.util.TargetContainers;

/**
 * Tests for STSClient pool feature.
 * 
 * @author Josef Cacek
 */
@RunWith(PicketLinkIntegrationTests.class)
@TargetContainers({ "jbas5", "jbas7", "eap5" })
@Ignore
public class STSClientPoolTestCase extends AbstractWSTrustIntegrationTests {

    private static int CLIENTS_NR = 10000;

    /**
     * Create STSClientFactory with negative pool size<br/>
     * Expected result: either an exception is thrown or an STSClientFactory instance with disabled pooling is created (not yet
     * documented)
     */
    @Test
    public void testNegativePoolSize() {
        final STSClientConfig stsClientConfig = createSTSClientConfig();
        final STSClientFactory fact;
        // not clear if a negative pool size value should result in disabled pooling or in an exception thrown
        try {
            fact = STSClientFactory.getInstance(-1);
        } catch (Exception e) {
            // OK
            return;
        }
        // if we are here, then pooling should be disabled
        Set<STSClient> stsClientSet = new HashSet<STSClient>();
        for (int i = 0; i < CLIENTS_NR; i++) {
            stsClientSet.add(fact.create(stsClientConfig));
        }
        assertEquals("Wrong number of STS clients created", CLIENTS_NR, stsClientSet.size());
    }

    /**
     * create many STS clients with pooling disabled Expected result: every call of STSClientFactory.create() method returns a
     * new STSClient instance
     */
    @Test
    public void testPoolDisabled() {
        final STSClientConfig stsClientConfig = createSTSClientConfig();
        final STSClientFactory fact = STSClientFactory.getInstance();
        Set<STSClient> stsClientSet = new HashSet<STSClient>();
        for (int i = 0; i < CLIENTS_NR; i++) {
            stsClientSet.add(fact.create(stsClientConfig));
        }
        assertEquals("Wrong number of STS clients created", CLIENTS_NR, stsClientSet.size());
    }

    /**
     * request more STS clients than is the pool limit (when pooling is enabled)<br/>
     * Expected result: exception thrown, when calling STSClientFactory.create() and the pool is empty
     */
    @Test
    public void testExceedingPoolLimit() {
        final int poolSize = 10;

        final STSClientConfig stsClientConfig = createSTSClientConfig();
        final STSClientFactory fact = STSClientFactory.getInstance(poolSize);
        Set<STSClient> stsClientSet = new HashSet<STSClient>();
        for (int i = 0; i < poolSize; i++) {
            stsClientSet.add(fact.create(stsClientConfig));
        }
        assertEquals("Wrong number of STS clients created", poolSize, stsClientSet.size());
        try {
            stsClientSet.add(fact.create(stsClientConfig));
            fail("Excption was expected. We exceeded STSClient pool size.");
        } catch (Exception e) {
            // OK we exceeded pool size
        }
    }

    /**
     * reuse STSClient - repeat requesting STSClient from pool and returning them; check if the clients are reused<br/>
     * Expected result: STSClients are reused
     */
    @Test
    public void testClientsReuse() {
        final int poolSize = 10;

        final STSClientConfig stsClientConfig = createSTSClientConfig();
        final STSClientFactory fact = STSClientFactory.getInstance(poolSize);
        final STSClientPool pool = STSClientPool.instance(poolSize);
        assertFalse("Pooling should be enabled", pool.isPoolingDisabled());

        Set<STSClient> overAllSTSClientSet = new HashSet<STSClient>();

        for (int i = 0; i < 10; i++) {
            Set<STSClient> stsClientSet = new HashSet<STSClient>();
            for (int j = 0; j < poolSize; j++) {
                stsClientSet.add(fact.create(stsClientConfig));
            }
            overAllSTSClientSet.addAll(stsClientSet);
            for (STSClient stsClient : stsClientSet) {
                pool.putIn(stsClientConfig, stsClient);
            }
        }
    }

    /**
     * create several pools (with different sizes) at once - for instance for different STSClientFactory instances<br/>
     * Expected result: it's possible to have more pools
     */
    @Test
    public void testMorePools() {
        assertNotSame(STSClientPool.instance(10), STSClientPool.instance(20));
        // TODO more tests here
    }

    /**
     * Get STSClient from pool and then try to return it to the pool 2 times<br/>
     * Expected result: Second call of pool.putIn() should throw exception
     */
    @Test
    public void testReturnClientMoreTimes() {
        final STSClientConfig stsClientConfig = createSTSClientConfig();
        final STSClient stsc = STSClientFactory.getInstance(2).create(stsClientConfig);

        final STSClientPool pool = STSClientPool.instance(0);
        pool.putIn(stsClientConfig, stsc);
        try {
            pool.putIn(stsClientConfig, stsc);
            fail("Returning STSClient to the pool should fail, because it was already returned");
        } catch (Exception e) {
            // OK expected
        }
    }

    /**
     * Get STSClient from pool and then try to return it to the pool 2 times<br/>
     * Expected result: Second call of pool.putIn() should throw exception
     */
    @Test
    public void testReturnNotPooledClient() {
        final STSClientConfig stsClientConfig = createSTSClientConfig();
        // get not pooled instance
        final STSClient stsc = STSClientFactory.getInstance(0).create(stsClientConfig);
        final STSClientPool pool = STSClientPool.instance(0);
        // try to return the STSClient instance to a pool
        try {
            pool.putIn(stsClientConfig, stsc);
            fail("Returning STSClient to the pool should fail, because it was not retrieved from the pool");
        } catch (Exception e) {
            // OK expected
        }
    }

    // TODO: Check if there could be synchronization issues in the STSClientPool

    private STSClientConfig createSTSClientConfig() {
        return new Builder() //
                .endpointAddress("http://localhost:8080/picketlink-sts/PicketLinkSTS") //
                .portName("PicketLinkSTSPort") //
                .serviceName("PicketLinkSTS") //
                .username("admin") //
                .password("admin") //
                .build();
    }
}
