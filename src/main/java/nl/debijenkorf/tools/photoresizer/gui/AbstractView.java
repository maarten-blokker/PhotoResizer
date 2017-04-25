package nl.debijenkorf.tools.photoresizer.gui;

import java.io.IOException;
import java.util.Optional;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Window;

/**
 *
 * @author Maarten Blokker
 */
public class AbstractView {

    private Parent parent;

    public AbstractView(String resource) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setController(this);
            loader.setLocation(AbstractView.class.getResource(resource));
            this.parent = loader.load();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load view: " + resource, ex);
        }
    }

    public Parent getParent() {
        return parent;
    }

    public Window getWindow() {
        return Optional.ofNullable(getParent().getScene())
                .map(Scene::getWindow)
                .orElse(null);
    }

}
