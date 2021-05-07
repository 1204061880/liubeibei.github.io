package com.suning.framework.scm.client;

public interface SCMNode {
    String getValue() throws SCMException;

    void sync() throws SCMException;

    @Deprecated
    void monitor(SCMListener paramSCMListener);

    void monitor(String paramString, SCMListener paramSCMListener);

    void destroy();
}

