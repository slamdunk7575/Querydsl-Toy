package me.study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

// @Data
public class MemberDto {
    private String userName;
    private int age;

    public MemberDto() {
    }

    @QueryProjection
    public MemberDto(String userName, int age) {
        this.userName = userName;
        this.age = age;
    }
}
