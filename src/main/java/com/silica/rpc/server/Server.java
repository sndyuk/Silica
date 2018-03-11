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
package com.silica.rpc.server;

import java.io.Serializable;

import com.silica.job.Job;
import com.silica.service.Service;
import com.silica.service.ServiceException;

public interface Server {

    public void activate() throws ServerException;

    public void disactivate() throws ServerException;

    public boolean isActive();

    public void unbind(Class<? extends Service> clazz) throws ServerException;

    public void bind(Service service) throws ServerException, ServiceException;

    public ServerContext getServerContext();

    public void cleanOldModules(boolean wait);

    public <R extends Serializable> R execute(Class<? extends Service> clazz, Job<R> job)
            throws ServiceException;
}
