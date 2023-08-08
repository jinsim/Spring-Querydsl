package study.querydsl.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

// querydsl DTO조회시 프로퍼티 접근 - setter 방법 때 기본 생성자 필요
@NoArgsConstructor
// @Getter, @Setter, @RequiredArgsConstructor, @ToString 등이 모두 포함
@Data
public class MemberDto {

    private String username;
    private int age;

    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
