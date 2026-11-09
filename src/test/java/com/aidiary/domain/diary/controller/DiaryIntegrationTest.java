package com.aidiary.domain.diary.controller;

import com.aidiary.domain.diary.entity.Diary;
import com.aidiary.domain.diary.repository.DiaryRepository;
import com.aidiary.domain.user.entity.User;
import com.aidiary.domain.user.repository.UserRepository;
import com.aidiary.global.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aidiary.domain.emotion.entity.Emotion;
import com.aidiary.domain.emotion.entity.EmotionAnalysis;
import com.aidiary.domain.emotion.repository.EmotionAnalysisRepository;

import java.util.Map;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DiaryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private EmotionAnalysisRepository emotionAnalysisRepository;

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
    void 일기를_삭제하면_목록에서_제외되고_상세조회는_404를_반환한다() throws Exception {
        User user = createUser("delete@example.com", "삭제테스트");
        String accessToken = createAccessToken(user);

        Diary diary = createDiary(
                user,
                "삭제할 일기",
                "소프트 삭제를 검증하기 위한 테스트 일기입니다."
        );

        mockMvc.perform(delete("/api/diaries/{diaryId}", diary.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/diaries")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/diaries/{diaryId}", diary.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }
    private Diary createDiary(User user, String title, String content) {
        Diary diary = new Diary(user, title, content);
        return diaryRepository.save(diary);
    }
    @Test
    void 다른_사용자의_일기는_삭제할_수_없다() throws Exception {
        User owner = createUser("owner@example.com", "일기주인");
        User otherUser = createUser("other@example.com", "다른사용자");

        Diary diary = createDiary(
                owner,
                "주인만 삭제할 수 있는 일기",
                "다른 사용자는 접근하거나 삭제할 수 없습니다."
        );

        String otherAccessToken = createAccessToken(otherUser);
        String ownerAccessToken = createAccessToken(owner);

        mockMvc.perform(delete("/api/diaries/{diaryId}", diary.getId())
                        .header("Authorization", "Bearer " + otherAccessToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/diaries/{diaryId}", diary.getId())
                        .header("Authorization", "Bearer " + ownerAccessToken))
                .andExpect(status().isOk());

    }
    @Test
    void 키워드로_내_일기를_검색할_수_있다() throws Exception {
        User user = createUser("search@example.com", "검색테스트");
        String accessToken = createAccessToken(user);

        createDiary(
                user,
                "카페에서 보낸 오후",
                "조용한 카페에서 과제를 했다."
        );

        createDiary(
                user,
                "집에서 보낸 저녁",
                "집에서 편하게 쉬었다."
        );

        mockMvc.perform(get("/api/diaries")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("keyword", "카페")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title")
                        .value("카페에서 보낸 오후"));
    }
    @Test
    void 감정으로_내_일기를_필터링할_수_있다() throws Exception {
        User user = createUser("emotion@example.com", "감정필터테스트");
        String accessToken = createAccessToken(user);

        Diary joyfulDiary = createDiary(
                user,
                "기분 좋은 날",
                "오랜만에 즐거운 하루를 보냈다."
        );

        Diary sadDiary = createDiary(
                user,
                "조금 힘든 날",
                "오늘은 마음이 조금 무거웠다."
        );

        createEmotionAnalysis(joyfulDiary, Emotion.JOY, 5);
        createEmotionAnalysis(sadDiary, Emotion.SADNESS, 4);

        mockMvc.perform(get("/api/diaries")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("emotion", "JOY")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title")
                        .value("기분 좋은 날"));
    }
    @Test
    void 키워드와_감정을_함께_적용해_일기를_검색할_수_있다() throws Exception {
        User user = createUser("combined@example.com", "복합검색테스트");
        String accessToken = createAccessToken(user);

        Diary cafeJoyDiary = createDiary(
                user,
                "카페에서 행복했던 오후",
                "조용한 카페에서 좋은 시간을 보냈다."
        );

        Diary homeJoyDiary = createDiary(
                user,
                "집에서 행복했던 저녁",
                "집에서 편하게 쉬며 기분이 좋았다."
        );

        Diary cafeSadDiary = createDiary(
                user,
                "카페에서 우울했던 오후",
                "카페에 있었지만 마음은 조금 힘들었다."
        );

        createEmotionAnalysis(cafeJoyDiary, Emotion.JOY, 5);
        createEmotionAnalysis(homeJoyDiary, Emotion.JOY, 4);
        createEmotionAnalysis(cafeSadDiary, Emotion.SADNESS, 4);

        mockMvc.perform(get("/api/diaries")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("keyword", "카페")
                        .param("emotion", "JOY")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title")
                        .value("카페에서 행복했던 오후"));
    }
    @Test
    void 감정_트렌드는_요청한_일수만큼_반환하고_오늘의_점수를_포함한다() throws Exception {
        User user = createUser("trend@example.com", "트렌드테스트");
        String accessToken = createAccessToken(user);

        Diary diary = createDiary(
                user,
                "오늘의 감정 기록",
                "기분 좋은 하루를 보냈다."
        );

        createEmotionAnalysis(diary, Emotion.JOY, 4);

        mockMvc.perform(get("/api/emotions/trends")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(7))
                .andExpect(jsonPath("$.trends.length()").value(7))
                .andExpect(jsonPath("$.trends[6].scores.JOY").value(4.0));
    }
    @Test
    void 삭제된_일기의_감정_분석은_트렌드에서_제외된다() throws Exception {
        User user = createUser("deleted-trend@example.com", "삭제트렌드테스트");
        String accessToken = createAccessToken(user);

        Diary diary = createDiary(
                user,
                "삭제할 감정 기록",
                "분석 결과가 있지만 삭제할 일기다."
        );

        createEmotionAnalysis(diary, Emotion.JOY, 5);

        mockMvc.perform(delete("/api/diaries/{diaryId}", diary.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/emotions/trends")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trends[6].scores.JOY").value(0.0));
    }
    private EmotionAnalysis createEmotionAnalysis(
            Diary diary,
            Emotion emotion,
            int intensity
    ) {
        EmotionAnalysis analysis = new EmotionAnalysis(
                diary,
                "GEMINI",
                emotion,
                intensity,
                Map.of(
                        Emotion.JOY, emotion == Emotion.JOY ? intensity : 0,
                        Emotion.SADNESS, emotion == Emotion.SADNESS ? intensity : 0,
                        Emotion.ANGER, emotion == Emotion.ANGER ? intensity : 0,
                        Emotion.ANXIETY, emotion == Emotion.ANXIETY ? intensity : 0,
                        Emotion.CALM, emotion == Emotion.CALM ? intensity : 0
                ),
                "테스트용 감정 분석 피드백입니다."
        );

        return emotionAnalysisRepository.save(analysis);
    }
}