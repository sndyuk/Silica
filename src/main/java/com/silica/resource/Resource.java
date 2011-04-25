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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * リソース
 */
public class Resource implements Serializable, Closeable {

	private static final long serialVersionUID = 3864307789062600099L;
	private static final Logger log = LoggerFactory.getLogger(Resource.class);

	private boolean closed;
	private String path;
	private String name;
	private byte[] buf;

	/**
	 * <p>
	 * デシリアライザ用コンストラクタ
	 * </p>
	 */
	@Deprecated
	public Resource() {
	}

	/**
	 * <p>
	 * コンストラクタ
	 * </p>
	 * @param path
	 *            リソースパス
	 */
	public Resource(String path) {
		this.path = path;

		int p = 0;
		p = (p = path.lastIndexOf("/")) > 0 ? p : path.lastIndexOf("\\");
		if (p > 0) {

			name = path.substring(p + 1, path.length());
		}
	}

	/**
	 * <p>
	 * リソースパスを取得する
	 * </p>
	 * @return リソースパス
	 */
	public String getPath() {

		return path;
	}

	/**
	 * <p>
	 * リソース名を取得する
	 * </p>
	 * @return リソース名
	 */
	public String getName() {

		return name;
	}

	/**
	 * <p>
	 * リソースのバイト配列を取得する
	 * </p>
	 * @return リソースのバイト配列
	 */
	public byte[] getData() {
		return buf;
	}

	private void ensureOpen() throws IOException {
		if (closed) {
			throw new IOException("The resource has been closed.");
		}
	}

	/**
	 * <p>
	 * リソースをメモリ上のバイト配列に展開する
	 * </p>
	 * @throws IOException
	 *             リソース読込みに失敗
	 */
	public void cacheOnMemory() throws IOException {

		synchronized (this) {
			ensureOpen();

			try {

				this.buf = rl.load(path);

			} catch (IOException e) {
				log.warn("Could not load resource {}", path);
				throw e;
			}
		}
	}

	private transient ResourceLoader<byte[], byte[]> rl = new ResourceLoader<byte[], byte[]>() {

		@Override
		protected byte[] defineResource(byte[] resources) throws IOException {
			return resources;
		}

		@Override
		protected byte[] loadResource(String path) throws IOException {

			int bs = 8192;

			ByteArrayOutputStream out = null;
			InputStream in = null;

			try {
				out = new ByteArrayOutputStream();
				in = new BufferedInputStream(
						new FileInputStream(new File(path)), bs);

				byte[] buf = new byte[bs];
				for (int pos = 0; (pos = in.read(buf)) != -1;) {
					out.write(buf, 0, pos);
				}
				out.flush();

			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} finally {
					if (out != null) {
						out.close();
					}
				}
			}
			return out.toByteArray();
		}
	};

	/**
	 * <p>
	 * リソースをクローズする
	 * </p>
	 * かならず呼ぶ必要がある
	 * 
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		synchronized (this) {

			closed = true;
			buf = null;
		}
	}
}
