package com.example.demo.model;

public class CancellationRefundPolicy {

    private double over48HoursPercent = 90.0;
    private double between24And48HoursPercent = 75.0;
    private double between6And24HoursPercent = 50.0;
    private double under6HoursPercent = 0.0;

    public CancellationRefundPolicy() {
    }

    public CancellationRefundPolicy(double over48HoursPercent,
                                    double between24And48HoursPercent,
                                    double between6And24HoursPercent,
                                    double under6HoursPercent) {
        this.over48HoursPercent = over48HoursPercent;
        this.between24And48HoursPercent = between24And48HoursPercent;
        this.between6And24HoursPercent = between6And24HoursPercent;
        this.under6HoursPercent = under6HoursPercent;
    }

    public double getOver48HoursPercent() {
        return over48HoursPercent;
    }

    public void setOver48HoursPercent(double over48HoursPercent) {
        this.over48HoursPercent = over48HoursPercent;
    }

    public double getBetween24And48HoursPercent() {
        return between24And48HoursPercent;
    }

    public void setBetween24And48HoursPercent(double between24And48HoursPercent) {
        this.between24And48HoursPercent = between24And48HoursPercent;
    }

    public double getBetween6And24HoursPercent() {
        return between6And24HoursPercent;
    }

    public void setBetween6And24HoursPercent(double between6And24HoursPercent) {
        this.between6And24HoursPercent = between6And24HoursPercent;
    }

    public double getUnder6HoursPercent() {
        return under6HoursPercent;
    }

    public void setUnder6HoursPercent(double under6HoursPercent) {
        this.under6HoursPercent = under6HoursPercent;
    }

    public boolean arePercentagesValid() {
        return isValidPercentage(over48HoursPercent)
                && isValidPercentage(between24And48HoursPercent)
                && isValidPercentage(between6And24HoursPercent)
                && isValidPercentage(under6HoursPercent);
    }

    private boolean isValidPercentage(double val) {
        return val >= 0.0 && val <= 100.0;
    }
}
