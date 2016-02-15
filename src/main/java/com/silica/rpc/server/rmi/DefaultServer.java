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
package com.silica.rpc.server.rmi;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.AccessException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.silica.Config;
import com.silica.Silica;
import com.silica.job.Job;
import com.silica.resource.Resource;
import com.silica.resource.ResourceLoader;
import com.silica.rpc.Resources;
import com.silica.rpc.server.SecurePipedServer;
import com.silica.rpc.server.ServerContext;
import com.silica.rpc.server.ServerException;
import com.silica.service.Service;
import com.silica.service.ServiceException;

/**
 * Default RMI Server.
 */
public class DefaultServer extends SecurePipedServer {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultServer.class);
    private static final int MAX_RETRY = 3;

    public DefaultServer(ServerContext context) {
        super(context);
    }

    @Override
    protected void bindLocal(Service service) throws ServerException {
        ensureActive();

        try {
            Remote r = UnicastRemoteObject.exportObject(service, getServerContext().getListenPort2());
            bindLocal(r, service, 0);
        } catch (Exception e) {
            throw new ServerException(MessageFormat.format(
                    "Could not bind the class name:[{0}].", service.getClass().getName()), e);
        }
    }

    private void bindLocal(Remote r, Service service, int tryCnt) throws ServerException, RemoteException, InterruptedException {

        String name = service.getClass().getName();
        try {
            String version = Silica.getGlobalConfig("version");
            name = version + name;
            Registry registry = getRegistry();

            registry.rebind(name, r);
            LOG.debug("bind: {} on {}", name, getServerContext().getInternalAddress());

            if (LOG.isInfoEnabled()) {
                LOG.info("RMI Object is ready for bind name:{}.", name);
            }
        } catch (ConnectException e) {
            if (tryCnt < MAX_RETRY) {
                LOG.info("Retry to connect RMI server:{}.", name);
                Thread.sleep(1500 * (++tryCnt));
                bindLocal(r, service, tryCnt);
            } else {
                throw new ServerException("Could not connect RMI server.");
            }
        }
    }

    @Override
    public void unbind(Class<? extends Service> clazz) throws ServerException {

        String name = clazz.getName();

        try {

            String version = Silica.getGlobalConfig("version");

            name = version + name;

            getRegistry().unbind(name);

        } catch (Exception e) {

            throw new ServerException(MessageFormat.format(
                    "Could not unbind the class name:[{}].", name), e);
        }

        LOG.info("RMI Object bind name:{} has been unbinded.", name);
    }

    private Registry getRegistry() throws RemoteException {

        Registry registry = LocateRegistry.getRegistry(
                getServerContext().isRemote()
                        ? getServerContext().getPublicAddress()
                        : getServerContext().getInternalAddress(),
                getServerContext().getListenPort1());

        return registry;
    }

    @Override
    public void activate() throws ServerException {

        super.activate();

        ServerContext conf = getServerContext();

        String command = MessageFormat.format(
                conf.getActivationCommand(),
                String.valueOf(conf.getListenPort1()),
                conf.getJavaHome(),
                conf.getClassPathString(),
                conf.getInternalAddress(),
                getServerContext().getResourceDirectory(),
                Boolean.valueOf(false).toString()); // RMI debug mode = true | false.

        LOG.debug("Server activation command: {}", command);

        execute(command);
    }

    @Override
    public void disactivate() throws ServerException {
        if (!isActive()) {
            return;
        }

        ServerContext conf = getServerContext();

        String command = MessageFormat.format(
                conf.getDeactivationCommand(),
                String.valueOf(conf.getListenPort1()),
                conf.getResourceDirectory());

        LOG.debug("Server deactivation command: {}", command);

        execute(command);

        super.disactivate();
    }

    @Override
    public <R extends Serializable> R execute(Class<? extends Service> clazz,
            Job<R> job) throws ServiceException {

        String name = Silica.getGlobalConfig(Config.KEY_VERSION) + clazz.getName();

        try {
            ensureActive();

            Registry registry = getRegistry();

            LOG.debug("will be calling rmi: {}", name);

            Service service = lookup(registry, name, 0);

            LOG.info("RMI Object lookup successed. bind name :{}.", name);

            Resource[] resources = getResources(job);

            if (resources != null) {
                for (Resource resource : resources) {

                    try {

                        service.setResources(getServerContext()
                                .getResourceDirectory(), resource);

                    } catch (IOException e) {

                        LOG.warn("The error was ignored.", e);

                    } finally {

                        if (resource != null) {
                            resource.close();
                        }
                    }
                }
            }

            return service.execute(job);

        } catch (Exception e) {

            throw new ServiceException("Could not execute the service.", e);
        }
    }

    private Service lookup(Registry registry, String serviceName, int tryCnt)
            throws InterruptedException, AccessException, RemoteException, ServerException {

        boolean canRetry = false;
        Exception tmp;
        try {
            return (Service) registry.lookup(serviceName);

        } catch (NotBoundException e) {
            canRetry = true;
            tmp = e;
            bind(getServerContext().getService());

        } catch (ConnectException e) {
            canRetry = true;
            tmp = e;
        }
        if (canRetry) {
            if (tryCnt < MAX_RETRY) {
                LOG.info("Retry to lookup service:{}.", serviceName);
                Thread.sleep(1500 * (++tryCnt));
                return lookup(registry, serviceName, tryCnt);
            } else {
                throw new ServiceException("Could not lookup the service.", tmp);
            }
        }
        throw new IllegalStateException("unknown state");
    }

    private Resource[] getResources(Job<?> job) throws IOException {

        Resources res = null;
        try {

            res = job.getClass().getMethod("execute").getAnnotation(Resources.class);

        } catch (Exception e) {
            // ignore
            LOG.error("execute() not found.", e);
        }

        if (res == null) {
            return null;
        }

        String paths = res.path();
        LOG.debug("resource paths: {}", paths);

        return ResourceLoader.getResources(paths);
    }

    @Override
    public boolean isActive() {

        try {

            Registry registry = getRegistry();
            LOG.debug("Connecting to RMI server...");
            registry.lookup("_silica");

            return true;

        } catch (ConnectException e) {
            LOG.debug("Could not find a active RMI server.");
            return false;
        } catch (NotBoundException e) {
            return true;
        } catch (Exception e) {
            LOG.error(e.toString(), e);
            return false;
        }
    }

    private void ensureActive() throws ServerException {
        if (isActive()) {
            return;
        }
        activate();
    }
}
