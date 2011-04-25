package com.silica.rpc.pipe;

import java.util.WeakHashMap;

import com.silica.Silica;
import com.silica.rpc.server.Server;

public class PipeHolder {

	// private static final Logger log =
	// LoggerFactory.getLogger(PipeHolder.class);

	private static WeakHashMap<String, Pipe> PIPE_CACHE = new WeakHashMap<String, Pipe>();

	private static final String host;

	static {

		host = Silica.getGlobalConfig("host.address");

	}

	public static Pipe getPipe(Server server) throws PipeException {

		synchronized (PIPE_CACHE) {

			String address = server.getServerContext().getAddress();

			Pipe pipe = PIPE_CACHE.get(address);

			if (pipe == null) {

				if (host.equals(address)) {

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
