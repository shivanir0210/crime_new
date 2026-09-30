package com.crimesphere.backend.service;

public class NotificationDispatchResult {

    private final boolean success;
    private final boolean mock;
    private final String externalMessageId;
    private final String errorMessage;

    private NotificationDispatchResult(boolean success, boolean mock, String externalMessageId, String errorMessage) {
        this.success = success;
        this.mock = mock;
        this.externalMessageId = externalMessageId;
        this.errorMessage = errorMessage;
    }

    public static NotificationDispatchResult success(String externalMessageId) {
        return new NotificationDispatchResult(true, false, externalMessageId, null);
    }

    public static NotificationDispatchResult mockSuccess(String externalMessageId) {
        return new NotificationDispatchResult(true, true, externalMessageId, "DEVELOPMENT MOCK — NOT ACTUALLY SENT");
    }

    public static NotificationDispatchResult failure(String errorMessage) {
        return new NotificationDispatchResult(false, false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isMock() {
        return mock;
    }

    public String getExternalMessageId() {
        return externalMessageId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
