package com.practice.aulaequilibrio.view.mdi;

import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;

final class MdiWindow extends BorderPane {
    private static final double MIN_WIDTH = 420;
    private static final double MIN_HEIGHT = 300;

    private final MdiDesktop desktop;
    private final String title;
    private final Path maximizeIcon = icon(new MoveTo(1, 1), new LineTo(13, 1),
            new LineTo(13, 13), new LineTo(1, 13), new ClosePath());
    private final Path restoreIcon = icon(new MoveTo(1, 5), new LineTo(9, 5), new LineTo(9, 13),
            new LineTo(1, 13), new ClosePath(), new MoveTo(5, 5), new LineTo(5, 1),
            new LineTo(13, 1), new LineTo(13, 9), new LineTo(9, 9));
    private final Button maximize = windowControl("Restaurar", restoreIcon);
    private final HBox resizeBar = new HBox();
    private SnapZone zone = SnapZone.MAXIMIZED;
    private double normalX;
    private double normalY;
    private double normalWidth;
    private double normalHeight;
    private Point2D dragAnchor;
    private Point2D dragStart;
    private Point2D normalPositionBeforeDrag;
    private boolean dragging;

    MdiWindow(MdiDesktop desktop, String title, Parent content,
              double width, double height, double offset, boolean closable) {
        this.desktop = desktop;
        this.title = title;
        normalX = offset;
        normalY = offset;
        normalWidth = width;
        normalHeight = height;
        setManaged(false);
        setFocusTraversable(true);
        getStyleClass().add("mdi-window");
        addEventFilter(MouseEvent.MOUSE_PRESSED, event -> desktop.bringToFront(this));

        Label caption = new Label(title);
        caption.getStyleClass().add("mdi-caption");
        caption.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(caption, Priority.ALWAYS);
        Button minimize = windowControl("Minimizar", icon(new MoveTo(1, 8), new LineTo(13, 8)));
        minimize.setOnAction(event -> desktop.minimize(this));
        HBox titleBar = new HBox(8, caption, minimize, maximize);
        if (closable) {
            Button close = windowControl("Cerrar", icon(new MoveTo(2, 2), new LineTo(12, 12),
                    new MoveTo(12, 2), new LineTo(2, 12)));
            close.getStyleClass().add("danger");
            close.setOnAction(event -> desktop.close(this));
            titleBar.getChildren().add(close);
        }
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.getStyleClass().add("mdi-title-bar");
        titleBar.setCursor(Cursor.MOVE);
        setTop(titleBar);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        setCenter(scroll);

        Label grip = new Label("◢");
        grip.getStyleClass().add("mdi-resize-grip");
        grip.setCursor(Cursor.SE_RESIZE);
        resizeBar.getChildren().add(grip);
        resizeBar.setAlignment(Pos.CENTER_RIGHT);
        setBottom(resizeBar);
        resizeBar.setVisible(false);
        resizeBar.managedProperty().bind(resizeBar.visibleProperty());

        maximize.setOnAction(event -> setZone(zone == null ? SnapZone.MAXIMIZED : null));
        titleBar.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2
                    && event.isStillSincePress() && isTitleSurface(titleBar, event)) {
                setZone(zone == SnapZone.MAXIMIZED ? null : SnapZone.MAXIMIZED);
            }
        });
        enableMove(titleBar);
        enableResize(grip);
    }

    String title() {
        return title;
    }

    private static Path icon(PathElement... elements) {
        Path icon = new Path(elements);
        icon.getStyleClass().add("window-icon");
        icon.setMouseTransparent(true);
        return icon;
    }

    private static Button windowControl(String text, Path icon) {
        Button button = new Button(text, icon);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.getStyleClass().add("window-control");
        button.accessibleTextProperty().bind(button.textProperty());
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(button.textProperty());
        button.setTooltip(tooltip);
        return button;
    }

    void arrange(double x, double y, double width, double height) {
        normalX = x;
        normalY = y;
        normalWidth = width;
        normalHeight = height;
        setZone(null);
    }

    private void setZone(SnapZone value) {
        zone = value;
        maximize.setText(value == null ? "Maximizar" : "Restaurar");
        maximize.setGraphic(value == null ? maximizeIcon : restoreIcon);
        resizeBar.setVisible(value == null);
        desktop.requestLayout();
    }

    void fitDesktop(double width, double height) {
        if (zone != null) {
            var bounds = zone.bounds(width, height);
            resizeRelocate(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
            return;
        }
        double windowWidth = Math.min(width, normalWidth);
        double windowHeight = Math.min(height, normalHeight);
        double x = Math.max(0, Math.min(normalX, width - windowWidth));
        double y = Math.max(0, Math.min(normalY, height - windowHeight));
        resizeRelocate(x, y, windowWidth, windowHeight);
    }

    private void enableMove(HBox titleBar) {
        titleBar.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY && isTitleSurface(titleBar, event)) {
                dragStart = pointer(event);
                dragAnchor = dragStart.subtract(getLayoutX(), getLayoutY());
                normalPositionBeforeDrag = new Point2D(normalX, normalY);
            }
        });
        titleBar.setOnMouseDragged(event -> {
            if (dragStart == null || !event.isPrimaryButtonDown()) {
                return;
            }
            Point2D pointer = pointer(event);
            if (!dragging) {
                if (pointer.distance(dragStart) < 5) {
                    return;
                }
                dragging = true;
                if (zone != null) {
                    double fraction = Math.clamp(dragAnchor.getX() / Math.max(1, getWidth()), 0, 1);
                    double restoredWidth = Math.min(normalWidth, desktop.getWidth());
                    dragAnchor = new Point2D(restoredWidth * fraction, dragAnchor.getY());
                    setZone(null);
                }
            }
            var position = pointer.subtract(dragAnchor);
            normalX = position.getX();
            normalY = position.getY();
            desktop.previewSnap(desktop.snapAt(pointer));
            desktop.requestLayout();
        });
        titleBar.setOnMouseReleased(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (dragging) {
                SnapZone target = desktop.snapAt(pointer(event));
                if (target != null) {
                    normalX = normalPositionBeforeDrag.getX();
                    normalY = normalPositionBeforeDrag.getY();
                    setZone(target);
                }
                desktop.previewSnap(null);
            }
            dragging = false;
            dragStart = null;
        });
    }

    private Point2D pointer(MouseEvent event) {
        return desktop.sceneToLocal(event.getSceneX(), event.getSceneY());
    }

    private static boolean isTitleSurface(HBox titleBar, MouseEvent event) {
        for (Node node = event.getPickResult().getIntersectedNode(); node != null && node != titleBar;
             node = node.getParent()) {
            if (node instanceof ButtonBase) {
                return false;
            }
        }
        return true;
    }

    private void enableResize(Label grip) {
        grip.setOnMouseDragged(event -> {
            if (!event.isPrimaryButtonDown()) {
                return;
            }
            var position = sceneToLocal(event.getSceneX(), event.getSceneY());
            normalX = getLayoutX();
            normalY = getLayoutY();
            normalWidth = Math.max(MIN_WIDTH, Math.min(position.getX(), desktop.getWidth() - normalX));
            normalHeight = Math.max(MIN_HEIGHT, Math.min(position.getY(), desktop.getHeight() - normalY));
            desktop.requestLayout();
        });
    }
}
