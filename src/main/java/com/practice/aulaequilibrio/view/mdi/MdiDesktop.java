package com.practice.aulaequilibrio.view.mdi;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.css.PseudoClass;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MdiDesktop extends Pane {
    private static final PseudoClass ACTIVE_WINDOW = PseudoClass.getPseudoClass("active-window");
    private final Map<String, MdiWindow> windows = new HashMap<>();
    private final Map<String, String> popupOwners = new HashMap<>();
    private final ReadOnlyStringWrapper activeTitle = new ReadOnlyStringWrapper();
    private final Rectangle snapPreview = new Rectangle();
    private final StackPane dialogLayer = new StackPane();
    private final Deque<DialogPane> dialogs = new ArrayDeque<>();
    private final ReadOnlyBooleanWrapper dialogShowing = new ReadOnlyBooleanWrapper();

    public MdiDesktop() {
        getStyleClass().add("mdi-desktop");
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);
        snapPreview.getStyleClass().add("mdi-snap-preview");
        snapPreview.setManaged(false);
        snapPreview.setMouseTransparent(true);
        snapPreview.setVisible(false);
        snapPreview.setViewOrder(-1);
        getChildren().add(snapPreview);
        dialogLayer.getStyleClass().add("mdi-dialog-layer");
        dialogLayer.setManaged(false);
        dialogLayer.setVisible(false);
        dialogLayer.setViewOrder(-2);
        getChildren().add(dialogLayer);
    }

    public ReadOnlyBooleanProperty dialogShowingProperty() {
        return dialogShowing.getReadOnlyProperty();
    }

    public void showDialog(DialogPane dialog) {
        dialogs.addLast(dialog);
        if (dialogs.size() == 1) {
            updateDialogLayer();
        }
    }

    public void closeDialog(DialogPane dialog) {
        if (dialogs.peekFirst() == dialog) {
            dialogs.removeFirst();
            updateDialogLayer();
        }
    }

    private void updateDialogLayer() {
        DialogPane dialog = dialogs.peekFirst();
        if (dialog == null) {
            dialogLayer.getChildren().clear();
            dialogLayer.setVisible(false);
            dialogShowing.set(false);
            selectTopVisibleWindow();
            return;
        }
        dialogShowing.set(true);
        dialogLayer.getChildren().setAll(dialog);
        dialogLayer.setVisible(true);
        requestLayout();
        dialog.requestFocus();
    }

    public ReadOnlyStringProperty activeTitleProperty() {
        return activeTitle.getReadOnlyProperty();
    }

    public boolean isOpen(String title) {
        return windows.containsKey(title);
    }

    public List<String> openTitles() {
        return List.copyOf(windows.keySet());
    }

    public boolean activate(String title) {
        MdiWindow window = windows.get(title);
        if (window == null) {
            return false;
        }
        window.setVisible(true);
        bringToFront(window);
        window.requestFocus();
        return true;
    }

    public boolean toggleTask(String title) {
        if (title.equals(activeTitle.get())) {
            minimize(windows.get(title));
            return true;
        }
        return activate(title);
    }

    public void open(String title, Parent content, int width, int height, boolean closable) {
        if (activate(title)) {
            return;
        }
        double offset = 18 + (windows.size() % 6) * 24;
        MdiWindow window = new MdiWindow(this, title, content, width, height, offset, closable);
        window.disableProperty().bind(dialogShowing);
        windows.put(title, window);
        getChildren().add(window);
        requestLayout();
        activate(title);
    }

    public void openPopup(String owner, String title, Parent content, int width, int height) {
        if (activate(title) || !isOpen(owner)) {
            return;
        }
        popupOwners.put(title, owner);
        open(title, content, width, height, true);
        windows.get(title).arrange(Math.max(0, (getWidth() - width) / 2),
                Math.max(0, (getHeight() - height) / 2), width, height);
    }

    public void close(String title) {
        MdiWindow window = windows.get(title);
        if (window != null) {
            close(window);
        }
    }

    void bringToFront(MdiWindow window) {
        window.toFront();
        updateWindowOrder(window);
    }

    void minimize(MdiWindow window) {
        window.setVisible(false);
        selectTopVisibleWindow();
    }

    void close(MdiWindow window) {
        String title = window.title();
        popupOwners.entrySet().stream().filter(entry -> title.equals(entry.getValue()))
                .map(Map.Entry::getKey).toList().forEach(this::close);
        popupOwners.remove(title);
        windows.remove(title);
        getChildren().remove(window);
        selectTopVisibleWindow();
    }

    private void selectTopVisibleWindow() {
        for (Node child : getChildren().reversed()) {
            if (child instanceof MdiWindow window && window.isVisible()) {
                updateWindowOrder(window);
                window.requestFocus();
                return;
            }
        }
        updateWindowOrder(null);
    }

    private void updateWindowOrder(MdiWindow active) {
        int layer = 0;
        for (Node child : getChildren().reversed()) {
            if (child instanceof MdiWindow window) {
                window.setViewOrder(layer++);
                window.pseudoClassStateChanged(ACTIVE_WINDOW, window == active);
            }
        }
        activeTitle.set(active == null ? null : active.title());
    }

    public void cascade() {
        var visibleWindows = getChildren().stream().filter(MdiWindow.class::isInstance)
                .filter(Node::isVisible).map(MdiWindow.class::cast).toList();
        int count = visibleWindows.size();
        if (count == 0) {
            return;
        }
        double margin = 14;
        int steps = Math.max(1, count - 1);
        double stepX = Math.min(180, getWidth() * 0.25 / steps);
        double stepY = Math.min(68, getHeight() * 0.24 / steps);
        double width = Math.max(1, getWidth() - 2 * margin - stepX * steps);
        double height = Math.max(1, getHeight() - 2 * margin - stepY * steps);
        for (int index = 0; index < count; index++) {
            MdiWindow window = visibleWindows.get(index);
            window.arrange(margin + index * stepX, margin + index * stepY, width, height);
        }
    }

    SnapZone snapAt(Point2D pointer) {
        return SnapZone.at(pointer, getWidth(), getHeight());
    }

    void previewSnap(SnapZone zone) {
        snapPreview.setVisible(zone != null);
        if (zone != null) {
            var bounds = zone.bounds(getWidth(), getHeight());
            snapPreview.setX(bounds.getMinX());
            snapPreview.setY(bounds.getMinY());
            snapPreview.setWidth(bounds.getWidth());
            snapPreview.setHeight(bounds.getHeight());
        }
    }

    @Override
    protected void layoutChildren() {
        dialogLayer.resizeRelocate(0, 0, getWidth(), getHeight());
        for (MdiWindow window : windows.values()) {
            window.fitDesktop(getWidth(), getHeight());
        }
    }
}
