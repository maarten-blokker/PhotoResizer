package nl.debijenkorf.tools.photoresizer;

import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nl.debijenkorf.tools.photoresizer.gui.ResizerController;

public class Launcher extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Platform.setImplicitExit(true);
        
        ResizerController controller = new ResizerController();

        Scene scene = new Scene(controller.getView().getParent());
        scene.getStylesheets().add("/styles/Styles.css");

        stage.setTitle("Resizer (test)");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
