package me.study.querydsl.repository;

import me.study.querydsl.dto.MemberSearchCondition;
import me.study.querydsl.dto.MemberTeamDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberRepositoryCustom {
    List<MemberTeamDto> search(MemberSearchCondition memberSearchCondition);
    Page<MemberTeamDto> searchPageSimple(MemberSearchCondition memberSearchCondition, Pageable pageable);
    Page<MemberTeamDto> searchPageComplex(MemberSearchCondition memberSearchCondition, Pageable pageable);
}
