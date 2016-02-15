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
import com.silica.service.ServiceException;

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
    private final int listenPort1;
    private final int listenPort2;
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

    /**
     * コンストラクタ
     * 
     * @param publicAddress
     *            サーバのIPアドレス
     * @throws IOException
     *             コンテクストの初期化に失敗
     */
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
        this.listenPort1 = Integer.parseInt(conf.get(Config.KEY_LISTEN_PORT1));
        this.listenPort2 = Integer.parseInt(conf.get(Config.KEY_LISTEN_PORT2));
        this.javaHome = conf.get(Config.KEY_JAVA_HOME);
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

    /**
     * サーバのプロパティを返す
     * 
     * @param key
     *            キー
     * @return 値が無ければ、null
     */
    protected String getProperty(String key) {
        return conf.get(key);
    }

    /**
     * サーバのアドレス(public)を返す
     * 
     * @return サーバのアドレス
     */
    public String getPublicAddress() {
        return publicAddress;
    }

    /**
     * サーバのアドレス(internal)を返す
     * 
     * @return サーバのアドレス
     */
    public String getInternalAddress() {
        return internalAddress;
    }

    /**
     * ポート1を返す
     * 
     * @return ポート1
     */
    public int getListenPort1() {
        return listenPort1;
    }

    /**
     * ポート2を返す
     * 
     * @return ポート2
     */
    public int getListenPort2() {
        return listenPort2;
    }

    /**
     * JAVA_HOMEを返す
     * 
     * @return JAVA_HOME
     */
    public String getJavaHome() {
        return javaHome;
    }

    /**
     * ベースディレクトリパスを返す
     * 
     * @return ベースディレクトリパス
     */
    public String getBasedir() {
        return basedir;
    }

    public String getCharset() {
        return charset;
    }

    /**
     * クラスパスの配列を返す
     * 
     * @return クラスパスの配列
     */
    public String[] getClassPaths() {

        return classPaths.toArray(new String[classPaths.size()]);
    }

    /**
     * 環境依存のクラスパスを返す
     * 
     * @return 環境依存のクラスパス
     */
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

    /**
     * リソースが置かれるルートディレクトリを返す
     * 
     * @return リソースが置かれるルートディレクトリ
     */
    public String getResourceDirectory() {
        return resourcedir;
    }

    /**
     * サービスインスタンスを返す
     * 
     * @return サービスインスタンス
     * @throws ServiceException
     *             サービスインスタンスの作成に失敗
     */
    public Service getService() throws ServerException {

        Class<? extends Service> serviceClass = Silica.getServiceClass();

        try {

            return serviceClass.newInstance();

        } catch (Exception e) {
            LOG.error("Could not define the Service [{}].", serviceClass.getName());
            throw new ServerException("Could not define the Service.", e);
        }
    }

    /**
     * クラスパスを追加する
     * 
     * @param path
     *            パス
     */
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

    /**
     * リモート環境にあるサーバかを確認する
     * 
     * @return リモート環境にあるサーバの場合、true
     */
    public boolean isRemote() {
        return remote;
    }

    /**
     * サーバの起動コマンドを返す
     * 
     * @return サーバのコマンド
     */
    public String getActivationCommand() {
        return activationCommand;
    }

    /**
     * サーバの終了コマンドを返す
     * 
     * @return サーバのコマンド
     */
    public String getDeactivationCommand() {
        return deactivationCommand;
    }

    /**
     * ssh portを返す
     * 
     * @return ssh port
     */
    public int getSshPort() {
        return sshPort;
    }

    /**
     * ssh private key pathを返す
     * 
     * @return ssh private key path
     */
    public String getSshPrivateKeyPath() {
        return sshPrivateKeyPath;
    }

    /**
     * ssh passを返す
     * 
     * @return ssh pass
     */
    public String getSshPass() {
        return sshPass;
    }

    /**
     * ssh userを返す
     * 
     * @return ssh user
     */
    public String getSshUser() {
        return sshUser;
    }

    /**
     * ssh timeout msecを返す
     * 
     * @return ssh timeout msec
     */
    public int getSshTimeout() {
        return sshTimeout;
    }

    /**
     * 有効、無効を返す
     * 
     * @return
     */
    public boolean isEnable() {
        return enable;
    }

    /**
     * 有効、無効を設定する
     * 
     * @param enable
     */
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
