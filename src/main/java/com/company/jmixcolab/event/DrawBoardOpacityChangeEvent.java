package com.company.jmixcolab.event;

import org.springframework.context.ApplicationEvent;

public class DrawBoardOpacityChangeEvent extends ApplicationEvent {

    private Double opacity;

    public DrawBoardOpacityChangeEvent(Double i) {
        super(i);
        this.opacity = i;
    }

    public Double getOpacity() {
        return opacity;
    }

    public void setOpacity(Double opacity) {
        this.opacity = opacity;
    }
}
