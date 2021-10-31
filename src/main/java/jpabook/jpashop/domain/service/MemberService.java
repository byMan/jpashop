package jpabook.jpashop.domain.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = false)
@RequiredArgsConstructor  //final 키워드를 가진 멤버변수에 대해 생성자 주입 코드를 자동으로 만들어 준다
public class MemberService {

    private final MemberRepository memberRepository;

    /**
        회원 가입 
     */
    @Transactional
    public Long join(Member member) {
        //중복회원검증
        validateDuplicateMember(member);
        memberRepository.save(member);
        return member.getId();
    }

    private void validateDuplicateMember(Member member) {
        //동시에 동일 이름으로 회원가입하게 되면 동시성 문제로 인해 가입 처리가 안될 수 있다.
        //실제 실무에서는 이름만 체크하지 말고 유니크한 정보를 만들어서 체크하도록 하자.
        List<Member> findMembers = memberRepository.findByName(member.getName());
        if (!findMembers.isEmpty()) {
            throw new IllegalStateException("이미 존재하는 회원입니다.");
        }
    }

    /**
     * 회원 전체 조회
     */
    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

    public Member findOne(Long memberId) {
        return memberRepository.findOne(memberId);
    }
}
