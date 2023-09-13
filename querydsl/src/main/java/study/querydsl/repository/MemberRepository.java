package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import study.querydsl.entity.Member;

import java.util.List;

// 인터페이스는 다중 상속이 가능하다.
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom, QuerydslPredicateExecutor<Member> {

    // 나머지 save, findById, findAll은 JpaRepository가 제공한다.
    // select m from Member m where m.username = ?
    List<Member> findByUsername(String username);
}
