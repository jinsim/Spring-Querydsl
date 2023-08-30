package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 나머지 save, findById, findAll은 JpaRepository가 제공한다.
    // select m from Member m where m.username = ?
    List<Member> findByUsername(String username);
}
