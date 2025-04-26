package com.example.cart.view;

import com.example.cart.model.OrderSummary;
import com.example.cart.model.ProduitCommande;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class OrderDetailsDialog extends Dialog<Void> {

    public OrderDetailsDialog(OrderSummary order) {
        setTitle("🧾 Détails de la Commande");

        DialogPane pane = getDialogPane();
        pane.getStyleClass().add("order-dialog");
        pane.setPrefWidth(520);
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.getStylesheets().add(getClass().getResource("/com/example/css/dialog.css").toExternalForm());

        VBox container = new VBox(15);
        container.setPadding(new Insets(25));
        container.getStyleClass().add("dialog-container");

        // Title
        Text title = new Text("🧾 Commande n°" + order.getId());
        title.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        title.getStyleClass().add("dialog-title");
        container.getChildren().add(title);

        // Infos générales
        GridPane generalInfo = new GridPane();
        generalInfo.setHgap(10);
        generalInfo.setVgap(10);

        generalInfo.add(labelBold("👤 Utilisateur : "), 0, 0);
        generalInfo.add(new Label(order.getUserId()), 1, 0);

        generalInfo.add(labelBold("📅 Date d'achat : "), 0, 1);
        generalInfo.add(new Label(order.getDateAchat()), 1, 1);

        generalInfo.add(labelBold("💰 Prix Total : "), 0, 2);
        generalInfo.add(new Label(String.format("%.2f DT", order.getPrixTotal())), 1, 2);

        container.getChildren().add(generalInfo);

        Separator separator = new Separator();
        container.getChildren().add(separator);

        // Liste des produits
        VBox productList = new VBox(10);
        productList.getStyleClass().add("product-list");

        for (ProduitCommande pc : order.getProduitsCommandes()) {
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("product-row");

            Label productName = new Label("🍅 " + pc.getNomProduit());
            productName.getStyleClass().add("product-name");

            Label details = new Label("📦 Quantité : " + pc.getQuantite() + " | 💵 PU : " + pc.getPrixUnitaire() + " DT");
            details.getStyleClass().add("product-details");

            row.getChildren().addAll(productName, details);
            productList.getChildren().add(row);
        }

        container.getChildren().add(productList);
        pane.setContent(container);
    }

    private Label labelBold(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        return label;
    }
}
