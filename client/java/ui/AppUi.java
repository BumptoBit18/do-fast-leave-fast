package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class AppUi {
    private AppUi() {
    }

    public static HBox pageHeader(String eyebrow, String title, String subtitle, Node... actions) {
        Label eyebrowLabel = new Label(eyebrow);
        eyebrowLabel.getStyleClass().add("eyebrow");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("page-title");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("muted-label");

        VBox left = new VBox(4, eyebrowLabel, titleLabel, subtitleLabel);
        left.getStyleClass().add("header-copy");

        HBox right = new HBox(8);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.getChildren().addAll(actions);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, left, spacer, right);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("page-header");
        return header;
    }

    public static VBox panelCard(String title, String subtitle, Node... body) {
        VBox panel = new VBox(10);
        panel.getStyleClass().addAll("card", "panel-card");
        panel.setPadding(new Insets(16));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");
        panel.getChildren().add(titleLabel);

        if (subtitle != null && !subtitle.isBlank()) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.getStyleClass().add("muted-label");
            subtitleLabel.setWrapText(true);
            panel.getChildren().add(subtitleLabel);
        }

        panel.getChildren().addAll(body);
        return panel;
    }

    public static VBox statCard(String label, String value, String hint) {
        VBox card = new VBox(4);
        card.getStyleClass().addAll("stat-card", "card");
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("stat-label");
        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("stat-value");
        card.getChildren().addAll(labelNode, valueNode);
        if (hint != null && !hint.isBlank()) {
            Label hintNode = new Label(hint);
            hintNode.getStyleClass().add("muted-label");
            card.getChildren().add(hintNode);
        }
        return card;
    }

    public static Label badge(String text) {
        Label badge = new Label(text);
        badge.getStyleClass().add("soft-badge");
        return badge;
    }

    public static VBox fieldGroup(String labelText, String helperText, Node field) {
        VBox group = new VBox(6);
        group.getStyleClass().add("field-group");

        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        group.getChildren().add(label);

        if (helperText != null && !helperText.isBlank()) {
            Label helper = new Label(helperText);
            helper.getStyleClass().add("field-helper");
            helper.setWrapText(true);
            group.getChildren().add(helper);
        }

        group.getChildren().add(field);
        return group;
    }
}
