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
package com.silica.service;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import com.silica.job.Job;
import com.silica.resource.Resource;

/**
 * サービス
 * Jobを実行するために使用する
 * Jobはサービスを利用しない限り実行できない
 */
public interface Service extends Remote, Serializable {
	
	/**
	 * Jobを実行するために必要なリソース設定する
	 * リソースは指定された出力先に転送される
	 * 
	 * @param destdir 出力先
	 * @param resources リソース
	 * @throws ServiceException 出力失敗
	 */
	public void setResources(String destdir, Resource... resources) throws RemoteException;
	
	/**
	 * 指定されたJobがサービスにより実行される
	 * 
	 * @param <R> Jobの実行結果
	 * @param job 実行したいJob
	 * @return Jobの実行結果
	 * @throws RemoteException Jobの実行に失敗
	 */
	public<R extends Serializable> R execute(Job<R> job) throws RemoteException;
}
