package com.example.cart.view;

import java.awt.Desktop;
import com.example.cart.model.OrderSummary;
import com.example.cart.model.ProduitCommande;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;

/** Dialogue et impression facture « light » – design modernisé */
public class OrderDetailsDialog extends Dialog<Void> {

    public OrderDetailsDialog(OrderSummary order) {
        setTitle("Détails commande");

        DialogPane pane = getDialogPane();
        pane.setPrefWidth(520);
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.getStylesheets().add(getClass()
                .getResource("/com/example/css/dialog.css").toExternalForm());

        VBox root = new VBox(18);
        root.setPadding(new Insets(25));
        // Style commun
        String buttonStyle = """
    -fx-background-color: linear-gradient(to right, #43cea2, #185a9d);
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-font-size: 14px;
    -fx-background-radius: 10;
    -fx-cursor: hand;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 2, 2);
""";

// 📄 Bouton Export CSV
        Button csvBtn = new Button("📄 Exporter en CSV");
        csvBtn.setPrefWidth(220);
        csvBtn.setPrefHeight(40);
        csvBtn.setStyle(buttonStyle);
        csvBtn.setOnMousePressed(e ->
                csvBtn.setStyle(buttonStyle + "-fx-translate-y: 2; -fx-opacity: 0.85;")
        );
        csvBtn.setOnMouseReleased(e ->
                csvBtn.setStyle(buttonStyle)
        );
        csvBtn.setOnAction(e -> exporterCSV(order));
        root.getChildren().add(csvBtn);

// 🖨️ Bouton Imprimer Facture
        Button printBtn = new Button("🖨️ Imprimer la facture");
        printBtn.setPrefWidth(220);
        printBtn.setPrefHeight(40);
        printBtn.setStyle(buttonStyle);
        printBtn.setOnMousePressed(e ->
                printBtn.setStyle(buttonStyle + "-fx-translate-y: 2; -fx-opacity: 0.85;")
        );
        printBtn.setOnMouseReleased(e ->
                printBtn.setStyle(buttonStyle)
        );
        printBtn.setOnAction(e -> imprimerFacture(order));
        root.getChildren().add(printBtn);


        /* titre */
        Text t = new Text("Commande n° " + order.getId());
        t.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        root.getChildren().add(t);

        /* infos */
        GridPane g = new GridPane();
        g.setVgap(6); g.setHgap(10);
        g.add(labelBold("Utilisateur :"), 0, 0);
        g.add(new Label(order.getUserId()), 1, 0);
        g.add(labelBold("Date :"), 0, 1);
        g.add(new Label(order.getDateAchat()), 1, 1);
        g.add(labelBold("Total :"), 0, 2);
        g.add(new Label(String.format("%.2f DT", order.getPrixTotal())), 1, 2);
        root.getChildren().add(g);

        pane.setContent(root);
    }

    /** Impression PDF épuré & modernisé */
    private void imprimerFacture(OrderSummary o) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream c = new PDPageContentStream(doc, page)) {

                /* bandeau vert en haut */
                c.setNonStrokingColor(0x4C, 0xAF, 0x50);
                c.addRect(0, 760, page.getMediaBox().getWidth(), 60);
                c.fill();

                /* logo */
                try {
                    PDImageXObject logo = PDImageXObject.createFromFile(
                            "src/main/resources/com/example/frontPages/icons/logo.png", doc);
                    c.drawImage(logo, 40, 770, 50, 50);
                } catch (IOException ex) { /* logo manquant ? on ignore */ }

                /* nom société blanc */
                c.setNonStrokingColor(1f);
                c.beginText();
                c.setFont(PDType1Font.HELVETICA_BOLD, 18);
                c.newLineAtOffset(110, 780);
                c.showText("AGRIPLANNER");
                c.endText();

                /* coordonnés société sous le bandeau */
                c.setNonStrokingColor(0f);
                write(c, 420, 740, PDType1Font.HELVETICA_BOLD, 12,
                        "Agriplanner");
                write(c, 420, 725, PDType1Font.HELVETICA, 10,
                        "Rue des Oliviers, Ariana 2080");
                write(c, 420, 713, PDType1Font.HELVETICA, 10, "Tunisie");
                write(c, 420, 701, PDType1Font.HELVETICA, 10,
                        "Tél : +216 20 345 678");
                write(c, 420, 689, PDType1Font.HELVETICA, 10,
                        "contact@agriplanner.tn");

                /* titre facture centré */
                write(c, 250, 680, PDType1Font.HELVETICA_BOLD, 22, "FACTURE");

                /* infos commande */
                int y = 630;
                write(c, 60, y,   PDType1Font.HELVETICA, 12,
                        "N° Commande : " + o.getId());
                write(c, 60, y-18, PDType1Font.HELVETICA, 12,
                        "Date : " + o.getDateAchat());
                write(c, 60, y-36, PDType1Font.HELVETICA, 12,
                        "Utilisateur : " + o.getUserId());

                /* ligne de séparation */
                c.moveTo(50, 570); c.lineTo(545, 570); c.setLineWidth(0.7f); c.stroke();

                /* encadré total */
                c.setNonStrokingColor(0xF2,0xF2,0xF2);
                c.addRect(50, 520, 495, 35); c.fill();
                c.setNonStrokingColor(0x33,0x33,0x33);
                c.addRect(50, 520, 495, 35); c.stroke();

                write(c, 60, 533,
                        PDType1Font.HELVETICA_BOLD, 14, "TOTAL À PAYER :");
                c.setNonStrokingColor(0x4C,0xAF,0x50);
                write(c, 380, 533,
                        PDType1Font.HELVETICA_BOLD, 16,
                        String.format("%.2f DT", o.getPrixTotal()));
                c.setNonStrokingColor(0x33,0x33,0x33);

                /* footer */
                c.moveTo(50, 100); c.lineTo(545, 100); c.stroke();
                write(c, 200, 85, PDType1Font.HELVETICA, 9,
                        "Merci pour votre confiance – Agriplanner © 2025");
            }

            File pdf = new File("facture_" + o.getId() + ".pdf");
            doc.save(pdf);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(pdf);
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    /* utilitaire écriture texte */
    private void write(PDPageContentStream c, float x, float y,
                       PDType1Font font, int size, String txt) throws IOException {
        c.beginText(); c.setFont(font, size);
        c.newLineAtOffset(x, y); c.showText(txt); c.endText();
    }

    private Label labelBold(String txt) {
        Label l = new Label(txt);
        l.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        return l;
    }
    public void exporterCSV(OrderSummary order) {
        StringBuilder csv = new StringBuilder();
        csv.append("Produit,Quantité,PU (DT),Total (DT)\n");

        for (ProduitCommande pc : order.getProduitsCommandes()) {
            double total = pc.getPrixUnitaire() * pc.getQuantite();
            csv.append(pc.getNomProduit()).append(",")
                    .append(pc.getQuantite()).append(",")
                    .append(pc.getPrixUnitaire()).append(",")
                    .append(String.format("%.2f", total)).append("\n");
        }

        csv.append("\nTOTAL À PAYER:,").append(String.format("%.2f DT", order.getPrixTotal()));

        try {
            File file = new File("commande_" + order.getId() + ".csv");
            java.nio.file.Files.writeString(file.toPath(), csv.toString());
            System.out.println("✅ CSV exporté : " + file.getAbsolutePath());

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
