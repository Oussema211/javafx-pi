package com.example.auth.controller;

import com.example.auth.service.AuthService;
import com.example.auth.service.GeminiChatService;
import com.example.auth.utils.EmailUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import javafx.scene.layout.VBox;

import javax.mail.MessagingException;
import java.io.*;
import java.util.Arrays;

public class SignupController {
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField travailField;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField numTelField;
    @FXML private Label messageLabel;
    @FXML private ImageView profilePhotoPreview;
    @FXML private Button chooseProfilePhotoButton;
    @FXML private ImageView facePhotoPreview;
    @FXML private Button chooseFacePhotoButton;
    @FXML private Button captureFaceButton;
    @FXML private Label photoErrorLabel;
    @FXML private Button signupButton;
    @FXML private Hyperlink loginLink;
    @FXML private VBox chatCard;
    @FXML private TextArea chatHistory;
    @FXML private TextField chatInput;
    @FXML private Button sendChatButton;

    private AuthService authService = new AuthService();
    private GeminiChatService chatService;
    private VideoCapture capture;
    private CascadeClassifier faceDetector;
    private boolean isCapturing = false;
    private File selectedProfilePhoto;
    private File selectedFacePhoto;
    private Mat capturedFace;

    @FXML
    public void initialize() {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            InputStream xmlStream = getClass().getResourceAsStream("/haarcascade_frontalface_default.xml");
            if (xmlStream == null) {
                System.err.println("ERROR: haarcascade_frontalface_default.xml not found");
                messageLabel.setText("Face detection file not found");
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
                messageLabel.setText("Face detection unavailable");
            }
            File facesDir = new File("faces");
            System.out.println("Creating faces directory at: " + facesDir.getAbsolutePath());
            if (facesDir.mkdirs()) {
                System.out.println("Faces directory created successfully");
            } else {
                System.out.println("Faces directory already exists or failed to create");
            }
            File profileDir = new File("profile_photos");
            System.out.println("Creating profile_photos directory at: " + profileDir.getAbsolutePath());
            if (profileDir.mkdirs()) {
                System.out.println("Profile_photos directory created successfully");
            } else {
                System.out.println("Profile_photos directory already exists or failed to create");
            }

            chatService = new GeminiChatService();
            System.out.println("chatCard: " + chatCard);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            messageLabel.setText("Cannot initialize face detection");
        }
    }

    @FXML
    private void onSignupClicked() {
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();
        String travail = travailField.getText().trim();
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String numTel = numTelField.getText().trim();

        if (email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ||
            nom.isEmpty() || prenom.isEmpty()) {
            messageLabel.setText("Please fill in all required fields");
            return;
        }

        if (!password.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match");
            return;
        }

        if (selectedProfilePhoto == null) {
            messageLabel.setText("Please select a profile photo");
            return;
        }

        if (selectedFacePhoto == null && capturedFace == null) {
            messageLabel.setText("Please select or capture a face photo");
            return;
        }

        String profilePhotoPath = "profile_photos/" + email + "_profile.jpg";
        try {
            Mat profileImage = Imgcodecs.imread(selectedProfilePhoto.getAbsolutePath());
            if (!profileImage.empty()) {
                Imgcodecs.imwrite(profilePhotoPath, profileImage);
                System.out.println("Profile photo saved at: " + new File(profilePhotoPath).getAbsolutePath());
            } else {
                messageLabel.setText("Failed to load profile photo");
                return;
            }
        } catch (Exception e) {
            messageLabel.setText("Error saving profile photo");
            System.out.println("Error saving profile photo: " + e.getMessage());
            return;
        }

        String facePhotoPath = "faces/" + email + ".jpg";
        try {
            Mat faceImage = null;
            if (selectedFacePhoto != null) {
                faceImage = Imgcodecs.imread(selectedFacePhoto.getAbsolutePath());
                if (!faceImage.empty()) {
                    MatOfRect faces = new MatOfRect();
                    faceDetector.detectMultiScale(faceImage, faces);
                    if (!faces.empty()) {
                        Rect face = faces.toArray()[0];
                        faceImage = new Mat(faceImage, face);
                    } else {
                        messageLabel.setText("No face detected in chosen photo");
                        return;
                    }
                } else {
                    messageLabel.setText("Failed to load face photo");
                    return;
                }
            } else if (capturedFace != null) {
                faceImage = capturedFace;
            }

            if (faceImage != null) {
                Imgproc.resize(faceImage, faceImage, new Size(100, 100));
                if (faceImage.channels() > 1) {
                    Imgproc.cvtColor(faceImage, faceImage, Imgproc.COLOR_BGR2GRAY);
                }
                Imgproc.equalizeHist(faceImage, faceImage);
                Imgcodecs.imwrite(facePhotoPath, faceImage);
                System.out.println("Face image saved at: " + new File(facePhotoPath).getAbsolutePath());
                if (!new File(facePhotoPath).exists()) {
                    messageLabel.setText("Failed to save face photo");
                    return;
                }
            }
        } catch (Exception e) {
            messageLabel.setText("Error saving face photo");
            System.out.println("Error saving face photo: " + e.getMessage());
            return;
        }

        String verificationCode = authService.generateVerificationCode();
        boolean success = authService.signup(email, password, travail, profilePhotoPath, nom, prenom, numTel, Arrays.asList("ROLE_USER"), verificationCode);
        if (success) {
            try {
                String emailBody = "<h2>Email Verification</h2>" +
                        "<p>Your verification code is: <b>" + verificationCode + "</b></p>" +
                        "<p>Enter this code in the application to verify your account.</p>" +
                        "<p>This code will expire in 24 hours.</p>";
                EmailUtil.sendEmail(email, "Verify Your Email", emailBody);
                messageLabel.setText("Signup successful! Please enter the verification code sent to your email.");

                Stage stage = (Stage) signupButton.getScene().getWindow();
                boolean isFullScreen = stage.isFullScreen();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auth/code-verification.fxml"));
                Parent root = loader.load();
                CodeVerificationController controller = loader.getController();
                controller.setEmail(email);
                Scene scene = new Scene(root, 400, 300);
                java.net.URL cssResource = getClass().getResource("/com/example/auth/modern-theme.css");
                if (cssResource != null) {
                    scene.getStylesheets().add(cssResource.toExternalForm());
                } else {
                    System.err.println("Warning: modern-theme.css not found");
                }
                cssResource = getClass().getResource("/com/example/auth/chat.css");
                if (cssResource != null) {
                    scene.getStylesheets().add(cssResource.toExternalForm());
                } else {
                    System.err.println("Warning: chat.css not found");
                }
                stage.setScene(scene);
                stage.setFullScreen(isFullScreen);
                stage.show();
            } catch (MessagingException e) {
                messageLabel.setText("Signup successful, but failed to send verification email.");
                System.err.println("Error sending verification email: " + e.getMessage());
            } catch (IOException e) {
                messageLabel.setText("Error loading verification page");
                System.err.println("Error loading verification page: " + e.getMessage());
            }
        } else {
            messageLabel.setText("Signup failed. Email may already exist.");
        }
    }

    @FXML
    private void onChooseProfilePhotoClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        selectedProfilePhoto = fileChooser.showOpenDialog(signupButton.getScene().getWindow());
        if (selectedProfilePhoto != null) {
            try {
                Image image = new Image(selectedProfilePhoto.toURI().toString());
                profilePhotoPreview.setImage(image);
            } catch (Exception e) {
                messageLabel.setText("Error loading profile photo preview");
            }
        }
    }

    @FXML
    private void onChooseFacePhotoClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Face Photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        selectedFacePhoto = fileChooser.showOpenDialog(signupButton.getScene().getWindow());
        if (selectedFacePhoto != null) {
            try {
                Image image = new Image(selectedFacePhoto.toURI().toString());
                facePhotoPreview.setImage(image);
            } catch (Exception e) {
                messageLabel.setText("Error loading face photo preview");
            }
        }
    }

    @FXML
    private void onLoginClicked() {
        try {
            Stage stage = (Stage) loginLink.getScene().getWindow();
            boolean isFullScreen = stage.isFullScreen();
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/login.fxml"));
            Scene scene = new Scene(root, 400, 500);
            java.net.URL cssResource = getClass().getResource("/com/example/auth/styles.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            } else {
                System.err.println("Warning: styles.css not found");
            }
            cssResource = getClass().getResource("/com/example/auth/chat.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            } else {
                System.err.println("Warning: chat.css not found");
            }
            stage.setScene(scene);
            stage.setFullScreen(isFullScreen);
            stage.show();
        } catch (IOException e) {
            messageLabel.setText("Error loading login page");
            System.err.println("Error loading login page: " + e.getMessage());
        }
    }

    @FXML
    private void captureFace() {
        if (faceDetector == null || faceDetector.empty()) {
            messageLabel.setText("Face detection not initialized");
            return;
        }

        if (isCapturing) {
            isCapturing = false;
            captureFaceButton.setText("Capture Face");
            if (capture != null && capture.isOpened()) {
                capture.release();
            }
            return;
        }

        isCapturing = true;
        captureFaceButton.setText("Stop Capture");
        capture = new VideoCapture(0);
        if (!capture.isOpened()) {
            messageLabel.setText("Cannot open camera");
            isCapturing = false;
            captureFaceButton.setText("Capture Face");
            return;
        }

        Mat frame = new Mat();
        new Thread(() -> {
            while (isCapturing && capture.isOpened()) {
                if (capture.read(frame) && !frame.empty()) {
                    MatOfRect faces = new MatOfRect();
                    faceDetector.detectMultiScale(frame, faces);
                    if (!faces.empty()) {
                        Rect face = faces.toArray()[0];
                        capturedFace = new Mat(frame, face);
                        Platform.runLater(() -> {
                            try {
                                Image image = matToImage(capturedFace);
                                facePhotoPreview.setImage(image);
                            } catch (Exception e) {
                                messageLabel.setText("Error displaying captured face");
                            }
                        });
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
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
                Platform.runLater(() -> {
                    chatHistory.appendText("AgriChat: " + response + "\n");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    chatHistory.appendText("AgriChat: Error: " + e.getMessage() + "\n");
                });
                System.err.println("DEBUG: Error sending chat message: " + e.getMessage());
            }
        }).start();
    }

    private Image matToImage(Mat mat) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", mat, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }
}
