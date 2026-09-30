package com.crimesphere.backend.dto.notification;

import com.crimesphere.backend.entity.NotificationChannel;

public class TestNotificationRequest {

    private NotificationChannel channel = NotificationChannel.EMAIL;

    public TestNotificationRequest() {}

    public TestNotificationRequest(NotificationChannel channel) {
        this.channel = channel;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }
}
