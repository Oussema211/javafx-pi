package com.example.auth.controller;

import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.auth.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import javafx.scene.layout.VBox;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckBox;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Button loginButton;
    @FXML private Hyperlink registerLink;
    @FXML private Label messageLabel;
    @FXML private Button signInWithFaceButton;
    @FXML private ImageView webcamPreview;

    private AuthService authService = new AuthService();
    private SessionManager sessionManager = SessionManager.getInstance();
    private VideoCapture capture;
    private CascadeClassifier faceDetector;
    private boolean isCapturing = false;
    private ScheduledExecutorService timer;
    private Stage captureStage;

    @FXML
    public void initialize() {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

            URL resource = getClass().getResource("/haarcascade_frontalface_default.xml");
            if (resource == null) {
                System.err.println("ERROR: haarcascade_frontalface_default.xml not found in resources");
                messageLabel.setText("Cannot load face detection file");
                return;
            }

            String haarCascadePath = Paths.get(resource.toURI()).toString();
            faceDetector = new CascadeClassifier(haarCascadePath);

            if (faceDetector.empty()) {
                System.err.println("ERROR: Failed to load haarcascade_frontalface_default.xml");
                messageLabel.setText("Cannot load face detection");
            }

        } catch (UnsatisfiedLinkError e) {
            System.err.println("ERROR: Failed to load OpenCV native library: " + e.getMessage());
            messageLabel.setText("Cannot load OpenCV library");
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            messageLabel.setText("Initialization error");
        }
    }

    @FXML
    private void onLoginClicked() throws IOException {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter username and password");
            return;
        }

        User user = authService.login(username, password);
        if (user == null) {
            messageLabel.setText("Invalid username or password");
            return;
        }

        sessionManager.setLoggedInUser(user);
        String fxmlFile = user.hasRole("ROLE_ADMIN") ? "/com/example/auth/dashboard.fxml" : "/com/example/reclamation/Reclamation.fxml";
        loadScene(fxmlFile);
    }

    @FXML
    private void onForgotPasswordClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auth/resetPassword.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Reset Password");
            stage.setScene(new Scene(root, 400, 400));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onRegisterClicked() {
        try {
            java.net.URL fxmlUrl = getClass().getResource("/com/example/auth/signup.fxml");
            if (fxmlUrl == null) {
                messageLabel.setText("Error: Signup page not found");
                System.out.println("DEBUG: signup.fxml not found at /com/example/auth/signup.fxml");
                return;
            }
            loadScene("/com/example/auth/signup.fxml");
        } catch (IOException e) {
            messageLabel.setText("Error loading signup page");
            System.out.println("DEBUG: Error switching to signup screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void signInWithFace() {
        if (!isCapturing) {
            startFaceCapture();
        } else {
            stopFaceCapture();
        }
    }

    private void startFaceCapture() {
        capture = new VideoCapture(0);
        if (!capture.isOpened()) {
            messageLabel.setText("Cannot open webcam");
            return;
        }

        // Create a new window for face capture
        captureStage = new Stage();
        captureStage.setTitle("Face Capture");
        
        ImageView captureView = new ImageView();
        captureView.setFitWidth(640);
        captureView.setFitHeight(480);
        captureView.setPreserveRatio(true);
        
        Button captureButton = new Button("Capture");
        captureButton.setOnAction(e -> captureAndVerifyFace());
        
        VBox root = new VBox(10, captureView, captureButton);
        Scene scene = new Scene(root);
        captureStage.setScene(scene);
        captureStage.show();
        
        isCapturing = true;
        signInWithFaceButton.setText("Stop Face Capture");

        // Create a timer to update the preview
        timer = Executors.newSingleThreadScheduledExecutor();
        timer.scheduleAtFixedRate(() -> {
            Mat frame = new Mat();
            if (capture.read(frame)) {
                // Detect faces
                MatOfRect faces = new MatOfRect();
                faceDetector.detectMultiScale(frame, faces);
                
                // Draw rectangles around faces
                for (Rect rect : faces.toArray()) {
                    Imgproc.rectangle(frame, new Point(rect.x, rect.y), 
                        new Point(rect.x + rect.width, rect.y + rect.height), 
                        new Scalar(0, 255, 0), 3);
                }
                
                // Convert to JavaFX image
                Image fxImage = matToImage(frame);
                
                // Update on JavaFX thread
                Platform.runLater(() -> captureView.setImage(fxImage));
            }
        }, 0, 33, TimeUnit.MILLISECONDS);
    }

    private void stopFaceCapture() {
        isCapturing = false;
        signInWithFaceButton.setText("Sign In With Face");
        
        if (timer != null && !timer.isShutdown()) {
            timer.shutdown();
            try {
                timer.awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                System.err.println("Exception in stopping the frame capture: " + e);
            }
        }
        
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
        
        if (captureStage != null) {
            captureStage.close();
        }
    }

    private void captureAndVerifyFace() {
        Mat frame = new Mat();
        if (capture.read(frame)) {
            MatOfRect faces = new MatOfRect();
            faceDetector.detectMultiScale(frame, faces);
            
            if (!faces.empty()) {
                Rect face = faces.toArray()[0];
                Mat liveFace = new Mat(frame, face);
                
                String username = usernameField.getText().trim();
                if (username.isEmpty()) {
                    Platform.runLater(() -> messageLabel.setText("Please enter a username"));
                    return;
                }
                
                File storedFaceFile = new File("faces/" + username + ".jpg");
                if (!storedFaceFile.exists()) {
                    Platform.runLater(() -> messageLabel.setText("No face data for " + username));
                    return;
                }
                
                Mat storedFace = Imgcodecs.imread(storedFaceFile.getPath());
                if (storedFace.empty()) {
                    Platform.runLater(() -> messageLabel.setText("Failed to load stored face"));
                    return;
                }
                
                double similarity = compareHistograms(liveFace, storedFace);
                if (similarity > 0.7) {
                    User user = authService.authenticate(username, null);
                    if (user != null) {
                        sessionManager.setLoggedInUser(user);
                        try {
                            String fxmlFile = user.hasRole("ROLE_ADMIN") ? "/com/example/auth/dashboard.fxml" : "/com/example/reclamation/Reclamation.fxml";
                            Platform.runLater(() -> {
                                try {
                                    loadScene(fxmlFile);
                                } catch (IOException e) {
                                    messageLabel.setText("Error loading dashboard");
                                    e.printStackTrace();
                                }
                            });
                        } catch (Exception e) {
                            Platform.runLater(() -> messageLabel.setText("Authentication error"));
                            e.printStackTrace();
                        }
                    } else {
                        Platform.runLater(() -> messageLabel.setText("Authentication failed"));
                    }
                } else {
                    Platform.runLater(() -> messageLabel.setText("Face does not match"));
                }
            } else {
                Platform.runLater(() -> messageLabel.setText("No face detected"));
            }
        } else {
            Platform.runLater(() -> messageLabel.setText("Failed to capture frame"));
        }
    }

    private double compareHistograms(Mat img1, Mat img2) {
        // Convert to HSV color space
        Mat hsv1 = new Mat(), hsv2 = new Mat();
        Imgproc.cvtColor(img1, hsv1, Imgproc.COLOR_BGR2HSV);
        Imgproc.cvtColor(img2, hsv2, Imgproc.COLOR_BGR2HSV);

        // Calculate histograms
        Mat hist1 = new Mat(), hist2 = new Mat();
        MatOfInt histSize = new MatOfInt(50);
        MatOfFloat ranges = new MatOfFloat(0f, 180f);
        MatOfInt channels = new MatOfInt(0);

        Imgproc.calcHist(Arrays.asList(hsv1), channels, new Mat(), hist1, histSize, ranges);
        Imgproc.calcHist(Arrays.asList(hsv2), channels, new Mat(), hist2, histSize, ranges);

        // Normalize histograms
        Core.normalize(hist1, hist1, 0, 1, Core.NORM_MINMAX, -1, new Mat());
        Core.normalize(hist2, hist2, 0, 1, Core.NORM_MINMAX, -1, new Mat());

        // Compare histograms
        return Imgproc.compareHist(hist1, hist2, Imgproc.HISTCMP_CORREL);
    }

    private void loadScene(String fxmlFile) throws IOException {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        boolean isFullScreen = stage.isFullScreen();
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        if (root == null) {
            System.out.println("DEBUG: Failed to load " + fxmlFile + " - root is null");
            return;
        }
        Scene scene = new Scene(root, 400, 500);
        java.net.URL stylesheetUrl = getClass().getClassLoader().getResource("com/example/auth/styles.css");
        if (stylesheetUrl != null) {
            scene.getStylesheets().add(stylesheetUrl.toExternalForm());
        } else {
            System.out.println("DEBUG: Could not find styles.css");
        }
        stage.setScene(scene);
        stage.setFullScreen(isFullScreen);
        stage.show();
    }

    private Image matToImage(Mat mat) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", mat, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

    public void shutdown() {
        stopFaceCapture();
    }
}