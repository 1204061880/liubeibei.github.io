package com.suning.framework.scm.client;

import com.suning.framework.scm.util.MD5Utils;
import com.suning.framework.scm.util.ServerNameGetter;
import com.suning.framework.scm.util.StringUtils;
import com.suning.framework.zookeeper.DataListener;
import com.suning.framework.zookeeper.ZkNode;
import com.suning.framework.zookeeper.exception.DataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SCMNodeImpl implements SCMNode, DataListener<String> {
    private static Logger logger = LoggerFactory.getLogger(SCMNodeImpl.class);
    private static final String SYNC_FAIL = "_SCM_SYNC_FAIL_";
    private static final String PATH_SEPARATOR = File.separator;
    private String path;
    private ZkNode<String> zkNode;
    private volatile String value;
    private SCMConfiguration configuration;
    private static final String FILE_SEPARATOR = "-";

    public SCMNodeImpl(SCMConfiguration configuration, String path, ZkNode<String> zkNode) {
        this.path = path;
        this.zkNode = zkNode;
        this.configuration = configuration;
    }

    @Override
    public void dataChanged(String oldData, String newData) {
        this.value = newData;
        saveToLocalFileCache(this.path, this.value);
    }

    @Override
    public String getValue()
            throws SCMException {
        if ("_SCM_SYNC_FAIL_".equals(this.value)) {
            throw new SCMException("Sync fail.");
        }
        return this.value;
    }

    @Override
    public void sync() throws SCMException {
        try {
            this.zkNode.sync(false);
            this.value = ((String) this.zkNode.getData());
            saveToLocalFileCache(this.path, this.value);
        } catch (DataException ex) {
            logger.warn("Can not get config content of " + this.path + ",will try to load from local file.", ex);
            try {
                this.value = loadFromLocalFileCache(this.path);
            } catch (Exception e) {
                logger.error("Exception when Load " + this.path + " from local file,Sync fail.", e);
                this.value = "_SCM_SYNC_FAIL_";
                throw new SCMException("Sync " + this.path + " fail.", e);
            }
        } finally {
            this.zkNode.monitor(this.value, this);
        }
    }

    @Deprecated
    @Override
    public void monitor(SCMListener scmListener) {
        monitor(this.value, scmListener);
    }

    @Override
    public void monitor(String expect, final SCMListener scmListener) {
        zkNode.monitor(expect, new DataListener<String>() {
            @Override
            public void dataChanged(String oldData, String newData) {
                scmListener.execute(oldData, newData);
            }
        });
    }

    @Override
    public void destroy() {
        this.zkNode.destroy();
    }

    private String loadFromLocalFileCache(String path)
            throws Exception {
        File file = new File(getLocalFilePath(this.configuration.getAppCode(), this.configuration.getScmServer(), path));
        if (!file.exists()) {
            throw new IOException("LocalFile " + file.getPath() + " not exist.");
        }
        FileInputStream in = null;
        int size = 512;
        StringBuilder sb = new StringBuilder(512);
        try {
            in = new FileInputStream(file);
            int length = 8192;
            byte[] data = new byte[8192];
            int n;
            while ((n = in.read(data)) != -1) {
                sb.append(new String(data, 0, n, "UTF-8"));
            }
            return sb.toString();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }
    }

    private synchronized void saveToLocalFileCache(String path, String value) {
        FileOutputStream out = null;
        PrintWriter writer = null;
        try {
            File file = new File(getLocalFilePath(this.configuration.getAppCode(), this.configuration.getScmServer(), path));
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (RuntimeException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            out = new FileOutputStream(file);
            BufferedOutputStream stream = new BufferedOutputStream(out);
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(stream, "UTF-8")));
            writer.write(value == null ? "" : value);
            writer.flush();
            return;
        } catch (IOException e) {
            logger.error("save localFile config error, path= " + this.configuration.getAppCode() + "-" + this.configuration.getScmServer() + "-" + path, e);
        } finally {
            if (writer != null) {
                writer.close();
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }
    }

    private String getLocalFilePath(String appCode, String scmServer, String path) {
        if (path.contains(PATH_SEPARATOR)) {
            path = StringUtils.substringAfterLast(path, PATH_SEPARATOR);
        }
        String res = SCMConstant.FILE_PATH + PATH_SEPARATOR + StringFilter(MD5Utils.getMD5(new StringBuilder().append(appCode).append("-").append(scmServer).append("-").append(ServerNameGetter.getServerName()).append("-").append(path).toString()));

        logger.info("LocalFilePath ==========" + res);
        return res;
    }

    public static String StringFilter(String str) throws PatternSyntaxException {
        Pattern p = Pattern.compile(SCMConstant.REGEX);
        Matcher m = p.matcher(str);
        return m.replaceAll("").trim();
    }
}
