/**
 *    Copyright (C) 2011-2016 sndyuk
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.silica.job;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.silica.Silica;
import com.silica.rpc.server.ServerSelector;

public class JobExecutorTest {

    private static final Logger LOG = LoggerFactory
            .getLogger(JobExecutorTest.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        System.setProperty("SILICA_CONF", "example.properties");
        Silica.boot();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {

        ServerSelector.createSelector().setDisactiveAll();
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testCall1() throws Exception {

        for (int i = 0; i < 3; i++) {
            call1();
        }
    }

    private void call1() throws Exception {

        String result = Silica.execute(new TestJob1());
        assertEquals("success", result);
    }

    @Test
    public void testCall2() throws Exception {

        for (int i = 0; i < 2; i++) {
            call2();
        }
    }

    private void call2() throws Exception {

        JobExecutor<BigDecimal[]> executor1 = new JobExecutor<BigDecimal[]>(new TestJob2(BigDecimal.valueOf(1), BigDecimal.valueOf(1), 5));
        JobExecutor<BigDecimal[]> executor2 = new JobExecutor<BigDecimal[]>(new TestJob2(BigDecimal.valueOf(2), BigDecimal.valueOf(2), 5));

        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<BigDecimal[]> future1 = service.submit(executor1);
        Future<BigDecimal[]> future2 = service.submit(executor2);

        try {

            BigDecimal[] result1 = future1.get();
            BigDecimal[] result2 = future2.get();

            BigDecimal[] expect1 = {
                    BigDecimal.valueOf(2),
                    BigDecimal.valueOf(2),
                    BigDecimal.valueOf(2),
                    BigDecimal.valueOf(2),
                    BigDecimal.valueOf(2) };

            assertArrayEquals(expect1, result1);

            BigDecimal[] expect2 = {
                    BigDecimal.valueOf(4),
                    BigDecimal.valueOf(4),
                    BigDecimal.valueOf(4),
                    BigDecimal.valueOf(4),
                    BigDecimal.valueOf(4) };

            assertArrayEquals(expect2, result2);

        } catch (Exception e) {
            LOG.error(e.toString(), e);

            if (service.isShutdown()) {
                service.shutdownNow();
            }
            throw e;
        } finally {

            service.shutdownNow();
        }
    }

    @Test
    public void testClean() throws Exception {

        ServerSelector.createSelector().cleanAll(true);
    }
}
