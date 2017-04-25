package nl.debijenkorf.tools.photoresizer.gui;

import java.nio.file.Path;
import java.util.Optional;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import nl.debijenkorf.tools.photoresizer.ResizerWorker;
import nl.debijenkorf.tools.photoresizer.resizer.Preset;

/**
 *
 * @author Maarten Blokker
 */
public class ResizerController {

    private final ResizerView view;

    public ResizerController() {
        this.view = new ResizerView(this);
    }

    public ResizerView getView() {
        return view;
    }

    public Optional<Path> chooseFolder(Path initialDirectory, String title) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        if (initialDirectory != null) {
            chooser.setInitialDirectory(initialDirectory.toFile());
        }

        return Optional.ofNullable(chooser.showDialog(view.getWindow()))
                .flatMap((result) -> Optional.ofNullable(result.toPath()));

    }

    public void start() {
        Preset preset = this.view.selectedPreset().get();
        Color color = this.view.colorProperty().get();
        Path srcDir = this.view.sourceFolder().get();
        Path dstDir = this.view.targetFolder().get();

        ResizerWorker worker = new ResizerWorker(preset, color, srcDir, dstDir);
        
        ProgressView progressView = new ProgressView();
        progressView.setOnCancel((evt) -> {
            worker.stop();
        });
        worker.setListener(progressView);
        worker.start();

        progressView.showAndWait(this.view.getWindow());
        Platform.exit();
    }

}
