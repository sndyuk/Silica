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
package com.silica.rpc.server;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.silica.Config;
import com.silica.Silica;
import com.silica.job.Callback;
import com.silica.resource.Resource;
import com.silica.rpc.pipe.Pipe;
import com.silica.rpc.pipe.PipeException;
import com.silica.rpc.pipe.PipeHolder;
import com.silica.service.Service;
import com.silica.service.ServiceException;

/**
 * セキュアシェル経由のサーバ
 * 
 * @author sndyuk
 */
public abstract class SecurePipedServer implements Server {

	private static final Logger LOG = LoggerFactory.getLogger(SecurePipedServer.class);

	private ServerContext context;
	private Pipe pipe;
	
	public SecurePipedServer(ServerContext context) {
		this.context = context;
	}
	
	/**
	 * サービスを実行可能状態にする
	 * 
	 * @param clazz
	 * @param service
	 * @throws ServerException
	 */
	protected abstract void bindLocal(Service service) throws ServerException;

	/**
	 * ssh経由でコマンドを投げてサービスを実行可能状態にする
	 * 
	 * @param clazz
	 * @param service
	 * @throws ServerException
	 */
	protected void bindRemote(Service service) throws ServerException {
		String serivceName = service.getClass().getName();
		try {

			StringBuilder options = new StringBuilder();
			options.append(" \"-s ").append(Config.KEY_HOST_ADDRESS).append("=").append(getServerContext().getPublicAddress());
			options.append(" -s ").append(Config.KEY_BASE_DIR).append("=").append(getServerContext().getBasedir());
			options.append(" -s ").append(Config.KEY_RESOURCE_DIR).append("=").append(getServerContext().getResourceDirectory());
			options.append(" -s ").append(Config.KEY_RESOURCE_ID).append("=").append(Silica.getResourceID());
			options.append(" -s ").append(Config.KEY_CLASS_PATHS).append("=");
			String[] paths = getServerContext().getClassPaths();
			for (String path : paths) {
				options.append(path);
				options.append(",");
			}
			if (paths.length > 0) {
				options.deleteCharAt(options.length() - 1);
			}
			options.append(" -s ").append(Config.KEY_SERVICE_CLASS).append("=").append(serivceName);
			options.append("\"");
			String bindCommand = MessageFormat.format(
					getServerContext().getProperty("bind.command"),
					String.valueOf(getServerContext().getListenPort1()),
					getServerContext().getJavaHome(),
					getServerContext().getClassPathString(), 
					getServerContext().getPublicAddress(),
					getServerContext().getResourceDirectory(),
					Silica.getConfigPath(),
					options.toString(),
					Boolean.valueOf(false).toString()); // debug option true | false.
			
			LOG.debug("bind remote command: {}", bindCommand);
			execute(bindCommand);

		} catch (Exception e) {
			throw new ServerException(MessageFormat.format(
					"Could not bind the class name:[{0}].", serivceName), e);
		}
	}
	
	@Override
	public void bind(Service service) throws ServerException {
		
		if (getServerContext().isRemote()) {

			bindRemote(service);

		} else {

			bindLocal(service);
		}
	}
	
	@Override
	public void activate() throws ServerException {

		try {

			if (isActive()) {

				LOG.debug("The server already has been activated.");
				return;
			}
			cloneModules();
			
			LOG.debug("The Server is now activated.");
		} catch (IOException e) {

			disactivate();
			throw new ServerException(e);
		}
	}

	@Override
	public void cleanOldModules(boolean wait) {
		if (!getServerContext().isRemote()) {
			return;
		}
		if (wait) {
			try {
				Silica.execute(new OldModuleCleaner());
			} catch (ServiceException e) {
				LOG.warn("", e);
			}
		} else {
			Silica.executeAsync(new OldModuleCleaner(), new Callback<Boolean>() {

				@Override
				public void execute(Boolean result) {
					if (result) {
						LOG.debug("Cleaned old modules.");
					} else {
						LOG.warn("Could not clean old modules.");
					}
				}
			});			
		}
	}
	
	private void cloneModules() throws ServerException {
		if (!getServerContext().isRemote()) {
			// localhost <-> localhostの時は転送不要
			return;
		}
		String clone = Silica.getGlobalConfig(Config.KEY_CLONE_PATHS);

		String[] clonepaths = clone.split(",");

		for (String clonepath : clonepaths) {
			transportFile(Silica.getBaseDirectory(), clonepath.trim());
		}
	}

	protected void transportFile(String localdir, String name) throws ServerException {
		if (name == null || name.length() == 0) {

			return;
		}
		File f = new File(localdir, name);
		File[] fc = null;
		if (f.isFile()) {
			String dest = getServerContext().getResourceDirectory();
			if (LOG.isDebugEnabled()) {
				LOG.debug("Resource output: {}{} to {}//{}", 
						new String[] {localdir, name, getServerContext().getPublicAddress(), dest});
			}

			put(dest, new Resource(f.getAbsolutePath(), name));

		} else if ((fc = f.listFiles()) != null && fc.length > 0) {

			for (File c : fc) {

				if (name.endsWith("/")) {

					transportFile(localdir, name + c.getName());

				} else {

					transportFile(localdir, name + "/" + c.getName());
				}
			}
		}
	}

	@Override
	public ServerContext getServerContext() {

		return context;
	}


	protected void put(String dest, Resource... resources)
			throws ServerException {

		try {

			if (pipe == null) {

				pipe = createPipe();
			}
			pipe.put(dest, resources);
			
		} catch (PipeException e) {

			throw new ServerException(e);
		}
	}

	protected void execute(String command) throws ServerException {

		try {

			if (pipe == null) {

				pipe = createPipe();
			}
			pipe.execute(command);

		} catch (PipeException e) {

			throw new ServerException(e);
		}
	}

	@Override
	public boolean isActive() {

		synchronized (this) {

			return context != null;
		}
	}

	@Override
	public void disactivate() throws ServerException {

		synchronized (this) {

			if (pipe != null) {

				LOG.info("Disconnecting the server [{}].", getServerContext().getPublicAddress());

				pipe.disconnect();

				LOG.info("Disconnected the server.");
			}

			pipe = null;
			context = null;
		}
	}

	protected Pipe createPipe() throws PipeException {

		return PipeHolder.getPipe(this);
	}
}
