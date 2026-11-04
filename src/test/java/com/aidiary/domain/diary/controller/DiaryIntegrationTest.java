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
}