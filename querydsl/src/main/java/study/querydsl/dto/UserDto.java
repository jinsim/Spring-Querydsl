package study.querydsl.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class UserDto { // MemberDto와 같은데, username 대신 name으로 필드명만 다름. -> Member Entity와도 다름.
    private String name;
    private int age;

    public UserDto(String name, int age) {
        this.name = name;
        this.age = age;
    }
}

