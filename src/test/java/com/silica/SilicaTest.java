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
package com.silica;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.concurrent.Future;

import org.junit.AfterClass;
import org.junit.Test;

import com.silica.job.Job;
import com.silica.job.JobException;
import com.silica.rpc.ProxyService;
import com.silica.rpc.server.ServerSelector;

public class SilicaTest {

    @AfterClass
    public static void tearDownAfterClass() throws Exception {

        ServerSelector.createSelector().setDisactiveAll();
    }

    @Test
    public void test_boot() throws Exception {

        System.setProperty("SILICA_CONF", "example.properties");

        String[] args = {
                "-o", "bind",
                "-s", "base.dir=src/test/resources/basedir",
                "-s", "service.class=" + ProxyService.class.getName(),
                "-s", "class.paths=test.jar" };

        Silica.boot(args);
        assertEquals("src/test/resources/basedir/", Silica.getGlobalConfig("base.dir"));
        assertEquals("src/test/resources/basedir/", Silica.getBaseDirectory());
        assertEquals(ProxyService.class, Silica.getServiceClass());
        assertEquals("example.jar,test.jar", Silica.getGlobalConfig("class.paths"));
    }

    @Test
    public void test_execute() throws Exception {

        System.setProperty("SILICA_CONF", "example.properties");

        String[] args = {
                "-o", "bind",
                "-s", "base.dir=src/test/resources/basedir" };

        Silica.boot(args);

        long start = System.currentTimeMillis();

        Future<String> futureResult1 = Silica.executeAsync(new LocalTestJob(1));
        Future<String> futureResult2 = Silica.executeAsync(new LocalTestJob(2));
        Future<String> futureResult3 = Silica.executeAsync(new LocalTestJob(3));
        assertThat(futureResult1.get(), is("OK: 1"));
        assertThat(futureResult2.get(), is("OK: 2"));
        assertThat(futureResult3.get(), is("OK: 3"));
        // Assert that these each jobs are executed on another threads.
        // 10sec = Binding time less than 5sec + Execution time 5sec
        assertTrue("You must have 3 processors at least.", Runtime.getRuntime().availableProcessors() >= 3);
        assertTrue(System.currentTimeMillis() - start < 10000);
    }

    public static class LocalTestJob implements Job<String>, Serializable {
        private static final long serialVersionUID = 1L;

        private final int id;

        public LocalTestJob(int id) {
            this.id = id;
        }

        @Override
        public String execute() throws JobException {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "OK: " + id;
        }

    }
}
