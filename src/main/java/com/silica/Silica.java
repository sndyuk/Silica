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

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.silica.job.Callback;
import com.silica.job.Job;
import com.silica.job.JobExecutor;
import com.silica.rpc.server.Server;
import com.silica.rpc.server.ServerSelector;
import com.silica.service.Service;
import com.silica.service.ServiceException;

/**
 * Silica.
 * <p>
 * Execute process on multiple node as a single node.
 * </p>
 */
public final class Silica {

    private static final Logger LOG = LoggerFactory.getLogger(Silica.class);

    private static String CONFIG_PATH;
    private static Config GLOBAL_CONFIG;
    private static Class<? extends Service> SERVICE_CLASS;

    private static ExecutorService EXECUTOR_POOL = Executors.newWorkStealingPool();

    /**
     * A configuration value from a global schema, which return a same value from any node.
     * 
     * @param key
     *            key name
     * @return a configuration value
     */
    public static String getGlobalConfig(String key) {
        return GLOBAL_CONFIG.getMine(key);
    }

    /**
     * Boot Silica.
     * 
     * @param args
     *            -o: (bind | unbind | exit) -S: xxx=hoge
     * @see #boot(String[])
     */
    public static void main(String[] args) {
        new Bootstrap().boot(args).execute();
    }

    /**
     * Boot Silica.
     * 
     * @param args
     *            -o: (bind | unbind | exit) -S: xxx=hoge
     */
    public static void boot(String[] args) {
        new Bootstrap().boot(args);
    }

    /**
     * Boot Silica with default settings.
     */
    public static void boot() {
        boot(new String[0]);
    }

    /**
     * A base directory of the node.
     * 
     * @return a base directory path.
     */
    public static String getBaseDirectory() {
        return GLOBAL_CONFIG.get(Config.KEY_BASE_DIR);
    }

    /**
     * A resource directory of the node.
     * 
     * @return a resource directory path.
     */
    public static String getResourceDirectory() {
        return GLOBAL_CONFIG.get(Config.KEY_RESOURCE_DIR);
    }

    /**
     * Å configuration file path.
     * 
     * @return a configuration file path
     */
    public static String getConfigPath() {
        return CONFIG_PATH;
    }

    /**
     * An instance ID of the node.
     * 
     * @return a instance ID of the node
     */
    public static String getResourceID() {
        return GLOBAL_CONFIG.get(Config.KEY_RESOURCE_ID);
    }

    /**
     * A service class
     * 
     * @return a Service class
     */
    public static Class<? extends Service> getServiceClass() {
        return SERVICE_CLASS;
    }

    /**
     * A job timeout max milli seconds.
     * 
     * @return a job timeout max milli seconds
     */
    public static long getJobTimeout() {
        String jobTimeout = GLOBAL_CONFIG.get(Config.KEY_JOB_TIMEOUT_MSEC);
        if (jobTimeout == null) {
            return -1L;
        }
        return Long.parseLong(jobTimeout);
    }

    /**
     * A max number of how many old resources that are keeped on the instance.
     * 
     * @return a max number of how many old resources that are keeped on the instance.
     */
    public static int getNumOfKeepDeployed() {
        String keepDeployedLast = GLOBAL_CONFIG.get(Config.KEY_KEEP_DEPLOYED_LAST);
        if (keepDeployedLast == null) {
            return 0;
        }
        return Integer.parseInt(keepDeployedLast);
    }

    /**
     * Execute job synchronous.
     * 
     * @param job
     * @param T a type of a result
     * @return a result of the job
     * @throws ServiceException
     */
    public static <T extends Serializable> T execute(Job<T> job) throws ServiceException {
        return execute(job, getJobTimeout());
    }

