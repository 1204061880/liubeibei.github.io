package com.suning.framework.scm.client;

public interface SCMClient {
    SCMNode getConfig(String paramString);

    void createConfig(String paramString1, String paramString2, String paramString3) throws SCMException;

    void updateConfig(String paramString1, String paramString2, String paramString3) throws SCMException;

    void deleteConfig(String paramString1, String paramString2) throws SCMException;

    void destroy();

    String getAppCode();

    String getSecureKey(String paramString);

    SCMNode getGlobalConfig(String paramString) throws SCMException;

    Environment getEnvironment();

    SCMNode readConfig(String paramString1, String paramString2);

    enum Environment {
        PRD, NOT_PRD;

        Environment() {
        }
    }
}
