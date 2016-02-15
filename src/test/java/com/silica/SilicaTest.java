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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.silica.rpc.ProxyService;

public class SilicaTest {

    @Test
    public void initialize() throws Exception {

        System.setProperty("SILICA_CONF", "example.properties");

        String[] args = {
                "-o", "bind",
                "-s", "base.dir=test",
                "-s", "service.class=" + ProxyService.class.getName(),
                "-s", "class.paths=test.jar" };

        Silica.boot(args);
        assertEquals("test/", Silica.getGlobalConfig("base.dir"));
        assertEquals("test/", Silica.getBaseDirectory());
        assertEquals(ProxyService.class, Silica.getServiceClass());
        assertEquals("example.jar,test.jar", Silica.getGlobalConfig("class.paths"));

    }
}
