package com.suning.framework.scm.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class SCMClientFactory {
    private static final Map<String, SCMClient> clients = new ConcurrentHashMap();

    public static SCMClient getSCMClient() throws SCMException {
        return getSCMClient(SCMConfiguration.CONFIG_FILE);
    }

    public static void setConfigFilePath(String path) {
        SCMConfiguration.CONFIG_FILE = path;
    }

    /**
     * 根据配置文件路径加载scm配置信息
     */
    public static SCMClient getSCMClient(String configFilePath) throws SCMException {
        SCMClient client = clients.get(configFilePath);
        if (client != null) {
            return client;
        }
        synchronized (clients) {
            client = clients.get(configFilePath);
            if (client == null) {
                //获取到配置信息
                SCMConfiguration config = SCMConfiguration.getInstance(configFilePath);
                //创建scmClient
                //创建zkClient
                client = new SCMClientImpl(config);
                clients.put(configFilePath, client);
            }
            return client;
        }
    }

    public static void destroy(SCMClient client) {
        synchronized (clients) {
            String key = null;
            for (Map.Entry<String, SCMClient> entry : clients.entrySet()) {
                if (client == entry.getValue()) {
                    key = entry.getKey();
                    break;
                }
            }
            if (key != null) {
                clients.remove(key);
            }
            client.destroy();
        }
    }

    public static void destroy(String configFile) {
        synchronized (clients) {
            SCMClient client = clients.remove(configFile);
            if (client != null) {
                client.destroy();
            }
        }
    }

    public static void shutdown() {
        synchronized (clients) {
            for (SCMClient client : clients.values()) {
                client.destroy();
            }
            clients.clear();
        }
    }
}
