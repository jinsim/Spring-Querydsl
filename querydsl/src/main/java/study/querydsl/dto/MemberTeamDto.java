package study.querydsl.dto;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class MemberTeamDto {
    private Long memberId;
    private String username;
    private int age;
    private Long teamId;
    private String teamName;

    @QueryProjection // 생성자에 @QueryProjection
    // 생성자에 @QueryProjection 을 사용하면 QMemberTeamDto 를 생성하지 않아도 된다.
    // 단점 : DTO가 순수해지지 않고, Querydsl에 의존해야 된다.
    // 이게 싫으면 프로젝션 빈이나, 프로젝션 필드 직접 설정, 생성자 방식을 사용하면 된다.
    public MemberTeamDto(Long memberId, String username, int age, Long teamId, String teamName) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}