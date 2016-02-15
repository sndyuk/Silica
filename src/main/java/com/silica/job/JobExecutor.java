/**
 *    Copyright (C) 2011-2016 sndyuk
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
import java.rmi.RemoteException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import com.silica.Silica;
import com.silica.rpc.ProxyService;
import com.silica.service.Service;
import com.silica.service.ServiceException;

/**
 * <p>
 * Jobを実行する
 * </p> {@link ExecutorService}を利用し実行する
 * 
 * @see java.util.concurrent.Callable
 * 
 * @param <R>
 *            Job実行結果
 */
public class JobExecutor<R extends Serializable> implements Callable<R>,
        Serializable {

    private static final long serialVersionUID = -5579620009129572616L;

    private final Job<R> job;

    /**
     * コンストラクタ
     * 
     * @param job
     *            実行対象のJob
     */
    public JobExecutor(Job<R> job) {
        this.job = job;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Callable#call()
     */
    @Override
    public R call() throws ServiceException {

        try {

            Service service = new ProxyService(Silica.getServiceClass());
            return service.execute(job);

        } catch (RemoteException e) {

            throw new ServiceException("", e);
        }
    }
}
