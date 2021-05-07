package com.suning.framework.scm.client;

import com.suning.framework.scm.util.*;
import com.suning.framework.statistics.VersionStatistics;
import com.suning.framework.zookeeper.Deserializer;
import com.suning.framework.zookeeper.ZkClient;
import com.suning.framework.zookeeper.ZkConnection;
import com.suning.framework.zookeeper.ZkNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SCMClientImpl implements SCMClient {
    private static Logger logger = LoggerFactory.getLogger(SCMClientImpl.class);
    private static final CompressStrDeserializer DESERIALIZER = new CompressStrDeserializer();
    private final ConcurrentHashMap<String, SCMNode> scmNodes = new ConcurrentHashMap();
    private static final String PATH_SEPARATOR = "/";
    private static final String GLOBAL_PATH = "/global";
    private static final String ROOT = "/scmv1";
    private static final String DB_ROOT = "/scm";
    private ZkClient zkClient;
    private SCMConfiguration config;

    @Deprecated
    public static SCMClient getInstance()
            throws SCMException {
        return SCMClientFactory.getSCMClient();
    }

    @Deprecated
    public static void setConfigFilePath(String path) {
        SCMClientFactory.setConfigFilePath(path);
    }

    @Deprecated
    public static SCMClient getInstance(String configFilePath)
            throws SCMException {
        return SCMClientFactory.getSCMClient(configFilePath);
    }

    protected SCMClientImpl(SCMConfiguration config) throws SCMException {
        VersionStatistics.reportVersion(config.getAppCode(), config.getScmServer(), SCMClientImpl.class);
        this.config = config;

        zkClient = new ZkClient(config.getZkServer().trim(), ZkConnection.digestCredentials(config.getAppCode().trim(), config.getSecretKey().trim()));
    }

    @Override
    public SCMNode getConfig(String path) {
        String formatedPath = parse(path);
        SCMNode scmNode = this.scmNodes.get(formatedPath);
        if (scmNode != null) {
            return scmNode;
        }
        synchronized (this.scmNodes) {
            scmNode = this.scmNodes.get(formatedPath);
            if (scmNode != null) {
                return scmNode;
            }
            //获取zk节点信息
            ZkNode<String> zkNode = new ZkNode(zkClient, getFullPath(this.config.getAppCode(), formatedPath), DESERIALIZER, true, false, true);
            //创建scmNode
            scmNode = new SCMNodeImpl(this.config, formatedPath, zkNode);
            this.scmNodes.put(formatedPath, scmNode);
            return scmNode;
        }
    }

    private static String getPropertyValue(String key) {
        String value = System.getenv(key);
        if (StringUtils.isEmpty(value)) {
            value = System.getProperty(key);
        }
        return value;
    }

    public static String parse(String path) {
        if ((path == null) || (path.length() == 0)) {
            return path;
        }
        int start = -1;
        int end = -1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            if ((path.charAt(i) == '$') && (start == -1) && (i < path.length() - 1) && (path.charAt(i + 1) == '{')) {
                start = i;
                i++;
            } else if ((path.charAt(i) == '}') && (start != -1)) {
                end = i;
            }
            if (start == -1) {
                sb.append(path.charAt(i));
            }
            if ((start != -1) && (end != -1)) {
                String value = getPropertyValue(path.substring(start + 2, end));
                if (StringUtils.isEmpty(value)) {
                    throw new IllegalArgumentException(String.format("No system property %s of %s", new Object[]{path.substring(start + 2, end), path}));
                }
                sb.append(value);
                start = -1;
                end = -1;
            }
        }
        if ((start != -1) && (end == -1)) {
            sb.append(path.substring(start));
        }
        return sb.toString();
    }

    //拼接zk全路径
    public static String getFullPath(String appCode, String path) {
        if (path.indexOf("/") == 0) {
            path = StringUtils.substringAfter(path, "/");
        }
        String fullPath;
        if (path.contains("/global".substring(1))) {
            fullPath = "/scmv1/" + path;
        } else {
            fullPath = "/scmv1/" + appCode + "/" + path;
        }
        return fullPath;
    }

    public static String getFullOperationPath(String appCode, String path) {
        if (path.indexOf("/") == 0) {
            path = StringUtils.substringAfter(path, "/");
        }
        String fullPath;
        if (path.contains("/global".substring(1))) {
            fullPath = "/scm/" + path;
        } else {
            fullPath = "/scm/" + appCode + "/" + path;
        }
        return fullPath;
    }

    @Override
    public void destroy() {
        synchronized (this.scmNodes) {
            for (SCMNode scmNode : this.scmNodes.values()) {
                try {
                    scmNode.destroy();
                } catch (Throwable ex) {
                }
            }
            zkClient.close();
        }
    }

    @Override
    public void createConfig(String user, String path, String value)
            throws SCMException {
        remoteOperationConfig(user, getFullOperationPath(this.config.getAppCode(), path), value, "create");
    }

    @Override
    public void updateConfig(String user, String path, String value)
            throws SCMException {
        remoteOperationConfig(user, getFullOperationPath(this.config.getAppCode(), path), value, "update");
    }

    @Override
    public void deleteConfig(String user, String path)
            throws SCMException {
        remoteOperationConfig(user, getFullOperationPath(this.config.getAppCode(), path), "", "delete");
    }

    public void remoteOperationConfig(String user, String path, String config, String operationType)
            throws SCMException {
        Map<String, Object> map = new HashMap();

        map.put("appCode", this.config.getAppCode());
        map.put("path", path);
        map.put("config", config);
        map.put("operationType", operationType);
        map.put("user", user);
        String result = sendDataByPost(this.config.getScmServer() + "/operationConfig.htm", map);

        String[] resultsStrings = result.split(",");
        if ("failure".equals(resultsStrings[0])) {
            throw new SCMException(resultsStrings[1]);
        }
    }

    protected String sendDataByPost(String url, Map<String, Object> dataMap) {
        if ((url == null) || (url.length() == 0)) {
            throw new IllegalArgumentException("url is empty!");
        }
        dataMap.put("timeStamp", Long.valueOf(System.currentTimeMillis()));
        try {
            String digestStr = ParamUtil.getParamFromMap(dataMap);

            String mac = EncryptUtil.encryptHMAC(digestStr, this.config.getSecretKey());

            String paramStr = ParamUtil.getEncodedParamFromMap(dataMap);

            String result = HttpClient.sendByPost(url, paramStr + "&mac=" + mac, 30000, 3, 2000);

            logger.info("Result from SCM server:" + result);

            return result;
        } catch (Exception e) {
            throw new SCMException("Exception occur when send data to scm server.", e);
        }
    }

    public ZkClient getInternalZkClient() {
        return zkClient;
    }

    @Override
    public String getAppCode() {
        return this.config.getAppCode();
    }

    @Override
    public String getSecureKey(String appCode) {
        try {
            ZkClient.ReadResult readResult = zkClient.readData("/scmv1/" + appCode, 30000L);

            return new String(readResult.data);
        } catch (Exception e) {
            throw new SCMException(e.getMessage(), e);
        }
    }

    public String getSecretKey() {
        return this.config.getSecretKey();
    }

    public String getZkServer() {
        return this.config.getZkServer();
    }

    @Override
    public SCMNode getGlobalConfig(String path)
            throws SCMException {
        if (path.indexOf("/") == 0) {
            path = StringUtils.substringAfter(path, "/");
        }
        return getConfig("/global/" + path);
    }

    @Override
    public SCMClient.Environment getEnvironment() {
        return this.config.getEnvironment();
    }

    @Override
    public SCMNode readConfig(String appCode, String path) {
        if ((!StringUtils.isEmpty(appCode)) && (!StringUtils.isEmpty(path))) {
            String formatedPath = parse(path);
            if (formatedPath.indexOf("/") == 0) {
                formatedPath = StringUtils.substringAfter(formatedPath, "/");
            }
            if (appCode.indexOf("/") == 0) {
                appCode = StringUtils.substringAfter(appCode, "/");
            }
            String zkPath = "/scmv1/" + appCode + "/" + formatedPath;
            SCMNode scmNode = (SCMNode) this.scmNodes.get(zkPath);
            if (scmNode != null) {
                return scmNode;
            }
            synchronized (this.scmNodes) {
                scmNode = (SCMNode) this.scmNodes.get(zkPath);
                if (scmNode != null) {
                    return scmNode;
                }
                ZkNode<String> zkNode = new ZkNode(zkClient, zkPath, DESERIALIZER, true, false, true);


                scmNode = new SCMNodeImpl(this.config, zkPath, zkNode);
                this.scmNodes.put(zkPath, scmNode);
                return scmNode;
            }
        }
        return null;
    }

    public static class CompressStrDeserializer implements Deserializer<String> {
        @Override
        public String deserialize(byte[] data) {
            return CompressUtils.deserialize(data);
        }
    }
}
