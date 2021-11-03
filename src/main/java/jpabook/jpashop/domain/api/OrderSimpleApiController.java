package jpabook.jpashop.domain.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.domain.repository.OrderRepository;
import jpabook.jpashop.domain.repository.OrderSearch;
import jpabook.jpashop.domain.repository.order.simplequery.OrderSimpleQueryDto;
import jpabook.jpashop.domain.repository.order.simplequery.OrderSimpleQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * xToOne(ManyToOne, OneToOne)
 * Order
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {
    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;

    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        for (Order order : orders) {
            order.getMember().getName();        //Lazy 강제 초기화
            order.getDelivery().getAddress();   //Lazy 강제 초기화
        }
        return orders;
    }


    /**
     * 엔티티를 DTO로 변환하는 일반적인 방법이다.
     * 쿼리가 총 1 + N + N번 실행된다. (v1과 쿼리수 결과는 같다.)
     *   order 조회 1번 (order 조회 결과 수가 N이 된다.)
     *   order -> member 지연 로딩 조회 N번
     *   order -> delivery 지연 로딩 조회 N번
     *   예> order의 결과가 4개면 최악의 경우 1+4+4 번 실행된다.(최악의 경우)
     *      지연로딩은 영속성 컨텍스트에서 조회하므로, 이미 조회된 경우 쿼리를 생략한다.
     * @return
     */
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        //Order 2개 조회
        //1+N+N 문제에 직면 -> Order 1회 조회결과(2(N)개) + 회원(2(N)번) + 배송(2(N)번) , 이리하여 Order 결과 2개에 대해 회원과 배송이 각각 2번씩 더 실행된다.
        //결국, Order 조회 결과 쿼리가 총 5개의 쿼리가 수행된다. 이는 결과 row수가 증가하는 만큼 그 수가 더 늘어난다.
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());

        return orders.stream()
                .map(SimpleOrderDto::new)
                .collect(Collectors.toList());
    }


    /**
     * v2와 같이 1+N+N 의 문제를 해결하기 위해 findAllWithMemberDelivery 메소드를 통해 join fetch 를 활용하여
     * 한번에 모두 조인해서 데이터를 모두 조회하여 리턴하도록 개선함으로 성능 향상을 도모한 버전이다.
     * @return
     */
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        return orders.stream()
                .map(SimpleOrderDto::new)
                .collect(Collectors.toList());
    }
/*
    --/api/v3/simple-orders 호출 시 생성 쿼리
    select
        order0_.order_id as order_id1_6_0_,
        member1_.member_id as member_i1_4_1_,
        delivery2_.delivery_id as delivery1_2_2_,
        order0_.city as city2_6_0_,
        order0_.street as street3_6_0_,
        order0_.zip_code as zip_code4_6_0_,
        order0_.delivery_id as delivery8_6_0_,
        order0_.member_id as member_i9_6_0_,
        order0_.name as name5_6_0_,
        order0_.order_date as order_da6_6_0_,
        order0_.status as status7_6_0_,
        member1_.city as city2_4_1_,
        member1_.street as street3_4_1_,
        member1_.zip_code as zip_code4_4_1_,
        member1_.name as name5_4_1_,
        delivery2_.city as city2_2_2_,
        delivery2_.street as street3_2_2_,
        delivery2_.zip_code as zip_code4_2_2_,
        delivery2_.status as status5_2_2_
    from
        orders order0_
    left outer join
        member member1_
            on order0_.member_id=member1_.member_id
    left outer join
        delivery delivery2_
            on order0_.delivery_id=delivery2_.delivery_id
 */





    /**
     * /api/v3/simple-orders 버전을 좀 더 업그레이드 한 버전
     *
     * 일반적인 SQL을 사용할 때 처럼 원하는 값을 선택해서 조회
     * new 명령어를 사용해서 JPQL의 결과를 DTO로 즉시 변환
     * SELECT 절에서 원하는 데이터를 직접 선택하므로 DB -> 애플리케이션 네트웍 용량 최적화(생각보다 성능 최적화는 미비함)
     * 리포지토리 재사용성 떨어짐, API 스펙에 맞춘 코드가 리포지토리에 들어가는 단점
     *
     * [정리]
     * 엔티티를 DTO로 변환하거나, DTO로 바로 조회하는 두 가지 방법은 각각 장단점이 있다.
     * 둘 중 상황에 다라서 더 나은 방법을 선택하면 된다.
     * 엔티티로 조회하면 리포지토리 재사용성도 좋고, 개발도 단순해진다.
     * 따라서 권장하는 방법은 다음과 같다.
     *   ## 쿼리 방식 선택 권장 순서 ##
     *   1. 우선 엔티티를 DTO로 변화하는 방법을 선택한다.
     *   2. 필요하면 fetch 조인으로 성능을 최적화 한다. -> 대부분의 성능 이슈가 해결된다.
     *   3. 그래도 안되면 DTO로 직접 조회하는 방법을 사용한다.
     *   4. 최후의 방법은 JPA가 제공하는 네이티브 SQL이나 스프링 JDBC Template을 사용해서 SQL을 직접 사용한다.
     *
     * @return
     */
    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> ordersV4() {
        return orderSimpleQueryRepository.findOrderDtos();
    }
/*
    --/api/v4/simple-orders 생성된 쿼리 v3 버전에 비해 실제 필요한 정보만 select 하도록 변경됨
    select
        order0_.order_id as col_0_0_,
        member1_.name as col_1_0_,
        order0_.order_date as col_2_0_,
        order0_.status as col_3_0_,
        delivery2_.city as col_4_0_,
        delivery2_.street as col_4_1_,
        delivery2_.zip_code as col_4_2_
    from
        orders order0_
    inner join
        member member1_
            on order0_.member_id=member1_.member_id
    inner join
        delivery delivery2_
            on order0_.delivery_id=delivery2_.delivery_id


 */




    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
        }
    }

    private class OrderSimpleQueryDtoDto {
    }
}
