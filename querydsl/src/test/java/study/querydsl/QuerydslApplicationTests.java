package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Hello;
import study.querydsl.entity.QHello;

@SpringBootTest
@Transactional
@Commit // 테스트에 Transactional 이 있으면 롤백이 되기 때문에, 이를 방지하기 위해 사용
class QuerydslApplicationTests {
	@Autowired
	EntityManager em;

	@Test
	void contextLoads() {
		Hello hello = new Hello();
		em.persist(hello);

		// 최근 버전에서는 JPAQueryFactory 를 권장한다.
		JPAQueryFactory query = new JPAQueryFactory(em);
		// QHello qHello = new QHello(("h")); // 아래의 코드와 동일
		QHello qHello = QHello.hello; // Querydsl Q타입 동작 확인

		Hello result = query
				.selectFrom(qHello) // 쿼리와 관련된 거는 다 Q타입을 넣어야 한다.
				.fetchOne();

		Assertions.assertThat(result).isEqualTo(hello);
		// lombok 동작 확인 (hello.getId())
		Assertions.assertThat(result.getId()).isEqualTo(hello.getId());
	}

}
