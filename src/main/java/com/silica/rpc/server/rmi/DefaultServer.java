/**
 *    Copyright (C) 2011 sndyuk
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

public class DefaultServer extends SecurePipedServer {

	private static final Logger log = LoggerFactory
			.getLogger(DefaultServer.class);

	@Override
	public void bind(String name, Service service) throws ServerException {

		if (!isConnected()) {

			throw new ServerException("Illegal state: not connected.");
		}
		if (getServerContext().isRemote()) {

			bindRemote(name, service);

		} else {

			bindLocal(name, service);
		}
	}

	private void bindRemote(String name, Service service)
			throws ServerException {

		try {

			String jarexec = "java -cp "
					+ getServerContext().getClassPathString() + " "
					+ "com.silica.Silica bind";

			log.debug("bind remote command: {}", jarexec);

			execute(jarexec);

		} catch (Exception e) {
			throw new ServerException(MessageFormat.format(
					"Could not bind the class name:[{0}].", name), e);
		}
	}

	private void bindLocal(String name, Service service) throws ServerException {

		try {

			Remote r = UnicastRemoteObject.exportObject(service, 0);

			String version = Silica.getGlobalConfig("version");

			name = version + name;

			Registry registry = getRegistry();
			log.debug("bind: {}", name);
			registry.rebind(name, r);

			if (log.isInfoEnabled()) {
				log.info("RMI Object is ready for bind name:{}.", name);
			}
		} catch (Exception e) {
			throw new ServerException(MessageFormat.format(
					"Could not bind the class name:[{0}].", name), e);
		}
	}

	@Override
	public void unbind(String name) throws ServerException {

		if (!isConnected()) {

			throw new ServerException("Illegal state: not connected.");
		}

		try {

			String version = Silica.getGlobalConfig("version");

			name = version + name;

			getRegistry().unbind(name);

		} catch (Exception e) {
			throw new ServerException(MessageFormat.format(
					"Could not unbind the class name:[{}].", name), e);
		}

		log.info("RMI Object bind name:{} has been unbinded.", name);
	}

	private Registry getRegistry() throws RemoteException {

		return LocateRegistry.getRegistry(getServerContext().getAddress(),
				getServerContext().getListenPort());
	}

	@Override
	public void activate() throws ServerException {

		// try {
		//
		// remove(new Resource(getServerContext().getClassPaths()));
		//
		// } catch (ServerException e) {
		//
		// log.debug("ignored the error.", e);
		// }

		String clone = Silica.getBaseDirectry()
				+ Silica.getGlobalConfig("clone.path");
		put(getServerContext().getResourceDir(), new Resource(clone));

		ServerContext conf = getServerContext();

		String command = Silica.getBaseDirectry() + MessageFormat.format(
				getServerContext().getProperty("activation.command"),
				Integer.toString(conf.getListenPort()),
				conf.getClassPathString(), conf.getAddress());

		log.debug("Server activation command: {}", command);

		execute(command);
	}

	@Override
	public void disactivate() throws ServerException {

		String command = Silica.getBaseDirectry() + MessageFormat.format(
				getServerContext().getProperty("deactivation.command"),
				getServerContext().getListenPort());

		log.debug("Server deactivation command: {}", command);

		execute(command);
	}

	@Override
	public <R extends Serializable> R execute(String name, Job<R> job)
			throws ServiceException {

		try {

			Registry registry = getRegistry();

			String version = Silica.getGlobalConfig("version");

			name = version + name;

			log.debug("will be calling rmi: {}", name);

			Service service = (Service) registry.lookup(name);

			log.info("RMI Object lookup successed. bind name :{}.", name);

			Resource[] resources = getResources(job);

			if (resources != null) {
				for (Resource resource : resources) {

					try {

						resource.cacheOnMemory();
						service.setResources(getServerContext()
								.getResourceDir(), resource);

					} catch (IOException e) {

						log.warn("The error was ignored.", e);

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

	private Resource[] getResources(Job<?> job) throws IOException {

		Resources res = null;
		try {
			res = job.getClass().getMethod("execute")
					.getAnnotation(Resources.class);
		} catch (Exception e) {
			// ignore
			log.error("execute() not found.", e);
		}

		if (res == null) {
			return null;
		}

		String paths = res.path();
		String basedir = getServerContext().getProperty("base.dir");

		if (log.isDebugEnabled()) {
			log.debug("base dir: {}", basedir);
			log.debug("resource paths: {}", paths);
		}

		return ResourceLoader.getResources(paths);
	}

	@Override
	public boolean isActive() {

		try {

			Registry registry = getRegistry();
			registry.lookup("_silica");

			return true;

		} catch (ConnectException e) {
			return false;
		} catch (NotBoundException e) {
			return true;
		} catch (Exception e) {
			log.error(e.toString(), e);
			return false;
		}
	}
}
