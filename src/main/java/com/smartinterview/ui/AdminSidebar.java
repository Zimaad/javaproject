package com.smartinterview.ui;

import com.smartinterview.model.User;
import com.smartinterview.navigation.NavigationManager;
import com.smartinterview.service.MockAuthService;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Sidebar for the admin console.
 *
 * Kept separate from {@link Sidebar} because the admin nav is a different
 * set of destinations, and because the console is marked as a privileged
 * area so it is never mistaken for the student view.
 *
 * Items with no destination yet are disabled rather than hidden, so the
 * shape of the console is visible but nothing dead-ends.
 */
public class AdminSidebar extends VBox {

    private record NavEntry(String key, String label, String icon, boolean available) {}

    private static final NavEntry[] MANAGEMENT = {
        new NavEntry("admin-dashboard", "Overview",  "mdi2v-view-dashboard-outline", true),
        new NavEntry("admin-questions", "Questions", "mdi2b-book-open-outline",      true),
        new NavEntry("admin-users",     "Users",     "mdi2a-account-group-outline",  false),
        new NavEntry("admin-reports",   "Reports",   "mdi2c-chart-line",             false),
    };

    private final java.util.Map<String, Button> items = new java.util.LinkedHashMap<>();

    private String active;

    public AdminSidebar() {
        getStyleClass().add("sidebar");
        setSpacing(0);

        getChildren().add(buildLogo());
        Separator rule = new Separator();
        rule.getStyleClass().add("divider");
        getChildren().add(rule);

        Label section = new Label("MANAGEMENT");
        section.getStyleClass().add("nav-section-title");
        getChildren().add(section);

        for (NavEntry e : MANAGEMENT) {
            Button btn = new Button(e.label());
            btn.getStyleClass().add("nav-item");
            btn.setGraphic(new FontIcon(e.icon()));
            btn.setMaxWidth(Double.MAX_VALUE);

            if (e.available()) {
                btn.setOnAction(ev -> {
                    if (!e.key().equals(active)) {
                        NavigationManager.getInstance().navigateTo(e.key());
                    }
                });
            } else {
                btn.setDisable(true);
                btn.setTooltip(new Tooltip("Not available in this demo build"));
            }

            items.put(e.key(), btn);
            getChildren().add(btn);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);

        getChildren().add(buildFooter());
    }

    /** Marks a nav item as the current location; set from FXML. */
    public void setActive(String active) {
        this.active = active;
        items.forEach((key, btn) -> {
            btn.getStyleClass().remove("nav-item-active");
            if (key.equals(active)) {
                btn.getStyleClass().add("nav-item-active");
            }
        });
    }

    public String getActive() {
        return active;
    }

    private HBox buildLogo() {
        StackPane mark = new StackPane(new FontIcon("mdi2b-brain"));
        mark.getStyleClass().add("logo-mark");
        ((FontIcon) mark.getChildren().get(0)).getStyleClass().addAll("icon-on-accent", "icon-md");

        Label name = new Label("SmartInterview");
        name.getStyleClass().add("logo-text");

        Label subtitle = new Label("ADMIN CONSOLE");
        subtitle.getStyleClass().add("logo-tagline");

        VBox text = new VBox(1, name, subtitle);
        text.setAlignment(Pos.CENTER_LEFT);

        HBox logo = new HBox(mark, text);
        logo.getStyleClass().add("sidebar-logo");
        return logo;
    }

    private VBox buildFooter() {
        User user = MockAuthService.getInstance().getCurrentUser();
        String fullName = user != null ? user.getFullName() : "Administrator";

        Label initial = new Label(fullName.isBlank() ? "A" : fullName.substring(0, 1).toUpperCase());
        initial.getStyleClass().add("avatar-text");
        StackPane avatar = new StackPane(initial);
        avatar.getStyleClass().add("avatar");

        Label nameLabel = new Label(fullName);
        nameLabel.getStyleClass().addAll("text-small", "text-bold");
        nameLabel.setStyle("-fx-text-fill: -fx-text-primary;");

        Label roleLabel = new Label("Administrator");
        roleLabel.getStyleClass().add("text-overline");

        VBox who = new VBox(1, nameLabel, roleLabel);
        who.setAlignment(Pos.CENTER_LEFT);

        HBox identity = new HBox(10, avatar, who);
        identity.setAlignment(Pos.CENTER_LEFT);

        Button logout = new Button("Log out");
        logout.getStyleClass().addAll("nav-item", "nav-logout", "btn-danger-quiet");
        logout.setGraphic(new FontIcon("mdi2l-logout"));
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(e -> {
            MockAuthService.getInstance().logout();
            NavigationManager.getInstance().navigateTo("landing");
        });

        VBox footer = new VBox(identity, logout);
        footer.getStyleClass().add("sidebar-footer");
        return footer;
    }
}
