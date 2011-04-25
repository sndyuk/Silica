package com.silica.rpc.server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.silica.Config;
import com.silica.Silica;
import com.silica.service.Service;
import com.silica.service.ServiceException;

public class ServerContext {

	private static final Logger log = LoggerFactory
			.getLogger(ServerContext.class);

	private boolean remote;
	private String address;
	private int port;
	private Config conf;
	private String basedir;
	private String resourcedir;
	private List<String> paths;
	private String pathsep;

	public ServerContext(String address) throws IOException {

		this.conf = new Config();
		this.conf.init(Silica.getGlobalConfig("server.config.dir"), "server."
				+ address + ".properties");

		this.port = Integer.parseInt(conf.get("listen.port"));
		this.address = address;

		this.basedir = conf.get("base.dir");
		if (!this.basedir.endsWith("/")) {
			this.basedir = this.basedir + "/";
		}

		this.pathsep = this.basedir.startsWith("/") ? ":" : ";";

		// Resource destination directry.
		String td = conf.get("resource.dest.dir");
		if (!td.endsWith("/")) {
			td = td + "/";
		}
		this.resourcedir = this.basedir + td;

		// Class paths.
		String[] s = conf.get("extra.class.paths").split(",");
		paths = new ArrayList<String>();
		for (int i = 0; i < s.length; i++) {
			this.paths.add(basedir + s[i]);
		}

		this.remote = !Silica.getLocalAddress().getHostAddress()
				.equals(InetAddress.getByName(address).getHostAddress());
	}

	public String getAddress() {
		return address;
	}

	public int getListenPort() {
		return port;
	}

	public String getProperty(String key) {
		return conf.get(key);
	}

	public String getBasePath() {
		return basedir;
	}

	public String[] getClassPaths() {

		return paths.toArray(new String[paths.size()]);
	}

	public String getClassPathString() {

		return makeClassPathString(getClassPaths());
	}

	private String makeClassPathString(String[] paths) {

		StringBuilder sb = new StringBuilder();
		for (String path : paths) {
			sb.append(path);
			sb.append(pathsep);
		}
		return sb.toString();
	}

	public String getResourceDir() {
		return resourcedir;
	}

	public Service getService(String serviceName) throws ServiceException {

		try {

			Class<?> clazz = Class.forName(serviceName);
			return (Service) clazz.newInstance();

			// RpcService serviceProxy = (RpcService) Proxy.newProxyInstance(
			// AbstractRpcService.class.getClassLoader(),
			// new Class[] { RpcService.class },
			// new ServiceInvocationHandler<Service>(service, this));
			//
			// return serviceProxy;

		} catch (Exception e) {
			log.error("Could not define the Service [{}].", serviceName);
			throw new ServiceException("Could not define the Service.", e);
		}
	}

	public void addClassPath(String path) {

		paths.add(path);
	}

	public boolean isRemote() {
		return remote;
	}
}
