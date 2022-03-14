package me.study.querydsl.repository;

import me.study.querydsl.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
// @Transactional
public class MemberRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @DisplayName("기본 JPA 테스트")
    @Test
    void basicTest() {
        Member member = new Member("test1", 10);
        memberRepository.save(member);

        Member findMemberById = memberRepository.findById(member.getId()).get();
        assertThat(findMemberById).isEqualTo(member);

        List<Member> result = memberRepository.findAll();
        assertThat(result).containsExactly(member);

        List<Member> findMemberByUsername = memberRepository.findByUsername("test1");
        assertThat(findMemberByUsername).containsExactly(member);
    }

}
