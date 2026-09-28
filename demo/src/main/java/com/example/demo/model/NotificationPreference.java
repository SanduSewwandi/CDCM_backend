package com.example.demo.model;

public class NotificationPreference {

    private boolean emailAppointments = true;
    private boolean emailVideoLinks = true;
    private boolean emailReports = true;
    private boolean smsUrgentAlerts = true;
    private boolean smsPaymentReceipts = false;
    private boolean inAppMessages = true;
    private boolean inAppSystemUpdates = true;

    public NotificationPreference() {}

    public boolean isEmailAppointments() {
        return emailAppointments;
    }

    public void setEmailAppointments(boolean emailAppointments) {
        this.emailAppointments = emailAppointments;
    }

    public boolean isEmailVideoLinks() {
        return emailVideoLinks;
    }

    public void setEmailVideoLinks(boolean emailVideoLinks) {
        this.emailVideoLinks = emailVideoLinks;
    }

    public boolean isEmailReports() {
        return emailReports;
    }

    public void setEmailReports(boolean emailReports) {
        this.emailReports = emailReports;
    }

    public boolean isSmsUrgentAlerts() {
        return smsUrgentAlerts;
    }

    public void setSmsUrgentAlerts(boolean smsUrgentAlerts) {
        this.smsUrgentAlerts = smsUrgentAlerts;
    }

    public boolean isSmsPaymentReceipts() {
        return smsPaymentReceipts;
    }

    public void setSmsPaymentReceipts(boolean smsPaymentReceipts) {
        this.smsPaymentReceipts = smsPaymentReceipts;
    }

    public boolean isInAppMessages() {
        return inAppMessages;
    }

    public void setInAppMessages(boolean inAppMessages) {
        this.inAppMessages = inAppMessages;
    }

    public boolean isInAppSystemUpdates() {
        return inAppSystemUpdates;
    }

    public void setInAppSystemUpdates(boolean inAppSystemUpdates) {
        this.inAppSystemUpdates = inAppSystemUpdates;
    }
}
