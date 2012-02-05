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
package com.silica.resource;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.MessageFormat;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * リソースライター
 */
public class ResourceWriter implements Closeable {

	private static ExecutorService FIXED_WRITER_THREAD = Executors.newFixedThreadPool(2);
	
	private final Resource resource;

	public ResourceWriter(Resource resource) {
		this.resource = resource;
	}

	public void publish(String path) throws IOException {

		final File d = new File(path);

		if (d.exists()) {
			if (!d.delete()) {

				throw new IOException(MessageFormat.format(
						"Could not delete old file [{0}].", path));
			}
		} else {
			File p = d.getParentFile();
			
			if (!p.exists() && !p.mkdirs()) {
				
				throw new IOException(MessageFormat.format(
						"Could not create new dir [{0}].", p.getAbsolutePath()));
			}
		}
		if (!d.createNewFile()) {

			throw new IOException(MessageFormat.format(
					"Could not create new file [{0}].", path));
		}
		if (!d.setReadable(resource.canRead())) {
			throw new IOException(MessageFormat.format(
					"Could not change parmission readable [{0}].", path));
		}
		if (!d.setWritable(resource.canWrite())) {
			throw new IOException(MessageFormat.format(
					"Could not change parmission writable [{0}].", path));
		}
		if (!d.setExecutable(resource.canExecute())) {
			throw new IOException(MessageFormat.format(
					"Could not change parmission executable [{0}].", path));
		}
		
		Callable<Void> writer = new Callable<Void>() {

			@Override
			public Void call() throws Exception {

				FileChannel srcChannel = resource.getData().getChannel();
				FileChannel destChannel = new FileOutputStream(d).getChannel();
				try {
					srcChannel.transferTo(0, srcChannel.size(), destChannel);
				} finally {
					try {
						destChannel.close();
					} finally {
						srcChannel.close();
					}
				}
				return null;
			}
		};

		Future<Void> future = null;
		synchronized (FIXED_WRITER_THREAD) {
			future = FIXED_WRITER_THREAD.submit(writer);
		}
		if (future != null) {
			try {
				try {
					future.get(30000, TimeUnit.MILLISECONDS);
				} catch (TimeoutException e) {
					throw e;
				} catch (InterruptedException e) {
					throw e;
				} catch (ExecutionException e) {
					throw e;
				}
			} catch (Throwable e) {
				future.cancel(true);
				throw new IOException("Could not write the resource.", e);
			}
		}
	}

	@Override
	public void close() throws IOException {
		if (resource != null) {
			resource.close();
		}
	}
}
