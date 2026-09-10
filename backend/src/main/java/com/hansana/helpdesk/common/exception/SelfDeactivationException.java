package com.hansana.helpdesk.common.exception;

public class SelfDeactivationException extends RuntimeException {

    public SelfDeactivationException(String message) {
        super(message);
    }
}
