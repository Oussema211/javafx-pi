package com.example.auth.service;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.Person;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoogleAuthHelper {
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/userinfo.email");
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    public static String authenticate() throws IOException, GeneralSecurityException {
        System.out.println("Attempting to authenticate with Google...");

        // Delete old credentials to force account selection
        deleteStoredCredentials();

        System.out.println("Loading Google client secrets from " + CREDENTIALS_FILE_PATH);
        InputStream inStream = GoogleAuthHelper.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (inStream == null) {
            // Fallback: Try loading from the project root or an absolute path
            File credentialsFile = new File("src/main/resources/credentials.json");
            if (credentialsFile.exists()) {
                System.out.println("Found credentials.json in src/main/resources as a fallback.");
                inStream = new FileInputStream(credentialsFile);
            } else {
                throw new IOException("Credentials file not found: " + CREDENTIALS_FILE_PATH +
                        ". Please ensure credentials.json is placed in src/main/resources.");
            }
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(inStream));

        System.out.println("Building Google authorization flow...");
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        // Add the profile scope to the existing SCOPES
        List<String> updatedScopes = new ArrayList<>(SCOPES);
        updatedScopes.add("https://www.googleapis.com/auth/userinfo.profile");

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, updatedScopes)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        System.out.println("Starting OAuth authorization with dynamic port...");
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().build(); // No setPort, auto-assigns available port
        int assignedPort = receiver.getPort(); // Get the dynamically assigned port
        System.out.println("Using dynamically assigned port: " + assignedPort);

        AuthorizationCodeInstalledApp authApp = new AuthorizationCodeInstalledApp(flow, receiver) {
            @Override
            protected void onAuthorization(AuthorizationCodeRequestUrl authorizationUrl) throws IOException {
                authorizationUrl.set("prompt", "select_account");
                super.onAuthorization(authorizationUrl);
            }
        };

        Credential credential;
        try {
            credential = authApp.authorize("user");
        } finally {
            receiver.stop();
            System.out.println("LocalServerReceiver stopped. Port " + assignedPort + " released.");
        }

        System.out.println("OAuth authorization completed.");
        System.out.println("Building PeopleService...");
        PeopleService peopleService = new PeopleService.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName("JavaFX Auth System")
                .build();

        System.out.println("Fetching user profile...");
        Person profile = peopleService.people().get("people/me")
                .setPersonFields("emailAddresses,names")
                .execute();

        String email = (profile.getEmailAddresses() != null && !profile.getEmailAddresses().isEmpty())
                ? profile.getEmailAddresses().get(0).getValue()
                : null;

        System.out.println("Retrieved email: " + email);
        return email;
    }

    private static void deleteStoredCredentials() {
        File tokensDir = new File(TOKENS_DIRECTORY_PATH);
        if (tokensDir.exists()) {
            File[] files = tokensDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            tokensDir.delete();
            System.out.println("Deleted stored Google credentials.");
        }
    }
}