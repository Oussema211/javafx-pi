package com.example.auth.controller;

import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.auth.service.GeminiChatService;
import com.example.auth.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    @FXML private VBox chatCard;
    @FXML private TextArea chatHistory;
    @FXML private TextField chatInput;
    @FXML private Button sendChatButton;

    private AuthService authService = new AuthService();
    private SessionManager sessionManager = SessionManager.getInstance();
    private GeminiChatService chatService;
    private VideoCapture capture;
    private CascadeClassifier faceDetector;
    private boolean isCapturing = false;
    private ScheduledExecutorService timer;
    private Stage captureStage;

    @FXML
    public void initialize() {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            InputStream xmlStream = getClass().getResourceAsStream("/haarcascade_frontalface_default.xml");
            if (xmlStream == null) {
                System.err.println("ERROR: haarcascade_frontalface_default.xml not found in resources");
                messageLabel.setText("Cannot load face detection file");
                return;
            }

            File tempFile = File.createTempFile("haarcascade_frontalface_default", ".xml");
            tempFile.deleteOnExit();
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = xmlStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            faceDetector = new CascadeClassifier(tempFile.getAbsolutePath());
            if (faceDetector.empty()) {
                System.err.println("ERROR: Failed to load haarcascade_frontalface_default.xml");
                messageLabel.setText("Cannot load face detection");
            }

            chatService = new GeminiChatService();
            System.out.println("chatCard: " + chatCard);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("ERROR: Failed to load OpenCV native library: " + e.getMessage());
            messageLabel.setText("Cannot load OpenCV library");
        } catch (Exception e) {
            System.err.println("ERROR: Initialization error: " + e.getMessage());
            messageLabel.setText("Initialization error");
        }
    }

    @FXML
    private void onLoginClicked() {
        String email = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter email and password");
            return;
        }

        User user = authService.login(email, password);
        if (user == null) {
            messageLabel.setText("Invalid email or password");
            return;
        }

        sessionManager.setLoggedInUser(user);
        String fxmlFile = user.hasRole("ROLE_ADMIN") ? "/com/example/auth/dashboard.fxml" : "/com/example/frontPages/dashboard.fxml";
        try {
            loadScene(fxmlFile);
        } catch (IOException e) {
            System.err.println("Error loading dashboard: " + e.getMessage());
            messageLabel.setText("Error loading dashboard: " + e.getMessage());
        }
    }

    @FXML
    private void handleGmailLogin() {
        new Thread(() -> {
            try {
                System.out.println("Starting Google authentication...");
                User user = authService.loginWithGmail();
                if (user == null) {
                    System.err.println("Gmail login failed: User is null");
                    Platform.runLater(() -> messageLabel.setText("Gmail login failed: Unable to authenticate"));
                    return;
                }

                System.out.println("Google authentication successful for user: " + user.getEmail());
                sessionManager.setLoggedInUser(user);
                String fxmlFile = user.hasRole("ROLE_ADMIN") ? "/com/example/auth/dashboard.fxml" : "/com/example/frontPages/dashboard.fxml";
                System.out.println("Attempting to load dashboard: " + fxmlFile);

                Platform.runLater(() -> {
                    try {
                        loadScene(fxmlFile);
                        System.out.println("Successfully loaded dashboard: " + fxmlFile);
                    } catch (IOException e) {
                        System.err.println("Error loading dashboard: " + e.getMessage());
                        e.printStackTrace();
                        messageLabel.setText("Error loading dashboard: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                System.err.println("Error during Gmail login: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> messageLabel.setText("Error during Gmail login: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onForgotPasswordClicked() {
        try {
            loadScene("/com/example/auth/resetPassword.fxml");
        } catch (IOException e) {
            System.err.println("Error loading reset password page: " + e.getMessage());
            messageLabel.setText("Error loading reset password page");
        }
    }

    @FXML
    private void onRegisterClicked() {
        try {
            loadScene("/com/example/auth/signup.fxml");
        } catch (IOException e) {
            System.err.println("Error loading signup page: " + e.getMessage());
            messageLabel.setText("Error loading signup page");
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

    @FXML
    private void sendChatMessage() {
        String message = chatInput.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        chatHistory.appendText("You: " + message + "\n");
        chatInput.clear();

        new Thread(() -> {
            try {
                String response = chatService.sendMessage(message);
                Platform.runLater(() -> chatHistory.appendText("AgriChat: " + response + "\n"));
            } catch (Exception e) {
                System.err.println("Error sending chat message: " + e.getMessage());
                Platform.runLater(() -> chatHistory.appendText("AgriChat: Error: " + e.getMessage() + "\n"));
            }
        }).start();
    }

    private void startFaceCapture() {
        boolean opened = false;
        for (int i = 0; i < 3; i++) {
            System.out.println("Trying camera index " + i + " with default backend");
            capture = new VideoCapture(i);
            if (capture.isOpened()) {
                opened = true;
                break;
            }
            capture.release();
        }

        if (!opened) {
            for (int i = 0; i < 3; i++) {
                System.out.println("Trying camera index " + i + " with DirectShow backend");
                capture = new VideoCapture(i, Videoio.CAP_DSHOW);
                if (capture.isOpened()) {
                    opened = true;
                    break;
                }
                capture.release();
            }
        }

        if (!opened) {
            messageLabel.setText("Cannot open webcam. Please check if it's connected and not in use.");
            System.err.println("ERROR: Failed to open webcam after trying multiple indices and backends.");
            return;
        }

        System.out.println("Webcam opened successfully.");

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

        timer = Executors.newSingleThreadScheduledExecutor();
        timer.scheduleAtFixedRate(() -> {
            Mat frame = new Mat();
            if (capture.read(frame)) {
                if (frame.empty()) {
                    System.err.println("Failed to grab frame from webcam.");
                    return;
                }
                MatOfRect faces = new MatOfRect();
                faceDetector.detectMultiScale(frame, faces);

                for (Rect rect : faces.toArray()) {
                    Imgproc.rectangle(frame, new Point(rect.x, rect.y),
                            new Point(rect.x + rect.width, rect.y + rect.height),
                            new Scalar(0, 255, 0), 3);
                }

                Image fxImage = matToImage(frame);
                Platform.runLater(() -> captureView.setImage(fxImage));
            } else {
                System.err.println("Failed to read frame from webcam.");
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
            System.out.println("Webcam released in stopFaceCapture.");
        }

        if (captureStage != null) {
            captureStage.close();
            System.out.println("Webcam window closed.");
        }
    }

    private void captureAndVerifyFace() {
        Mat frame = new Mat();
        if (capture.read(frame)) {
            if (frame.empty()) {
                Platform.runLater(() -> messageLabel.setText("Failed to capture frame"));
                return;
            }
            MatOfRect faces = new MatOfRect();
            faceDetector.detectMultiScale(frame, faces);

            if (!faces.empty()) {
                Rect face = faces.toArray()[0];
                Mat liveFace = new Mat(frame, face);

                String email = usernameField.getText().trim();
                if (email.isEmpty()) {
                    Platform.runLater(() -> messageLabel.setText("Please enter an email"));
                    return;
                }

                System.out.println("Looking for face data with email: '" + email + "'");
                File storedFaceFile = new File("faces/" + email + ".jpg");
                System.out.println("Checking file at: " + storedFaceFile.getAbsolutePath());
                if (!storedFaceFile.exists()) {
                    Platform.runLater(() -> messageLabel.setText("No face data for " + email));
                    System.out.println("Files in faces directory: " + Arrays.toString(new File("faces").list()));
                    return;
                }

                Mat storedFace = Imgcodecs.imread(storedFaceFile.getPath());
                if (storedFace.empty()) {
                    Platform.runLater(() -> messageLabel.setText("Failed to load stored face"));
                    return;
                }

                Imgproc.resize(liveFace, liveFace, new Size(100, 100));
                Imgproc.resize(storedFace, storedFace, new Size(100, 100));

                double similarity = compareHistograms(liveFace, storedFace);
                System.out.println("Similarity score: " + similarity);
                if (similarity > 0.3) {
                    User user = authService.authenticate(email, null);
                    if (user != null) {
                        sessionManager.setLoggedInUser(user);
                        String fxmlFile = user.hasRole("ROLE_ADMIN") ? "/com/example/auth/dashboard.fxml" : "/com/example/frontPages/dashboard.fxml";
                        Platform.runLater(() -> {
                            try {
                                stopFaceCapture();
                                loadScene(fxmlFile);
                            } catch (IOException e) {
                                messageLabel.setText("Error loading dashboard");
                                System.err.println("Error loading dashboard: " + e.getMessage());
                            }
                        });
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
        Mat gray1 = new Mat(), gray2 = new Mat();
        Imgproc.cvtColor(img1, gray1, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(img2, gray2, Imgproc.COLOR_BGR2GRAY);

        Imgproc.equalizeHist(gray1, gray1);
        Imgproc.equalizeHist(gray2, gray2);

        Mat hist1 = new Mat(), hist2 = new Mat();
        MatOfInt histSize = new MatOfInt(50);
        MatOfFloat ranges = new MatOfFloat(0f, 256f);
        MatOfInt channels = new MatOfInt(0);

        Imgproc.calcHist(Arrays.asList(gray1), channels, new Mat(), hist1, histSize, ranges);
        Imgproc.calcHist(Arrays.asList(gray2), channels, new Mat(), hist2, histSize, ranges);

        Core.normalize(hist1, hist1, 0, 1, Core.NORM_MINMAX, -1, new Mat());
        Core.normalize(hist2, hist2, 0, 1, Core.NORM_MINMAX, -1, new Mat());

        return Imgproc.compareHist(hist1, hist2, Imgproc.HISTCMP_CORREL);
    }

    private void loadScene(String fxmlFile) throws IOException {
        System.out.println("Loading FXML: " + fxmlFile);
        java.net.URL fxmlUrl = getClass().getResource(fxmlFile);
        if (fxmlUrl == null) {
            throw new IOException("Cannot find FXML file: " + fxmlFile);
        }

        Stage stage = (Stage) usernameField.getScene().getWindow();
        boolean isFullScreen = stage.isFullScreen();
        Parent root = FXMLLoader.load(fxmlUrl);
        Scene scene = new Scene(root, 800, 600);

        java.net.URL stylesheetUrl = getClass().getResource("/com/example/auth/styles.css");
        if (stylesheetUrl != null) {
            scene.getStylesheets().add(stylesheetUrl.toExternalForm());
        } else {
            System.out.println("Warning: styles.css not found");
        }
        stylesheetUrl = getClass().getResource("/com/example/auth/chat.css");
        if (stylesheetUrl != null) {
            scene.getStylesheets().add(stylesheetUrl.toExternalForm());
        } else {
            System.out.println("Warning: chat.css not found");
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
        if (capture != null && capture.isOpened()) {
            capture.release();
            System.out.println("Webcam released in shutdown (LoginController).");
        }
    }
}