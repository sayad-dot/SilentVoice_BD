package com.example.silentvoice_bd.ai.services;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.silentvoice_bd.ai.dto.PredictionResponse;
import com.example.silentvoice_bd.model.ExtractedFrame;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PythonAIIntegrationService {
    
    @Value("${ai.python.executable:python3}")
    private String pythonExecutable;
    
    @Value("${ai.python.scripts.path:./python-ai/scripts/}")
    private String pythonScriptsPath;
    
    @Value("${ai.python.timeout:30}")
    private int timeoutSeconds;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public PredictionResponse processVideoFrames(List<ExtractedFrame> frames) {
        try {
            long startTime = System.currentTimeMillis();
            
            // Step 1: Extract frame paths
            List<String> framePaths = extractFramePaths(frames);
            
            if (framePaths.isEmpty()) {
                return PredictionResponse.error("No valid frame paths found");
            }
            
            // Step 2: Extract poses using MediaPipe
            JsonNode poseResult = extractPosesFromFrames(framePaths);
            
            if (!poseResult.get("success").asBoolean()) {
                return PredictionResponse.error("Pose extraction failed: " + poseResult.get("error").asText());
            }
            
            // Step 3: Predict sign language using LSTM
            JsonNode predictionResult = predictSignLanguage(poseResult.get("pose_sequence"));
            
            if (predictionResult.has("error")) {
                return PredictionResponse.error("Prediction failed: " + predictionResult.get("error").asText());
            }
            
            // Step 4: Create response
            int processingTime = (int) (System.currentTimeMillis() - startTime);
            
            PredictionResponse response = PredictionResponse.success(
                predictionResult.get("predicted_text").asText(),
                predictionResult.get("confidence").asDouble(),
                processingTime,
                null // Will be set after saving to database
            );
            
            response.setModelVersion(predictionResult.get("model_version").asText("bangla_lstm_v1"));
            
            return response;
            
        } catch (Exception e) {
            return PredictionResponse.error("Processing failed: " + e.getMessage());
        }
    }
    
    private List<String> extractFramePaths(List<ExtractedFrame> frames) {
        List<String> paths = new ArrayList<>();
        for (ExtractedFrame frame : frames) {
            if (frame.getFilePath() != null && new File(frame.getFilePath()).exists()) {
                paths.add(frame.getFilePath());
            }
        }
        return paths;
    }
    
    private JsonNode extractPosesFromFrames(List<String> framePaths) throws Exception {
        String framePathsJson = objectMapper.writeValueAsString(framePaths);
        
        return executePythonScript("pose_extractor.py", "frames", framePathsJson);
    }
    
    private JsonNode predictSignLanguage(JsonNode poseSequence) throws Exception {
        String poseSequenceJson = objectMapper.writeValueAsString(poseSequence);
        
        return executePythonScript("sign_predictor.py", poseSequenceJson);
    }
    
    private JsonNode executePythonScript(String scriptName, String... args) throws Exception {
        // Build command
        CommandLine cmdLine = new CommandLine(pythonExecutable);
        cmdLine.addArgument(pythonScriptsPath + scriptName);
        
        for (String arg : args) {
            cmdLine.addArgument(arg, false); // false = don't handle quotes
        }
        
        // Setup executor
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(0);
        
        // Setup timeout
        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeoutSeconds * 1000L);
        executor.setWatchdog(watchdog);
        
        // Capture output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
        executor.setStreamHandler(streamHandler);
        
        // Set working directory to project root
        File workingDir = new File(".");
        
        try {
            int exitCode = executor.execute(cmdLine);
            
            if (exitCode != 0) {
                throw new Exception("Python script failed with exit code: " + exitCode + 
                                  ". Error: " + errorStream.toString());
            }
            
            String output = outputStream.toString().trim();
            
            if (output.isEmpty()) {
                throw new Exception("Python script produced no output");
            }
            
            return objectMapper.readTree(output);
            
        } catch (Exception e) {
            String errorOutput = errorStream.toString();
            throw new Exception("Failed to execute Python script '" + scriptName + "': " + 
                              e.getMessage() + (errorOutput.isEmpty() ? "" : ". Error output: " + errorOutput));
        }
    }
    
    public boolean isAISystemReady() {
        try {
            // Test if Python and required scripts are available
            JsonNode testResult = executePythonScript("sign_predictor.py", "[]");
            return testResult != null;
        } catch (Exception e) {
            return false;
        }
    }
}
