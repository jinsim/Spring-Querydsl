package study.querydsl.repository;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;

import java.util.List;

public interface MemberRepositoryCustom {

    // 직접 구현한 기능으로 사용하고 싶다.
    List<MemberTeamDto> search(MemberSearchCondition condition);
}