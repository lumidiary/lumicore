package com.example.lumicore.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "diary_qa")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DiaryQA extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @Column(nullable = false, length = 500)
    private String aiQuestion;

    @Column(nullable = false, length = 1000)
    private String userAnswer;

    /** 질문과 답변 세팅 */
    public void setQuestionAndAnswer(String question, String answer) {
        this.aiQuestion = question;
        this.userAnswer = answer;
    }

}