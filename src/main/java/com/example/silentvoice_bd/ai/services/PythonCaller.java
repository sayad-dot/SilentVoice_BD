package com.example.silentvoice_bd.ai.services;

import java.io.ByteArrayOutputStream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PythonCaller {

    private static final Logger logger = LoggerFactory.getLogger(PythonCaller.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Execute a Python script with arguments and parse JSON output.
     */
    public static JsonNode callPython(
            String pythonExec,
            String scriptPath,
            String arg1,
            String arg2) throws Exception {

        CommandLine cmd = new CommandLine(pythonExec);
        cmd.addArgument(scriptPath, false);
        cmd.addArgument(arg1, false);
        cmd.addArgument(arg2, false);

        DefaultExecutor exec = new DefaultExecutor();
        exec.setExitValue(0);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(120 * 1000);
        exec.setWatchdog(watchdog);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        exec.setStreamHandler(new PumpStreamHandler(out, err));

        try {
            logger.debug("Running Python: {}", cmd);
            exec.execute(cmd);
        } catch (ExecuteException e) {
            logger.error("Python script error: {}", err.toString());
            throw new RuntimeException("Python script failed: " + err.toString());
        }

        String output = out.toString().trim();
        logger.debug("Python output: {}", output);
        return mapper.readTree(output);
    }
}