    /**
     * Execute job synchronous.
     * 
     * @param job
     * @param jobTimeoutMsec
     * @param T a type of a result
     * @return a result of the job
     * @throws ServiceException
     */
    public static <T extends Serializable> T execute(Job<T> job, long jobTimeoutMsec) throws ServiceException {

        JobExecutor<T> executor = new JobExecutor<T>(job);
        Future<T> future = EXECUTOR_POOL.submit(executor);
        Exception err = null;
        try {
            return future.get(jobTimeoutMsec, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.info(job.toString(), e);
            err = e;
        } catch (ExecutionException e) {
            LOG.error(job.toString(), e);
            err = e;
        } catch (TimeoutException e) {
            LOG.warn(job.toString(), e);
            err = e;
        }
        throw new ServiceException(job.toString(), err);
    }

    /**
     * Execute job asynchronous.
     * 
     * @param job
     * @param T a type of a result
     * @returna a {@link Future} object of a result of the job
     */
    public static <T extends Serializable> Future<T> executeAsync(Job<T> job) {

        JobExecutor<T> executor = new JobExecutor<T>(job);
        return EXECUTOR_POOL.submit(executor);
    }

    /**
     * Execute job asynchronous and run a jobCallback function.
     * 
     * @param job
     * @param jobCallback
     * @param T a type of a result
     */
    public static <T extends Serializable> void executeAsync(final Job<T> job, final Callback<T> jobCallback) {
        executeAsync(job, jobCallback, getJobTimeout());
    }

    /**
     * Execute job asynchronous and run a jobCallback function.
     * 
     * @param job
     * @param jobCallback
     * @param jobTimeoutMsec
     * @param T a type of a result
     */
    public static <T extends Serializable> void executeAsync(final Job<T> job, final Callback<T> jobCallback, final long jobTimeoutMsec) {

        JobExecutor<T> executor = new JobExecutor<T>(job);
        final Future<T> future = EXECUTOR_POOL.submit(executor);
        EXECUTOR_POOL.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    T result = getResult(future, jobTimeoutMsec, job.toString());
                    jobCallback.execute(result);
                } catch (ServiceException e) {
                    LOG.error("", e);
                }
            }
        });
    }

    private static <T extends Serializable> T getResult(Future<T> future, long jobTimeoutMsec, String jobDescription) throws ServiceException {
        Exception exception = null;
        try {
            return future.get(jobTimeoutMsec, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.info(jobDescription, e);
            exception = e;
        } catch (ExecutionException e) {
            LOG.error(jobDescription, e);
            exception = e;
        } catch (TimeoutException e) {
            LOG.warn(jobDescription, e);
            exception = e;
        }
        throw new ServiceException(jobDescription, exception);
    }

    /**
     * Shutdown all jobs.
     */
    protected static void shutdownAllJob() {
        EXECUTOR_POOL.shutdown();
        try {
            if (!EXECUTOR_POOL.awaitTermination(1, TimeUnit.SECONDS)) {
                EXECUTOR_POOL.shutdownNow();
                if (!EXECUTOR_POOL.awaitTermination(1, TimeUnit.SECONDS))
                    LOG.warn("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            EXECUTOR_POOL.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static class Bootstrap {
        private static final String KEY_CONFIG_PATH = "SILICA_CONF";
        private static final Options OPTS = new Options();
        static {
            OPTS.addOption("o", true, "bind | unbind");
            OPTS.addOption("c", true, "silica confing file");
            OPTS.addOption("s", true, "override properties");
        }

        private boolean initialized;
        private TypeOrderCmd order;

        private static enum TypeOrderCmd {
            bind, unbind
        }

        Bootstrap boot(String[] args) {
            LOG.info("Start initializing Silica");
            if (initialized) {
                throw new IllegalStateException("Already initialized");
            }
            if (args == null) {
                args = new String[0];
            }
            if (LOG.isDebugEnabled()) {
                for (int i = 0; i < args.length; i++) {
                    LOG.debug("args[{}]={}", i, args[i]);
                }
            }
            try {
                CommandLineParser parser = new DefaultParser();
                CommandLine cmd = parser.parse(OPTS, args);
                parseOptions(cmd);
            } catch (Exception e) {
                new HelpFormatter().printHelp("example", OPTS);

                throw new IllegalArgumentException(e);
            } finally {
                initialized = true;
            }
            LOG.info("Succeeded initializing Silica");
            return this;
        }

        Bootstrap execute() {

            Server localServer = ServerSelector.createSelector().getLocalServer();

            try {

                switch (order) {
                case bind:
                    localServer.bind(localServer.getServerContext().getService());
                    break;
                case unbind:
                    localServer.unbind(SERVICE_CLASS);
                    break;
                default:
                    throw new RuntimeException(MessageFormat.format("Unknown order command {0}", order));
                }
            } catch (Exception e) {
                throw new RuntimeException("Unknown error", e);
            }
            return this;
        }

        private CommandLine parseOptions(CommandLine cmd)
                throws ParseException, IOException {

            String o = cmd.getOptionValue("o");
            if (o != null) {
                order = TypeOrderCmd.valueOf(o);
            }
            String c = cmd.getOptionValue("c");
            if (c != null) {
                System.setProperty(KEY_CONFIG_PATH, c);
            }

            GLOBAL_CONFIG = new Config();

            String confPath = System.getProperty(KEY_CONFIG_PATH);
            if (confPath == null || confPath.length() == 0) {

                confPath = System.getenv(KEY_CONFIG_PATH);
                LOG.info("Use a configuration file which set on System.env(SILICA_CONF)={}", confPath);
            } else {
                LOG.info("Use a configuration file which set on System.property(SILICA_CONF)={}", confPath);
            }
            if (confPath == null || confPath.length() == 0) {
                LOG.info("Use a default configiguration file: silica.properties");
                confPath = "silica.properties";
            }
            CONFIG_PATH = confPath;
            LOG.info("Configuration file path={}", confPath);

            GLOBAL_CONFIG.init(Thread.currentThread().getContextClassLoader().getResource(confPath));

            String[] props = cmd.getOptionValues("s");
            if (props != null) {
                for (String prop : props) {

                    int pos = prop.indexOf("=");
                    String key = prop.substring(0, pos);
                    String value = prop.substring(pos + 1, prop.length());

                    if (key == null || key.length() == 0 || value == null || value.length() == 0) {
                        continue;
                    }
                    if (key.equals(Config.KEY_BASE_DIR)) {
                        String baseDir = value.endsWith("/") ? value : value + "/";
                        LOG.info("Update global config: {}={}", Config.KEY_BASE_DIR, value);
                        GLOBAL_CONFIG.set(Config.KEY_BASE_DIR, baseDir);

                    } else if (key.equals(Config.KEY_CLASS_PATHS)) {

                        String extpaths = value;
                        if (extpaths != null && extpaths.length() > 0) {
                            String orgpaths = getGlobalConfig(Config.KEY_CLASS_PATHS);
                            if (orgpaths != null && orgpaths.length() > 0) {
                                extpaths = orgpaths + "," + extpaths;
                            }
                            LOG.info("Update global config: {}={}", Config.KEY_CLASS_PATHS, value);
                            GLOBAL_CONFIG.set(Config.KEY_CLASS_PATHS, extpaths);
                        }
                    } else {
                        LOG.info("Update global config: {}={}", key, value);
                        GLOBAL_CONFIG.set(key, value);
                    }
                }
            }
            String resourceID = getResourceID();
            if (resourceID == null) {
                /*
                 * * This is a first instance. *
                 * * Create a resource id. *
                 */
                GLOBAL_CONFIG.set(Config.KEY_RESOURCE_ID, Long.toString(System.currentTimeMillis(), Character.MAX_RADIX));
                GLOBAL_CONFIG.set(Config.KEY_RESOURCE_DIR, getBaseDirectory());
                GLOBAL_CONFIG.set(Config.KEY_HOST_ADDRESS, "localhost");
                LOG.info("The instance has been assinged. ID: {}", GLOBAL_CONFIG.get(Config.KEY_RESOURCE_ID));
            } else {
                LOG.info("The instance has alredy been assinged for the ID: {}", resourceID);
            }

            try {
                @SuppressWarnings("unchecked")
                Class<? extends Service> serviceClass = (Class<? extends Service>) Class.forName(getGlobalConfig("service.class"));
                SERVICE_CLASS = serviceClass;
                LOG.info("Set a service class: {}", SERVICE_CLASS);

            } catch (ClassNotFoundException e) {
                LOG.error("Could not difine service class.", e);
            }

            return cmd;
        }
    }
}
