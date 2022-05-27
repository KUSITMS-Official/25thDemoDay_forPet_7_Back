package com.kusitms.forpet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoUserDto {
        private Long userId;
        private String name;
        private String email;
        private String imageUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignUpDto {
        private String nickname;
        @JsonProperty("phone_number")
        private String phoneNumber;
        private String address;

    }

    @Data
    @AllArgsConstructor
    public static class LoginDto {
        @JsonProperty("is_signup")
        boolean isSignUp;
        String token;
    }


}
