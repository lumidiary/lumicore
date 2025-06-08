package com.example.lumicore.config;

import com.example.lumicore.dto.digest.request.DigestRequestDto;
import com.example.lumicore.service.DigestQueueService;
import com.example.lumicore.service.DiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.beans.factory.annotation.Value;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class DigestBatchConfig {

    private final JobRepository              jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobLauncher                jobLauncher;
    private final DiaryService               diaryService;
    private final DigestQueueService         digestQueueService;
    private final ApplicationContext         applicationContext;

    @Bean
    @StepScope
    public ItemReader<UUID> userIdReader() {
        List<UUID> allUserIds = diaryService.getAllUserIds();
        return new ListItemReader<>(allUserIds);
    }

    @Bean
    @StepScope
    public ItemProcessor<UUID, DigestRequestDto> digestProcessor(
            @Value("#{jobParameters['periodStart']}") String start,
            @Value("#{jobParameters['periodEnd']}")   String end
    ) {
        LocalDateTime periodStart = LocalDate.parse(start).atStartOfDay();
        LocalDateTime periodEnd   = LocalDate.parse(end).atTime(LocalTime.MAX);

        return userId -> {
            var entries = diaryService.getEntriesForDigest(userId, periodStart, periodEnd);
            return DigestRequestDto.builder()
                    .id(userId.toString())
                    .userLocale(diaryService.getUserLocale(userId))
                    .entries(entries)
                    .build();
        };
    }

    @Bean
    public ItemWriter<DigestRequestDto> digestItemWriter() {
        // Chunk<DigestRequestDto>는 Iterable<DigestRequestDto>로 바로 전달 가능
        return items -> digestQueueService.sendDigestRequests(items);
    }

    @Bean
    public Step digestStep(
            ItemReader<UUID> reader,
            ItemProcessor<UUID, DigestRequestDto> processor,
            ItemWriter<DigestRequestDto> writer
    ) {
        return new StepBuilder("digestStep", jobRepository)
                .<UUID, DigestRequestDto>chunk(100, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job weeklyDigestJob(Step digestStep) {
        return new JobBuilder("weeklyDigestJob", jobRepository)
                .start(digestStep)
                .build();
    }

    @Bean
    public Job monthlyDigestJob(Step digestStep) {
        return new JobBuilder("monthlyDigestJob", jobRepository)
                .start(digestStep)
                .build();
    }

    /**
     * 매주 월요일 02:00에 이전 주(月~日) 범위로 Batch 실행
     */
    @Scheduled(cron = "0 0 2 * * MON")
    public void runWeekly() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate end   = today.minusWeeks(1).with(DayOfWeek.SUNDAY);
        LocalDate start = end.minusDays(6);

        JobParameters params = new JobParametersBuilder()
                .addString("periodStart", start.toString())
                .addString("periodEnd",   end.toString())
                .addLong("run.id",        System.currentTimeMillis())
                .toJobParameters();

        Job job = applicationContext.getBean("weeklyDigestJob", Job.class);
        jobLauncher.run(job, params);
    }

    /**
     * 매월 1일 03:00에 이전 월(1일~말일) 범위로 Batch 실행
     */
    @Scheduled(cron = "0 0 3 1 * *")
    public void runMonthly() throws Exception {
        LocalDate first     = LocalDate.now().withDayOfMonth(1);
        LocalDate prevMonth = first.minusMonths(1);
        LocalDate start     = prevMonth.withDayOfMonth(1);
        LocalDate end       = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth());

        JobParameters params = new JobParametersBuilder()
                .addString("periodStart", start.toString())
                .addString("periodEnd",   end.toString())
                .addLong("run.id",        System.currentTimeMillis())
                .toJobParameters();

        Job job = applicationContext.getBean("monthlyDigestJob", Job.class);
        jobLauncher.run(job, params);
    }
}
