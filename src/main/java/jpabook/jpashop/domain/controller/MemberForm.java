package jpabook.jpashop.domain.controller;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

@Getter @Setter
public class MemberForm {
    @NotEmpty(message = "회원 이름은 필수입니다.")
    private String name;

    private String city;
    private String street;
    private String zipCode;
}

/*

javax.validation.constraints

@NotNull : 해당 값에 Null을 허용하지 않음
@NotBlank : Null을 허용하지 않으며 문자가 한 개 이상 포함되어야 함 (공백 제외)
@NotEmpty : Null을 허용하지 않으며 공백 문자열을 허용하지 않음
@AssertTrue : true인지 확인
@Min : 값이 Min보다 작은지 확인
@Max : 값이 Max보다 큰지 확인
@Size : 값이 min과 max사이에 해당하는지 확인 (CharSequence, Collection, Map, Array에 해당)

 */