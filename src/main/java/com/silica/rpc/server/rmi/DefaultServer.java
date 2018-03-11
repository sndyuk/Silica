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
            Remote r = UnicastRemoteObject.exportObject(service, getServerContext().getListenPortRmi());
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

    @Override
    public void activate() throws ServerException {
        ensureRMIRegistry();
        super.activate();

        ServerContext conf = getServerContext();

        String command = MessageFormat.format(
                conf.getActivationCommand(),
                String.valueOf(conf.getListenPortRmiServer()),
                conf.getJavaHome(),
                conf.getClassPathString(),
                conf.getInternalAddress(),
                getServerContext().getResourceDirectory(),
                Boolean.valueOf(false).toString()); // RMI debug mode = true | false.

        if (command == null || command.equals("")) {
            LOG.debug("Server activation command is not defined.");
            return;
        }
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
                String.valueOf(conf.getListenPortRmiServer()),
                conf.getResourceDirectory());

        try {
            if (command != null && !command.equals("")) {
                LOG.debug("Server deactivation command: {}", command);
                execute(command);
            } else {
                LOG.debug("Server deactivation command is not defined.");
            }
        } finally {
            try {
                shutdownRMIRegistry();
            } finally {
                super.disactivate();
            }
        }
    }

    @Override
    public <R extends Serializable> R execute(Class<? extends Service> clazz,
            Job<R> job) throws ServiceException {

        String name = Silica.getGlobalConfig(Config.KEY_VERSION) + clazz.getName();

        try {
            ensureActive();

            Registry registry = getRegistry();
            Service service = lookup(registry, name, 0);

            LOG.debug("Succeeded RMI Object lookup:{}.", name);

            Resource[] resources = getResources(job);

            if (resources != null) {
                LOG.info("Deploy resources");
                try {

                    service.deployResources(getServerContext().getResourceDirectory(), resources);

                } finally {

                    for (Resource resource : resources) {
                        resource.close();
                    }
                }
            }
            LOG.info("Execute the job: {}", job.getClass());

            return service.execute(job);

        } catch (Exception e) {

            throw new ServiceException("Could not execute the service.", e);
        }
    }

    private synchronized Service lookup(Registry registry, String serviceName, int tryCnt)
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
            throw new RuntimeException("Job must have execute method", e);
        }

        if (res == null) {
            return null;
        }

        String paths = res.path();

        if (LOG.isDebugEnabled()) {
            LOG.debug("resource paths to execute the job {}: {}", job.getClass(), paths);
        }

        return ResourceLoader.defineResources(paths);
    }

    @Override
    public boolean isActive() {

        try {

            Registry registry = getRegistry();
            LOG.debug("Trying to connect to RMI server.");
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

    private synchronized void ensureActive() throws ServerException {
        if (isActive()) {
            return;
        }
        activate();
    }

    private void ensureRMIRegistry() throws ServerException {
        ensureRMIRegistry(0);
    }

    private void ensureRMIRegistry(int tryCnt) throws ServerException {
        if (!getServerContext().isAutoStartRmiregistry()) {
            return;
        }
        try {
            Registry registry = getRegistry();
            registry.lookup("_silica");
        } catch (ConnectException e) {
            // Re-try.
        } catch (RemoteException e) {
            throw new ServerException(e);
        } catch (NotBoundException e) {
            // It's an expected exception.
            return;
        }
        if (tryCnt == 0) {
            LOG.info("Starting RMI server using the command: {}", getServerContext().getRmiregistryCommand());
            executeAsDaemonProcess(getServerContext().getRmiregistryCommand());
            LOG.info("Started RMI server.");
        }
        if (tryCnt < MAX_RETRY) {
            try {
                Thread.sleep(1500 * (++tryCnt));
            } catch (InterruptedException _e) {
                // Ignore the exception
            }
            ensureRMIRegistry(tryCnt);
        } else {
            throw new ServerException("Could not start RMI server.");
        }
    }

    private void shutdownRMIRegistry() throws ServerException {
        ServerContext ctx = getServerContext();
        if (!ctx.isAutoStartRmiregistry()) {
            return;
        }
        execute(ctx.getResourceDirectory() + "cmd/shutdown_rmi." + (isWindows() ? "bat" : "sh"));
    }

    private Registry getRegistry() throws RemoteException {

        String address = getServerContext().isRemote()
                ? getServerContext().getPublicAddress()
                : getServerContext().getInternalAddress();

        if (getServerContext().getListenPortRmiServer() <= 0) {
            return LocateRegistry.getRegistry(address);
        }
        return LocateRegistry.getRegistry(address, getServerContext().getListenPortRmiServer());
    }
}
