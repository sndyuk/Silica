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

import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.silica.resource.Resource;
import com.silica.rpc.server.SecurePipedServer;
import com.silica.rpc.server.Server;
import com.silica.rpc.server.ServerContext;

/**
 * SSH用のパイプ
 */
public class SecurePipe extends Pipe {

	private static final Logger LOG = LoggerFactory.getLogger(SecurePipe.class);

	private Server server;
	private Session session;
	private ChannelSftp channelSftp;
	private ChannelExec channelExec;
	private int connectionTimeout = 10000;
	private long retryInterval = 100;

	static {

		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no");
		JSch.setConfig(config);
	}

	@Override
	protected void connect(Server server) throws PipeException {

		if (!(server instanceof SecurePipedServer)) {

			throw new PipeException("A server use Secure pipe need to instance of SecurePipedServer.");
		}
		this.server = server;
		try {

			ServerContext context = server.getServerContext();
			String address = context.getPublicAddress();
			int sshport = context.getSshPort();

			LOG.debug("Connecting to {}:{}", address, sshport);

			JSch jsch = new JSch();

			String pass = context.getSshPass();

			if (pass == null || pass.length() == 0) {

				jsch.addIdentity(context.getSshPrivateKeyPath());

			} else {

				jsch.addIdentity(context.getSshPrivateKeyPath(), pass);
			}

			session = jsch.getSession(context.getSshUser(), address, sshport);
			session.connect(context.getSshTimeout());
			
		} catch (JSchException e) {

			disconnect();

			throw new PipeException(MessageFormat.format("Could not pipe the server [{0}].", server.toString()), e);
		}
	}

	@Override
	public void disconnect() {

		if (session != null && session.isConnected()) {

			LOG.debug("Disconnecting connection [{}].", session.getHost());

			session.disconnect();
		}
	}

	@Override
	public void put(String dest, Resource... resources) throws PipeException {
		ensureConnect();

		for (Resource resource : resources) {
			FileInputStream in = resource.getData();
			if (in == null) {
				return;
			}

			String destpath = dest + resource.getName();

			try {

				int sp = 0;
				sp = (sp = destpath.lastIndexOf('/')) > 0 ? sp : destpath.lastIndexOf('\\');
				String destdir = destpath.substring(0, sp);
				execute("mkdir -p " + destdir);

				ChannelSftp sftp = useSftpChannel();
				sftp.put(in, destpath);
				sftp.chmod(resource.getpermissions(), destpath);
				
			} catch (Exception e) {

				disconnect();

				throw new PipeException(MessageFormat.format(
						"Could not put the resource [{0}].", destpath), e);
			} finally {
				try {
					in.close();
				} catch (IOException e) {
					throw new PipeException(MessageFormat.format(
							"Could not close the resource [{0}].", destpath), e);
				}
			}
		}
	}

	@Override
	public boolean isConnected() {
		return session != null && session.isConnected();
	}

	@Override
	public void execute(String command) throws PipeException {
		ensureConnect();

		try {

			final ChannelExec exec = useExecChannel();
			exec.setCommand(command);
			exec.connect(connectionTimeout);
			if (LOG.isDebugEnabled()) {
				debug(exec.getExtInputStream(), server.getServerContext().getCharset());
			}
			final Thread thread = new Thread() {
				public void run() {
					int retryCnt = 0;
					while (!exec.isClosed()) {
						try {
							sleep(retryInterval * (++retryCnt));
						} catch (InterruptedException e) {
							LOG.warn("interrupted", e);
							return;
						}
					}
				}
			};
			thread.start();
			thread.join(connectionTimeout);
			
			int exitStatus = exec.getExitStatus();
			LOG.debug("Exit status: {}", exitStatus);
			
			if (thread.isAlive()) {
				throw new PipeException("connection time out");
			}
		} catch (Exception e) {
			
			disconnect();
			throw new PipeException(MessageFormat.format(
					"Could not execute command [{0}].", command), e);
		}
	}

	private synchronized void ensureConnect() throws PipeException {
		if (isConnected()) {
			return;
		}
		connect(server);
	}

	private synchronized ChannelSftp useSftpChannel() throws JSchException {
		if (channelSftp != null && channelSftp.isConnected()) {
			return channelSftp;
		}
		channelSftp = (ChannelSftp) session.openChannel("sftp");
		channelSftp.connect(connectionTimeout);
		return channelSftp;
	}

	private synchronized ChannelExec useExecChannel() throws JSchException {
		if (channelExec != null && channelExec.isConnected()) {
			return channelExec;
		}
		channelExec = (ChannelExec) session.openChannel("exec");
		return channelExec;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			disconnect();
		} catch (Throwable e) {
			// nop
		}
		super.finalize();
	}
}
