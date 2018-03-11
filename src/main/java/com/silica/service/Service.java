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
package com.silica.service;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import com.silica.job.Job;
import com.silica.resource.Resource;

/**
 * A service runs a job.
 */
public interface Service extends Remote, Serializable {

    /**
     * Deploy resources to the destination directory.
     */
    public void deployResources(String destinationDirectoryOnTheTargetServer, Resource... resources) throws RemoteException;

    public <R extends Serializable> R execute(Job<R> job) throws RemoteException;
}
