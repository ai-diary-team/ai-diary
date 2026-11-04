package com.aidiary.domain.user.controller;

import com.aidiary.domain.user.entity.User;
import com.aidiary.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aidiary.domain.diary.repository.DiaryRepository;
import com.aidiary.global.jwt.JwtProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 회원가입에_성공하면_사용자가_DB에_저장된다() throws Exception {
        String email = "signup@example.com";
        String password = "password123!";
        String nickname = "회원가입테스트";

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "nickname": "%s"
                                }
                                """.formatted(email, password, nickname)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.nickname").value(nickname))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        User savedUser = userRepository.findByEmail(email).orElseThrow();

        assertTrue(passwordEncoder.matches(password, savedUser.getPasswordHash()));
    }

    @Test
    void 이미_사용중인_이메일로_회원가입하면_409를_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "duplicate@example.com",
                                  "password": "password123!",
                                  "nickname": "첫번째닉네임"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "duplicate@example.com",
                                  "password": "password123!",
                                  "nickname": "두번째닉네임"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
    }
    @Test
    void 로그인에_성공하면_JWT를_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login@example.com",
                                  "password": "password123!",
                                  "nickname": "로그인테스트"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login@example.com",
                                  "password": "password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void 잘못된_비밀번호로_로그인하면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "wrong-password@example.com",
                                  "password": "password123!",
                                  "nickname": "비밀번호테스트"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "wrong-password@example.com",
                                  "password": "wrong-password!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
    @Test
    void JWT_없이_일기를_작성하면_403을_반환한다() throws Exception {
        mockMvc.perform(post("/api/diaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "인증 없는 일기",
                                  "content": "로그인하지 않은 요청입니다."
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void JWT가_있으면_일기를_작성하고_DB에_저장한다() throws Exception {
        User user = userRepository.save(new User(
                "diary@example.com",
                passwordEncoder.encode("password123!"),
                "일기작성테스트"
        ));

        String accessToken = jwtProvider.createAccessToken(
                user.getId(),
                user.getEmail()
        );

        mockMvc.perform(post("/api/diaries")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "자동 테스트 일기",
                                  "content": "JWT를 포함한 일기 작성 통합 테스트입니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("자동 테스트 일기"))
                .andExpect(jsonPath("$.content").value(
                        "JWT를 포함한 일기 작성 통합 테스트입니다."
                ));

        assertEquals(1, diaryRepository.count());
    }
}