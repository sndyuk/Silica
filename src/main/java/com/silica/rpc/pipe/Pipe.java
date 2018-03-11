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
package com.silica.rpc.pipe;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.silica.resource.Resource;
import com.silica.rpc.server.Server;

public abstract class Pipe {

	private static final Logger LOG = LoggerFactory.getLogger(Pipe.class);

	protected abstract void connect(Server server) throws PipeException;

	public abstract void disconnect();

	public abstract boolean isConnected();

	public abstract void put(String dest, Resource... resources) throws PipeException;

	public abstract void execute(String command) throws PipeException;

	protected Thread debug(final InputStream strm, final String charset) {

		Thread th = new Thread(new Runnable() {

			@Override
			public void run() {
				try {

					InputStreamReader in = new InputStreamReader(strm, charset);
					try {

						int len = -1;
						char[] b = new char[1024];
						StringBuilder sb = new StringBuilder();
						while ((len = in.read(b, 0, b.length)) != -1) {
							sb.append(b, 0, len);
						}
						if (LOG.isDebugEnabled()) {
							LOG.debug("[remote]{}", sb.toString());
						}
					} finally {
						in.close();
					}
				} catch (IOException e) {
					LOG.trace("It is not an error.", e);
				} finally {
					try {
						strm.close();
					} catch (IOException e) {
						LOG.debug("Could not close stream.", e);
					}
				}
			}
		});
		th.start();
		return th;
	}
}
