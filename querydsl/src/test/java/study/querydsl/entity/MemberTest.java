package study.querydsl.entity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Transactional
@SpringBootTest
class MemberTest {

    @Autowired
    EntityManager em;

    @Test
    void testEntity() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member memberA = new Member("memberA", 10, teamA);
        Member memberB = new Member("memberB", 20, teamA);
        Member memberC = new Member("memberB", 30, teamB);
        Member memberD = new Member("memberB", 40, teamB);
        em.persist(memberA);
        em.persist(memberB);
        em.persist(memberC);
        em.persist(memberD);

        // 초기화
        em.flush(); // 영속성 컨텍스트에 있는 객체를, 실제 쿼리를 만들어 DB 에 날림
        em.clear(); // 영속성 컨텍스트를 완전히 초기화. 캐시까지 다 날라간다.
        // 이 다음에 쿼리를 날리면 깔끔하게 나간다.

        // 확인
        List<Member> members = em.createQuery("select m from Member m", Member.class)
                .getResultList();

        for (Member member : members) {
            // 원래는 Assert로 해야하지만, 지금은 공부하는 것이니 콘솔에 찍는다.
            System.out.println("member = " + member);
            System.out.println("-> member.team" + member.getTeam());
        }
    }
}
