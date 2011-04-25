package com.silica.rpc.server;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.silica.Silica;
import com.silica.resource.Resource;
import com.silica.rpc.pipe.Pipe;
import com.silica.rpc.pipe.PipeException;
import com.silica.rpc.pipe.PipeHolder;

public abstract class SecurePipedServer implements Server {

	private static final Logger log = LoggerFactory
			.getLogger(SecurePipedServer.class);

	private volatile ServerContext context;
	private Pipe pipe;

	public abstract void activate() throws ServerException;

	public abstract void disactivate() throws ServerException;

	public abstract boolean isActive();

	private boolean actived;

	@Override
	public void connect(String address) throws ServerException {

		try {

			synchronized (this) {

				if (isConnected()) {

					log.debug("Already connected.");
					return;
				}

				if (!actived) {

					this.context = new ServerContext(address);

					if (!isActive()) {
						log.debug("Server is not active.");

						activate();

						log.debug("Server is active.");

					}

					String serviceName = Silica
							.getGlobalConfig("service.class");

					bind(serviceName, getServerContext()
							.getService(serviceName));

					actived = true;
				}
			}
		} catch (IOException e) {

			throw new ServerException(e);
		}
	}

	@Override
	public ServerContext getServerContext() {

		synchronized (this) {

			return context;
		}
	}

	protected void remove(Resource resource) throws ServerException {

		try {

			if (pipe == null) {

				pipe = createPipe();
			}
			pipe.remove(resource);

		} catch (PipeException e) {

			throw new ServerException(e);
		}
	}

	protected void put(String dest, Resource... resources)
			throws ServerException {

		try {

			if (pipe == null) {

				pipe = createPipe();
			}
			for (Resource resource : resources) {

				getServerContext().addClassPath(dest + resource.getName());

				pipe.put(dest, resource);
			}
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
	public boolean isConnected() {

		synchronized (this) {

			return context != null;
		}
	}

	@Override
	public void disconnect() throws ServerException {

		synchronized (this) {

			if (pipe != null) {

				pipe.disconnect();
				pipe = null;
				context = null;
			}
		}
	}

	protected Pipe createPipe() throws PipeException {

		return PipeHolder.getPipe(this);
	}
}
