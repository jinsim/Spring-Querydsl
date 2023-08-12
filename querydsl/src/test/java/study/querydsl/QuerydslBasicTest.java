package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;
import study.querydsl.entity.QMember;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static com.querydsl.jpa.JPAExpressions.select;
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

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and
     t.name='teamA'
     */
    @Test
    public void join_on_filtering() throws Exception {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     *2. 연관관계 없는 엔티티 외부 조인
     *예)회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name */
    @Test
    public void join_on_no_relation() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("t=" + tuple);
        }
    }


    @PersistenceUnit
    // getPersistenceUtiUtil().isLoaded() 를 쓸 수 있다. 초기화 되었는지 확인
    EntityManagerFactory emf;
    @Test
    public void fetchJoinNo() throws Exception {
        em.flush();
        em.clear();
        // 페치 조인을 할 때는, 영속성 컨텍스트를 바로바로 안 지우면 결과를 제대로 보기 어렵다.
        // 영속성 컨텍스트에 있는 것을 DB에 반영하고, 영속성 컨텍스트를 비운 다음에 시작한다.

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        // Member에서 team의 연관관게를 LAZY로 세팅했다. 따라서 Member만 조회된다.

        boolean loaded =
                emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        // emf : 엔티티매니저를 만드는 공장
        // isLoaded를 하면 이미 로딩된 엔티티인지, 초기화가 안된 엔티티인지 알려준다.

        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoin() throws Exception {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                // member를 조회할 때 연관된 team을 한 쿼리로 한번에 긁어온다.
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded =
                emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        // true일 것이다.

        assertThat(loaded).as("페치 조인").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                )) .fetch();
        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 나이 이상인 회원
     */
    @Test
    public void subQueryGoe() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30,40);
    }

    /**
     * 서브쿼리 여러 건 처리, in 사용
     */
    @Test
    public void subQueryIn() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                )) .fetch();
        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    /**
     * select 절에 subquery
     */
    @Test
    public void selectSubQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        // 서브 쿼리: 유저 평균 나이 조회, JPAExpressions static import
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }


    @Test
    void basicCase() throws Exception {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    @Test
    void complexCase() throws Exception {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    @Test
    public void orderByCase() throws Exception {
        // 1. 0 ~ 30살 아닌 회원 가장 먼저 출력
        // 2. 0 ~ 20살 회원 출력
        // 3. 21 ~ 30살 회원 출력

        // 위의 설명에 따라서 우선순위 부여(우선순위 높을 수록 큰 숫자 부여)
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath) // 나이에 따라서 rankPath는 1, 2, 3이 조회됨
                .from(member)
                .orderBy(rankPath.desc()) // rankPath로 정렬
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);
            System.out.println("username = " + username + " age = " + age + " rank = "
                    + rank);
        }
    }

    /**
     * 상수 - Expressions.constant("상수")
     */
    @Test
    public void constant() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A")) // 상수
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 문자 더하기
     */
    @Test
    public void concat() throws Exception {

        // username_age로 문자열 합쳐서 select하고 싶을 때
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                // 문자 아닌 타입들은 stringValue()로 문자로 변환 가능(ENUM 처리시에도 자주 사용!)
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();


        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 프로젝션 - 프로젝션 대상이 하나
     */
    @Test
    public void simpleProjection() throws Exception {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        // member 객체 조회 -> 이것도 프로젝션 대상이 하나(반환 타입 명확하게 지정 Member라고 가능)
        List<Member> result2 = queryFactory
                .select(member)
                .from(member)
                .fetch();
    }

    /**
     * 프로젝션 - 프로젝션 대상이 둘 이상: 튜플 조회
     */
    @Test
    public void tupleProjection() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                // username, age 2개 프로젝션 -> 반환 타입 지정 못하니까 Tuple
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            // 튜플에서 값 꺼내기 : Tuple.get(Q타입.속성)
            Integer age = tuple.get(member.age);

            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }


    /**
     * 프로젝션 결과 반환 - DTO 조회
     * JPQL 버전
     */
    @Test
    public void findDtoByJPQL() throws Exception {
        List<MemberDto> result = em.createQuery(
                "select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m",
                        MemberDto.class)
                // select절에 MemberDto의 생성자 호출(단, 패키지 명을 풀로 작성)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 방법 1) 프로퍼티 접근 - Setter
    // * 기본 생성자, setter 필요
    @Test
    public void findDtoBySetter() throws Exception {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class, // bean
                        member.username, // 순서 상관 X
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 방법 2) 필드 직접 접근
    // * getter, setter 필요 X, 필드에 바로 값을 넣음.
    // * 기본 생성자는 필요
    @Test
    public void findDtoByField() throws Exception {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class, // fields
                        member.age, // 순서 상관 X
                        member.username))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 방법 3) 생성자 사용
    // * 기본 생성자 필요 X, 생성자 파라미터 순서 지켜야함.
    @Test
    public void findDtoByConstructor() throws Exception {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class, // constructor
                        member.username,// 대신 이 순서가 DTO의 생성자와 순서가 맞아야함.
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * setter 프로퍼티접근, 필드 직접 접근 : DTO와 Entity의 필드명이 다를 경우 별칭 부여 필요
     *
     * + 서브 쿼리 별칭 부여 필요
     */
    @Test
    public void findUserDto() throws Exception {
        QMember memberSub = new QMember("memberSub"); // 서브쿼리용

        // 내용 1) Entity와 Dto의 필드명이 다를 경우 : as로 해결!
        // as : 필드에 별칭 적용
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        //member.username, // 문제 : 프로퍼티, 필드 접근 방식에서 이름이 다를 경우 제대로 select 안됨 -> sout name = null로 나옴
                        member.username.as("name"),// 해결 : as.("DTO의 필드명") 또는 ExpressionUtils(member.username, "name")
                        member.age))
                .from(member)
                .fetch();

        // 내용 2) ExpressionUtils.as(source, alias) : 필드, 서브쿼리에 별칭 적용
        List<UserDto> result2 = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        // 서브쿼리 작성시, 어떤 필드에 값을 넣어줘야하는지 필드명 별칭부여 필요
                        ExpressionUtils.as(select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }

        for (UserDto userDto : result2) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * 생성자 사용의 경우: DTO와 Entity의 필드명이 달라도, 이름이 아닌 타입을 보기 때문에 상관없다.
     */
    @Test
    public void findUserDtoByConstructor() throws Exception {
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class, // constructor, UserDto
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * 프로젝션과 결과 반환 - @QueryProjection
     */
    @Test
    public void findDtoByQueryProjection() throws Exception {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age)) // 컴파일러로 타입 체크 가능
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
        // 단점 : DTO에 QueryDsl 어노테이션 유지 + DTO까지 Q파일 생성 필요
    }

    /**
     * 동적 쿼리 - BooleanBuilder 사용
     */
    @Test
    public void 동적쿼리_BooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    /**
     * 파라미터의 null 여부에 따라서 쿼리가 동적으로 바뀌어야함.
     */
    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        // BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond));
        // 초기값 설정도 가능. username은 필수라서 null이 들어오지 않는 경우.

        if(usernameCond != null){ // usernameCond가 null이 아니면
            builder.and(member.username.eq(usernameCond)); // builder에 and 조건 추가
        }

        if(ageCond != null){ // ageCond가 null이 아니면
            builder.and(member.age.eq(ageCond)); // builder에 and 조건 추가
        }

        return queryFactory
                .selectFrom(member)
                .where(builder) // where에 builder
                .fetch();
    }

    /**
     * 동적 쿼리 - Where 다중 파라미터 사용
     */
    @Test
    public void 동적쿼리_WhereParam() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))// where: 파라미터에 null이 들어오면 무시
                // .where(allEq(usernameCond, ageCond)) // 두개 다 조립해서 결과 받는 함수 사용도 가능
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null; // if-else 간단해서 삼항연산자 사용
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;

    }

    // 조립 가능, 재사용도 가능
    private BooleanExpression allEq(String usernameCond, Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    /**
     * 수정, 삭제 벌크 연산
     * 예) 모든 사람 연봉 연상: 엔티티 하나 하나 update 쿼리 날리기(by 더티체킹)보다는 한번의 update 쿼리로 처리하는 것이 효율적
     */
    @Test
    public void bulkUpdate() throws Exception {
        // member1 = 10살 -> 비회원
        // member2 = 20살 -> 비회원

        // 회원 중에서 나이가 28살보다 작은 회원의 이름을 "비회원"으로 변경
        long count = queryFactory // count = update의 영향을 받은 row의 수
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        // 벌크 연산 이후, 영속성 컨텍스트 초기화를 해야한다.
        // 벌크 연산은 DB에 바로 쿼리를 날리기 때문에 괴리가 생긴다.
        em.flush();
        em.clear();

        // 조회
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    /**
     * 벌크 연산 : add, multiply
     */
    @Test
    public void bulkAdd() throws Exception {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                // minus 없음. add(-1)사용. 곱하기: multiply(x)
                .execute();

    }

    /**
     * 쿼리 한번으로 대량 데이터 삭제
     */
    @Test
    public void bulkDelete() throws Exception {
        long count = queryFactory
                .delete(member) // 삭제
                .where(member.age.gt(18)) // 18살 이상
                .execute();

    }

    /**
     * member M으로 변경하는 replace 함수 사용
     */
    @Test
    public void sqlFunction1() {
        // SQL function 호출하기
        List<String> result1 = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();
    }

    /**
     * 소문자로 변경해서 비교해라.
     */
    @Test
    public void sqlFunction2() {
        List<String> result2 = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(
                        Expressions.stringTemplate(
                                "function('lower', {0})",
                                member.username)))
                .fetch();
    }

    /**
     * lower 같은 ansi 표준 함수들은 querydsl이 상당부분 내장하고 있다. 따라서 다음과 같이 처리해도 결과 는 같다.
     */
    @Test
    public void sqlFunction3() {
        List<String> result2 = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(member.username.lower()))
                .fetch();
    }
}
