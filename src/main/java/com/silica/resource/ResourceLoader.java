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
 * リソースローダー
 * 
 * @param <T>
 *            読込後の型
 */
public abstract class ResourceLoader<T, A> {

    private static final Pattern PATH_SEP = Pattern.compile("^[\\s|\"|\']+|([\"|\']*[\\s]*[,]+[\\s]*[\"|\']*)");

    private A path;

    protected abstract T loadResource(A path) throws IOException;

    /**
     * @param path
     *            リソースのパス
     * @return 読込まれたリソース
     * @throws IOException
     *             リソースを読めなかった
     */
    public T load(A path) throws IOException {
        this.path = path;
        return reload();
    }

    /**
     * 再度リソースを読込む
     * 
     * @return 再読込みされたリソース
     * @throws IOException
     *             リソースを読めなかった
     */
    public T reload() throws IOException {

        return loadResource(path);
    }

    /**
     * リソースの配列を取得する
     * 
     * @param paths
     *            (カンマ区切りされた)複数のリソースパス
     * @return リソースの配列
     * @throws IOException
     *             リソースを読めなかった
     */
    public static Resource[] getResources(String paths) throws IOException {

        if (paths == null || paths.length() == 0) {
            return null;
        }

        String[] arr = parseResourcePaths(paths);
        List<Resource> resources = new ArrayList<Resource>();

        for (String path : arr) {
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
