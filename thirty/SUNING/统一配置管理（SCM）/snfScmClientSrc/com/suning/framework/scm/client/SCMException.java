package com.suning.framework.scm.client;

public class SCMException extends RuntimeException {
    private static final long serialVersionUID = -567786786121301727L;

    public SCMException(String message) {
        super(message);
    }

    public SCMException(String message, Throwable cause) {
        super(message, cause);
    }
}
