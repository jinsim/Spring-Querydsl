package study.querydsl;

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

import static org.assertj.core.api.Assertions.assertThat;

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
        QMember m = new QMember("m"); // 구분하는 별칭이다. 크게 중요하진 않다. 잘 안쓴다.
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1")) // 파라미터 바인딩 안해도, 자동으로 해준다.
                // 문자열 더하기로 직접 넣으면 SQL Injection 공격에 취약하다.
                // jdbc prepare statement 의 파라미터 바인딩 방식으로 넣어준다.
                // DB 입장에서 성능 측면에서도 도움이 된다.
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }
// EntityManager 로 JPAQueryFactory 생성
// Querydsl은 JPQL 빌더
// JPQL: 문자(실행 시점 오류), Querydsl: 코드(컴파일 시점 오류)
// JPQL: 파라미터 바인딩 직접, Querydsl: 파라미터 바인딩 자동 처리
}
