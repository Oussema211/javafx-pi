package com.example.produit.controller;

import com.example.cart.CartManager;
import com.example.produit.model.Produit;
import com.example.produit.service.ProduitDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.List;
import java.util.Optional;

public class FastBuyController {

    private final List<Produit> allProducts;

    public FastBuyController() {
        try {
            this.allProducts = ProduitDAO.getAllProducts();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load products: " + e.getMessage());
        }
    }

    public void showFastBuyDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Fast Buy");
        dialog.getDialogPane().getStyleClass().add("fast-buy-dialog");
        dialog.getDialogPane().setPrefSize(500, 400);

        ButtonType submitButtonType = new ButtonType("Add to Cart", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/example/frontPages/pages/products.css").toExternalForm());

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setAlignment(Pos.TOP_LEFT);

        Label instructions = new Label("Enter your groceries (e.g., '3kg of tomatoes, 4kg of potatoes'):");
        instructions.getStyleClass().add("dialog-label");

        TextArea inputArea = new TextArea();
        inputArea.setPromptText("e.g., 3kg of tomatoes, 4kg of potatoes");
        inputArea.setPrefRowCount(10);
        inputArea.setPrefHeight(200);
        inputArea.getStyleClass().add("input-textarea");

        content.getChildren().addAll(instructions, inputArea);
        dialog.getDialogPane().setContent(content);

        Button submitButton = (Button) dialog.getDialogPane().lookupButton(submitButtonType);
        submitButton.setDisable(true);
        inputArea.textProperty().addListener((obs, old, newValue) ->
                submitButton.setDisable(newValue.trim().isEmpty()));

        dialog.showAndWait().ifPresent(result -> {
            if (result == submitButtonType) {
                processFastBuyInput(inputArea.getText().trim());
            }
        });
    }

    private void processFastBuyInput(String input) {
        String[] items = input.split(",\\s*");
        StringBuilder feedback = new StringBuilder();
        boolean allSuccessful = true;

        for (String item : items) {
            item = item.trim();
            if (item.isEmpty()) continue;

            try {
                String[] parts = item.split("\\s+", 2);
                if (parts.length < 2) {
                    feedback.append("Invalid format: '").append(item).append("'\n");
                    allSuccessful = false;
                    continue;
                }

                String quantityStr = parts[0].replaceAll("[^0-9.]", "");
                double quantity = Double.parseDouble(quantityStr);
                int qty = (int) Math.ceil(quantity);

                String productName = parts[1].toLowerCase();
                productName = productName.replaceAll("^(of\\s+)", "").trim();

                String finalProductName = productName;
                Optional<Produit> matchedProduct = allProducts.stream()
                        .filter(p -> p.getNom().toLowerCase().contains(finalProductName))
                        .findFirst();

                if (matchedProduct.isPresent()) {
                    Produit product = matchedProduct.get();
                    try {
                        CartManager.addProduct(product, qty);
                        feedback.append("Added ").append(qty).append("x ").append(product.getNom()).append(" to cart\n");
                    } catch (IllegalArgumentException e) {
                        feedback.append(e.getMessage()).append("\n");
                        allSuccessful = false;
                    }
                } else {
                    feedback.append("Product not found: '").append(productName).append("'\n");
                    allSuccessful = false;
                }
            } catch (NumberFormatException e) {
                feedback.append("Invalid quantity in: '").append(item).append("'\n");
                allSuccessful = false;
            } catch (Exception e) {
                feedback.append("Error processing: '").append(item).append("': ").append(e.getMessage()).append("\n");
                allSuccessful = false;
            }
        }

        showAlert(
                allSuccessful ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING,
                allSuccessful ? "Fast Buy Completed" : "Fast Buy Issues",
                feedback.toString()
        );
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStyleClass().add("dialog");
        if (type == Alert.AlertType.INFORMATION) {
            alert.getDialogPane().getStyleClass().add("alert-success");
        }
        alert.showAndWait();
    }
}