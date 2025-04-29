package com.example.Evenement.Assistant;

import com.example.Evenement.Model.Place;
import javafx.scene.control.TextArea;
import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AssistantVocal {
    private final Map<String, String> messageTexts;
    private final TextArea textArea;
    private final ExecutorService executorService;
    private final AtomicBoolean isActive;
    private final AtomicReference<Place> selectedPlace;
    private Voice voice;

    public AssistantVocal(TextArea textArea) {
        this.textArea = textArea;
        this.executorService = Executors.newFixedThreadPool(4);
        this.isActive = new AtomicBoolean(false);
        this.selectedPlace = new AtomicReference<>(null);
        this.messageTexts = initializeMessageTexts();
        initializeVoice();
    }

    private void initializeVoice() {
        try {
            System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
            VoiceManager voiceManager = VoiceManager.getInstance();
            voice = voiceManager.getVoice("kevin16");
            if (voice != null) {
                voice.allocate();
                // Réglages pour améliorer la voix
                voice.setRate(150); // Vitesse de parole (mots par minute)
                voice.setPitch(100); // Hauteur de la voix (Hz)
                voice.setVolume(0.9f); // Volume (0.0 à 1.0)
            } else {
                logMessage("Erreur: Impossible d'initialiser la voix");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logMessage("Erreur d'initialisation de la voix: " + e.getMessage());
        }
    }

    private Map<String, String> initializeMessageTexts() {
        Map<String, String> texts = new HashMap<>();
        texts.put("acceuil", "Bonjour, je suis votre assistante vocale. Je peux vous aider à trouver une place. Que souhaitez-vous faire ?");
        texts.put("aide", "Je peux vous aider à choisir une place. Préférez-vous être au premier rang, au milieu ou à l'arrière de la salle ?");
        texts.put("premier_rang", "Les places du premier rang, rangées A à C, offrent une vue imprenable. Je vous conseille particulièrement les places centrales A4 à A7.");
        texts.put("milieu", "Les places au milieu sont idéales ! Dans les rangées D à F, vous aurez une vue parfaitement équilibrée.");
        texts.put("arriere", "Les places à l'arrière offrent une excellente vue d'ensemble. Les rangées G et H sont parfaites pour avoir une vision globale.");
        texts.put("prix", "Les tarifs varient selon l'emplacement. Premier rang : 50 euros, milieu : 40 euros, arrière : 30 euros.");
        texts.put("disponible", "Je vais vous montrer les places disponibles. En vert, vous verrez les places libres, en gris les places occupées.");
        texts.put("merci", "Je vous en prie ! C'est un plaisir de vous aider.");
        texts.put("recommandation", "Pour une expérience optimale, je vous recommande la rangée E au milieu. Vous aurez une vue parfaite et une excellente acoustique.");
        texts.put("incompris", "Je ne suis pas sûre d'avoir bien compris. Pouvez-vous reformuler votre demande ?");
        texts.put("au_revoir", "Au revoir et à bientôt !");
        return texts;
    }

    private void speakMessage(String messageKey) {
        if (!isActive.get()) return;
        
        String message = messageTexts.get(messageKey);
        if (message != null) {
            logMessage("Assistant: " + message);
            
            executorService.submit(() -> {
                try {
                    if (voice != null) {
                        voice.speak(message);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logMessage("Erreur lors de la synthèse vocale: " + e.getMessage());
                }
            });
        }
    }

    private void logMessage(String message) {
        Platform.runLater(() -> {
            if (textArea != null) {
                textArea.appendText(message + "\n");
            }
        });
    }

    public void start() {
        isActive.set(true);
        speakMessage("acceuil");
        askUserPreference();
    }

    public void stop() {
        isActive.set(false);
        if (voice != null) {
            try {
                voice.deallocate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void askUserPreference() {
        Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Assistant vocal");
            dialog.setHeaderText("Où souhaitez-vous être assis ?");
            dialog.setContentText("Premier rang, milieu ou arrière ?");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(this::processUserInput);
        });
    }

    public void processUserInput(String input) {
        if (!isActive.get()) return;

        input = input.toLowerCase().trim();

        if (input.contains("premier") || input.contains("avant")) {
            speakMessage("premier_rang");
        } else if (input.contains("milieu") || input.contains("centre")) {
            speakMessage("milieu");
        } else if (input.contains("arrière") || input.contains("derrière")) {
            speakMessage("arriere");
        } else if (input.contains("prix") || input.contains("tarif")) {
            speakMessage("prix");
        } else if (input.contains("disponible") || input.contains("libre")) {
            speakMessage("disponible");
        } else if (input.contains("merci")) {
            speakMessage("merci");
        } else if (input.contains("aide") || input.contains("help")) {
            speakMessage("aide");
        } else if (input.contains("au revoir") || input.contains("bye")) {
            speakMessage("au_revoir");
            stop();
        } else {
            speakMessage("incompris");
        }
    }

    public void selectPlace(Place place) {
        if (place != null) {
            selectedPlace.set(place);
            String message = "Vous avez sélectionné la place " + place.getNumeroColonne() + 
                           " dans la rangée " + (char)('A' + place.getNumeroLigne() - 1) + 
                           ". Voulez-vous confirmer cette sélection ?";
            logMessage("Assistant: " + message);
            
            executorService.submit(() -> {
                try {
                    if (voice != null) {
                        voice.speak(message);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logMessage("Erreur lors de la synthèse vocale: " + e.getMessage());
                }
            });
        }
    }
}