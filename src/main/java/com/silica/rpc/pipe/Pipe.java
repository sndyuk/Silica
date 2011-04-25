package com.silica.rpc.pipe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.silica.resource.Resource;
import com.silica.rpc.server.Server;

public abstract class Pipe {

	private static final Logger log = LoggerFactory.getLogger(Pipe.class);

	protected abstract void connect(Server server) throws PipeException;

	public abstract void disconnect();

	public abstract boolean isConnected();

	public abstract void put(String dest, Resource resource)
			throws PipeException;

	public abstract void remove(Resource... resources) throws PipeException;

	public abstract void execute(String command) throws PipeException;

	protected void debug(final InputStream strm, final String charset) {

		if (!log.isDebugEnabled()) {
			return;
		}

		Thread th = new Thread(new Runnable() {

			@Override
			public void run() {
				try {

					ByteArrayOutputStream sb = new ByteArrayOutputStream();
					byte[] buf = new byte[256];
					for (int i = 0; i > 0; i = strm.read(buf)) {
						sb.write(buf);
					}
					log.debug(sb.toString(charset == null ? "utf-8" : charset));

				} catch (IOException e) {
					log.warn("Could not read stream.", e);
				} finally {
					try {
						strm.close();
					} catch (IOException e) {
						log.warn("Could not close stream.", e);
					}
				}
			}
		});
		th.start();
	}
}
