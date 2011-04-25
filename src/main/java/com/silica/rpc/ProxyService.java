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
package com.silica.rpc;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.silica.job.Job;
import com.silica.rpc.server.ServerSelector;
import com.silica.service.ServiceException;

public final class ProxyService extends AbstractRpcService {

	private static final Logger log = LoggerFactory.getLogger(ProxyService.class);
	
	private static final long serialVersionUID = 2969871989070029618L;

	private final String name;
	
	private long start;

	public ProxyService(String name) throws ServiceException {

		this.name = name;
		
		this.start = System.nanoTime();
	}

	@Override
	public <R extends Serializable> R execute(Job<R> job) throws ServiceException {

		R r = ServerSelector.select(this).execute(name, job);
		
		log.info("Elapsed time: {} nano sec.", System.nanoTime() - start);
		return r; 
	}
}
