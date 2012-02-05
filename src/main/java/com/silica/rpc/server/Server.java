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

import java.io.Serializable;

import com.silica.job.Job;
import com.silica.service.Service;
import com.silica.service.ServiceException;

/**
 * サービスを実行する媒体
 * 
 * @author sndyuk
 */
public interface Server {

	/**
	 * サーバを動かす
	 * 
	 * @throws ServerException
	 *             サーバへの接続に失敗
	 */
	public void activate() throws ServerException;

	/**
	 * サーバを停止する
	 * 
	 * @throws ServerException
	 *             サーバに繋がらない、また切断に失敗
	 */
	public void disactivate() throws ServerException;
	
	/**
	 * サーバが動いてるか
	 * 
	 * @return true = 接続, false= 未接続
	 */
	public boolean isActive();

	/**
	 * サービスをアンバインドする
	 * 
	 * @param clazz
	 *            アンバインド対象のサービス
	 * @throws ServerException
	 *             アンバインド失敗
	 */
	public void unbind(Class<? extends Service> clazz) throws ServerException;

	/**
	 * サービスをバインドする
	 * 
	 * @param service
	 *            バインドするサービス
	 * @throws ServerException
	 *             バインドに失敗
	 * @throws ServiceException
	 *             不正なサービスをバインドしようとした
	 */
	public void bind(Service service) throws ServerException,
			ServiceException;

	/**
	 * サーバのコンテクストを取得する
	 * 
	 * @return サーバのコンテクスト
	 */
	public ServerContext getServerContext();

	public void cleanOldModules(boolean wait);
	
	/**
	 * Jobを実行する
	 * 
	 * @param <R>
	 *            Jobの実行結果
	 * @param clazz
	 *            サービスのクラス
	 * @param job
	 *            実行対象のJob
	 * @return Jobの実行結果
	 * @throws ServiceException
	 *             実行失敗
	 */
	public <R extends Serializable> R execute(Class<? extends Service> clazz, Job<R> job)
			throws ServiceException;
}
