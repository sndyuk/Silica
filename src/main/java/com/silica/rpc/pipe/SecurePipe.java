package com.silica.rpc.pipe;

import java.io.ByteArrayInputStream;
import java.io.File;
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

public class SecurePipe extends Pipe {

	private static final Logger log = LoggerFactory.getLogger(SecurePipe.class);

	private Session session;
	private ChannelSftp sftp;
	private ChannelExec exec;
	private String charset;

	static {

		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no");
		JSch.setConfig(config);
	}

	@Override
	protected void connect(Server server) throws PipeException {

		if (!(server instanceof SecurePipedServer)) {

			throw new PipeException(
					"A server use Secure pipe need to instance of SecurePipedServer.");
		}

		try {

			ServerContext context = server.getServerContext();
			String address = context.getAddress();
			charset = context.getProperty("charset");
			if (charset == null || charset.length() == 0) {
				charset = "utf-8";
			}
			int sshport = Integer.parseInt(context.getProperty("ssh.port"));

			log.debug("Connecting to {}:{}", address, sshport);

			JSch jsch = new JSch();

			String pass = context.getProperty("private.key.pass");

			if (pass == null || pass.length() == 0) {

				jsch.addIdentity(context.getProperty("private.key.path"));

			} else {

				jsch.addIdentity(context.getProperty("private.key.path"), pass);
			}

			session = jsch.getSession(context.getProperty("private.key.user"),
					address, sshport);
			session.connect(Integer.parseInt(context
					.getProperty("ssh.timeout.msec")));
			
		} catch (JSchException e) {

			disconnect();

			throw new PipeException(MessageFormat.format(
					"Could not pipe the server [{0}].", server.toString()), e);
		}
	}

	@Override
	public void disconnect() {

		if (sftp != null && !sftp.isConnected()) {
			sftp.disconnect();
		}
		if (exec != null && !exec.isConnected()) {
			exec.disconnect();
		}
		if (session != null && !session.isConnected()) {

			log.debug("Disconnecting connection [{}].", session.getHost());

			session.disconnect();
		}
	}

	@Override
	public void put(String dest, Resource resource) throws PipeException {
		ensureConnect();

		String destpath = dest + resource.getName();

		try {

			resource.cacheOnMemory();
			useSftpChannel();
			sftp.connect();

			int sp = 0;
			sp = (sp = destpath.lastIndexOf("/")) > 0 ? sp : destpath
					.lastIndexOf("\\");
			execute("mkdir -p " + destpath.substring(0, sp));
			sftp.put(new ByteArrayInputStream(resource.getData()), destpath);
			
			int exitStatus = sftp.getExitStatus();

			if (exitStatus != 0) {
				throw new PipeException(MessageFormat.format(
						"Exit Status is [{0}].", exitStatus));
			}
		} catch (Exception e) {

			disconnect();

			throw new PipeException(MessageFormat.format(
					"Could not put the resource [{0}].", destpath), e);
		}
	}

	@Override
	public void remove(Resource... resources) throws PipeException {
		ensureConnect();

		try {

			useSftpChannel();
			sftp.connect();

			for (Resource resource : resources) {

				sftp.rm(new File(resource.getName()).getAbsolutePath());
			}
			
			int exitStatus = sftp.getExitStatus();

			if (exitStatus != 0) {
				throw new PipeException(MessageFormat.format(
						"Exit Status is [{0}].", exitStatus));
			}
		} catch (Exception e) {

			disconnect();

			throw new PipeException(e);
		}
	}

	@Override
	public boolean isConnected() {
		return session != null ? session.isConnected() : false;
	}

	@Override
	public void execute(String command) throws PipeException {
		ensureConnect();

		try {
			useExecChannel();
			exec.setCommand(command);

			exec.connect();
			
			debug(exec.getInputStream(), charset);
			debug(exec.getErrStream(), charset);
			
			exec.disconnect();
			
			int exitStatus = exec.getExitStatus();
			exec = null;

			if (exitStatus != 0) {
				throw new PipeException(MessageFormat.format(
						"Exit Status is [{0}]: [{1}].", exitStatus, command));
			}
		} catch (Exception e) {

			disconnect();

			throw new PipeException(MessageFormat.format(
					"Could not execute command [{0}].", command), e);
		}
	}

	private void ensureConnect() throws PipeException {
		if (!isConnected()) {

			throw new PipeException("Illegal state: connection closed.");
		}
	}

	private void useSftpChannel() throws JSchException {

		if (sftp == null || !sftp.isConnected()) {

			sftp = (ChannelSftp) session.openChannel("sftp");
		}
	}

	private void useExecChannel() throws JSchException {

		if (exec == null || !exec.isConnected()) {

			exec = (ChannelExec) session.openChannel("exec");
		}
	}
}
