package jpabook.jpashop.domain.api;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.service.MemberService;
import lombok.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class MemberApiController {
    private final MemberService memberService;

    /**
     * 응답 값으로 엔티티를 직접 외부에 노출한 경우
     * [문제점]
     *   1) 인테티에 프레젠테이션 계층을 위한 로직이 추가된다.
     *   2) 기본적으로 엔팉티의 모든 값이 노출된다.
     *   3) 응답 스펙을 맞추기 위해 로직이 추가도니다.(@JsonIgnore, 별도의 뷰 로직 등등)
     *   4) 실무에서는 같은 엔티티에 대해 API가 용도에 따라 다양하게 만들어지는데, 한 엔티티에 각각의 API를 위한 프레젠테이션 응답 로직을 담기는 어렵다.
     *   5) 엔티티가 변경되면 API 스펙이 변경됨으로 많은 장애를 유발할 수 있다.
     *   6) 추가로 컬렉션을 직접 반환하면 API 스펙을 변경하기 어렵다.(별도의 Result 클래스 생성으로 해결)
     * [결론]
     *   API 응답 스펙에 맞춰 별도의 DTO를 반환한다.
     *   절대로 엔티티를 외부에 노출하지 말자!!!!
     * @return
     */
    @GetMapping("/api/v1/members")
    public List<Member> membersV1() {
        return memberService.findMembers();
    }


    @GetMapping("/api/v2/members")
    public Result memberV2() {
        List<Member> findMembers = memberService.findMembers();
        List<MemberDto> collect = findMembers.stream()
                .map(m -> new MemberDto(m.getName()))
                .collect(Collectors.toList());
        return new Result(collect.size(), collect);
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private int count;
        private T data;
    }

    @Data
    @AllArgsConstructor
    static class MemberDto {
        private String name;
    }



    /**
     * 이 API의 경우 기존 생성한 엔티티 클래스인 Member 클래스를 재활용하여 API 통해 전달받은 JSON 데이터를 Member 인스턴스로 직접 받을 수 있는 장점이 있다.
     * 그러나 치명적인 문제점은 Validation 체크를 하고자 할 경우 엔티티 클래스의 멤버변수를 NotEmpty나 NotNull 설정을 해야하는 경우
     * 실제 테이블에는 해당 제약 조건이 없는데 입력 데이터 때문에 제약이 걸리면 문제가 발생될 소지가 높다.
     * @param member
     * @return
     */
    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }


    /**
     * 등록용 API
     * 위의 API의 문제를 보완하기 위해 엔티티 클래스인 Member 클래스 대신 CreateMemberRequest 클래스를 생성하여 JSON 데이터를 담아오고
     * 입력 데이터에 대한 제약 조건 처리를 엔티티 클래스에 영향을 주지 않고 할 수 있도록 구성해준다.
     * 이렇게 하면 기존 엔티티 클래스에는 변경점이 없고 API 스펙에 따라 생성된 CreateMemberRequest 클래스에만 제약 조건을 설정하므로
     * 위에서 발생하는 문제점이 사라진다.
     * @param request
     * @return
     */
    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {
        Member member = new Member();
        member.setName(request.getName());
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }


    /**
     * 수정용 API
     * @param id
     * @param request
     * @return
     */
    @PutMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(@PathVariable("id") Long id, @RequestBody @Valid UpdateMemberRequest request) {
        memberService.update(id, request.getName());
        Member findMember = memberService.findOne(id);
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }

    @Data
    static class UpdateMemberRequest {
        @NotEmpty
        private String name;
    }

    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse {
        private Long id;
        private String name;
    }


    @Data
    static class CreateMemberRequest {
        @NotEmpty
        private String name;
    }


    @Data
    static class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }

}
