package com.example.purchasingbackend;

import com.example.purchasingbackend.dao.CommandeFinaliseeDAO;
import com.example.purchasingbackend.model.CommandeFinalisee;
import com.example.purchasingbackend.model.ProduitCommandeTemp;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.produit.model.Produit;
import com.example.produit.service.ProduitDAO;

import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PurchasingController {

    @FXML
    private TableView<CommandeFinalisee> commandeTable;

    @FXML
    private TableColumn<CommandeFinalisee, String> colUser;

    @FXML
    private TableColumn<CommandeFinalisee, String> colDate;

    @FXML
    private TableColumn<CommandeFinalisee, Double> colPrix;

    @FXML
    private TableColumn<CommandeFinalisee, String> colProduit;

    private ObservableList<CommandeFinalisee> commandeList;
    @FXML
    private TableColumn<CommandeFinalisee, Void> colActions;

    @FXML
    public void initialize() {
        colUser.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getUtilisateur().getUsername()
        ));

        colDate.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDate().toString()
        ));

        colProduit.setCellValueFactory(data -> {
            List<ProduitCommandeTemp> produits = data.getValue().getProduitsAvecQuantites();
            String noms = produits.stream()
                    .map(p -> p.getProduit().getNom() + " (x" + p.getQuantite() + ")")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("Aucun");
            return new SimpleStringProperty(noms);
        });

        colPrix.setCellValueFactory(new PropertyValueFactory<>("prixTotal"));

        // 👇 Ajoute les boutons "Modifier" et "Supprimer"
        addActionButtonsToTable();

        // 👇 Charge les commandes dans la table
        loadCommandes();
    }
    private void addActionButtonsToTable() {
        TableColumn<CommandeFinalisee, Void> colActions = new TableColumn<>("Actions");

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnModifier = new Button(" Modifier");
            private final Button btnSupprimer = new Button(" Supprimer");
            private final HBox hbox = new HBox(10, btnModifier, btnSupprimer);

            {
                btnModifier.getStyleClass().add("button-modifier");
                btnSupprimer.getStyleClass().add("button-supprimer");

                btnModifier.setOnAction(event -> {
                    CommandeFinalisee selected = getTableView().getItems().get(getIndex());
                    commandeTable.getSelectionModel().select(selected);
                    handleModifierCommande();
                });

                btnSupprimer.setOnAction(event -> {
                    CommandeFinalisee selected = getTableView().getItems().get(getIndex());
                    commandeTable.getSelectionModel().select(selected);
                    handleSupprimerCommande();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });

        if (!commandeTable.getColumns().contains(colActions)) {
            commandeTable.getColumns().add(colActions);
        }
    }




    private void loadCommandes() {
        commandeList = FXCollections.observableArrayList(CommandeFinaliseeDAO.getAllCommandes());
        commandeTable.setItems(commandeList);
    }

    @FXML
    private void handleAddCommande() {
        Dialog<CommandeFinalisee> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une commande");

        ComboBox<User> userComboBox = new ComboBox<>();
        DatePicker datePicker = new DatePicker(LocalDate.now());
        TextField prixField = new TextField();
        prixField.setEditable(false);

        List<User> users = new AuthService().getAllUsers();
        List<Produit> produits = ProduitDAO.getAllProducts();
        userComboBox.getItems().addAll(users);

        ObservableList<ProduitCommandeTemp> selectedProduits = FXCollections.observableArrayList();
        TableView<ProduitCommandeTemp> produitTableView = new TableView<>(selectedProduits);
        produitTableView.setPrefHeight(150);

        Label summaryLabel = new Label("📝 Aucune commande sélectionnée...");
        summaryLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #555;");
        final Runnable[] updateSummary = new Runnable[1];

        TableColumn<ProduitCommandeTemp, String> nomCol = new TableColumn<>("Produit");
        nomCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduit().getNom()));
        nomCol.setPrefWidth(200);

        TableColumn<ProduitCommandeTemp, Number> quantiteCol = new TableColumn<>("Quantité");
        quantiteCol.setCellValueFactory(data -> data.getValue().quantiteProperty());
        quantiteCol.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.NumberStringConverter()));
        quantiteCol.setOnEditCommit(event -> {
            event.getRowValue().setQuantite(event.getNewValue().intValue());
            updateSummary[0].run();
        });
        quantiteCol.setPrefWidth(100);

        produitTableView.getColumns().addAll(nomCol, quantiteCol);
        produitTableView.setEditable(true);

        ComboBox<Produit> produitComboBox = new ComboBox<>();
        produitComboBox.getItems().addAll(produits);
        Button ajouterProduitBtn = new Button("➕ Ajouter");

        ajouterProduitBtn.setOnAction(e -> {
            Produit selected = produitComboBox.getValue();
            if (selected != null) {
                Optional<ProduitCommandeTemp> existing = selectedProduits.stream()
                        .filter(p -> p.getProduit().getId().equals(selected.getId()))
                        .findFirst();

                if (existing.isPresent()) {
                    existing.get().setQuantite(existing.get().getQuantite() + 1);
                    produitTableView.refresh(); // 👈 très important pour que la table se mette à jour visuellement
                } else {
                    selectedProduits.add(new ProduitCommandeTemp(selected, 1));
                }

                produitComboBox.getSelectionModel().clearSelection();
                updateSummary[0].run();
            }
        });


        updateSummary[0] = () -> {
            String user = userComboBox.getValue() != null ? userComboBox.getValue().getUsername() : "Utilisateur ?";
            String produitsText = selectedProduits.isEmpty()
                    ? "Aucun produit"
                    : selectedProduits.stream()
                    .map(ProduitCommandeTemp::toString)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            double total = selectedProduits.stream()
                    .mapToDouble(p -> p.getProduit().getPrixUnitaire() * p.getQuantite()).sum();
            prixField.setText(String.valueOf(total));
            summaryLabel.setText("🧾 Commande pour " + user + " | Produits : " + produitsText + " | Total : " + total + " DT");
        };

        userComboBox.setOnAction(e -> updateSummary[0].run());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("👤 Utilisateur:"), 0, 0);
        grid.add(userComboBox, 1, 0);
        grid.add(new Label("🛍 Produit:"), 0, 1);
        grid.add(produitComboBox, 1, 1);
        grid.add(ajouterProduitBtn, 2, 1);
        grid.add(produitTableView, 1, 2, 2, 1);
        grid.add(new Label("📅 Date:"), 0, 3);
        grid.add(datePicker, 1, 3);
        grid.add(new Label("💰 Prix Total:"), 0, 4);
        grid.add(prixField, 1, 4);
        grid.add(summaryLabel, 0, 5, 3, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(600);
        dialog.getDialogPane().setPrefHeight(500);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                User user = userComboBox.getValue();
                LocalDate date = datePicker.getValue();
                double prix = Double.parseDouble(prixField.getText());

                if (user != null && !selectedProduits.isEmpty() && date != null) {
                    return new CommandeFinalisee(UUID.randomUUID(), user, new ArrayList<>(selectedProduits), date.atStartOfDay(), prix);
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(cmd -> {
            CommandeFinaliseeDAO.saveCommande(cmd);
            commandeList.add(cmd);
        });
    }
    @FXML
    private void handleModifierCommande() {
        CommandeFinalisee selected = commandeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Aucune commande sélectionnée", "Veuillez sélectionner une commande à modifier.");
            return;
        }

        // 👉 On réutilise quasiment le même code que handleAddCommande, mais avec des valeurs pré-remplies
        Dialog<CommandeFinalisee> dialog = new Dialog<>();
        dialog.setTitle("Modifier la commande");

        ComboBox<User> userComboBox = new ComboBox<>();
        DatePicker datePicker = new DatePicker(selected.getDate().toLocalDate());
        TextField prixField = new TextField();
        prixField.setEditable(false);

        List<User> users = new AuthService().getAllUsers();
        List<Produit> produits = ProduitDAO.getAllProducts();
        userComboBox.getItems().addAll(users);
        userComboBox.setValue(selected.getUtilisateur());

        ObservableList<ProduitCommandeTemp> selectedProduits = FXCollections.observableArrayList(selected.getProduitsAvecQuantites());
        TableView<ProduitCommandeTemp> produitTableView = new TableView<>(selectedProduits);
        produitTableView.setPrefHeight(150);

        Label summaryLabel = new Label();
        summaryLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #555;");
        final Runnable[] updateSummary = new Runnable[1];

        TableColumn<ProduitCommandeTemp, String> nomCol = new TableColumn<>("Produit");
        nomCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduit().getNom()));
        nomCol.setPrefWidth(200);

        TableColumn<ProduitCommandeTemp, Number> quantiteCol = new TableColumn<>("Quantité");
        quantiteCol.setCellValueFactory(data -> data.getValue().quantiteProperty());
        quantiteCol.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.NumberStringConverter()));
        quantiteCol.setOnEditCommit(event -> {
            event.getRowValue().setQuantite(event.getNewValue().intValue());
            updateSummary[0].run();
        });
        quantiteCol.setPrefWidth(100);

        produitTableView.getColumns().addAll(nomCol, quantiteCol);
        produitTableView.setEditable(true);

        ComboBox<Produit> produitComboBox = new ComboBox<>();
        produitComboBox.getItems().addAll(produits);
        Button ajouterProduitBtn = new Button("➕ Ajouter");

        ajouterProduitBtn.setOnAction(e -> {
            Produit selectedP = produitComboBox.getValue();
            if (selectedP != null) {
                Optional<ProduitCommandeTemp> existing = selectedProduits.stream()
                        .filter(p -> p.getProduit().getId().equals(selectedP.getId()))
                        .findFirst();

                if (existing.isPresent()) {
                    existing.get().setQuantite(existing.get().getQuantite() + 1);
                    produitTableView.refresh(); // important pour voir le changement à l’écran
                } else {
                    selectedProduits.add(new ProduitCommandeTemp(selectedP, 1));
                }

                produitComboBox.getSelectionModel().clearSelection();
                updateSummary[0].run();
            }
        });


        updateSummary[0] = () -> {
            String user = userComboBox.getValue() != null ? userComboBox.getValue().getUsername() : "Utilisateur ?";
            String produitsText = selectedProduits.isEmpty()
                    ? "Aucun produit"
                    : selectedProduits.stream().map(ProduitCommandeTemp::toString).reduce((a, b) -> a + ", " + b).orElse("");
            double total = selectedProduits.stream().mapToDouble(p -> p.getProduit().getPrixUnitaire() * p.getQuantite()).sum();
            prixField.setText(String.valueOf(total));
            summaryLabel.setText("🧾 Commande pour " + user + " | Produits : " + produitsText + " | Total : " + total + " DT");
        };
        // 👇 AJOUTE CETTE LIGNE JUSTE ICI :
        updateSummary[0].run();

        userComboBox.setOnAction(e -> updateSummary[0].run());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("👤 Utilisateur:"), 0, 0);
        grid.add(userComboBox, 1, 0);
        grid.add(new Label("🛍 Produit:"), 0, 1);
        grid.add(produitComboBox, 1, 1);
        grid.add(ajouterProduitBtn, 2, 1);
        grid.add(produitTableView, 1, 2, 2, 1);
        grid.add(new Label("📅 Date:"), 0, 3);
        grid.add(datePicker, 1, 3);
        grid.add(new Label("💰 Prix Total:"), 0, 4);
        grid.add(prixField, 1, 4);
        grid.add(summaryLabel, 0, 5, 3, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(600);
        dialog.getDialogPane().setPrefHeight(500);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                User user = userComboBox.getValue();
                LocalDate date = datePicker.getValue();
                double prix = Double.parseDouble(prixField.getText());

                if (user != null && !selectedProduits.isEmpty() && date != null) {
                    return new CommandeFinalisee(selected.getId(), user, new ArrayList<>(selectedProduits), date.atStartOfDay(), prix);
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(cmd -> {
            CommandeFinaliseeDAO.updateCommande(cmd);  // 🔧 tu dois avoir cette méthode
            int index = commandeList.indexOf(selected);
            commandeList.set(index, cmd);
        });
    }
    @FXML
    private void handleSupprimerCommande() {
        CommandeFinalisee selected = commandeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Aucune commande sélectionnée", "Veuillez sélectionner une commande à supprimer.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer la commande ?");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette commande ?");

        alert.showAndWait().ifPresent(button -> {
            if (button == ButtonType.OK) {
                CommandeFinaliseeDAO.deleteCommande(selected.getId()); // 🔧 tu dois avoir cette méthode aussi
                commandeList.remove(selected);
            }
        });
    }
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


}
