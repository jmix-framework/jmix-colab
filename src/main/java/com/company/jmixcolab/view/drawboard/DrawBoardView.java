package com.company.jmixcolab.view.drawboard;


import com.company.jmixcolab.event.DrawBoardMoveEvent;
import com.company.jmixcolab.event.DrawBoardOpacityChangeEvent;
import com.company.jmixcolab.view.main.MainView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.UiEventPublisher;
import io.jmix.flowui.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.vaadin.pekkam.Canvas;
import org.vaadin.pekkam.CanvasRenderingContext2D;
import org.vaadin.pekkam.event.MouseDownEvent;
import org.vaadin.pekkam.event.MouseMoveEvent;
import org.vaadin.pekkam.event.MouseUpEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

@Route(value = "draw-board-view", layout = MainView.class)
@ViewController("DrawBoardView")
@ViewDescriptor("draw-board-view.xml")
public class DrawBoardView extends StandardView {

    private static final Logger log = LoggerFactory.getLogger(DrawBoardView.class);

    static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    @ViewComponent
    private Div canvasContainer;

    protected CanvasRenderingContext2D ctx;

    protected Boolean drawingEnabled = false;

    @Autowired
    private UiEventPublisher uiEventPublisher;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        Canvas canvas = new Canvas(800, 500);
        ctx = canvas.getContext();


        canvas.addMouseMoveListener(this::onCanvasMouseMove);
        canvas.addMouseDownListener(this::onCanvasMouseDown);
        canvas.addMouseUpListener(this::onCanvasMouseUp);
        canvasContainer.add(canvas);
    }

    public void onCanvasMouseMove(MouseMoveEvent event) {
        if (drawingEnabled) {
            uiEventPublisher.publishEventForUsers(new DrawBoardMoveEvent(event, currentAuthentication.getUser().getUsername()), null);
        }
        log.info("event.x: {}, event.y: {}", event.getOffsetX(), event.getOffsetY());
    }

    public void onCanvasMouseDown(MouseDownEvent event) {
        this.drawingEnabled = true;
        ctx.beginPath();
    }
    public void onCanvasMouseUp(MouseUpEvent event) {
        ctx.closePath();
        this.drawingEnabled = false;
    }

    public static List<Double> genOpacitySeqStream(double start, double end, double step) {
        return DoubleStream.
                concat(DoubleStream.iterate(start, d -> d < end, d -> d + step),
                        DoubleStream.iterate(end, d -> d > start, d -> d - step))
                .boxed()
                .collect(Collectors.toList());
    }

    @EventListener
    public void boardMoveEventHandler(DrawBoardMoveEvent event) {
        boolean isCurrentUserDrawing = currentAuthentication.getUser().getUsername().equals(event.getUsername());
        ctx.setStrokeStyle(isCurrentUserDrawing? "red" : "green");
        ctx.lineTo(event.getMouseMoveEvent().getOffsetX(), event.getMouseMoveEvent().getOffsetY());
        ctx.stroke();
        List<String> users = new ArrayList<>() {{ add(currentAuthentication.getUser().getUsername() );}};
        if (!isCurrentUserDrawing) {
            Iterator<Integer> it = IntStream.range(0, 5).boxed().iterator();
            genOpacitySeqStream(0.6, 1.0, 0.2).forEach((i) -> {
                long nextTime = Double.valueOf(it.next() * 500.0).longValue();
                executorService.schedule(() -> {
                    uiEventPublisher.publishEventForUsers(new DrawBoardOpacityChangeEvent(i), users);
                }, nextTime, TimeUnit.MILLISECONDS);
            });
        } else {
            uiEventPublisher.publishEventForUsers(new DrawBoardOpacityChangeEvent(1.0), users);
        }
    }

    @EventListener
    public void opacityChangeEventHandler(DrawBoardOpacityChangeEvent event) {
        canvasContainer.getElement().setAttribute("style", "opacity: " + String.valueOf(event.getOpacity()));
    }
}