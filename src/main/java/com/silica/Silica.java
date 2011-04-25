package com.silica;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.silica.rpc.server.SecurePipedServer;
import com.silica.rpc.server.Server;
import com.silica.rpc.server.ServerException;
import com.silica.rpc.server.ServerSelector;
import com.silica.service.ServiceException;

/**
 * Silica
 */
public final class Silica {

	private static final Logger log = LoggerFactory.getLogger(Silica.class);

	/**
	 * 共通スキーム
	 */
	private static Config GLOBAL_CONFIG;
	
	/**
	 * ローカルアドレス
	 */
	private static InetAddress LOCAL_HOST;
	
	/**
	 * ベースディレクトリ
	 */
	private static String BASE_DIR;
	
	/**
	 * Jobタイムアウト
	 */
	private static int JOB_TIMEOUT;

	static {

		try {

//			LOCAL_HOST = InetAddress.getLocalHost();
			LOCAL_HOST = InetAddress.getByName("localhost");
			
		} catch (UnknownHostException e) {

			log.error("Could not get local address.", e);
		}

		
		String confPath = null;
		
		URL u = Thread.currentThread().getContextClassLoader().getResource("silica.properties");
		
		if (u != null) {
			
			confPath = u.getPath();
			
		} else {
			
			confPath = System.getProperty("SILICA_CONF");
			
			if (confPath == null) {
				confPath = System.getenv("SILICA_CONF");
			}
		}
		if (confPath == null) {
			
			log.error("Could not find [SILICA_CONF].");
		} else {
			
			log.info("[SILICA_CONF]: {}", confPath);
		}

		GLOBAL_CONFIG = new Config();
		try {
			GLOBAL_CONFIG.init(confPath);

		} catch (IOException e) {
			log.error("Could not load SILICA_CONF.", e);
		}

		BASE_DIR = (BASE_DIR = GLOBAL_CONFIG.get("base.dir")).endsWith("/") ? BASE_DIR
				: BASE_DIR + "/";
		
		JOB_TIMEOUT = Integer.parseInt(GLOBAL_CONFIG.get("job.timeout"));
	}

	/**
	 * 共通スキームから構成値を取得する
	 * 
	 * @param key 変数名
	 * @return 構成値
	 */
	public static String getGlobalConfig(String key) {
		return GLOBAL_CONFIG.getMine(key);
	}

	/**
	 * ベースディレクトリを取得する
	 * 
	 * @return ベースディレクトリ
	 */
	public static String getBaseDirectry() {
		return BASE_DIR;
	}
	
	//
	// public static void setGlobalConfig(String key, String value) {
	// GLOBAL_CONFIG.setMine(key, value);
	// }

	/**
	 * ローカルアドレスを取得する
	 * 
	 * @return ローカルアドレス
	 */
	public static InetAddress getLocalAddress() {

		return LOCAL_HOST;
	}

	/**
	 * Silicaを操作する
	 * 
	 * @param args 0: (bind | unbind), 1: サービス名(任意：指定しない場合、共通スキームに定義されたサービスが適用される)
	 */
	public static void main(String[] args) {

		if (args == null || args.length == 0) {

			throwUsage(null);
		}

		Server server = ServerSelector.getLocalServer();

		try {

			if ("bind".equals(args[0])) {

				String serviceName = null;

				if (args.length == 1) {

					serviceName = server.getServerContext().getProperty(
							"service.class");

				} else if (args.length == 2) {

					serviceName = args[1];

				} else {

					throwUsage(null);
				}
				server.bind(serviceName,
						server.getServerContext().getService(serviceName));

			} else if ("unbind".equals(args[0])) {

				if (args.length == 1) {

					server.unbind(server.getServerContext().getProperty(
							"service.class"));

				} else if (args.length == 2) {

					server.unbind(args[1]);

				} else {

					throwUsage(null);
				}
			} else if ("start".equals(args[0])) {

				SecurePipedServer spserver = (SecurePipedServer) server;
				spserver.activate();
				main(new String[] { "bind" });

			} else if ("exit".equals(args[0])) {

				SecurePipedServer spserver = (SecurePipedServer) server;
				spserver.disactivate();

			} else {

				throwUsage(null);
			}
			server.disconnect();

		} catch (ServiceException e) {

			throwUsage(e);

		} catch (ServerException e) {

			throwUsage(e);
		}
	}

	/**
	 * job最大実行時間を取得する
	 * 
	 * @return job最大実行時間
	 */
	public static int getJobTimeout() {
		return JOB_TIMEOUT;
	}
	
	private static void throwUsage(Exception e) throws IllegalArgumentException {
		if (e != null) {

			throw new IllegalArgumentException("", e);
		}
		throw new IllegalArgumentException("");
	}
}
