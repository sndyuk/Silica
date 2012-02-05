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
package com.silica.rpc.pipe;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.silica.resource.Resource;
import com.silica.resource.ResourceWriter;
import com.silica.rpc.server.Server;

/**
 * Pipe for the local host.
 * 
 * @author sndyuk
 */
public class DummyPipe extends Pipe {

	private static final Logger LOG = LoggerFactory.getLogger(DummyPipe.class);

	private Server server;
	private int connectionTimeout = 10000;

	public DummyPipe() {
		LOG.debug("created dummy pipe for localhost.");
	}

	@Override
	protected void connect(Server server) throws PipeException {
		this.server = server;
	}

	@Override
	public void disconnect() {

		// nop : no need to disconnect.
	}

	@Override
	public boolean isConnected() {

		return true;
	}

	@Override
	public void put(String dest, Resource... resources) throws PipeException {

		File df = new File(dest);
		if (!df.exists() && !df.mkdirs()) {

			throw new PipeException(MessageFormat.format(
					"Could not make directry [{0}].", dest));
		}
		for (Resource resource : resources) {
			try {

				ResourceWriter writer = new ResourceWriter(resource);
				writer.publish(new File(df, resource.getName()).getAbsolutePath());

			} catch (IOException e) {

				throw new PipeException(MessageFormat.format(
						"Could not put resource [{0}].", resource.getName()), e);
			}
		}
	}


	@Override
	public void execute(String command) throws PipeException {

		final String[] c = command.split(" ");

		Process p = null;
		try {

			ProcessBuilder pb = new ProcessBuilder().command(c);
			p = pb.start();
			Thread dErr = null;
			Thread dStd = null;
			if (LOG.isDebugEnabled()) {
				dErr = debug(p.getErrorStream(), server.getServerContext().getCharset());
				dStd = debug(p.getInputStream(), server.getServerContext().getCharset());
			}
			p.waitFor();
			if (LOG.isDebugEnabled()) {
				dErr.join(connectionTimeout);
				dStd.join(connectionTimeout);
			}
				
		} catch (Exception e) {

			throw new PipeException(MessageFormat.format(
					"Could not execute the command [{0}]", command), e);
		} finally {

			if (p != null) {

				p.destroy();
			}
		}
	}
}
