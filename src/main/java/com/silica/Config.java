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
package com.silica;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.silica.resource.ResourceLoader;

/**
 * <p>Manages configurations of Silica.</p>
 * init method must be called after create the instance.
 */
public final class Config {
    private static final Logger LOG = LoggerFactory.getLogger(Config.class);

    // --- All keys of the configuration.
    public static final String KEY_VERSION = "version";
    public static final String KEY_BASE_DIR = "base.dir";
    public static final String KEY_RESOURCE_DIR = "resource.dir";
    public static final String KEY_RESOURCE_ID = "resource.id";
    public static final String KEY_HOST_ADDRESS = "host.address";
    public static final String KEY_LISTEN_PORT1 = "listen.port.1";
    public static final String KEY_LISTEN_PORT2 = "listen.port.2";
    public static final String KEY_RMIREGISTRY_COMMAND = "rmiregistry.command";
    public static final String KEY_JAVA_HOME = "java.home";
    public static final String KEY_CLASS_PATHS = "class.paths";
    public static final String KEY_SERVICE_CLASS = "service.class";
    public static final String KEY_CHARSET = "charset";
    public static final String KEY_CLONE_PATHS = "clone.paths";
    public static final String KEY_ACTIVATION_COMMAND = "activation.command";
    public static final String KEY_DEACTIVATION_COMMAND = "deactivation.command";
    public static final String KEY_SSH_PORT = "ssh.port";
    public static final String KEY_SSH_PRIVATE_KEY_PATH = "ssh.private.key.path";
    public static final String KEY_SSH_USER = "ssh.user";
    public static final String KEY_SSH_PASS = "ssh.pass";
    public static final String KEY_SSH_TIMEOUT_MSEC = "ssh.timeout.msec";
    public static final String KEY_JOB_TIMEOUT_MSEC = "job.timeout.msec";
    public static final String KEY_KEEP_DEPLOYED_LAST = "keep.deployed.last";

    private final ResourceLoader<Map<String, String>, InputStream> resource = new ConfigLoader();
    private Map<String, String> props;

    public void init(String dir, String name) throws IOException {
        init(dir.endsWith("/") ? (dir + name) : (dir + "/" + name));
    }

    public void init(URI u) throws IOException {
        init(new FileInputStream(new File(u)));
    }

    public void init(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            throw new IOException("Path(" + path + ") doesn't exist. Current directory: " + new File(".").getAbsolutePath());
        }
        init(new FileInputStream(new File(path)));
    }

    public void init(URL u) throws IOException {
        init(u.openStream());
    }

    public void init(InputStream is) throws IOException {
        this.props = resource.load(is);
    }

    /**
     * Get a configuration from the current scheme. If not available in the scheme, get it from global scheme.
     */
    public String get(String key) {
        String value = null;
        return (props != null && (value = props.get(key)) != null) ? value : Silica.getGlobalConfig(key);
    }

    /**
     * Get a configuration from the scheme.
     */
    protected String getMine(String key) {
        return props.get(key);
    }

    protected void set(String key, String value) {
        props.put(key, value);
    }

    private static class ConfigLoader extends
            ResourceLoader<Map<String, String>, InputStream> {

        private static final Map<String, String> DEFAULT_ENTRIES = new HashMap<String, String>();
        {
            String path = new File("").getAbsolutePath();
            DEFAULT_ENTRIES.put("root.dir", path + "/");
            DEFAULT_ENTRIES.put("base.dir", path + "/");
        }

        @Override
        protected Map<String, String> loadResource(InputStream in)
                throws IOException {

            Properties tmp = new Properties();
            tmp.load(in);
            Map<String, String> map = new HashMap<String, String>();

            map.putAll(DEFAULT_ENTRIES);
            for (Entry<Object, Object> entry : tmp.entrySet()) {
                map.put((String) entry.getKey(), parse((String) entry.getValue(), map));
            }

            return map;
        }

        private String parse(String value, Map<String, String> prop) {

            int s = -1;
            boolean x = false;
            int i = -1;
            StringBuilder sb = new StringBuilder();
            for (char c : value.toCharArray()) {
                ++i;
                if (c == '\\')
                    continue;
                if (c == '$')
                    s = i;
                if (s >= 0 && s == i - 1 && c == '{')
                    x = true;
                if (x && c == '}') {
                    String k = value.substring(s + 2, i);
                    String v = prop.get(k);
                    if (v == null || v.equals("")) {
                        v = DEFAULT_ENTRIES.get(k);
                    }
                    if (v == null || v.equals("")) {
                        v = System.getProperty(k);
                    }
                    if (v == null) {
                        LOG.warn("Property [{}] is missing. It is supporsed to be used by {}", k, value);
                    } else {
                        sb.append(v);
                    }

                    s = -1;
                    x = false;

                } else if (s == -1)
                    sb.append(c);
            }
            return sb.toString();
        }
    };
}
