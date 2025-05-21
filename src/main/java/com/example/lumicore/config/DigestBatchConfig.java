//package com.example.lumicore.config;
//
//import com.example.lumicore.dto.Digest.response.DigestResponseDto;
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.Step;
//import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
//import org.springframework.batch.item.ItemReader;
//import org.springframework.batch.item.ItemWriter;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.UUID;
//
//@Configuration
//@EnableBatchProcessing
//public class DigestBatchConfig {
//
//    @Bean
//    public Job digestJob(JobBuilderFactory jobs, Step digestStep) {
//        return jobs.get("digestJob")
//                .start(digestStep)
//                .build();
//    }
//
//    @Bean
//    public Step digestStep(
//            StepBuilderFactory steps,
//            ItemReader<UUID> userReader,
//            ItemProcessor<UserDigestParam, DigestResponseDto> processor,
//            ItemWriter<DigestResponseDto> writer
//    ) {
//        return steps.get("digestStep")
//                .<UserDigestParam, DigestResponseDto>chunk(1)
//                .reader(userReader)
//                .processor(processor)
//                .writer(writer)
//                .build();
//    }
//
//    @Bean public RestTemplate restTemplate() { return new RestTemplate(); }
//}