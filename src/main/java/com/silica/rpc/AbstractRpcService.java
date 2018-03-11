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
package com.silica.rpc;

import java.io.File;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.silica.resource.Resource;
import com.silica.service.Service;
import com.silica.service.ServiceException;

public abstract class AbstractRpcService implements Service {

    private static final long serialVersionUID = 5310157636970723507L;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRpcService.class);

    protected AbstractRpcService() {
    }

    @Override
    public void deployResources(String destdir, Resource... resources) throws ServiceException {

        try {
            for (Resource resource : resources) {

                File rf = new File(resource.getName());
                String destpath = Paths.get(destdir, rf.getName()).toString();

                LOG.debug("destpath: {}", destpath);

                resource.writer().write(destpath);
            }
        } catch (Exception e) {
            for (Resource resource : resources) {
                File f = new File(resource.getName());
                if (!f.delete()) {
                    LOG.error("Could not rollback: deleting file: {}", resource.getName());
                }
            }
            throw new ServiceException("There is an error when deploying resources", e);
        }
    }
}
