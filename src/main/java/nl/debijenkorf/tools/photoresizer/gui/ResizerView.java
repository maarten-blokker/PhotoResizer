package nl.debijenkorf.tools.photoresizer.gui;

import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import nl.debijenkorf.tools.photoresizer.Configuration;
import nl.debijenkorf.tools.photoresizer.resizer.Preset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Maarten Blokker
 */
public class ResizerView extends AbstractView {

    private static final Logger LOG = LoggerFactory.getLogger(ResizerView.class);

    public static final String PREFERENCE_LAST_SOURCE_FOLDER = "pref.last.source.folder";
    public static final String PREFERENCE_LAST_TARGET_FOLDER = "pref.last.target.folder";
    public static final String PREFERENCE_LAST_PRESET_INDEX = "pref.last.preset.index";

    private final ObjectProperty<Path> sourceFolder = new SimpleObjectProperty<>();
    private final ObjectProperty<Path> targetFolder = new SimpleObjectProperty<>();
    private final ObjectProperty<Preset> selectedPreset = new SimpleObjectProperty<>();
    private final ResizerController controller;

    @FXML
    private TextField txtSourceFolder;

    @FXML
    private TextField txtTargetFolder;

    @FXML
    private Button btnChooseSourceFolder;

    @FXML
    private Button btnChooseTargetFolder;

    @FXML
    private Button btnCancel;

    @FXML
    private Button btnStart;

    @FXML
    private RadioButton radioOrientationLandscape;

    @FXML
    private ToggleGroup toggleOrientation;

    @FXML
    private RadioButton radioOrientationPortrait;

    @FXML
    private VBox containerMeasurements;

    @FXML
    private ColorPicker cboxColor;

    public ResizerView(ResizerController controller) {
        super("/fxml/Scene.fxml");
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        txtSourceFolder.textProperty().bind(sourceFolder().asString());
        btnChooseSourceFolder.setOnAction((evt) -> controller
                .chooseFolder(sourceFolder().get(), "Kies een bron map met foto's")
                .ifPresent(sourceFolder()::set));
        txtTargetFolder.textProperty().bind(targetFolder().asString());
        btnChooseTargetFolder.setOnAction((evt) -> controller
                .chooseFolder(targetFolder().get(), "Kies een doel map met foto's")
                .ifPresent(targetFolder()::set));

        bindFolderProperty(sourceFolder(), PREFERENCE_LAST_SOURCE_FOLDER);
        bindFolderProperty(targetFolder(), PREFERENCE_LAST_TARGET_FOLDER);

        initMeasurements();

        btnStart.setOnAction((evt) -> controller.start());
        btnCancel.setOnAction((evt) -> Platform.exit());
    }

    private void bindFolderProperty(ObjectProperty<Path> property, String preferenceKey) {
        //load initial directory
        Configuration.getPreference(preferenceKey)
                .map((path) -> Paths.get(path))
                .ifPresent(property::set);

        //update preference when the property changes
        property.addListener((obs, oldVal, currentVal) -> {
            Configuration.setPreference(preferenceKey, currentVal.toString());
        });
    }

    private void initMeasurements() {
        try {
            Configuration.getPreference(PREFERENCE_LAST_PRESET_INDEX).map(Integer::parseInt).ifPresent((index) -> {
                selectedPreset().set(Configuration.getPresets().get(index));
            });
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            LOG.error("Failed to load preset preference", e);
        }

        ToggleGroup group = new ToggleGroup();

        for (int i = 0; i < Configuration.getPresets().size(); i++) {
            final int index = i;
            Preset preset = Configuration.getPresets().get(i);
            String label = String.format("%.2f - %.2f", preset.getTopLine(), preset.getBaseLine());

            RadioButton button = new RadioButton(label);
            button.setSelected(preset == selectedPreset().get());
            button.setToggleGroup(group);
            button.setOnAction((evt) -> {
                Configuration.setPreference(PREFERENCE_LAST_PRESET_INDEX, String.valueOf(index));
                selectedPreset().set(preset);
            });

            containerMeasurements.getChildren().add(button);
        }

        //if no toggle is selected, select the first one
        if (group.getSelectedToggle() == null) {
            group.selectToggle(group.getToggles().get(0));
        }
    }

    public ObjectProperty<Color> colorProperty() {
        return cboxColor.valueProperty();
    }

    public ObjectProperty<Preset> selectedPreset() {
        return selectedPreset;
    }

    public ObjectProperty<Path> sourceFolder() {
        return sourceFolder;
    }

    public ObjectProperty<Path> targetFolder() {
        return targetFolder;
    }

}
