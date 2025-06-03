package com.example.lumicore.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeParseException;

//Batch 작동 테스트용 컨트롤러
@RestController
@RequestMapping("/core/batch")
@RequiredArgsConstructor
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job weeklyDigestJob;
    private final Job monthlyDigestJob;

    @GetMapping("/weekly")
    public ResponseEntity<String> runWeeklyManually(
            @RequestParam("start") String start,
            @RequestParam("end")   String end
    ) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("periodStart", start)
                    .addString("periodEnd",   end)
                    .addLong("run.id",        System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(weeklyDigestJob, params);
            return ResponseEntity.ok("Weekly Digest Batch launched: " + start + " ~ " + end);
        } catch (DateTimeParseException dte) {
            return ResponseEntity.badRequest().body("날짜 형식이 잘못되었습니다. YYYY-MM-DD로 입력해주세요.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Batch 실행 실패: " + e.getMessage());
        }
    }

    @GetMapping("/monthly")
    public ResponseEntity<String> runMonthlyManually(
            @RequestParam("start") String start,
            @RequestParam("end")   String end
    ) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("periodStart", start)
                    .addString("periodEnd",   end)
                    .addLong("run.id",        System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(monthlyDigestJob, params);
            return ResponseEntity.ok("Monthly Digest Batch launched: " + start + " ~ " + end);
        } catch (DateTimeParseException dte) {
            return ResponseEntity.badRequest().body("날짜 형식이 잘못되었습니다. YYYY-MM-DD로 입력해주세요.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Batch 실행 실패: " + e.getMessage());
        }
    }
}