package me.study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import me.study.querydsl.dto.MemberDto;
import me.study.querydsl.dto.QMemberDto;
import me.study.querydsl.dto.UserDto;
import me.study.querydsl.entity.Member;
import me.study.querydsl.entity.QMember;
import me.study.querydsl.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static me.study.querydsl.entity.QMember.member;
import static me.study.querydsl.entity.QTeam.team;
import static org.assertj.core.api.Assertions.anyOf;
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

    /**
     * ?????? 0 ~ 3
     */
    @DisplayName("JPQL Member ??????")
    @Test
    void startJPQL() {
        // Member1 ??????
        String qlString = "select m from Member m " +
                "where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "Member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("Member1");
    }

    @DisplayName("Querydsl Member ??????")
    @Test
    void startQuerydsl() {
        // QMember m = new QMember("m");

        Member findMember = queryFactory.select(member)
                .from(member)
                .where(member.username.eq("Member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("Member1");
    }

    @DisplayName("and ?????? ??????")
    @Test
    void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("Member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("Member1");
    }

    @DisplayName("and ?????? ?????? ??????")
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

    @DisplayName("?????? ??????")
    @Test
    void resultFetch() {
        // ?????????
        List<Member> fetch = queryFactory.selectFrom(member)
                .fetch();

        // ?????? ??????
        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();

        // limit(1)
        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        // ?????????
        QueryResults<Member> fetchResults = queryFactory.selectFrom(member)
                .fetchResults();

        fetchResults.getTotal();
        List<Member> results = fetchResults.getResults();

        // count ??????
        long total = queryFactory.selectFrom(member)
                .fetchCount();
    }

    /**
     * ?????? ?????? ??????
     * 1. ?????? ?????? ???????????? (desc)
     * 2. ?????? ?????? ???????????? (asc)
     * ???, 2?????? ?????? ????????? ????????? ???????????? ?????? (nulls last)
     */
    @DisplayName("??????")
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

    @DisplayName("????????? ??????")
    @Test
    void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // skip ??????
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @DisplayName("????????? ?????? (+ ?????? count)")
    @Test
    void paging2() {
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // skip ??????
                .limit(2)
                .fetchResults();

        assertThat(results.getTotal()).isEqualTo(4);
        assertThat(results.getLimit()).isEqualTo(2);
        assertThat(results.getOffset()).isEqualTo(1);
        assertThat(results.getResults().size()).isEqualTo(2);
    }

    @DisplayName("??????")
    @Test
    void aggregation() {
        List<Tuple> result = queryFactory.select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                ).from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    @DisplayName("??? ????????? ??? ?????? ?????? ??????")
    @Test
    void group() {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("TeamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("TeamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    @DisplayName("??? A??? ?????? ?????? ??????")
    @Test
    void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("TeamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("Member1", "Member2");
    }

    @DisplayName("?????? ?????? (????????? ????????? ??? ????????? ?????? ?????? ??????)")
    @Test
    void theta_join() {
        em.persist(new Member("TeamA"));
        em.persist(new Member("TeamB"));

        List<Member> result = queryFactory
                .selectFrom(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("TeamA", "TeamB");
    }

    /**
     * ???: ????????? ?????? ??????????????? ??? ?????? TeamA??? ?????? ?????? ????????? ?????? ??????
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @DisplayName("?????? on???")
    @Test
    void join_on_filtering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                // .leftJoin(member.team, team).on(team.name.eq("TeamA"))
                .join(member.team, team)
                .where(team.name.eq("TeamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple: " + tuple);
        }
    }

    /**
     * ???????????? ?????? ????????? ?????? ??????
     * ???: ????????? ????????? ??? ????????? ?????? ?????? ?????? ??????
     */
    @Test
    void join_on_no_relation() {
        em.persist(new Member("TeamA"));
        em.persist(new Member("TeamB"));
        em.persist(new Member("TeamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @DisplayName("?????? ?????? ?????????")
    @Test
    void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("Member1"))
                .fetchOne();

        // ????????? ???????????? ?????? ???????????? ??????
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).as("?????? ?????? ?????????").isFalse();
    }

    @DisplayName("?????? ?????? ??????")
    @Test
    void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("Member1"))
                .fetchOne();

        // ????????? ???????????? ?????? ???????????? ??????
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).as("?????? ?????? ??????").isTrue();
    }

    @DisplayName("????????? ?????? ?????? ?????? ??????")
    @Test
    void subQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    @DisplayName("????????? ?????? ????????? ??????")
    @Test
    void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    @DisplayName("???????????? In ??????")
    @Test
    void subQueryIn() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @DisplayName("select ???????????? ??????")
    @Test
    void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("??????")
                        .when(20).then("?????????")
                        .otherwise("??????"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20???")
                        .when(member.age.between(21, 30)).then("21???~30???")
                        .otherwise("??????"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @DisplayName("??????")
    @Test
    void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @DisplayName("?????? ?????????")
    @Test
    void concat() {
        // {username}_{age}
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("Member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }


    /**
     * ?????? 3 ~
     */
    @DisplayName("?????? ???????????? ??????")
    @Test
    void simpleProjection() {
        List<String> results = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String result : results) {
            System.out.println("result: " + result);
        }
    }

    @DisplayName("?????? ???????????? ??????")
    @Test
    void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String userName = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username: " + userName);
            System.out.println("age: " + age);
        }
    }

    @DisplayName("JPQL ????????? DTO ??????")
    @Test
    void findDtoByJPQL() {
        List<MemberDto> result = em.createQuery("select new me.study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto: result) {
            System.out.println("MemberDto: " + memberDto);
        }
    }

    @DisplayName("QueryDsl ????????? DTO ?????? - ???????????? ??????(setter)")
    @Test
    void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("MemberDto: " + memberDto);
        }
    }

    @DisplayName("QueryDsl ????????? DTO ?????? - ?????? ??????")
    @Test
    void findDtoByFields() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("MemberDto: " + memberDto);
        }
    }

    @DisplayName("QueryDsl ????????? UserDto ?????? - ?????? ??????")
    @Test
    void findUserDto() {
        QMember subMember = new QMember("subMember");
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(JPAExpressions.select(subMember.age.max())
                                .from(subMember), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("UserDto: " + userDto);
        }
    }

    @DisplayName("QueryDsl ????????? DTO ?????? - ????????? ??????")
    @Test
    void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age,
                        member.id))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("MemberDto: " + memberDto);
        }
    }

    @DisplayName("@QueryProjection ??????")
    @Test
    void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("MemberDto: " + memberDto);
        }
    }

    @DisplayName("???????????? - BooleanBuilder ??????")
    @Test
    void dynamicQuery_booleanBuilder() {
        String userNameParam = "Member1";
        int ageParam = 10;

        List<Member> result = searchMemberBooleanBuilder(userNameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMemberBooleanBuilder(String userNameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (userNameCond != null) {
            builder.and(member.username.eq(userNameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @DisplayName("???????????? - Where ?????? ????????????")
    @Test
    void dynamicQuery_whereParams() {
        String userNameParam = "Member1";
        int ageParam = 10;

        List<Member> result = searchMemberWhereParams(userNameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMemberWhereParams(String userNameCond, int ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(allEq(userNameCond, ageCond))
                .fetch();
    }

    private BooleanExpression userNameEq(String userNameCond) {
        if (userNameCond != null) {
            return member.username.eq(userNameCond);
        }
        return null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        if (ageCond != null) {
            return member.age.eq(ageCond);
        }
        return null;
    }

    private BooleanExpression allEq(String userNameCond, Integer ageCond) {
        return userNameEq(userNameCond).and(ageEq(ageCond));
    }

    @DisplayName("?????? ????????????")
    @Test
    void bulkUpdate() {

        // ????????? ???????????? ???????????? ?????? DB ??????
        long count = queryFactory
                .update(member)
                .set(member.username, "?????????")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();

        // ????????? ????????????         DB
        // (1) Member1 = 10 -> (1) ????????? = 10
        // (2) Member2 = 20 -> (2) ????????? = 20
        // (3) Member3 = 30 -> (3) Member3 = 30
        // (4) Member4 = 40 -> (4) Member4 = 40

        // ????????? ???????????? ?????? (DB?????? ????????? ?????? -> ????????? ??????)
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member : result) {
            System.out.println("member: " + member);
        }
    }

    @DisplayName("?????? ??????, ??????, ??????")
    @Test
    void bulkAdd() {
        long addCount = queryFactory.update(member)
                .set(member.age, member.age.add(1))
                .execute();

        long minusCount = queryFactory.update(member)
                .set(member.age, member.age.add(-1))
                .execute();

        long multiplyCount = queryFactory.update(member)
                .set(member.age, member.age.multiply(2))
                .execute();
    }

    @DisplayName("?????? ??????")
    @Test
    void bulkDelete() {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @DisplayName("SQL function ????????????")
    @Test
    void sqlFunction() {
        List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "Member", "M"
                )).from(member)
                .fetch();

        for (String name : result) {
            System.out.println("result= " + name);
        }
    }

    @DisplayName("QueryDsl ?????? SQL function ??????")
    @Test
    void sqlFunctionQueryDsl() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                /*.where(member.username.eq(
                        Expressions.stringTemplate(
                                "function('lower', {0})",
                                member.username))*/
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String name : result) {
            System.out.println("result= " + name);
        }
    }
}
