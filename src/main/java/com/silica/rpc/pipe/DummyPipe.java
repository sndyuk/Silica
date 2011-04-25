package com.silica.rpc.pipe;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.silica.resource.Resource;
import com.silica.resource.ResourceWriter;
import com.silica.rpc.server.Server;

public class DummyPipe extends Pipe {

	private static final Logger log = LoggerFactory.getLogger(DummyPipe.class);

	public DummyPipe() {
		log.debug("created dummy pipe for localhost.");
	}

	@Override
	protected void connect(Server server) throws PipeException {

		// nop :no need to connect.
	}

	@Override
	public void disconnect() {

		// nop : no need to disconnect.
	}

	@Override
	public boolean isConnected() {

		return true;
	}

	@Override
	public void put(String dest, Resource resource) throws PipeException {

		File df = new File(dest);
		if (!df.exists() && !df.mkdirs()) {

			throw new PipeException(MessageFormat.format(
					"Could not make directry [{0}].", dest));
		}

		try {

			resource.cacheOnMemory();
			ResourceWriter writer = new ResourceWriter(resource);
			writer.publish(new File(df, resource.getName()).getAbsolutePath());

		} catch (IOException e) {

			throw new PipeException(MessageFormat.format(
					"Could not put resource [{0}].", resource.getName()), e);
		}
	}

	@Override
	public void remove(Resource... resources) throws PipeException {

		for (Resource resource : resources) {

			File file = new File(resource.getName());

			if (file.exists() && !file.delete()) {

				throw new PipeException(MessageFormat.format(
						"Could not remove file [{0}]", file.getAbsoluteFile()));
			}
		}
	}

	@Override
	public void execute(String command) throws PipeException {

		Future<Void> f = null;
		ExecutorService executor = Executors.newSingleThreadExecutor();
		final String[] c = command.split(" ");
		Callable<Void> quit = new Callable<Void>() {

			@Override
			public Void call() throws Exception {

				ProcessBuilder pb = new ProcessBuilder().command(c);
				pb.start();
				return null;
			}
		};
		try {

			f = executor.submit(quit);
			f.get(30, TimeUnit.SECONDS);
			
		} catch (Exception e) {
			if (f != null) {
				f.cancel(false);
			}
			// try {
			// f = executor.submit(quit);
			// f.get(30, TimeUnit.SECONDS);
			//
			// } catch (Exception e1) {
			// if (f != null) {
			// f.cancel(false);
			// }
			throw new PipeException(MessageFormat.format(
					"Could not execute the command [{0}]", command), e);
			// }
		}
	}
}
