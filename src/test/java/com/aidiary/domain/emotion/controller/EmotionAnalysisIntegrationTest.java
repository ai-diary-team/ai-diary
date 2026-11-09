package com.aidiary.domain.emotion.controller;

import com.aidiary.domain.diary.entity.Diary;
import com.aidiary.domain.diary.repository.DiaryRepository;
import com.aidiary.domain.emotion.repository.EmotionAnalysisRepository;
import com.aidiary.domain.user.entity.User;
import com.aidiary.domain.user.repository.UserRepository;
import com.aidiary.global.gemini.GeminiClient;
import com.aidiary.global.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aidiary.global.gemini.GeminiClientException;
import com.aidiary.domain.emotion.entity.Emotion;
import com.aidiary.global.gemini.dto.GeminiAnalyzeResponse;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmotionAnalysisIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private EmotionAnalysisRepository emotionAnalysisRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private GeminiClient geminiClient;
    private User createUser(String email, String nickname) {
        User user = new User(
                email,
                passwordEncoder.encode("password123!"),
                nickname
        );

        return userRepository.save(user);
    }

    private String createAccessToken(User user) {
        return jwtProvider.createAccessToken(user.getId(), user.getEmail());
    }
    @Test
    void 감정_분석에_실패하면_502를_반환하고_결과를_저장하지_않는다() throws Exception {
        User user = createUser("analysis-fail@example.com", "분석실패테스트");
        String accessToken = createAccessToken(user);

        Diary diary = createDiary(
                user,
                "분석 실패 일기",
                "Gemini 서버 오류를 가정하는 테스트입니다."
        );

        when(geminiClient.analyze(diary.getContent()))
                .thenThrow(new GeminiClientException("Gemini 서버 연결 실패"));

        mockMvc.perform(post("/api/diaries/{diaryId}/analyses", diary.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code")
                        .value("GEMINI_ANALYSIS_FAILED"));

        assertEquals(0, emotionAnalysisRepository.count());
    }
    @Test
    void 감정_분석에_성공하면_결과를_저장하고_201을_반환한다() throws Exception {
        User user = createUser("analysis@example.com", "분석테스트");
        String accessToken = createAccessToken(user);

        Diary diary = createDiary(
                user,
                "분석할 일기",
                "오늘은 기분이 좋아서 즐거운 하루를 보냈다."
        );

        when(geminiClient.analyze(diary.getContent()))
                .thenReturn(new GeminiAnalyzeResponse(
                        Emotion.JOY,
                        4,
                        Map.of(
                                Emotion.JOY, 4,
                                Emotion.SADNESS, 0,
                                Emotion.ANGER, 0,
                                Emotion.ANXIETY, 1,
                                Emotion.CALM, 3
                        ),
                        "오늘의 즐거운 감정을 충분히 느껴보세요."
                ));

        mockMvc.perform(post("/api/diaries/{diaryId}/analyses", diary.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diaryId").value(diary.getId()))
                .andExpect(jsonPath("$.emotion").value("JOY"))
                .andExpect(jsonPath("$.intensity").value(4))
                .andExpect(jsonPath("$.feedback")
                        .value("오늘의 즐거운 감정을 충분히 느껴보세요."));

        assertEquals(1, emotionAnalysisRepository.count());
    }
    private Diary createDiary(User user, String title, String content) {
        Diary diary = new Diary(user, title, content);
        return diaryRepository.save(diary);
    }
}