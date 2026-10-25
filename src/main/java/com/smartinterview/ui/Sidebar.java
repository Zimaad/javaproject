package com.smartinterview.ui;

import com.smartinterview.model.User;
import com.smartinterview.navigation.NavigationManager;
import com.smartinterview.service.AuthService;
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
 * The application sidebar, as a single reusable control.
 *
 * Previously every authenticated view hand-rolled its own copy of this
 * markup, which meant eight places to keep in sync and eight chances to
 * drift. Views now declare {@code <Sidebar active="dashboard"/>} and the
 * nav, icons, active state and user footer are built here once.
 *
 * Navigation goes straight through {@link NavigationManager}, so views no
 * longer need per-screen nav handlers.
 */
public class Sidebar extends VBox {

    /** A single nav destination: view name, label, icon, section. */
    private record NavEntry(String key, String label, String icon) {}

    private static final NavEntry[] MAIN = {
        new NavEntry("dashboard",        "Dashboard",       "mdi2v-view-dashboard-outline"),
        new NavEntry("interview-select", "Start Interview", "mdi2t-target"),
        new NavEntry("question-bank",    "Question Bank",   "mdi2b-book-open-outline"),
    };

    private static final NavEntry[] ANALYTICS = {
        new NavEntry("performance-report", "Performance",       "mdi2c-chart-line"),
        new NavEntry("interview-history",  "Interview History", "mdi2h-history"),
    };

    private static final NavEntry[] ACCOUNT = {
        new NavEntry("profile",  "Profile",  "mdi2a-account-circle-outline"),
        new NavEntry("settings", "Settings", "mdi2c-cog-outline"),
    };

    /** Nav buttons by view key, so the active one can be marked later. */
    private final java.util.Map<String, Button> items = new java.util.LinkedHashMap<>();

    private String active;

    public Sidebar() {
        getStyleClass().add("sidebar");
        setSpacing(0);

        getChildren().add(buildLogo());
        getChildren().add(separator());

        getChildren().add(sectionTitle("MAIN"));
        addEntries(MAIN);

        getChildren().add(sectionTitle("ANALYTICS"));
        addEntries(ANALYTICS);

        getChildren().add(sectionTitle("ACCOUNT"));
        addEntries(ACCOUNT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);

        getChildren().add(buildFooter());
    }

    /**
     * Marks a nav item as the current location. Set from FXML via
     * {@code active="..."} using the NavigationManager view name.
     */
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

    private void addEntries(NavEntry[] entries) {
        for (NavEntry e : entries) {
            Button btn = new Button(e.label());
            btn.getStyleClass().add("nav-item");
            btn.setGraphic(new FontIcon(e.icon()));
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(ev -> {
                // Re-navigating to the current screen would pointlessly
                // rebuild it and reset scroll position.
                if (!e.key().equals(active)) {
                    NavigationManager.getInstance().navigateTo(e.key());
                }
            });
            items.put(e.key(), btn);
            getChildren().add(btn);
        }
    }

    private HBox buildLogo() {
        StackPane mark = new StackPane(new FontIcon("mdi2b-brain"));
        mark.getStyleClass().add("logo-mark");
        ((FontIcon) mark.getChildren().get(0)).getStyleClass().addAll("icon-on-accent", "icon-md");

        Label name = new Label("SmartInterview");
        name.getStyleClass().add("logo-text");

        Label tagline = new Label("Practice. Perform. Improve.");
        tagline.getStyleClass().add("logo-tagline");

        VBox text = new VBox(1, name, tagline);
        text.setAlignment(Pos.CENTER_LEFT);

        HBox logo = new HBox(mark, text);
        logo.getStyleClass().add("sidebar-logo");
        return logo;
    }

    private VBox buildFooter() {
        User user = AuthService.getInstance().getCurrentUser();
        String fullName = user != null ? user.getFullName() : "Guest";
        String role = user != null && "admin".equalsIgnoreCase(user.getRole()) ? "Administrator" : "Student";

        Label initial = new Label(fullName.isBlank() ? "?" : fullName.substring(0, 1).toUpperCase());
        initial.getStyleClass().add("avatar-text");
        StackPane avatar = new StackPane(initial);
        avatar.getStyleClass().add("avatar");

        Label nameLabel = new Label(fullName);
        nameLabel.getStyleClass().addAll("text-small", "text-bold");
        nameLabel.setStyle("-fx-text-fill: -fx-text-primary;");

        Label roleLabel = new Label(role);
        roleLabel.getStyleClass().add("text-overline");

        VBox who = new VBox(1, nameLabel, roleLabel);
        who.setAlignment(Pos.CENTER_LEFT);

        HBox identity = new HBox(10, avatar, who);
        identity.setAlignment(Pos.CENTER_LEFT);

        Button logout = new Button("Log out");
        logout.getStyleClass().addAll("nav-item", "nav-logout", "btn-danger-quiet");
        logout.setGraphic(new FontIcon("mdi2l-logout"));
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setTooltip(new Tooltip("Sign out of SmartInterview"));
        logout.setOnAction(e -> {
            AuthService.getInstance().logout();
            NavigationManager.getInstance().navigateTo("landing");
        });

        VBox footer = new VBox(identity, logout);
        footer.getStyleClass().add("sidebar-footer");
        return footer;
    }

    private Label sectionTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("nav-section-title");
        return l;
    }

    private Separator separator() {
        Separator s = new Separator();
        s.getStyleClass().add("divider");
        return s;
    }
}
