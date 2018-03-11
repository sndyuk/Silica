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
package com.silica.rpc.server;

import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.silica.Config;
import com.silica.Silica;
import com.silica.service.Service;

public final class ServerSelector {

    private static final Logger LOG = LoggerFactory.getLogger(ServerSelector.class);

    private ServerSelectLogic selectLogic;
    private TreeMap<String, Server> serverMap = new TreeMap<String, Server>();
    private List<String> serverMapsKeyList = new ArrayList<String>();

    private ServerSelector() {

        try {
            {
                String classname = Silica.getGlobalConfig("server.select.logic");

                Class<?> s = ServerSelector.class.getClassLoader().loadClass(
                        classname);

                selectLogic = (ServerSelectLogic) s.newInstance();

            }
            {
                String classname = Silica.getGlobalConfig("server.class");
                Class<?> s = ServerSelector.class.getClassLoader().loadClass(classname);

                String[] addresses = Silica.getGlobalConfig("server.addresses").split(",");

                for (String address : addresses) {
                    address = address.trim();
                    if (address.length() == 0) {
                        throw new ServerException("server.addresses must not be empty: " + Silica.getGlobalConfig("server.addresses"));
                    }
                    if (serverMap.get(address) == null) {
                        createServer(address, s);
                    } else {
                        LOG.warn("server [{}] is already in silica config file.", address);
                    }
                }
            }
        } catch (Exception e) {

            LOG.error("Could not initialize ServerSelector.", e);
        }
    }

    private void createServer(String address, Class<?> serverClass) {

        try {

            address = address.replaceAll("[ ].", "");

            Constructor<?> cons = serverClass.getConstructor(ServerContext.class);

            ServerContext sc = new ServerContext(address);
            sc.setEnable(true);
            Server server = (Server) cons.newInstance(new Object[] { sc });
            serverMap.put(address, server);
            serverMapsKeyList.add(address);

            LOG.debug("cached server: {}", address);

        } catch (Exception e) {
            LOG.error(MessageFormat.format(
                    "Could not create Server [{0}].", serverClass.getName()), e);
        }
    }

    private static ServerSelector SINGLE_SELECTOR = new ServerSelector();

    public static ServerSelector createSelector() {

        return SINGLE_SELECTOR;
    }

    public Server select(Service service) {
        List<Server> activeServers = new ArrayList<Server>();
        for (Entry<String, Server> entry : serverMap.entrySet()) {
            if (entry.getValue().getServerContext().isEnable()) {
                activeServers.add(entry.getValue());
            }
        }
        if (activeServers.size() == 0) {

            throw new IllegalStateException("Server are unavailable.");
        }

        return selectLogic.select(service, activeServers);
    }

    Map<String, Server> selectAll() {

        return Collections.unmodifiableMap(serverMap);
    }

    public void setDisactiveAll() throws ServerException {

        try {
            Map<String, Server> servers = selectAll();

            for (Entry<String, Server> entry : servers.entrySet()) {
                Server server = entry.getValue();

                server.getServerContext().setEnable(false);
                server.disactivate();
            }
        } catch (Exception e) {
            LOG.error(e.toString(), e);
            throw new ServerException(e);
        }
    }

    public void cleanAll(boolean wait) throws ServerException {

        try {
            Map<String, Server> servers = selectAll();

            for (Entry<String, Server> entry : servers.entrySet()) {
                Server server = entry.getValue();

                server.cleanOldModules(wait);
            }
        } catch (Exception e) {
            LOG.error(e.toString(), e);
            throw new ServerException(e);
        }
    }

    public Server getLocalServer() {

        return serverMap.get(Silica.getGlobalConfig(Config.KEY_HOST_ADDRESS));
    }
}
