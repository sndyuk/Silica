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
package com.silica.job;

import java.io.Serializable;

/**
 * <p>
 * Job
 * </p>
 * メンバ変数はシリアライズ可能にする必要がある
 * 
 * @param <R>
 *            Jobの実行結果
 */
public interface Job<R extends Serializable> extends Serializable {

	/**
	 * <p>
	 * Jobを実行する
	 * </p>
	 * 
	 * @return Jobの実行結果
	 * @throws JobException
	 *             Jobの実行に失敗
	 */
	R execute() throws JobException;
}
