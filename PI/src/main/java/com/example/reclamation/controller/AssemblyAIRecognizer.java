package com.example.reclamation.controller;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONObject;


public class AssemblyAIRecognizer {

    private static final String API_KEY = "4362f4327c61461c8e36b27e0edf3907";

    public String transcribe(File audioFile) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        // Upload audio
        HttpRequest uploadRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.assemblyai.com/v2/upload"))
                .header("authorization", API_KEY)
                .POST(HttpRequest.BodyPublishers.ofFile(audioFile.toPath()))
                .build();

        HttpResponse<String> uploadResponse = client.send(uploadRequest, HttpResponse.BodyHandlers.ofString());
        String uploadUrl = new JSONObject(uploadResponse.body()).getString("upload_url");

        // Start transcription
        HttpRequest transcribeRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.assemblyai.com/v2/transcript"))
                .header("authorization", API_KEY)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"audio_url\": \"" + uploadUrl + "\"}"))
                .build();

        HttpResponse<String> transcribeResponse = client.send(transcribeRequest, HttpResponse.BodyHandlers.ofString());
        String transcriptId = new JSONObject(transcribeResponse.body()).getString("id");

        // Poll for result
        String transcriptText = "";
        while (true) {
            Thread.sleep(3000);
            HttpRequest pollRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.assemblyai.com/v2/transcript/" + transcriptId))
                    .header("authorization", API_KEY)
                    .build();

            HttpResponse<String> pollResponse = client.send(pollRequest, HttpResponse.BodyHandlers.ofString());
            JSONObject pollJson = new JSONObject(pollResponse.body());

            if (pollJson.getString("status").equals("completed")) {
                transcriptText = pollJson.getString("text");
                break;
            } else if (pollJson.getString("status").equals("error")) {
                throw new IOException("Transcription failed: " + pollJson.getString("error"));
            }
        }

        return transcriptText;
    }
}
