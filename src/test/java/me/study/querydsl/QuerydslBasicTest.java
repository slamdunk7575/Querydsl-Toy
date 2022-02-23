package me.study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import me.study.querydsl.entity.Member;
import me.study.querydsl.entity.QMember;
import me.study.querydsl.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static me.study.querydsl.entity.QMember.member;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    void setUp() {
        queryFactory = new JPAQueryFactory(em);

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
    }

    @DisplayName("JPQL Member 조회")
    @Test
    void startJPQL() {
        // Member1 조회
        String qlString = "select m from Member m " +
                "where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "Member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("Member1");
    }

    @DisplayName("Querydsl Member 조회")
    @Test
    void startQuerydsl() {
        // QMember m = new QMember("m");

        Member findMember = queryFactory.select(member)
                .from(member)
                .where(member.username.eq("Member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("Member1");
    }

    @DisplayName("and 검색 조건")
    @Test
    void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("Member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("Member1");
    }

    @DisplayName("and 검색 다른 방법")
    @Test
    void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("Member1"),
                        member.age.eq(10))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("Member1");
    }

    @DisplayName("결과 조회")
    @Test
    void resultFetch() {
        // 리스트
        List<Member> fetch = queryFactory.selectFrom(member)
                .fetch();

        // 단건 조회
        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();

        // limit(1)
        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        // 페이징
        QueryResults<Member> fetchResults = queryFactory.selectFrom(member)
                .fetchResults();

        fetchResults.getTotal();
        List<Member> results = fetchResults.getResults();

        // count 쿼리
        long total = queryFactory.selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순 (desc)
     * 2. 회원 이름 오름차순 (asc)
     * 단, 2에서 회원 이름이 없으면 마지막에 출력 (nulls last)
     */
    @DisplayName("정렬")
    @Test
    void sort() {
        // given
        em.persist(new Member(null, 100));
        em.persist(new Member("Member5", 100));
        em.persist(new Member("Member6", 100));

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        // then
        assertThat(member5.getUsername()).isEqualTo("Member5");
        assertThat(member6.getUsername()).isEqualTo("Member6");
        assertThat(memberNull.getUsername()).isNull();
    }


}
