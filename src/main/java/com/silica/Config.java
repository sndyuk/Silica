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
package com.silica;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.silica.resource.ResourceLoader;

/**
 * 各種構成変数に対する値を一括管理・保持する インスタンス生成後、initメソッドを呼び出すこと
 */
public final class Config {

	public static final String KEY_VERSION = "version";
	public static final String KEY_BASE_DIR = "base.dir";
	public static final String KEY_RESOURCE_DIR = "resource.dir";
	public static final String KEY_RESOURCE_ID = "resource.id";
	public static final String KEY_HOST_ADDRESS = "host.address";
	public static final String KEY_LISTEN_PORT1 = "listen.port.1";
	public static final String KEY_LISTEN_PORT2 = "listen.port.2";
	public static final String KEY_JAVA_HOME = "java.home";
	public static final String KEY_CLASS_PATHS = "class.paths";
	public static final String KEY_SERVICE_CLASS = "service.class";
	public static final String KEY_CHARSET = "charset";
	public static final String KEY_CLONE_PATHS = "clone.paths";
	public static final String KEY_ACTIVATION_COMMAND = "activation.command";
	public static final String KEY_DEACTIVATION_COMMAND = "deactivation.command";
	public static final String KEY_SSH_PORT = "ssh.port";
	public static final String KEY_SSH_PRIVATE_KEY_PATH = "ssh.private.key.path";
	public static final String KEY_SSH_USER = "ssh.user";
	public static final String KEY_SSH_PASS = "ssh.pass";
	public static final String KEY_SSH_TIMEOUT_MSEC = "ssh.timeout.msec";
	public static final String KEY_JOB_TIMEOUT_MSEC = "job.timeout.msec";
	public static final String KEY_KEEP_DEPLOYED_LAST = "keep.deployed.last";
	
	/**
	 * リソースローダ
	 */
	private final ResourceLoader<Map<String, String>, InputStream> resource = new ConfigLoader();
	
	/**
	 * 現在の構成変数に対する値
	 */
	private Map<String, String> props;

	/**
	 * 構成を読込む
	 * 
	 * @param dir
	 *            ディレクトリパス
	 * @param name
	 *            リソース名
	 * @throws IOException
	 *             リソースの読込みに失敗した
	 */
	public void init(String dir, String name) throws IOException {
		init(dir.endsWith("/") ? (dir + name) : (dir + "/" + name));
	}

	/**
	 * 構成を読込む
	 * 
	 * @param u
	 *            URI
	 * @throws IOException
	 *             リソースの読込みに失敗した
	 */
	public void init(URI u) throws IOException {
		this.props = resource.load(new FileInputStream(new File(u)));
	}

	/**
	 * 構成を読込む
	 * 
	 * @param path
	 *            リソースパス
	 * @throws IOException
	 *             リソースの読込みに失敗した
	 */
	public void init(String path) throws IOException {
		this.props = resource.load(new FileInputStream(new File(path)));
	}

	/**
	 * 構成を読込む
	 * 
	 * @param path
	 *            リソースパス
	 * @throws IOException
	 *             リソースの読込みに失敗した
	 */
	public void init(URL u) throws IOException {
		if (u == null) {
			throw new IllegalArgumentException();
		}
		this.props = resource.load(u.openStream());
	}

	/**
	 * 変数に対する構成を個別スキームから取得、無い場合、共通スキームから取得する
	 * 
	 * @param key
	 *            変数名
	 * @return 構成値
	 */
	public String get(String key) {
		String value = null;
		return (props != null && (value = props.get(key)) != null) ? value : Silica.getGlobalConfig(key);
	}

	/**
	 * 変数に対する構成を個別スキームから取得する
	 * 
	 * @param key
	 *            変数名
	 * @return 構成値
	 */
	protected String getMine(String key) {
		return props.get(key);
	}

	
	 protected void set(String key, String value) {
	 props.put(key, value);
	 }

	private static class ConfigLoader extends
			ResourceLoader<Map<String, String>, InputStream> {

		private static final Map<String, String> DEFAULT_ENTRIES = new HashMap<String, String>();
		{
			String path = new File("").getAbsolutePath();
			DEFAULT_ENTRIES.put("base.dir", path + "/");
		}

		
		@Override
		protected Map<String, String> loadResource(InputStream in)
				throws IOException {

			Properties tmp = new Properties();

			try {
				tmp.load(in);
			} finally {
				if (in != null) {
					in.close();
				}
			}

			Map<String, String> map = new HashMap<String, String>();

			map.putAll(DEFAULT_ENTRIES);
			for (Entry<Object, Object> entry : tmp.entrySet()) {
				map.put((String) entry.getKey(), parse((String) entry.getValue(), tmp));
			}
			
			return map;
		}
		
		private String parse(String value, Properties prop) {
			
			int s = -1;
			boolean x = false;
			int i = -1;
			StringBuilder sb = new StringBuilder();
			for (char c : value.toCharArray()) {
				++i;
				if (c == '\\') continue;
				if (c == '$') s = i;
				if (s >= 0 && s == i - 1 && c == '{') x = true;
				if (x && c == '}') {
					String k = value.substring(s + 2, i);
					String v = prop.getProperty(k);
					if (v == null) v = DEFAULT_ENTRIES.get(k); 
					if (v == null) throw new IllegalArgumentException("Property [{" + k + "}] is missing."); 
					sb.append(v);
					
					s = -1;
					x = false;
					
				} else if (s == -1) sb.append(c);
			}
			return sb.toString();
		}
	};
}
