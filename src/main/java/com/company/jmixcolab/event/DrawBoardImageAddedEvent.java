package com.company.jmixcolab.event;

import org.springframework.context.ApplicationEvent;

public class DrawBoardImageAddedEvent extends ApplicationEvent {

    private double x;

    private double y;

    private String src;

    public DrawBoardImageAddedEvent(Object source, double x, double y, String src) {
        super(source);
        this.x = x;
        this.y = y;
        this.src = src;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }
}
