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
package com.silica.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.silica.rpc.server.ServerContext;
import com.silica.rpc.server.ServerSelector;

/**
 * Resource loader.
 * 
 * @param <R>
 *            Resource
 */
public abstract class ResourceLoader<R, P> {

    private static final Pattern PATH_SEP = Pattern.compile("^[\\s|\"|\']+|([\"|\']*[\\s]*[,]+[\\s]*[\"|\']*)");

    protected abstract R loadResource(P path) throws IOException;

    public R load(P path) throws IOException {
        return loadResource(path);
    }

    public static Resource[] defineResources(String paths) throws IOException {
        return defineResources(parseResourcePaths(paths));
    }

    public static Resource[] defineResources(String[] paths) throws IOException {
        if (paths == null || paths.length == 0) {
            return null;
        }
        List<Resource> resources = new ArrayList<Resource>();

        for (String path : paths) {
            if (path == null || path.isEmpty()) {
                continue;
            }
            ServerContext localSc = ServerSelector.createSelector().getLocalServer().getServerContext();
            if (localSc.isRootDirectory(path)) {

                resources.add(new Resource(path));

            } else {

                resources.add(new Resource(localSc.getResourceDirectory() + path));
            }
        }

        return resources.toArray(new Resource[resources.size()]);
    }

    protected static final String[] parseResourcePaths(String paths) {

        return PATH_SEP.split(paths);
    }
}
