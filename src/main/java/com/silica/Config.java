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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.silica.resource.ResourceLoader;

/**
 * 各種構成変数に対する値を一括管理・保持する
 * インスタンス生成後、initメソッドを呼び出すこと
 */
public class Config {

	/**
	 * リソースローダ
	 */
	private ResourceLoader<Map<String, String>, Properties> resource;
	
	/**
	 * 現在の構成変数に対する値
	 */
	private Map<String, String> props;

	/**
	 * 構成を読込む
	 * 
	 * @param dir ディレクトリパス
	 * @param name ファイル名
	 * @throws IOException ファイルの読込みに失敗した
	 */
	public void init(String dir, String name) throws IOException {
		init(dir.endsWith(File.separator) ? (dir + name) : (dir
				+ File.separator + name));
	}

	/**
	 * 構成を読込む
	 * 
	 * @param path ファイルパス
	 * @throws IOException ファイルの読込みに失敗した
	 */
	public void init(String path) throws IOException {

		this.resource = new ResourceLoader<Map<String, String>, Properties>() {

			@Override
			public Map<String, String> defineResource(
					Properties tmp) throws IOException {

				Map<String, String> map = new HashMap<String, String>();

				for (Entry<Object, Object> entry : tmp.entrySet()) {
					map.put((String) entry.getKey(), (String) entry.getValue());
				}

				return map;
			}

			@Override
			protected Properties loadResource(String path) throws IOException {
				File f = new File(path);

				Properties tmp = new Properties();
				InputStream in = null;
				try {
					in = new FileInputStream(f);
					tmp.load(in);
				} finally {
					if (in != null) {
						in.close();
					}
				}
				return tmp;
			}
		};

		this.props = resource.load(path);
	}

	/**
	 * 変数に対する構成を個別スキームから取得、無い場合、共通スキームから取得する
	 * 
	 * @param key 変数名
	 * @return 構成値
	 */
	public String get(String key) {
		String value = null;
		return (props != null && (value = props.get(key)) != null) ? value
				: Silica.getGlobalConfig(key);
	}

	/**
	 * 変数に対する構成を個別スキームから取得する
	 * 
	 * @param key 変数名
	 * @return 構成値
	 */
	protected String getMine(String key) {
		return props.get(key);
	}
//
//	protected void setMine(String key, String value) {
//		props.put(key, value);
//	}

	/**
	 * 構成を再読込みする
	 * 
	 * @throws IOException ファイルの読込みに失敗した
	 */
	protected void reload() throws IOException {
		resource.reload();
	}
}
