package com.company.jmixcolab.view.drawboard;


import com.company.jmixcolab.event.DrawBoardImageAddedEvent;
import com.company.jmixcolab.event.DrawBoardMoveEvent;
import com.company.jmixcolab.event.DrawBoardOpacityChangeEvent;
import com.company.jmixcolab.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.router.Route;
import elemental.json.JsonObject;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Route(value = "draw-board-view", layout = MainView.class)
@ViewController("DrawBoardView")
@ViewDescriptor("draw-board-view.xml")
public class DrawBoardView extends StandardView {

    private static final Logger log = LoggerFactory.getLogger(DrawBoardView.class);

    static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    static ExecutorService httpExecutorService = Executors.newVirtualThreadPerTaskExecutor();

    protected HttpClient client;

    @ViewComponent
    private Div canvasContainer;

    protected CanvasRenderingContext2D ctx;

    protected Boolean drawingEnabled = false;

    protected double canvasLeft;
    protected double canvasTop;

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

        client = HttpClient.newBuilder()
                .executor(httpExecutorService)
                .build();
    }

    @Subscribe(id = "canvasContainer", subject = "doubleClickListener")
    public void onCanvasContainerClick(final ClickEvent<Div> event) {
        PendingJavaScriptResult res = canvasContainer.getElement().callJsFunction("getBoundingClientRect");
        res.then((rect) -> {
            canvasLeft = ((JsonObject) rect).getNumber("left");
            canvasTop = ((JsonObject) rect).getNumber("top");
            try {
                HttpRequest request = HttpRequest.newBuilder(new URI("https://upload.wikimedia.org/wikipedia/commons/5/50/Smile_Image.png"))
                        .version(HttpClient.Version.HTTP_1_1)
                        .header("Content-type", "image/png")
                        .build();
                client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).thenApply((response) -> {
                    log.info("Image received for event.x: {}, event.y: {}", event.getClientX(), event.getClientY());
                    uiEventPublisher.publishEventForUsers(new DrawBoardImageAddedEvent(response,
                            event.getClientX() - canvasLeft, event.getClientY() - canvasTop,
                            "data:image/png;base64," + Base64.getEncoder().encodeToString(response.body())), null);

                    return null;
                });
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });
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

    @EventListener
    public void boardMoveEventHandler(DrawBoardMoveEvent event) {
        boolean isCurrentUserDrawing = currentAuthentication.getUser().getUsername().equals(event.getUsername());
        ctx.setStrokeStyle(isCurrentUserDrawing? "red" : "green");
        ctx.lineTo(event.getMouseMoveEvent().getOffsetX(), event.getMouseMoveEvent().getOffsetY());
        ctx.stroke();
        List<String> users = new ArrayList<>() {{ add(currentAuthentication.getUser().getUsername() );}};
        if (!isCurrentUserDrawing) {
            Iterator<Integer> it = IntStream.range(0, 5).boxed().iterator();
            Stream.of(1.0, 0.8, 0.6, 0.8, 1.0).forEach((i) -> {
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

    @EventListener
    public void boardImageAddedHandler(DrawBoardImageAddedEvent event) {
        ctx.beginPath();
        ctx.drawImage(event.getSrc(), event.getX(), event.getY(), 100, 100);
        ctx.closePath();
    }
}