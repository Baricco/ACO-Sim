package com.example.metrics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.example.graphics.Coord;

public class MetricsCollector {
    
    private static MetricsCollector instance;
    private List<LogEntry> entries;
    private long simulationStartTime;
    private String currentExperimentName;
    public static final String LOGS_PATH = "logs" + File.separator; // Directory per i file di log
    
    private MetricsCollector() {
        entries = new ArrayList<>();
        simulationStartTime = System.nanoTime();
    }
    
    public static MetricsCollector getInstance() {
        if (instance == null) {
            instance = new MetricsCollector();
        }
        return instance;
    }
    
    public void startExperiment(String experimentName) {
        this.currentExperimentName = experimentName;
        entries.clear();
        simulationStartTime = System.nanoTime();
        logEvent("EXPERIMENT_START", experimentName, null, null);
    }
    
    public void logEvent(String eventType, String description, Coord position, Object data) {
        long timestamp = System.nanoTime() - simulationStartTime;
        entries.add(new LogEntry(timestamp, eventType, description, position, data));
    }
    
    public void exportToCSV(String filename) throws IOException {
        try (FileWriter writer = new FileWriter(LOGS_PATH + filename)) {
            writer.write("timestamp_ns,event_type,description,x,y,data\n");
            for (LogEntry entry : entries) {
                writer.write(entry.toCSV() + "\n");
            }
        }
    }
    
    public List<LogEntry> getEntries() { return new ArrayList<>(entries); }
    
    public static class LogEntry {
        public final long timestamp;
        public final String eventType;
        public final String description;
        public final Coord position;
        public final Object data;
        
        public LogEntry(long timestamp, String eventType, String description, Coord position, Object data) {
            this.timestamp = timestamp;
            this.eventType = eventType;
            this.description = description;
            this.position = position;
            this.data = data;
        }
        
        public String toCSV() {

            String dataString = (data != null) ? data.toString().replace(",", ";") : "";


            return String.format("%d,%s,%s,%s,%s,%s", 
                timestamp, eventType, description,
                position != null ? position.x : "",
                position != null ? position.y : "",
                dataString);
        }
    }
}