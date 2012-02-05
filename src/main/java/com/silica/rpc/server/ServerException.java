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
package com.silica.rpc.server;

import java.io.IOException;

/**
 * サーバ上で起こる例外
 * 
 * @author sndyuk
 */
public class ServerException extends IOException {

	private static final long serialVersionUID = -8698214427617984871L;

	/**
	 * コンストラクタ
	 * 
	 * @param s エラーの詳細
	 * @param e 原因
	 */
	public ServerException(String s, Exception e) {
		super(s, e);
	}
	
	/**
	 * コンストラクタ
	 * 
	 * @param s エラーの詳細
	 */
	public ServerException(String s) {
		super(s);
	}
	
	/**
	 * コンストラクタ
	 * 
	 * @param e 原因
	 */
	public ServerException(Exception e) {
		super(e);
	}
}
