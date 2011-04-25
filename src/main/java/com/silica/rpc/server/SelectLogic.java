package com.silica.rpc.server;

import java.util.Map;

import com.silica.service.Service;

public interface SelectLogic {

	public Server select(Service service, String[] keys, Map<String, Server> servers);
}
