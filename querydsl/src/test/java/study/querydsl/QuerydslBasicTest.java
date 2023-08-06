package study.querydsl;

import com.querydsl.core.QueryResults;
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
}
