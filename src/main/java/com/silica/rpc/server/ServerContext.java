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

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.silica.Config;
import com.silica.Silica;
import com.silica.service.Service;

/**
 * Server context
 */
public class ServerContext {

    private static final Logger LOG = LoggerFactory.getLogger(ServerContext.class);

    private static final String PATH_SEP_UNIX = ":";
    private static final String PATH_SEP_WIN = ";";

    private final boolean remote;
    private final String publicAddress;
    private final String internalAddress;
    private final int listenPortRmiServer;
    private final int listenPortRmi;
    private final String rmiregistryCommand;
    private final Config conf;
    private final String javaHome;
    private final String basedir;
    private final String resourcedir;
    private final List<String> classPaths;
    private final String osClassPathsep;
    private final String charset;
    private final String activationCommand;
    private final String deactivationCommand;
    private final int sshPort;
    private final String sshPrivateKeyPath;
    private final String sshPass;
    private final String sshUser;
    private final int sshTimeout;

    private boolean enable;

    public ServerContext(String publicAddress) throws IOException {

        this.conf = new Config();
        this.conf.init(Silica.getResourceDirectory() + "configs", "server." + publicAddress + ".properties");

        String hostAddr = Silica.getGlobalConfig(Config.KEY_HOST_ADDRESS);
        if (isLocalHost(hostAddr))
            hostAddr = "localhost";

        this.remote = !hostAddr.equals(publicAddress);

        String _basedir = conf.get(Config.KEY_BASE_DIR);
        this.basedir = _basedir.endsWith("/") ? _basedir : _basedir + "/";
        this.resourcedir = remote ? basedir + ".res/" + Silica.getResourceID() + "/" : Silica.getBaseDirectory();
        this.publicAddress = publicAddress;
        this.internalAddress = InetAddress.getByName(publicAddress).getHostName();
        String listenPortRmiServer = conf.get(Config.KEY_LISTEN_PORT1);
        if (listenPortRmiServer == null || listenPortRmiServer.equals("")) {
        	listenPortRmiServer = "0";
        }
        this.listenPortRmiServer = Integer.parseInt(listenPortRmiServer);

        String listenPortRmi = conf.get(Config.KEY_LISTEN_PORT2);
        if (listenPortRmi == null || listenPortRmi.equals("")) {
        	listenPortRmi = "0";
        }
        this.listenPortRmi = Integer.parseInt(listenPortRmi);
        this.rmiregistryCommand = conf.get(Config.KEY_RMIREGISTRY_COMMAND);
        String javaHome = conf.get(Config.KEY_JAVA_HOME);
        if (javaHome == null || javaHome.equals("")) {
            this.javaHome = System.getProperty("java.home");
        } else {
            this.javaHome = javaHome;
        }
        this.osClassPathsep = basedir.charAt(0) == '/' ? PATH_SEP_UNIX : PATH_SEP_WIN;
        this.classPaths = new ArrayList<String>();
        String[] s = conf.get(Config.KEY_CLASS_PATHS).split(",");
        for (int i = 0; i < s.length; i++) {
            addClassPath(resourcedir + s[i].trim());
        }
        s = Silica.getGlobalConfig(Config.KEY_CLONE_PATHS).split(",");
        for (int i = 0; i < s.length; i++) {
            addClassPath(resourcedir + s[i].trim());
        }
        String _charset = conf.get(Config.KEY_CHARSET);
        if (_charset == null || _charset.length() == 0) {
            if (this.osClassPathsep.equals(PATH_SEP_UNIX)) {
                charset = "utf-8";
            } else {
                charset = "windows-31j";
            }
        } else {
            charset = _charset;
        }
        this.activationCommand = conf.get(Config.KEY_ACTIVATION_COMMAND);
        this.deactivationCommand = conf.get(Config.KEY_DEACTIVATION_COMMAND);
        String sshPortStr = conf.get(Config.KEY_SSH_PORT);
        if (sshPortStr == null || sshPortStr.length() == 0) {
            this.sshPort = 22;
        } else {
            this.sshPort = Integer.parseInt(sshPortStr);
        }
        this.sshPrivateKeyPath = conf.get(Config.KEY_SSH_PRIVATE_KEY_PATH);
        this.sshPass = conf.get(Config.KEY_SSH_PASS);
        this.sshUser = conf.get(Config.KEY_SSH_USER);
        String sshTimeoutMsecStr = conf.get(Config.KEY_SSH_TIMEOUT_MSEC);
        if (sshTimeoutMsecStr == null || sshTimeoutMsecStr.length() == 0) {
            this.sshTimeout = 60000 * 60;
        } else {
            this.sshTimeout = Integer.parseInt(sshTimeoutMsecStr);
        }
    }

    protected String getProperty(String key) {
        return conf.get(key);
    }

    public String getPublicAddress() {
        return publicAddress;
    }

    public String getInternalAddress() {
        return internalAddress;
    }

    public int getListenPortRmiServer() {
        return listenPortRmiServer;
    }

    public int getListenPortRmi() {
        return listenPortRmi;
    }
    public boolean isAutoStartRmiregistry() {
        return rmiregistryCommand != null && !rmiregistryCommand.equals("");
    }
    public String getRmiregistryCommand() {
        return rmiregistryCommand;
    }

    public String getJavaHome() {
        return javaHome;
    }

    public String getBasedir() {
        return basedir;
    }

    public String getCharset() {
        return charset;
    }

    public String[] getClassPaths() {

        return classPaths.toArray(new String[classPaths.size()]);
    }

    public String getClassPathString() {

        return makeClassPathString(getClassPaths());
    }

    private String makeClassPathString(String[] paths) {

        StringBuilder sb = new StringBuilder();
        for (String path : paths) {
            sb.append(path);
            sb.append(osClassPathsep);
        }
        return sb.toString();
    }

    public String getResourceDirectory() {
        return resourcedir;
    }

    public Service getService() throws ServerException {

        Class<? extends Service> serviceClass = Silica.getServiceClass();

        try {

            return serviceClass.newInstance();

        } catch (Exception e) {
            LOG.error("Could not define the Service [{}].", serviceClass.getName());
            throw new ServerException("Could not define the Service.", e);
        }
    }

    protected void addClassPath(String path) {
        if (path == null || path.length() == 0) {
            return;
        }
        if (path.endsWith("jar")) {

            classPaths.add(path);

        } else {

            int p = path.lastIndexOf('/');
            if (p > 0 && path.lastIndexOf('.') > p) {
                path = path.substring(0, p);
            }
            if (classPaths.contains(path)) {
                return;
            }
            classPaths.add(path);
        }
    }

    public boolean isRemote() {
        return remote;
    }

    public String getActivationCommand() {
        return activationCommand;
    }

    public String getDeactivationCommand() {
        return deactivationCommand;
    }

    public int getSshPort() {
        return sshPort;
    }

    public String getSshPrivateKeyPath() {
        return sshPrivateKeyPath;
    }

    public String getSshPass() {
        return sshPass;
    }

    public String getSshUser() {
        return sshUser;
    }

    public int getSshTimeout() {
        return sshTimeout;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isRootDirectory(String path) {
        if (osClassPathsep.equals(PATH_SEP_UNIX)) {
            return path.charAt(0) == '/';
        } else if (path.length() >= 1) {
            return path.charAt(1) == ':';
        }
        return false;
    }

    private static boolean isLocalHost(String address) {
        return "localhost".equalsIgnoreCase(address)
                || "127.0.0.1".equals(address)
                || "127.0.1.1".equals(address);
    }
}
