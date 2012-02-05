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

import java.util.List;

import com.silica.service.Service;

/**
 * サーバ選択ロジック
 * 
 * @author sndyuk
 */
public interface SelectLogic {

	/**
	 * サービスを実行するサーバを選択する
	 * 
	 * @param service
	 *            サービス
	 * @param activeServers
	 *            有効なサーバ
	 * @return サーバ
	 */
	public Server select(Service service, List<Server> activeServers);
}