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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * リソース
 */
public class Resource implements Serializable, Closeable {

	private static final long serialVersionUID = 3864307789062600099L;
	private static final Logger LOG = LoggerFactory.getLogger(Resource.class);
	
	private boolean closed;
	private final String path;
	private final String name;
	private final int permission;

	/**
	 * <p>
	 * デシリアライザ用コンストラクタ
	 * </p>
	 * 
	 * @deprecated デシリアライザ用
	 */
	public Resource() {
		this(null);
	}

	/**
	 * <p>
	 * コンストラクタ
	 * </p>
	 * 
	 * @param path
	 *            リソースパス
	 */
	public Resource(String path) {
		this(path, getName(path));
	}

	/**
	 * <p>
	 * コンストラクタ
	 * </p>
	 * 
	 * @param path
	 *            リソースパス
	 * @param as
	 *            別名
	 */
	public Resource(String path, String as) {
		this.path = path;
		this.name = as;
		
		File f = new File(path);
		int p = 0;
		if (f.canRead()) p += 4;
		if (f.canWrite()) p += 2;
		if (f.canExecute()) p += 1;
		permission = p;
	}
	
	private static String getName(String path) {
		if (path == null) {
			return null;
		}
		int p = 0;
		p = (p = path.lastIndexOf("/")) > 0 ? p : path.lastIndexOf("\\");
		String name = null;
		if (p > 0) {
			name = path.substring(p + 1, path.length());
		}
		return name;
	}
	
	/**
	 * <p>
	 * リソースパスを取得する
	 * </p>
	 * 
	 * @return リソースパス
	 */
	public String getPath() {

		return path;
	}

	/**
	 * <p>
	 * リソース名を取得する
	 * </p>
	 * 
	 * @return リソース名
	 */
	public String getName() {

		return name;
	}

	/**
	 * <p>
	 * FileInputStremを取得する
	 * </p>
	 * 
	 * @return リソースのFileInputStrem, リソースがない場合は、null
	 */
	public FileInputStream getData() {
		return getInputStream();
	}

	private void ensureOpen() throws IOException {
		if (closed) {
			throw new IOException("The resource has been closed.");
		}
	}

	private FileInputStream getInputStream() {

		synchronized (this) {
			try {

				ensureOpen();

				return rl.load(path);

			} catch (IOException e) {
				LOG.warn("Could not load resource {}", path);
				close();
			}
		}
		return null;
	}

	private transient ResourceLoader<FileInputStream, String> rl = new ResourceLoader<FileInputStream, String>() {

		@Override
		protected FileInputStream loadResource(String path) throws IOException {
			return new FileInputStream(new File(path));
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
	public void close() {
		synchronized (this) {

			closed = true;
		}
	}
	
	public boolean canRead() {
		return permission >> 2 == 1;
	}

	public boolean canWrite() {
		return permission >> 1 == 1;
	}

	public boolean canExecute() {
		return permission == 1;
	}
	
	public int getpermissions() {
		return permission << 6 | permission << 3 | 04;
	}
}
