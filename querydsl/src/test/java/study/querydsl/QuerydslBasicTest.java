package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;
import study.querydsl.entity.QMember;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;


    // 각 테스트 실행 전에 데이터를 세팅하고 갈 것.
    @BeforeEach
    public void before() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    // JPQL부터 작성하고, 후에 Querydsl로 변경하며 비교한다.
    @Test
    public void startJPQL() {
        // 1. JPQL 로 member1 찾기
        String qlString = "select m from Member m where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    // 2. Querydsl 사용하기
    @Test
    public void startQuerydsl() {
        queryFactory = new JPAQueryFactory(em);
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }
// EntityManager 로 JPAQueryFactory 생성
// Querydsl은 JPQL 빌더
// JPQL: 문자(실행 시점 오류), Querydsl: 코드(컴파일 시점 오류)
// JPQL: 파라미터 바인딩 직접, Querydsl: 파라미터 바인딩 자동 처리

    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10))) // or 등 여러가지 가능
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
        // 멤버 목록을 리스트로 조회
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        // 멤버 단건 조회
        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();

        // 처음 한 건 조회. limit(1).fetchOne()과 동일
        Member oneMember =
                queryFactory.selectFrom(QMember.member)
                        .fetchFirst();

        // 페이징이 나감 -> 카운트 쿼리 + 데이터 쿼리 같이나감
        QueryResults<Member> results = queryFactory
                .selectFrom(QMember.member)
                .fetchResults();

        results.getTotal(); // 페이징을 하기 위해서 total count를 가져와야 한다.
        // total이 있어야 1,2,3,,, 몇번째 페이지까지 있는지 보여줄 수 있다.
        List<Member> content = results.getResults();
        // results를 가져올 때는 member 데이터를 다 가져오지만, total을 가져올 때는 count 쿼리만 나간다.

        long total = queryFactory.selectFrom(member)
                .fetchCount();
        // select절에서 count 쿼리만 나간다.
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순 (Desc)
     * 2. 회원 이름 오름차순 (asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력 (Nulls last)
     */
    @Test
    public void sort() {

        em.persist(new Member(null,100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    // 조회 건수 제한
    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //앞에 몇개 스킵할거야? -> 1은 하나 스킵할거야. (0부터 시작)
                .limit(2) //최대 2건 조회
                .fetch();


        assertThat(result.size()).isEqualTo(2);
    }

    // 전체 조회 수가 필요하면?
    @Test
    public void paging2() {
        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //앞에 몇개 스킵할거야? -> 1은 하나 스킵할거야. (0부터 시작)
                .limit(2)
                .fetchResults();

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getResults().size()).isEqualTo(2);
    }

    /**
     * JPQL
     * select
     *   COUNT(m), //회원수
     *   SUM(m.age), //나이 합
     *   AVG(m.age), //평균 나이
     *   MAX(m.age), //최대 나이
     *   MIN(m.age) //최소 나이
     * from Member m
     */

    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    @Test
    public void havingTest() throws Exception {

        em.persist(new Member("test",40));
        em.persist(new Member("test",50));


        List<Tuple> fetch = queryFactory
                .select(member.age,member.count())
                .from(member)
                .groupBy(member.age)
                .having(member.age.gt(30))
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println(tuple);
        }

        Tuple targetAge = fetch.get(0);
        assertThat(targetAge.get(member.age)).isEqualTo(40);
    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() throws Exception {

        // QMember member = QMember.member;
        // QTeam team = QTeam.team;

        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                // .rightJoin(member.team, team)
                // .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch(); // inner join

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join() throws Exception {

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member,team) // 세타 조인은 from 절에 여러 엔티티를 선택한다.
                // 모든 회원과 모든 팀을 조인한다.
                // 그 다음 조인한 테이블을 Where 절에서 필터링 한다.
                // DB마다 성능 최적화를 하긴 한다.
                .where(member.username.eq(team.name)) // 회원의 이름과 팀 이름과 같은 경우
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA","teamB");
    }
}
