package com.queuectl.service;

import com.queuectl.dto.JobExecutionResult;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Service
public class JobRunner {

    public JobExecutionResult executeJob(String command, int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        StringBuilder output = new StringBuilder();
        int exitCode = -1;

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                processBuilder.command("cmd.exe", "/c", command);
            } else {
                processBuilder.command("sh", "-c", command);
            }

            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null){
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if(finished){
                exitCode = process.exitValue();
            }else{
                process.destroyForcibly();
                output.append(exitCode).append("\n");
                exitCode = 124;
            }
        }catch (Exception e){
            output.append(e.getMessage()).append("\n");
            exitCode = 1;
        }
        long endTime = System.currentTimeMillis()-startTime;
        return new JobExecutionResult(exitCode, output.toString(), endTime);
    }
}
