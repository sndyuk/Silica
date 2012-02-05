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

import java.util.WeakHashMap;

import com.silica.Config;
import com.silica.Silica;
import com.silica.rpc.server.Server;

public final class PipeHolder {

	private static final WeakHashMap<String, Pipe> PIPE_CACHE = new WeakHashMap<String, Pipe>();

	private PipeHolder() {
	}
	
	public static Pipe getPipe(final Server server) throws PipeException {

		synchronized (PIPE_CACHE) {

			String address = server.getServerContext().getPublicAddress();

			Pipe pipe = PIPE_CACHE.get(address);

			if (pipe == null) {

				if (Silica.getGlobalConfig(Config.KEY_HOST_ADDRESS).equals(address)) {

					pipe = new DummyPipe();

				} else {

					pipe = new SecurePipe();
				}
				pipe.connect(server);
				PIPE_CACHE.put(address, pipe);
			}

			return pipe;
		}
	}
}