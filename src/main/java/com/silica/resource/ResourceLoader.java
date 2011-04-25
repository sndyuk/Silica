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
package com.silica.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.silica.Silica;

public abstract class ResourceLoader<T, G> {

	private String path;

	protected abstract T defineResource(G props) throws IOException;

	protected abstract G loadResource(String path) throws IOException;

	public T load(String path) throws IOException {
		this.path = path;
		return reload();
	}

	public T reload() throws IOException {
		G props = loadResource(path);
		return defineResource(props);
	}

	public static Resource[] getResources(String paths) throws IOException {

		if (paths == null || paths.length() == 0) {
			return null;
		}

		ResourceLoader<Resource[], String[]> rl = new ResourceLoader<Resource[], String[]>() {

			@Override
			protected Resource[] defineResource(String[] paths)
					throws IOException {

				List<Resource> resources = new ArrayList<Resource>();

				for (String path : paths) {
					if (path == null) {
						continue;
					}
					resources
							.add(new Resource(Silica.getBaseDirectry() + path));
				}

				return (Resource[]) resources.toArray(new Resource[resources
						.size()]);
			}

			@Override
			protected String[] loadResource(String resourcepath)
					throws IOException {
				return parseResourcePaths(resourcepath);
			}
		};

		return rl.load(paths);
	}

	protected static final String[] parseResourcePaths(String paths) {
		return paths.split("^[\\s|\"|\']+|([\"|\']*[\\s]*[,]+[\\s]*[\"|\']*)");
	}
}
