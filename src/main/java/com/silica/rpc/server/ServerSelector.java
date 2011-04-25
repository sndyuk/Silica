package com.silica.rpc.server;

import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.silica.Silica;
import com.silica.service.Service;

public final class ServerSelector {

	private static final Logger log = LoggerFactory
			.getLogger(ServerSelector.class);

	private static SelectLogic SELECT_LOGIC;

	private static Map<String, Server> SERVERS_M = new HashMap<String, Server>();
	private static List<String> SERVERS_KEY = new ArrayList<String>();

	static {

		{
			String classname = Silica
					.getGlobalConfig("server.select.logic");

			try {

				Class<?> s = ServerSelector.class.getClassLoader()
						.loadClass(classname);

				SELECT_LOGIC = (SelectLogic) s.newInstance();

			} catch (Exception e) {

				log.error(MessageFormat.format(
						"Could not create Server select logic [{0}].",
						classname), e);
			}
		}

		{
			String classname = Silica.getGlobalConfig("server.class");
			String[] addresses = Silica.getGlobalConfig("server.addresses")
					.split(",");

			for (String address : addresses) {

				try {

					InetAddress addr = InetAddress.getByName(address);
					if (addr != null) {
						
						Class<?> s = ServerSelector.class.getClassLoader().loadClass(
								classname);
						
						Server server = (Server) s.newInstance();
						server.connect(address);
						
						address = addr.getHostAddress();
						SERVERS_M.put(address, server);
						SERVERS_KEY.add(address);
					}
				} catch (Exception e) {

					log.error(MessageFormat.format(
							"Could not create Server [{0}].", classname), e);
				}
			}
		}
	}

	public static Server select(Service service) {

		return SELECT_LOGIC.select(service, SERVERS_KEY.toArray(new String[SERVERS_KEY.size()]), 
				Collections.unmodifiableMap(SERVERS_M));
	}
	
	public static Map<String, Server> selectAll() {

		return Collections.unmodifiableMap(SERVERS_M);
	}
	
	public static Server getLocalServer() {
		
		return SERVERS_M.get(Silica.getLocalAddress().getHostAddress());
	}
}
