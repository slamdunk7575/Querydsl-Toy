package me.study.querydsl.repository;

import me.study.querydsl.dto.MemberSearchCondition;
import me.study.querydsl.dto.MemberTeamDto;
import me.study.querydsl.entity.Member;
import me.study.querydsl.entity.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @DisplayName("기본 JPA 테스트")
    @Test
    void basicTest() {
        Member member = new Member("test1", 10);
        memberJpaRepository.save(member);

        Member findMemberById = memberJpaRepository.findById(member.getId()).get();
        assertThat(findMemberById).isEqualTo(member);

        // List<Member> result = memberJpaRepository.findAll();
        List<Member> result = memberJpaRepository.findAll_QueryDsl();
        assertThat(result).containsExactly(member);

        // List<Member> findMemberByUsername = memberJpaRepository.findByUsername("test1");
        List<Member> findMemberByUsername = memberJpaRepository.findByUsername_QueryDsl("test1");
        assertThat(findMemberByUsername).containsExactly(member);
    }

    @Test
    void searchTest() {
        // given
        Team teamA = new Team("TeamA");
        Team teamB = new Team("TeamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("Member1", 10, teamA);
        Member member2 = new Member("Member2", 20, teamA);
        Member member3 = new Member("Member3", 30, teamB);
        Member member4 = new Member("Member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        // condition.setAgeGoe(35);
        // condition.setAgeLoe(40);
        // condition.setTeamName("TeamB");

        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
        assertThat(result).extracting("username").containsExactly("Member4");
    }
}
