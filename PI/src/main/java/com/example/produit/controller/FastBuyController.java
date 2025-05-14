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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

        TextArea feedbackArea = new TextArea();
        feedbackArea.setEditable(false);
        feedbackArea.setPrefRowCount(5);
        feedbackArea.setPrefHeight(100);
        feedbackArea.getStyleClass().add("feedback-textarea");

        content.getChildren().addAll(instructions, inputArea, feedbackArea);
        dialog.getDialogPane().setContent(content);

        Button submitButton = (Button) dialog.getDialogPane().lookupButton(submitButtonType);
        submitButton.setDisable(true);
        inputArea.textProperty().addListener((obs, old, newValue) ->
                submitButton.setDisable(newValue.trim().isEmpty()));

        dialog.showAndWait().ifPresent(result -> {
            if (result == submitButtonType) {
                feedbackArea.clear();
                processFastBuyInput(inputArea.getText().trim(), feedbackArea);
            }
        });
    }

    private void processFastBuyInput(String input, TextArea feedbackArea) {
        String[] items = input.split(",\\s*");
        boolean allSuccessful = true;

        for (String item : items) {
            item = item.trim();
            if (item.isEmpty()) continue;

            try {
                // Parse quantity and product name
                Pattern pattern = Pattern.compile("(\\d+\\.?\\d*)\\s*(kg|units?)\\s*of\\s*(.+)");
                Matcher matcher = pattern.matcher(item.toLowerCase());
                if (!matcher.matches()) {
                    feedbackArea.appendText("Invalid format for '" + item + "'. Use 'Xkg of product' or 'X units of product'.\n");
                    allSuccessful = false;
                    continue;
                }

                double quantity = Double.parseDouble(matcher.group(1));
                String unit = matcher.group(2);
                String productName = matcher.group(3).trim();
                int qty = (unit.equals("kg") && quantity > 0) ? (int) Math.ceil(quantity) : (int) quantity;

                if (qty <= 0) {
                    feedbackArea.appendText("Invalid quantity for '" + item + "'. Must be positive.\n");
                    allSuccessful = false;
                    continue;
                }

                // Match product
                Optional<Produit> matchedProduct = allProducts.stream()
                        .filter(p -> p.getNom().toLowerCase().contains(productName))
                        .min((p1, p2) -> {
                            int dist1 = levenshteinDistance(p1.getNom().toLowerCase(), productName);
                            int dist2 = levenshteinDistance(p2.getNom().toLowerCase(), productName);
                            return Integer.compare(dist1, dist2);
                        });

                if (matchedProduct.isPresent()) {
                    Produit product = matchedProduct.get();
                    try {
                        if (product.getQuantite() < qty) {
                            feedbackArea.appendText("Insufficient stock for '" + product.getNom() + "'. Available: " + product.getQuantite() + "\n");
                            allSuccessful = false;
                        } else {
                            CartManager.addProduct(product, qty);
                            feedbackArea.appendText("Added " + qty + "x " + product.getNom() + " to cart\n");
                        }
                    } catch (IllegalArgumentException e) {
                        feedbackArea.appendText(e.getMessage() + "\n");
                        allSuccessful = false;
                    }
                } else {
                    feedbackArea.appendText("Product not found: '" + productName + "'. Did you mean one of: " +
                            suggestProducts(productName, 3) + "?\n");
                    allSuccessful = false;
                }
            } catch (NumberFormatException e) {
                feedbackArea.appendText("Invalid quantity in: '" + item + "'\n");
                allSuccessful = false;
            } catch (Exception e) {
                feedbackArea.appendText("Error processing: '" + item + "': " + e.getMessage() + "\n");
                allSuccessful = false;
            }
        }

        if (feedbackArea.getText().isEmpty()) {
            feedbackArea.appendText("No valid items processed.\n");
        }
    }

    private String suggestProducts(String target, int limit) {
        return allProducts.stream()
                .filter(p -> levenshteinDistance(p.getNom().toLowerCase(), target) < 3)
                .limit(limit)
                .map(p -> p.getNom())
                .collect(Collectors.joining(", "));
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[s1.length()][s2.length()];
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