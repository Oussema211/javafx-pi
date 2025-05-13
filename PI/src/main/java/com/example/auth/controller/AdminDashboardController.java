package com.example.auth.controller;

import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.auth.utils.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardController {
    @FXML private Label welcomeLabel;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> nomColumn;
    @FXML private TableColumn<User, String> prenomColumn;
    @FXML private TableColumn<User, String> rolesColumn;
    @FXML private TableColumn<User, Void> actionsColumn;

    private SessionManager sessionManager = SessionManager.getInstance();
    private AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        User user = sessionManager.getLoggedInUser();
        if (user == null) {
            System.err.println("No user logged in; redirecting to login");
            try {
                redirectToLogin();
            } catch (IOException e) {
                System.err.println("Error redirecting to login: " + e.getMessage());
            }
            return;
        }

        welcomeLabel.setText("Welcome, " + user.getPrenom() + " " + user.getNom() + "!");
        setupTable();
        loadUsers();
    }

    private void setupTable() {
        emailColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmail()));
        nomColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNom()));
        prenomColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPrenom()));
        rolesColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRoles().toString()));

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");

            {
                editButton.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    showEditUserForm(user);
                });
                deleteButton.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    deleteUser(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(10, editButton, deleteButton);
                    setGraphic(buttons);
                }
            }
        });

        // Add placeholder for empty table
        usersTable.setPlaceholder(new Label("No users found. Add a user or check database."));
    }

    private void loadUsers() {
        try {
            List<User> users = authService.getAllUsers();
            if (users == null || users.isEmpty()) {
                System.err.println("No users returned from getAllUsers() or list is empty/null");
                usersTable.setItems(FXCollections.observableArrayList(new ArrayList<>()));
            } else {
                System.out.println("Loaded " + users.size() + " users into table");
                usersTable.setItems(FXCollections.observableArrayList(users));
            }
        } catch (Exception e) {
            System.err.println("Error loading users: " + e.getMessage());
            e.printStackTrace();
            usersTable.setItems(FXCollections.observableArrayList(new ArrayList<>()));
        }
    }

    @FXML
    private void showAddUserForm() throws IOException {
        Stage stage = new Stage();
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/addUser.fxml"));
        stage.setTitle("Add New User");
        stage.setScene(new Scene(root));
        stage.showAndWait();
        loadUsers();
    }

    private void showEditUserForm(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auth/editUser.fxml"));
            Parent root = loader.load();
            EditUserController controller = loader.getController();
            controller.setUser(user);
            Stage stage = new Stage();
            stage.setTitle("Edit User");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadUsers();
        } catch (IOException e) {
            System.err.println("Error opening edit user form: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteUser(User user) {
        if (authService.deleteUser(user.getId())) {
            loadUsers();
        } else {
            System.err.println("Failed to delete user");
        }
    }

    @FXML
    private void handleLogout() throws IOException {
        sessionManager.clearSession();
        redirectToLogin();
    }

    private void redirectToLogin() throws IOException {
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/login.fxml"));
        stage.setScene(new Scene(root));
    }
}