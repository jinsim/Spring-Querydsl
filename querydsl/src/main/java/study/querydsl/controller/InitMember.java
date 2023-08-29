package study.querydsl.controller;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

    private final InitMemberService initMemberService;

    @PostConstruct
    // 의존성 주입이 이루어진 후 초기화를 수행하는 메서드이다.
    // 생성자(일반)가 호출 되었을 때, 빈(bean)은 아직 초기화 되지 않은 상태이다. (예를 들어, 주입된 의존성이 없음)
    // 하지만, @PostConstruct를 사용하면, 빈(bean)이 초기화 됨과 동시에 의존성을 확인할 수 있다.
    // 즉, @PostConstruct를 사용하면, 생성자가 호출된 후, 의존성 주입이 이루어진 후 초기화를 수행하는 메서드를 지정할 수 있다.
    // bean lifecycle에서 오직 한 번만 수행된다는 것을 보장할 수 있다.
    // WAS가 올라가면서 bean이 생성될 때 딱 한 번 초기화한다.
    public void init() {
        initMemberService.init();
    }

    // 시작할 때 db에 데이터를 다 넣는다.
    @Component
    static class InitMemberService {
        @PersistenceContext
        private EntityManager em;

        @Transactional
        public void init() {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            em.persist(teamA);
            em.persist(teamB);

            for (int i = 0; i < 100; i++) {
                Team selectedTeam = i % 2 == 0 ? teamA : teamB;
                em.persist(new Member("member" + i, i, selectedTeam));
            }

        }
    }
}
