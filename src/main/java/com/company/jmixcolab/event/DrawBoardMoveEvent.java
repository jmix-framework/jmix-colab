package com.company.jmixcolab.event;

import org.springframework.context.ApplicationEvent;
import org.vaadin.pekkam.event.MouseMoveEvent;

public class DrawBoardMoveEvent extends ApplicationEvent {

    protected MouseMoveEvent mouseMoveEvent;

    protected String username;

    public DrawBoardMoveEvent(MouseMoveEvent event, String username) {
        super(event);
        this.mouseMoveEvent = event;
        this.username = username;
    }

    public MouseMoveEvent getMouseMoveEvent() {
        return mouseMoveEvent;
    }

    public String getUsername() {
        return username;
    }
}
