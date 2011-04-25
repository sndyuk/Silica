package com.silica.rpc.server;

import java.util.Map;

import com.silica.service.Service;

public class SingleServerSelector implements SelectLogic {

	@Override
	public Server select(Service service, String[] keys, Map<String, Server> servers) {

		return servers.get(keys[0]);
	}
}
