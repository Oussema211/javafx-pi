package com.example.auth.controller;

import com.example.auth.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

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

    private AuthService authService = new AuthService();
    private VideoCapture capture;
    private CascadeClassifier faceDetector;
    private boolean isCapturing = false;
    private File selectedProfilePhoto;
    private File selectedFacePhoto;
    private Mat capturedFace; // To store the captured face image

    @FXML
    public void initialize() {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            InputStream xmlStream = getClass().getResourceAsStream("/haarcascade_frontalface_default.xml");
            if (xmlStream == null) {
                System.err.println("ERROR: haarcascade_frontalface_default.xml not found in resources");
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
            } else {
                messageLabel.setText("Face detection loaded");
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

        } catch (UnsatisfiedLinkError e) {
            System.err.println("ERROR: Failed to load OpenCV: " + e.getMessage());
            messageLabel.setText("Cannot load face detection");
        } catch (IOException e) {
            System.err.println("ERROR: Error reading face detection file");
            messageLabel.setText("Cannot read face detection file");
        }
    }

    @FXML
    private void onChooseProfilePhotoClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        Stage stage = (Stage) chooseProfilePhotoButton.getScene().getWindow();
        selectedProfilePhoto = fileChooser.showOpenDialog(stage);
        if (selectedProfilePhoto != null) {
            Image image = new Image(selectedProfilePhoto.toURI().toString());
            profilePhotoPreview.setImage(image);
            photoErrorLabel.setVisible(false);
            messageLabel.setText("Profile photo selected");
        } else {
            photoErrorLabel.setText("No profile photo selected");
            photoErrorLabel.setVisible(true);
        }
    }

    @FXML
    private void onChooseFacePhotoClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        Stage stage = (Stage) chooseFacePhotoButton.getScene().getWindow();
        selectedFacePhoto = fileChooser.showOpenDialog(stage);
        if (selectedFacePhoto != null) {
            Image image = new Image(selectedFacePhoto.toURI().toString());
            facePhotoPreview.setImage(image);
            photoErrorLabel.setVisible(false);
            messageLabel.setText("Face photo selected");
        } else {
            photoErrorLabel.setText("No face photo selected");
            photoErrorLabel.setVisible(true);
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
                System.out.println("Failed to load profile image: " + selectedProfilePhoto.getAbsolutePath());
                messageLabel.setText("Failed to load profile photo");
                return;
            }
        } catch (Exception e) {
            messageLabel.setText("Error saving profile photo");
            System.out.println("Error saving profile photo: " + e.getMessage());
            return;
        }

        // Save the face image
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
                Imgproc.cvtColor(faceImage, faceImage, Imgproc.COLOR_BGR2GRAY);
                Imgproc.equalizeHist(faceImage, faceImage);
                Imgcodecs.imwrite(facePhotoPath, faceImage);
                System.out.println("Face image saved at: " + new File(facePhotoPath).getAbsolutePath());
                if (!new File(facePhotoPath).exists()) {
                    System.out.println("ERROR: Face image file does not exist after saving!");
                    messageLabel.setText("Failed to save face photo");
                    return;
                }
            }
        } catch (Exception e) {
            messageLabel.setText("Error saving face photo");
            System.out.println("Error saving face photo: " + e.getMessage());
            return;
        }

        boolean success = authService.signup(email, password, travail, profilePhotoPath, nom, prenom, numTel, Arrays.asList("ROLE_USER"));
        if (success) {
            messageLabel.setText("Signup successful! Please login.");
        } else {
            messageLabel.setText("Signup failed. Email may already exist.");
        }
    }

    @FXML
    private void onLoginClicked() {
        try {
            Stage stage = (Stage) loginLink.getScene().getWindow();
            boolean isFullScreen = stage.isFullScreen();
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/login.fxml"));
            Scene scene = new Scene(root, 400, 500);
            scene.getStylesheets().add(getClass().getClassLoader().getResource("com/example/auth/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setFullScreen(isFullScreen);
            stage.show();
        } catch (IOException e) {
            messageLabel.setText("Error loading login page");
            e.printStackTrace();
        }
    }

    @FXML
    private void captureFace() {
        if (!isCapturing) {
            capture = new VideoCapture(0);
            if (!capture.isOpened()) {
                messageLabel.setText("Cannot open webcam");
                return;
            }
            isCapturing = true;
            new Thread(() -> {
                Mat frame = new Mat();
                while (isCapturing && capture.read(frame)) {
                    MatOfRect faces = new MatOfRect();
                    faceDetector.detectMultiScale(frame, faces);
                    if (!faces.empty()) {
                        Rect face = faces.toArray()[0];
                        Mat faceROI = new Mat(frame, face);
                        Imgproc.cvtColor(faceROI, faceROI, Imgproc.COLOR_BGR2RGB);
                        Imgproc.resize(faceROI, faceROI, new Size(100, 100));
                        Image fxImage = matToImage(faceROI);
                        facePhotoPreview.setImage(fxImage);
                    }
                }
            }).start();
        } else {
            isCapturing = false;
            Mat frame = new Mat();
            if (capture.read(frame)) {
                MatOfRect faces = new MatOfRect();
                faceDetector.detectMultiScale(frame, faces);
                if (!faces.empty()) {
                    Rect face = faces.toArray()[0];
                    capturedFace = new Mat(frame, face);
                    Imgproc.resize(capturedFace, capturedFace, new Size(100, 100));
                    Imgproc.cvtColor(capturedFace, capturedFace, Imgproc.COLOR_BGR2GRAY);
                    Imgproc.equalizeHist(capturedFace, capturedFace);
                    messageLabel.setText("Face captured successfully");
                    photoErrorLabel.setVisible(false);
                    Image fxImage = matToImage(capturedFace);
                    facePhotoPreview.setImage(fxImage);
                } else {
                    messageLabel.setText("No face detected");
                    photoErrorLabel.setVisible(true);
                }
            } else {
                messageLabel.setText("Failed to capture face");
                photoErrorLabel.setVisible(true);
            }
            capture.release();
        }
    }

    private Image matToImage(Mat mat) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", mat, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }
}