package com.suning.framework.scm.client;

import com.suning.framework.scm.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class SCMConfiguration {
    private static Logger logger = LoggerFactory.getLogger(SCMConfiguration.class);
    protected static String CONFIG_FILE = "/scm.properties";
    public static ConcurrentHashMap<String, SCMConfiguration> configs = new ConcurrentHashMap();
    private SCMClient.Environment environment;
    private String appCode;
    private String secretKey;
    private String scmServer;
    private String zkServer;

    public static void setConfigFilePath(String path) {
        CONFIG_FILE = path;
    }

    public static SCMConfiguration getInstance() {
        return getInstance(CONFIG_FILE);
    }

    public static SCMConfiguration getInstance(String configFile) {
        if (configs.containsKey(configFile)) {
            return configs.get(configFile);
        }
        InputStream inputStream = SCMConfiguration.class.getResourceAsStream(configFile);
        if (inputStream == null) {
            throw new SCMException("Can not load file " + configFile);
        }
        try {
            Properties properties = new Properties();
            properties.load(inputStream);
            SCMConfiguration config = new SCMConfiguration(properties);
            SCMConfiguration existed = configs.putIfAbsent(configFile, config);
            if (existed != null) {
                config = existed;
            }
            return config;
        } catch (Exception e) {
            throw new SCMException("load scm config error", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.error("close stream error", e);
            }
        }
    }

    private String getDC() {
        String value = System.getenv("ldc");
        if (StringUtils.isEmpty(value)) {
            value = System.getProperty("ldc");
            if (StringUtils.isEmpty(value)) {
                throw new RuntimeException("Can not find ldc in environment.");
            }
        }
        return value;
    }

    private SCMConfiguration(Properties properties) {
        this.appCode = ((String) properties.get("appCode"));
        this.secretKey = ((String) properties.get("secretKey"));
        this.scmServer = ((String) properties.get("scmServer"));
        Properties localCached = new Properties();
        try {
            localCached = loadFromLocal();
        } catch (Exception e) {
            logger.warn("Can not load properties from local file.", e);
        }
        try {
            Map<String, Object> map = new HashMap();
            map.put("dc", getDC());
            this.zkServer = HttpClient.sendByPost(this.scmServer + "/zk.htm", ParamUtil.getEncodedParamFromMap(map), 30000, 3, 2000);

            localCached.put("zkServer", this.zkServer);
        } catch (Exception ex) {
            logger.error("Can not get zk list from scm server.", ex);
            this.zkServer = ((String) localCached.get("zkServer"));
        }
        try {
            String isPrd = HttpClient.sendByPost(this.scmServer + "/isPrd.htm", "", 30000, 3, 2000);
            this.environment = (Boolean.valueOf(isPrd).booleanValue() ? SCMClient.Environment.PRD : SCMClient.Environment.NOT_PRD);
            localCached.put("scmEnv", this.environment.toString());
        } catch (Exception ex) {
            logger.error("Can not get environment from scm server.", ex);
            Object env = localCached.get("scmEnv");
            if (null != env) {
                this.environment = SCMClient.Environment.valueOf(env.toString());
            }
        }
        if ((StringUtils.isEmpty(this.zkServer)) || (StringUtils.isEmpty(this.secretKey)) || (StringUtils.isEmpty(this.appCode)) || (StringUtils.isEmpty(this.scmServer)) || (null == this.environment)) {
            throw new SCMException("load scm config error,zkServer or appCode or scmServer or secretKey or scmEnv is null ");
        }
        saveToLocal(localCached);
    }

    private Properties loadFromLocal() throws Exception {
        String filePath = getLocalFilePath();
        File file = new File(filePath);
        if (!file.exists()) {
            return new Properties();
        }
        FileInputStream in = new FileInputStream(file);
        Properties properties = new Properties();
        try {
            properties.load(in);
            return properties;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                logger.error("close stream error", e);
            }
        }
    }

    private String getLocalFilePath() {
        String md5 = MD5Utils.getMD5(this.scmServer + "_" + ServerNameGetter.getServerName());
        return System.getProperty("user.home") + "/" + SCMNodeImpl.StringFilter(md5);
    }

    private void saveToLocal(Properties properties) {
        String filePath = getLocalFilePath();
        File file = new File(filePath);
        Writer fw = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            fw = new FileWriter(file);
            properties.store(fw, "scm properties");
            if (null != fw) {
                try {
                    fw.close();
                } catch (IOException e) {
                    logger.error("close stream error", e);
                }
            }
            return;
        } catch (IOException ex) {
            logger.error("Exception when save object to local.", ex);
            if (null != fw) {
                try {
                    fw.close();
                } catch (IOException e) {
                    logger.error("close stream error", e);
                }
            }
        } finally {
            if (null != fw) {
                try {
                    fw.close();
                } catch (IOException e) {
                    logger.error("close stream error", e);
                }
            }
        }
    }

    public SCMClient.Environment getEnvironment() {
        return this.environment;
    }

    public String getAppCode() {
        return this.appCode;
    }

    public String getSecretKey() {
        return this.secretKey;
    }

    public String getScmServer() {
        return this.scmServer;
    }

    public String getZkServer() {
        return this.zkServer;
    }
}
