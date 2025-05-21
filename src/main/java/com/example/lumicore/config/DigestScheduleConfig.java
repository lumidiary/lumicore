//package com.example.lumicore.config;
//
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.JobParameters;
//import org.springframework.batch.core.JobParametersBuilder;
//import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
//import org.springframework.batch.core.launch.JobLauncher;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//
//import java.util.Date;
//
//@Configuration
//@EnableBatchProcessing
//@EnableScheduling
//public class DigestScheduleConfig {
//
//    private final JobLauncher jobLauncher;
//    private final Job digestJob;
//
//    public DigestScheduleConfig(JobLauncher jobLauncher, Job digestJob) {
//        this.jobLauncher = jobLauncher;
//        this.digestJob = digestJob;
//    }
//
//    /** 매주 월요일 00:00에 실행 → 직전 주 범위 처리 */
//    @Scheduled(cron = "0 0 0 * * MON")
//    public void launchWeeklyDigest() throws Exception {
//        JobParameters params = new JobParametersBuilder()
//                .addString("periodType", "WEEKLY")
//                .addDate("runDate", new Date())
//                .toJobParameters();
//        jobLauncher.run(digestJob, params);
//    }
//
//    /** 매월 1일 00:00에 실행 → 직전월 범위 처리 */
//    @Scheduled(cron = "0 0 0 1 * *")
//    public void launchMonthlyDigest() throws Exception {
//        JobParameters params = new JobParametersBuilder()
//                .addString("periodType", "MONTHLY")
//                .addDate("runDate", new Date())
//                .toJobParameters();
//        jobLauncher.run(digestJob, params);
//    }
//
//}
