package com.example.reclamation.controller;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import javafx.util.Duration;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFileFormat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
public class AudioRecorder {

    private TargetDataLine line;
    private File audioFile;
    private AudioFormat format;
    private ByteArrayOutputStream out;
    private boolean running;

    public AudioRecorder() {
        format = new AudioFormat(16000.0f, 16, 1, true, false);
    }

    public void start() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        running = true;
        out = new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];
        while (running) {
            int count = line.read(buffer, 0, buffer.length);
            if (count > 0) {
                out.write(buffer, 0, count);
            }
        }
    }

    public File stopAndSave() throws IOException {
        running = false;
        line.stop();
        line.close();

        audioFile = File.createTempFile("record", ".wav");
        try (AudioInputStream ais = new AudioInputStream(
                new ByteArrayInputStream(out.toByteArray()), format, out.size())) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, audioFile);
        }
        return audioFile;
    }
}