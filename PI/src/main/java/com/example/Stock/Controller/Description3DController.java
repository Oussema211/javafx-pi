package com.example.Stock.Controller;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public class Description3DController {
    // Contrôles FXML
    @FXML private TextField lengthField;
    @FXML private TextField widthField;
    @FXML private TextField heightField;
    @FXML private TextField aislesField;
    @FXML private ComboBox<String> storageTypeCombo;
    @FXML private TextArea freeDescriptionArea;
    @FXML private ImageView previewImageView;
    @FXML private ScrollPane previewScrollPane;
    @FXML private Button exportImageButton;
    @FXML private StackPane view3DContainer;
    @FXML private ProgressBar loadingProgressBar;
    @FXML private Label loadingLabel;
    @FXML private VBox previewContainer;
    @FXML private Label previewLabel;
    @FXML private CheckBox lightingCheckBox;
    @FXML private CheckBox transparencyCheckBox;
    @FXML private CheckBox animationCheckBox;
    @FXML private Slider cameraSlider;
    @FXML private Button rotateLeftButton;
    @FXML private Button rotateRightButton;
    @FXML private Button zoomInButton;
    @FXML private Button zoomOutButton;
    @FXML private Button toggleGridButton;
    @FXML private Button toggleMeasureButton;
    @FXML private Button toggleSectionButton;
    @FXML private Button toggleWireframeButton;
    @FXML private ColorPicker wallColorPicker;
    @FXML private ColorPicker floorColorPicker;
    @FXML private TextField annotationField;
    @FXML private Button addAnnotationButton;
    @FXML private Label measurementLabel;
    @FXML private Label volumeLabel;
    @FXML private Label spaceUsedLabel;

    // Variables d'état
    private String lastImageData;
    private AnimationTimer renderTimer;
    private SubScene subScene;
    private PerspectiveCamera camera;
    private double cameraDistance;
    private double cameraYaw = 180;
    private double cameraPitch = 29;
    private double targetX, targetY, targetZ;
    private long lastUpdateTime = 0;
    private final long UPDATE_INTERVAL = 16; // ~60 FPS
    private Group warehouseGroup;
    private Group gridGroup;
    private Box sectionPlane;
    private boolean isGridVisible = false;
    private boolean isMeasuring = false;
    private List<Node> measurePoints = new ArrayList<>();
    private Line measureLine;
    private boolean isWireframe = false;
    private Node selectedNode;
    private PhongMaterial originalMaterial;
    private Group forklift;
    private Timeline forkliftAnimation;
    private List<Node> draggableNodes = new ArrayList<>();
    private List<Text> annotations = new ArrayList<>();
    private double totalVolume;
    private double usedSpace;

    @FXML
    private void initialize() {
        // Populate storage types (removing "Premium" as it's no longer needed)
        storageTypeCombo.getItems().addAll("Palettes", "Étagères mobiles", "Rayonnages métalliques");
        exportImageButton.setDisable(true);

        // Configuration de l'image de prévisualisation
        configurePreviewImage();

        // Configuration des écouteurs
        setupListeners();

        // Configuration des boutons de contrôle de la caméra
        setupCameraControls();
    }

    private void configurePreviewImage() {
        previewImageView.setPreserveRatio(true);
        previewImageView.setSmooth(true);
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setFitToHeight(true);

        previewImageView.setOnScroll(event -> {
            double delta = event.getDeltaY();
            double scale = previewImageView.getScaleX() + delta * 0.005;
            scale = Math.max(0.3, Math.min(4.0, scale));
            previewImageView.setScaleX(scale);
            previewImageView.setScaleY(scale);
            updateScrollPaneViewport();
        });
    }

    private void updateScrollPaneViewport() {
        if (previewImageView.getImage() != null) {
            double imageWidth = previewImageView.getFitWidth();
            double imageHeight = previewImageView.getFitHeight();
            previewScrollPane.setContent(previewImageView);
            Platform.runLater(() -> {
                previewScrollPane.layout();
                previewImageView.setTranslateX((previewScrollPane.getWidth() - imageWidth) / 2);
                previewImageView.setTranslateY((previewScrollPane.getHeight() - imageHeight) / 2);
            });
        }
    }

    private void setupListeners() {
        cameraSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            cameraYaw = newVal.doubleValue();
            updateCameraPosition();
        });

        wallColorPicker.setOnAction(e -> updateWallColor());
        floorColorPicker.setOnAction(e -> updateFloorColor());
    }

    private void setupCameraControls() {
        rotateLeftButton.setOnAction(e -> {
            cameraYaw -= 15;
            updateCameraPosition();
        });

        rotateRightButton.setOnAction(e -> {
            cameraYaw += 15;
            updateCameraPosition();
        });

        zoomInButton.setOnAction(e -> {
            cameraDistance = Math.max(5, cameraDistance - 2);
            updateCameraPosition();
        });

        zoomOutButton.setOnAction(e -> {
            cameraDistance = Math.min(50, cameraDistance + 2);
            updateCameraPosition();
        });
    }

    private boolean validateFields() {
        try {
            double length = Double.parseDouble(lengthField.getText());
            double width = Double.parseDouble(widthField.getText());
            double height = Double.parseDouble(heightField.getText());
            int aisles = Integer.parseInt(aislesField.getText());
            String storageType = storageTypeCombo.getValue();
            if (length <= 0 || width <= 0 || height <= 0 || aisles <= 0) {
                showAlert("Erreur", "Les dimensions et le nombre d'allées doivent être positifs.", Alert.AlertType.ERROR);
                return false;
            }
            if (storageType == null || storageType.isEmpty()) {
                showAlert("Erreur", "Veuillez sélectionner un type de stockage.", Alert.AlertType.ERROR);
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Veuillez entrer des valeurs numériques valides.", Alert.AlertType.ERROR);
            return false;
        }
    }

    private void showLoadingState() {
        loadingProgressBar.setProgress(0);
        loadingLabel.setText("Chargement...");
        loadingProgressBar.setVisible(true);
        loadingLabel.setVisible(true);
    }

    private void resetLoadingState() {
        loadingProgressBar.setVisible(false);
        loadingLabel.setVisible(false);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void lookAt(PerspectiveCamera camera, double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;

        double yaw = Math.toDegrees(Math.atan2(dx, dz));
        double distanceXZ = Math.sqrt(dx * dx + dz * dz);
        double pitch = Math.toDegrees(Math.atan2(-dy, distanceXZ));

        cameraYaw = yaw;
        cameraPitch = pitch;

        camera.getTransforms().clear();
        camera.getTransforms().add(new Rotate(yaw, Rotate.Y_AXIS));
        camera.getTransforms().add(new Rotate(pitch, Rotate.X_AXIS));
    }

    private void updateCameraPosition() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL) {
            return;
        }
        lastUpdateTime = currentTime;

        double radYaw = Math.toRadians(cameraYaw);
        double radPitch = Math.toRadians(cameraPitch);

        double cameraX = targetX + cameraDistance * Math.cos(radPitch) * Math.sin(radYaw);
        double cameraY = targetY - cameraDistance * Math.sin(radPitch);
        double cameraZ = targetZ + cameraDistance * Math.cos(radPitch) * Math.cos(radYaw);

        camera.setTranslateX(cameraX);
        camera.setTranslateY(cameraY);
        camera.setTranslateZ(cameraZ);

        lookAt(camera, cameraX, cameraY, cameraZ, targetX, targetY, targetZ);
    }

    private PhongMaterial createMaterial(Color baseColor, boolean transparent) {
        PhongMaterial material = new PhongMaterial(baseColor);
        if (lightingCheckBox.isSelected()) {
            material.setSpecularColor(Color.WHITE);
            material.setSpecularPower(20);
        } else {
            material.setSpecularColor(Color.TRANSPARENT);
        }
        if (transparent && transparencyCheckBox.isSelected()) {
            material.setDiffuseColor(baseColor.deriveColor(1, 1, 1, 0.3));
        }
        return material;
    }

    @FXML
    private void generate3DView() {
        if (!validateFields()) return;

        showLoadingState();
        double length = Double.parseDouble(lengthField.getText());
        double width = Double.parseDouble(widthField.getText());
        double height = Double.parseDouble(heightField.getText());
        int aisles = Integer.parseInt(aislesField.getText());
        String storageType = storageTypeCombo.getValue();
        String description = freeDescriptionArea.getText();

        previewImageView.setImage(null);
        exportImageButton.setDisable(true);
        view3DContainer.getChildren().clear();
        previewContainer.setVisible(false);
        previewLabel.setVisible(false);
        previewImageView.setVisible(false);

        Platform.runLater(() -> {
            try {
                warehouseGroup = new Group();
                draggableNodes.clear();
                annotations.clear();

                // Calculer le volume total
                totalVolume = length * width * height;
                volumeLabel.setText(String.format("Volume total: %.2f m³", totalVolume));

                // Création du sol
                Box floor = createFloor(length, width, height);
                warehouseGroup.getChildren().add(floor);

                // Création de la grille (initialement invisible)
                createGrid(length, width);
                warehouseGroup.getChildren().add(gridGroup);

                // Création des murs
                createWalls(warehouseGroup, length, width, height);

                // Création du toit
                Box roof = createRoof(length, width, height);
                warehouseGroup.getChildren().add(roof);

                // Ajout des portes et fenêtres
                createDoorsAndWindows(warehouseGroup, length, width, height);

                // Ajout des étagères selon le type sélectionné
                createStorageUnits(warehouseGroup, length, width, height, aisles, storageType);

                // Ajout d'équipements (chariots, unités de refroidissement)
                addEquipment(warehouseGroup, length, width, height);

                // Calculer l'espace utilisé
                calculateUsedSpace(length, width, height, aisles);

                // Configuration de l'éclairage
                if (lightingCheckBox.isSelected()) {
                    setupEnhancedLighting(warehouseGroup, length, width, height);
                } else {
                    AmbientLight ambientLight = new AmbientLight(Color.WHITE.deriveColor(1, 1, 0.5, 1));
                    PointLight ceilingLight1 = new PointLight(Color.WHITE);
                    ceilingLight1.setTranslateX(-length / 4);
                    ceilingLight1.setTranslateY(-height / 2 + 0.1);
                    ceilingLight1.setTranslateZ(-width / 4);
                    PointLight ceilingLight2 = new PointLight(Color.WHITE);
                    ceilingLight2.setTranslateX(length / 4);
                    ceilingLight2.setTranslateY(-height / 2 + 0.1);
                    ceilingLight2.setTranslateZ(width / 4);
                    warehouseGroup.getChildren().addAll(ambientLight, ceilingLight1, ceilingLight2);
                }

                // Configuration de la caméra
                setupCamera(length, width, height);

                // Création de la SubScene
                setupSubScene(warehouseGroup);

                // Animation de chargement
                startLoadingAnimation();

            } catch (Exception e) {
                showAlert("Erreur", "Échec génération 3D: " + e.getMessage(), Alert.AlertType.ERROR);
                resetLoadingState();
                if (renderTimer != null) {
                    renderTimer.stop();
                }
            }
        });
    }

    private Box createFloor(double length, double width, double height) {
        Box floor = new Box(length, 0.1, width);
        floor.setTranslateY(height / 2);
        floor.setMaterial(createMaterial(Color.DARKGRAY, false));
        floor.setCache(true);
        floor.setCacheHint(CacheHint.SPEED);
        floor.setUserData("floor");
        return floor;
    }

    private void createGrid(double length, double width) {
        gridGroup = new Group();
        int gridSize = (int) Math.max(length, width);
        double spacing = 1.0;
        PhongMaterial gridMaterial = new PhongMaterial(Color.GRAY);

        for (int i = -gridSize / 2; i <= gridSize / 2; i++) {
            Line hLine = new Line(i * spacing, -gridSize / 2, i * spacing, gridSize / 2);
            hLine.setStroke(gridMaterial.getDiffuseColor());
            hLine.setStrokeWidth(0.05);
            double height=0;
            hLine.setTranslateY(height / 2 - 0.01);
            gridGroup.getChildren().add(hLine);

            Line vLine = new Line(-gridSize / 2, i * spacing, gridSize / 2, i * spacing);
            vLine.setStroke(gridMaterial.getDiffuseColor());
            vLine.setStrokeWidth(0.05);
            vLine.setTranslateY(height / 2 - 0.01);
            gridGroup.getChildren().add(vLine);
        }
        gridGroup.setVisible(false);
    }

    @FXML
    private void toggleGrid() {
        if (gridGroup == null) return;
        isGridVisible = !isGridVisible;
        gridGroup.setVisible(isGridVisible);
        toggleGridButton.setStyle(isGridVisible ?
                "-fx-background-color: linear-gradient(to bottom, #4CAF50, #388E3C);" :
                "-fx-background-color: linear-gradient(to bottom, #9C27B0, #7B1FA2);");
    }

    private void createWalls(Group warehouseGroup, double length, double width, double height) {
        PhongMaterial wallMaterial = createMaterial(Color.LIGHTGRAY, true);

        Box leftWall = new Box(0.2, height, width);
        leftWall.setTranslateX(-length / 2);
        leftWall.setMaterial(wallMaterial);
        leftWall.setCache(true);
        leftWall.setCacheHint(CacheHint.SPEED);
        leftWall.setUserData("wall-left");
        warehouseGroup.getChildren().add(leftWall);

        Box rightWall = new Box(0.2, height, width);
        rightWall.setTranslateX(length / 2);
        rightWall.setMaterial(wallMaterial);
        rightWall.setCache(true);
        rightWall.setCacheHint(CacheHint.SPEED);
        rightWall.setUserData("wall-right");
        warehouseGroup.getChildren().add(rightWall);

        Box backWall = new Box(length, height, 0.2);
        backWall.setTranslateZ(-width / 2);
        backWall.setMaterial(wallMaterial);
        backWall.setCache(true);
        backWall.setCacheHint(CacheHint.SPEED);
        backWall.setUserData("wall-back");
        warehouseGroup.getChildren().add(backWall);
    }

    private Box createRoof(double length, double width, double height) {
        Box roof = new Box(length, 0.2, width);
        roof.setTranslateY(-height / 2);
        roof.setMaterial(createMaterial(Color.DARKSLATEGRAY, true));
        roof.setCache(true);
        roof.setCacheHint(CacheHint.SPEED);
        roof.setUserData("roof");
        return roof;
    }

    private void createDoorsAndWindows(Group warehouseGroup, double length, double width, double height) {
        Box door = new Box(2, 3, 0.1);
        door.setTranslateX(-length / 4);
        door.setTranslateY(height / 2 - 1.5);
        door.setTranslateZ(width / 2 - 0.05);
        door.setMaterial(createMaterial(Color.BROWN, false));
        door.setCache(true);
        door.setCacheHint(CacheHint.SPEED);
        door.setUserData("door");
        warehouseGroup.getChildren().add(door);

        for (int i = 0; i < 2; i++) {
            Box window = new Box(0.1, 1, 1);
            window.setTranslateX(length / 2 - 0.05);
            window.setTranslateY(height / 2 - 2 - i * 2);
            window.setTranslateZ(-width / 4 + i * (width / 2));
            PhongMaterial windowMaterial = new PhongMaterial(Color.LIGHTCYAN);
            windowMaterial.setSpecularColor(Color.WHITE);
            windowMaterial.setSpecularPower(20);
            window.setMaterial(windowMaterial);
            window.setCache(true);
            window.setCacheHint(CacheHint.SPEED);
            window.setUserData("window-" + i);
            warehouseGroup.getChildren().add(window);
        }
    }

    private void createStorageUnits(Group warehouseGroup, double length, double width, double height, int aisles, String storageType) {
        double aisleWidth = width / (aisles + 1);

        for (int i = 0; i < aisles; i++) {
            Group shelfGroup = new Group();
            double xPos = -length / 2 + (i + 1) * (length / (aisles + 1));

            if (storageType.contains("Rayonnages")) {
                // Always use the enhanced version of metal shelves (previously premium)
                createMetalShelves(shelfGroup, width, height, true, i);
            } else if (storageType.contains("Palettes")) {
                createPallets(shelfGroup, length, width, height, i);
            } else {
                createBasicShelves(shelfGroup, width, height, i);
            }

            shelfGroup.setTranslateX(xPos);
            shelfGroup.setUserData("shelf-system-" + i);
            warehouseGroup.getChildren().add(shelfGroup);
        }
    }

    private void createMetalShelves(Group shelfGroup, double width, double height, boolean enhanced, int aisleIndex) {
        int levels = enhanced ? 5 : 4;
        double shelfWidth = width * 0.7;
        double shelfDepth = enhanced ? 1.0 : 0.8;

        PhongMaterial shelfMaterial = createMaterial(Color.SILVER, false);
        PhongMaterial supportMaterial = createMaterial(Color.DARKGRAY, false);

        for (int level = 0; level < levels; level++) {
            double zPos = (level + 1) * height / (levels + 1);

            Box shelf = new Box(shelfDepth, shelfWidth, 0.05);
            shelf.setTranslateZ(zPos);
            shelf.setMaterial(shelfMaterial);
            shelf.setCache(true);
            shelf.setCacheHint(CacheHint.SPEED);
            shelf.setUserData("shelf-aisle" + aisleIndex + "-level" + level);
            shelfGroup.getChildren().add(shelf);

            if (level < levels - 1) {
                double supportHeight = height / (levels + 1);

                Cylinder leftSupport = new Cylinder(0.05, supportHeight, 8);
                leftSupport.setTranslateY(-shelfWidth / 2 + 0.1);
                leftSupport.setTranslateZ(zPos + supportHeight / 2);
                leftSupport.setRotationAxis(Rotate.X_AXIS);
                leftSupport.setRotate(90);
                leftSupport.setMaterial(supportMaterial);
                leftSupport.setCache(true);
                leftSupport.setCacheHint(CacheHint.SPEED);
                leftSupport.setUserData("support-left-aisle" + aisleIndex + "-level" + level);
                shelfGroup.getChildren().add(leftSupport);

                Cylinder rightSupport = new Cylinder(0.05, supportHeight, 8);
                rightSupport.setTranslateY(shelfWidth / 2 - 0.1);
                rightSupport.setTranslateZ(zPos + supportHeight / 2);
                rightSupport.setRotationAxis(Rotate.X_AXIS);
                rightSupport.setRotate(90);
                rightSupport.setMaterial(supportMaterial);
                rightSupport.setCache(true);
                rightSupport.setCacheHint(CacheHint.SPEED);
                rightSupport.setUserData("support-right-aisle" + aisleIndex + "-level" + level);
                shelfGroup.getChildren().add(rightSupport);
            }

            if (enhanced && level > 0) {
                addEnhancedShelfDetails(shelfGroup, shelfWidth, zPos, level, aisleIndex);
            }
        }
    }

    private void addEnhancedShelfDetails(Group shelfGroup, double shelfWidth, double zPos, int level, int aisleIndex) {
        Sphere label = new Sphere(0.05);
        label.setMaterial(new PhongMaterial(Color.RED));
        label.setTranslateY(-shelfWidth / 2 + 0.2);
        label.setTranslateZ(zPos + 0.1);
        label.setCache(true);
        label.setCacheHint(CacheHint.SPEED);
        label.setUserData("label-aisle" + aisleIndex + "-level" + level);
        shelfGroup.getChildren().add(label);

        if (level == 3) {
            Box safetyBar = new Box(0.8, 0.05, 0.05);
            safetyBar.setTranslateZ(zPos + 0.2);
            safetyBar.setMaterial(new PhongMaterial(Color.YELLOW));
            safetyBar.setCache(true);
            safetyBar.setCacheHint(CacheHint.SPEED);
            safetyBar.setUserData("safetyBar-aisle" + aisleIndex + "-level" + level);
            shelfGroup.getChildren().add(safetyBar);
        }
    }

    private void createPallets(Group shelfGroup, double length, double width, double height, int aisleIndex) {
        double aisleWidth = width / (aislesField.getText().isEmpty() ? 1 : Integer.parseInt(aislesField.getText()) + 1);
        int crateCount = (int) (length / 2);
        boolean isVegetables = (aisleIndex % 2 == 0);

        for (int j = 0; j < crateCount; j++) {
            Box crate = new Box(1.5, 0.8, 1.5);
            crate.setTranslateX(-length * 0.4 + j * (length * 0.8 / crateCount));
            crate.setTranslateY(height / 2 - 0.4);
            crate.setTranslateZ((aisleIndex + 1) * aisleWidth - width / 2);
            PhongMaterial crateMaterial = new PhongMaterial();
            crateMaterial.setDiffuseColor(isVegetables ? Color.GREEN : Color.ORANGE);
            crateMaterial.setSpecularColor(Color.WHITE);
            crateMaterial.setSpecularPower(10);
            crate.setMaterial(crateMaterial);
            crate.setCache(true);
            crate.setCacheHint(CacheHint.SPEED);
            crate.setUserData("crate-aisle" + aisleIndex + "-index" + j);
            shelfGroup.getChildren().add(crate);
        }
    }

    private void createBasicShelves(Group shelfGroup, double width, double height, int aisleIndex) {
        double shelfWidth = width * 0.7;
        int levels = 3;

        PhongMaterial shelfMaterial = createMaterial(Color.BROWN, false);

        for (int level = 0; level < levels; level++) {
            Box shelf = new Box(0.8, shelfWidth, 0.05);
            shelf.setTranslateZ((level + 1) * height / (levels + 1));
            shelf.setMaterial(shelfMaterial);
            shelf.setCache(true);
            shelf.setCacheHint(CacheHint.SPEED);
            shelf.setUserData("basic-shelf-aisle" + aisleIndex + "-level" + level);
            shelfGroup.getChildren().add(shelf);
        }
    }

    private void addEquipment(Group warehouseGroup, double length, double width, double height) {
        for (int i = 0; i < 2; i++) {
            Box coolingUnit = new Box(1, 2, 0.5);
            coolingUnit.setTranslateX(-length / 2 + 0.3);
            coolingUnit.setTranslateY(height / 2 - 1);
            coolingUnit.setTranslateZ(-width / 4 + i * (width / 2));
            coolingUnit.setMaterial(createMaterial(Color.LIGHTBLUE, false));
            coolingUnit.setCache(true);
            coolingUnit.setCacheHint(CacheHint.SPEED);
            coolingUnit.setUserData("coolingUnit-" + i);
            makeNodeDraggable(coolingUnit, length, width, height);
            warehouseGroup.getChildren().add(coolingUnit);
            draggableNodes.add(coolingUnit);
        }

        createForklift(warehouseGroup, length, width, height);
    }

    private void createForklift(Group warehouseGroup, double length, double width, double height) {
        forklift = new Group();

        Box body = new Box(1.5, 0.8, 0.8);
        body.setMaterial(createMaterial(Color.ORANGE, false));
        body.setUserData("forklift-body");

        Box cabin = new Box(0.8, 0.7, 0.7);
        cabin.setTranslateX(0.3);
        cabin.setTranslateZ(0.5);
        cabin.setMaterial(createMaterial(Color.BLUE, false));
        cabin.setUserData("forklift-cabin");

        Box fork1 = new Box(0.1, 0.6, 0.2);
        fork1.setTranslateX(-0.5);
        fork1.setTranslateZ(0.3);
        fork1.setMaterial(createMaterial(Color.SILVER, false));
        fork1.setUserData("forklift-fork1");

        Box fork2 = new Box(0.1, 0.6, 0.2);
        fork2.setTranslateX(-0.5);
        fork2.setTranslateZ(0.1);
        fork2.setMaterial(createMaterial(Color.SILVER, false));
        fork2.setUserData("forklift-fork2");

        Box mast = new Box(0.1, 0.1, 1.5);
        mast.setTranslateX(-0.5);
        mast.setTranslateZ(1.1);
        mast.setMaterial(createMaterial(Color.SILVER, false));
        mast.setUserData("forklift-mast");

        forklift.getChildren().addAll(body, cabin, fork1, fork2, mast);
        forklift.setTranslateX(-length / 3);
        forklift.setTranslateY(height / 2 - 0.4);
        forklift.setTranslateZ(width / 3);
        forklift.setUserData("forklift");
        makeNodeDraggable(forklift, length, width, height);
        warehouseGroup.getChildren().add(forklift);
        draggableNodes.add(forklift);

        if (animationCheckBox.isSelected()) {
            startForkliftAnimation(length);
        }
    }

    private void makeNodeDraggable(Node node, double length, double width, double height) {
        final double[] mousePos = new double[2];
        node.setOnMousePressed(event -> {
            mousePos[0] = event.getSceneX();
            mousePos[1] = event.getSceneY();
        });

        node.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - mousePos[0];
            double deltaZ = event.getSceneY() - mousePos[1];

            double newX = node.getTranslateX() + deltaX * 0.05;
            double newZ = node.getTranslateZ() + deltaZ * 0.05;

            // Limiter le déplacement à l'intérieur de l'entrepôt
            double halfLength = length / 2 - 1;
            double halfWidth = width / 2 - 1;
            newX = Math.max(-halfLength, Math.min(halfLength, newX));
            newZ = Math.max(-halfWidth, Math.min(halfWidth, newZ));

            node.setTranslateX(newX);
            node.setTranslateZ(newZ);

            mousePos[0] = event.getSceneX();
            mousePos[1] = event.getSceneY();
        });
    }

    private void startForkliftAnimation(double length) {
        if (forklift == null) return;

        forkliftAnimation = new Timeline();
        forkliftAnimation.setCycleCount(Timeline.INDEFINITE);
        forkliftAnimation.setAutoReverse(true);

        KeyFrame moveFrame = new KeyFrame(Duration.seconds(5),
                event -> {
                    double startX = -length / 3;
                    double endX = length / 3;
                    forklift.setTranslateX(startX);
                    forklift.setTranslateX(endX);
                });

        forkliftAnimation.getKeyFrames().add(moveFrame);
        if (animationCheckBox.isSelected()) {
            forkliftAnimation.play();
        }
    }

    private void calculateUsedSpace(double length, double width, double height, int aisles) {
        // Approximation: chaque unité de stockage (palette ou étagère) utilise un volume fixe
        double unitVolume;
        String storageType = storageTypeCombo.getValue();
        if (storageType.contains("Palettes")) {
            unitVolume = 1.5 * 0.8 * 1.5; // Volume d'une palette
        } else {
            unitVolume = 1.0 * (width * 0.7) * 0.05 * (storageType.contains("Rayonnages") ? 5 : 4); // Volume d'une étagère
        }
        usedSpace = (unitVolume * aisles) / totalVolume * 100;
        spaceUsedLabel.setText(String.format("Espace utilisé: %.2f%%", usedSpace));
    }

    private void setupEnhancedLighting(Group warehouseGroup, double length, double width, double height) {
        PointLight mainLight = new PointLight(Color.WHITE.deriveColor(1, 1, 0.9, 1));
        mainLight.setTranslateX(0);
        mainLight.setTranslateY(-height / 2 + 0.1);
        mainLight.setTranslateZ(0);
        warehouseGroup.getChildren().add(mainLight);

        AmbientLight ambientLight = new AmbientLight(Color.WHITE.deriveColor(1, 1, 0.4, 1));
        warehouseGroup.getChildren().add(ambientLight);

        for (int i = 0; i < 4; i++) {
            PointLight spotLight = new PointLight(Color.WHITE.deriveColor(1, 1, 0.8, 1));
            spotLight.setTranslateX(-length / 2 + 0.5 + i * (length / 3));
            spotLight.setTranslateY(-height / 2 + 0.1);
            spotLight.setTranslateZ(-width / 2 + 0.5 + (i % 2) * (width - 1));
            warehouseGroup.getChildren().add(spotLight);
        }
    }

    private void setupCamera(double length, double width, double height) {
        camera = new PerspectiveCamera(true);
        double maxDimension = Math.max(length, Math.max(width, height));
        cameraDistance = maxDimension * 1.5;
        targetX = 0;
        targetY = height / 4;
        targetZ = 0;
        camera.setFarClip(maxDimension * 10);
        camera.setNearClip(0.1);
        camera.setFieldOfView(45);
        updateCameraPosition();
    }

    private void setupSubScene(Group warehouseGroup) {
        subScene = new SubScene(warehouseGroup, view3DContainer.getWidth(), view3DContainer.getHeight(),
                true, SceneAntialiasing.BALANCED);
        subScene.widthProperty().bind(view3DContainer.widthProperty());
        subScene.heightProperty().bind(view3DContainer.heightProperty());
        subScene.setCamera(camera);
        subScene.setFill(Color.WHITE);

        setupCameraControls(subScene);
        setupObjectInteraction(subScene);

        view3DContainer.getChildren().clear();
        view3DContainer.getChildren().add(subScene);
    }

    private void setupCameraControls(SubScene subScene) {
        final double[] mousePos = new double[2];

        subScene.setOnMousePressed(event -> {
            mousePos[0] = event.getSceneX();
            mousePos[1] = event.getSceneY();
        });

        subScene.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - mousePos[0];
            double deltaY = event.getSceneY() - mousePos[1];

            cameraYaw -= deltaX * 0.2;
            cameraPitch = Math.max(-80, Math.min(80, cameraPitch - deltaY * 0.2));
            updateCameraPosition();

            mousePos[0] = event.getSceneX();
            mousePos[1] = event.getSceneY();
        });

        subScene.setOnScroll(event -> {
            double delta = event.getDeltaY();
            double maxDimension = Math.max(
                    Double.parseDouble(lengthField.getText()),
                    Math.max(
                            Double.parseDouble(widthField.getText()),
                            Double.parseDouble(heightField.getText())
                    )
            );
            cameraDistance = Math.max(maxDimension * 0.5, Math.min(maxDimension * 3, cameraDistance - delta * 0.1));
            updateCameraPosition();
        });
    }

    private void setupObjectInteraction(SubScene subScene) {
        subScene.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (warehouseGroup == null) return;

            Node picked = event.getPickResult().getIntersectedNode();
            if (picked != null && picked != subScene && picked != warehouseGroup) {
                if (isMeasuring) {
                    handleMeasurement(picked);
                } else {
                    highlightObject(picked);
                    showObjectInfo(picked);
                }
            }
        });
    }

    private void highlightObject(Node node) {
        if (selectedNode != null && selectedNode instanceof Shape3D) {
            ((Shape3D) selectedNode).setMaterial(originalMaterial);
        }

        if (node instanceof Shape3D) {
            Shape3D shape = (Shape3D) node;
            originalMaterial = (PhongMaterial) shape.getMaterial();
            PhongMaterial highlightMaterial = new PhongMaterial(Color.YELLOW);
            shape.setMaterial(highlightMaterial);
            selectedNode = shape;
        }
    }

    private void showObjectInfo(Node node) {
        String userData = (String) node.getUserData();
        if (userData != null) {
            showAlert("Information", "Objet sélectionné: " + userData, Alert.AlertType.INFORMATION);
        }
    }

    @FXML
    private void toggleMeasureMode() {
        isMeasuring = !isMeasuring;
        toggleMeasureButton.setStyle(isMeasuring ?
                "-fx-background-color: linear-gradient(to bottom, #4CAF50, #388E3C);" :
                "-fx-background-color: linear-gradient(to bottom, #9C27B0, #7B1FA2);");
        measurementLabel.setVisible(isMeasuring);
        if (!isMeasuring) {
            resetMeasurement();
        }
    }

    private void handleMeasurement(Node node) {
        measurePoints.add(node);
        if (measurePoints.size() == 2) {
            createMeasurementLine();
            calculateDistance();
        }
    }

    private void createMeasurementLine() {
        if (measureLine != null) {
            warehouseGroup.getChildren().remove(measureLine);
        }

        Node point1 = measurePoints.get(0);
        Node point2 = measurePoints.get(1);

        measureLine = new Line(
                point1.getTranslateX(), point1.getTranslateZ(),
                point2.getTranslateX(), point2.getTranslateZ()
        );
        measureLine.setStroke(Color.RED);
        measureLine.setStrokeWidth(2);
        measureLine.setTranslateY(point1.getTranslateY());
        warehouseGroup.getChildren().add(measureLine);
    }

    private void calculateDistance() {
        if (measurePoints.size() < 2) return;

        Node point1 = measurePoints.get(0);
        Node point2 = measurePoints.get(1);

        double dx = point1.getTranslateX() - point2.getTranslateX();
        double dz = point1.getTranslateZ() - point2.getTranslateZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        measurementLabel.setText(String.format("Mesure: %.2f m", distance));
    }

    private void resetMeasurement() {
        if (measureLine != null) {
            warehouseGroup.getChildren().remove(measureLine);
            measureLine = null;
        }
        measurePoints.clear();
        measurementLabel.setText("Mesure: 0 m");
    }

    @FXML
    private void toggleSection() {
        if (sectionPlane != null) {
            warehouseGroup.getChildren().remove(sectionPlane);
            sectionPlane = null;
            toggleSectionButton.setStyle("-fx-background-color: linear-gradient(to bottom, #9C27B0, #7B1FA2);");
            return;
        }

        double length = Double.parseDouble(lengthField.getText());
        double width = Double.parseDouble(widthField.getText());
        double height = Double.parseDouble(heightField.getText());

        sectionPlane = new Box(length * 2, width * 2, 0.05);
        sectionPlane.setMaterial(new PhongMaterial(Color.CYAN.deriveColor(1, 1, 1, 0.3)));
        sectionPlane.setTranslateY(height / 4);
        sectionPlane.setRotationAxis(Rotate.X_AXIS);
        sectionPlane.setRotate(90);
        warehouseGroup.getChildren().add(sectionPlane);
        toggleSectionButton.setStyle("-fx-background-color: linear-gradient(to bottom, #4CAF50, #388E3C);");
    }

    @FXML
    private void toggleWireframe() {
        isWireframe = !isWireframe;
        toggleWireframeButton.setStyle(isWireframe ?
                "-fx-background-color: linear-gradient(to bottom, #4CAF50, #388E3C);" :
                "-fx-background-color: linear-gradient(to bottom, #9C27B0, #7B1FA2);");

        warehouseGroup.getChildren().forEach(node -> {
            if (node instanceof Shape3D) {
                Shape3D shape = (Shape3D) node;
                shape.setDrawMode(isWireframe ? DrawMode.LINE : DrawMode.FILL);
            }
        });
    }

    private void updateWallColor() {
        Color newColor = wallColorPicker.getValue();
        warehouseGroup.getChildren().forEach(node -> {
            String userData = (String) node.getUserData();
            if (userData != null && (userData.startsWith("wall") || userData.equals("roof"))) {
                if (node instanceof Shape3D) {
                    Shape3D shape = (Shape3D) node;
                    shape.setMaterial(createMaterial(newColor, true));
                }
            }
        });
    }

    private void updateFloorColor() {
        Color newColor = floorColorPicker.getValue();
        warehouseGroup.getChildren().forEach(node -> {
            String userData = (String) node.getUserData();
            if (userData != null && userData.equals("floor")) {
                if (node instanceof Shape3D) {
                    Shape3D shape = (Shape3D) node;
                    shape.setMaterial(createMaterial(newColor, false));
                }
            }
        });
    }

    @FXML
    private void addAnnotation() {
        if (selectedNode == null || annotationField.getText().isEmpty()) {
            showAlert("Erreur", "Veuillez sélectionner un objet et entrer un texte d'annotation.", Alert.AlertType.WARNING);
            return;
        }

        Text annotation = new Text(annotationField.getText());
        annotation.setFill(Color.BLACK);
        annotation.setTranslateX(selectedNode.getTranslateX());
        annotation.setTranslateY(selectedNode.getTranslateY() - 1);
        annotation.setTranslateZ(selectedNode.getTranslateZ());
        warehouseGroup.getChildren().add(annotation);
        annotations.add(annotation);
        annotationField.clear();
    }

    private void startLoadingAnimation() {
        final int[] frameCount = {0};
        renderTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                frameCount[0]++;
                double progress = Math.min(frameCount[0] / 180.0, 1.0);
                loadingProgressBar.setProgress(progress);
                loadingLabel.setText("Chargement: " + (int) (progress * 100) + "%");

                if (frameCount[0] >= 180) {
                    exportImageButton.setDisable(false);
                    resetLoadingState();
                    stop();
                }
            }
        };
        renderTimer.start();
    }

    private ChangeListener<Number> createRenderListener(Runnable callback) {
        ChangeListener<Number> listener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> obs, Number oldValue, Number newValue) {
                obs.removeListener(this);
                if (obs == subScene.widthProperty()) {
                    subScene.heightProperty().removeListener(this);
                } else {
                    subScene.widthProperty().removeListener(this);
                }

                Platform.runLater(() -> {
                    if (subScene.getWidth() > 0 && subScene.getHeight() > 0) {
                        callback.run();
                    } else {
                        showAlert("Erreur", "SubScene non rendu: dimensions invalides.", Alert.AlertType.ERROR);
                    }
                });
            }
        };
        return listener;
    }

    private void ensureRendered(Runnable callback) {
        if (subScene == null || subScene.getRoot() == null) {
            showAlert("Erreur", "Aucune visualisation 3D à exporter.", Alert.AlertType.WARNING);
            return;
        }

        subScene.getScene().getRoot().requestLayout();
        subScene.requestFocus();

        ChangeListener<Number> renderListener = createRenderListener(callback);

        subScene.widthProperty().addListener(renderListener);
        subScene.heightProperty().addListener(renderListener);

        subScene.getScene().getRoot().requestLayout();
        subScene.getScene().getRoot().layout();
    }

    @FXML
    private void exportImage() {
        if (subScene == null) {
            showAlert("Erreur", "Aucune visualisation 3D à exporter", Alert.AlertType.WARNING);
            return;
        }

        double length = Double.parseDouble(lengthField.getText());
        double width = Double.parseDouble(widthField.getText());
        double height = Double.parseDouble(heightField.getText());
        double maxDimension = Math.max(length, Math.max(width, height));

        cameraDistance = maxDimension * 1.5;
        cameraYaw = 180;
        cameraPitch = 29;
        targetX = 0;
        targetY = height / 4;
        targetZ = 0;
        updateCameraPosition();

        ensureRendered(() -> {
            try {
                SnapshotParameters params = new SnapshotParameters();
                params.setCamera(camera);
                params.setFill(Color.WHITE);
                double snapshotWidth = Math.min(subScene.getWidth(), 1280);
                double snapshotHeight = Math.min(subScene.getHeight(), 720);

                WritableImage snapshot = subScene.snapshot(params, new WritableImage((int) snapshotWidth, (int) snapshotHeight));
                if (snapshot.getWidth() <= 0 || snapshot.getHeight() <= 0) {
                    showAlert("Erreur", "Échec capture image: snapshot vide", Alert.AlertType.ERROR);
                    return;
                }

                previewImageView.setScaleX(1.0);
                previewImageView.setScaleY(1.0);
                previewImageView.setFitWidth(snapshotWidth);
                previewImageView.setFitHeight(snapshotHeight);
                previewImageView.setImage(snapshot);
                previewContainer.setVisible(true);
                previewLabel.setVisible(true);
                previewImageView.setVisible(true);
                updateScrollPaneViewport();

                lastImageData = convertImageToBase64(snapshot);

                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Sauvegarder l'image 3D");
                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("PNG Image", "*.png"));
                fileChooser.setInitialFileName("entrepot_3d.png");

                File file = fileChooser.showSaveDialog(view3DContainer.getScene().getWindow());
                if (file != null) {
                    String base64 = lastImageData.replace("data:image/png;base64,", "");
                    byte[] imageBytes = Base64.getDecoder().decode(base64);
                    BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
                    ImageIO.write(bufferedImage, "png", file);
                    showAlert("Succès", "Image exportée avec succès: " + file.getAbsolutePath(),
                            Alert.AlertType.INFORMATION);
                }
            } catch (Exception e) {
                showAlert("Erreur", "Échec de l'exportation: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private String convertImageToBase64(WritableImage image) throws Exception {
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
    }

    @FXML
    private void resetForm() {
        lengthField.clear();
        widthField.clear();
        heightField.clear();
        aislesField.clear();
        storageTypeCombo.getSelectionModel().clearSelection();
        freeDescriptionArea.clear();
        previewImageView.setImage(null);
        previewContainer.setVisible(false);
        previewLabel.setVisible(false);
        previewImageView.setVisible(false);
        exportImageButton.setDisable(true);
        view3DContainer.getChildren().clear();
        resetLoadingState();
        if (renderTimer != null) {
            renderTimer.stop();
        }
        if (forkliftAnimation != null) {
            forkliftAnimation.stop();
        }
        gridGroup = null;
        sectionPlane = null;
        measureLine = null;
        measurePoints.clear();
        selectedNode = null;
        isGridVisible = false;
        isMeasuring = false;
        isWireframe = false;
        draggableNodes.clear();
        annotations.clear();
        volumeLabel.setText("Volume total: 0 m³");
        spaceUsedLabel.setText("Espace utilisé: 0%");
    }

    @FXML
    private void goBack() {
        try {
            Stage stage = (Stage) view3DContainer.getScene().getWindow();
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/com/example/Stock/view/AddEntrepotForm.fxml")));
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            showAlert("Erreur", "Navigation impossible: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}