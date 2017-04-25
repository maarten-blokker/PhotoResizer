package nl.debijenkorf.tools.photoresizer.gui;

import java.io.PrintWriter;
import java.io.StringWriter;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Window;
import nl.debijenkorf.tools.photoresizer.ResizerWorker;

/**
 *
 * @author Maarten Blokker
 */
public class ProgressView extends AbstractView implements ResizerWorker.Listener {

    private final DoubleProperty progressProperty = new SimpleDoubleProperty();

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label lblProgress;

    private Dialog dialog;
    private EventHandler<ActionEvent> cancelHandler;

    public ProgressView() {
        super("/fxml/Dialog.fxml");

        initComponents();
    }

    private void initComponents() {
        progressBar.progressProperty().bind(progressProperty);
        lblProgress.textProperty().bind(progressProperty.multiply(100).asString("%.0f"));
    }

    private void showExceptionDialog(String title, String description, Throwable exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(title);
        alert.setContentText(description);

        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));

        TextArea textArea = new TextArea(writer.toString());
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        Label label = new Label("Stacktrace:");
        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);
        alert.showAndWait();
    }

    public void setOnCancel(EventHandler<ActionEvent> cancelHandler) {
        this.cancelHandler = cancelHandler;
    }

    public void showAndWait(Window window) {
        if (dialog != null) {
            return;
        }

        dialog = new Dialog();
        dialog.setDialogPane((DialogPane) getParent());
        if (cancelHandler != null) {
            dialog.getDialogPane().addEventFilter(ActionEvent.ANY, cancelHandler);
        }
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(window);
        dialog.showAndWait();
    }

    @Override
    public void onProgress(ResizerWorker worker, double progress) {
        Platform.runLater(() -> {
            progressProperty.set(progress);
        });
    }

    @Override
    public void onFinish(ResizerWorker worker, boolean succesfull) {
        Platform.runLater(() -> {
            if (!succesfull) {
                showExceptionDialog("Kon niet alle foto's omzetten", ""
                        + "Er ging iets mis tijdens het omzetten van de foto's:\n"
                        + worker.getException().getLocalizedMessage(),
                        worker.getException());
            }
            dialog.close();
        });
    }
}
