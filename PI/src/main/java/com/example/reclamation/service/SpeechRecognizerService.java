package com.example.reclamation.service;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.LibVosk;

import javax.sound.sampled.*;

public class SpeechRecognizerService {

    private Model model;

    public SpeechRecognizerService(String modelPath) {
        try {
            this.model = new Model(modelPath); // this can throw IOException
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load Vosk model from path: " + modelPath);
        }
    }

    public String recognize(int maxSeconds) {
        StringBuilder result = new StringBuilder();

        try (Recognizer recognizer = new Recognizer(model, 16000)) {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            byte[] buffer = new byte[4096];
            long endTime = System.currentTimeMillis() + maxSeconds * 1000;

            while (System.currentTimeMillis() < endTime) {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    result.append(recognizer.getResult()).append("\n");
                }
            }

            microphone.stop();
            microphone.close();
            result.append(recognizer.getFinalResult());


        } catch (Exception e) {
            e.printStackTrace();
        }

        return extractText(result.toString());
    }

    private String extractText(String json) {
        int idx = json.indexOf("\"text\" : \"");
        if (idx >= 0) {
            int start = idx + 10;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        }
        return "No speech detected.";
    }
    
}
