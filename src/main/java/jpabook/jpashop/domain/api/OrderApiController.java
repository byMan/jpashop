package jpabook.jpashop.domain.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.domain.repository.OrderRepository;
import jpabook.jpashop.domain.repository.OrderSearch;
import jpabook.jpashop.domain.repository.order.query.OrderFlatDto;
import jpabook.jpashop.domain.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop.domain.repository.order.query.OrderQueryDto;
import jpabook.jpashop.domain.repository.order.query.OrderQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@RestController
@RequiredArgsConstructor
public class OrderApiController {
    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    /**
     * 이 버전은 API 스펙에 엔티티 정보를 그대로 노출하므로 사용하지 않는다.
     *
     * @return
     */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getAddress();
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(orderItem -> orderItem.getItem().getName());
        }
        return all;
    }


    /**
     * API 스펙에 엔티티의 속성 정보 노출을 모두 차단하고 DTO로 대체하여 스펙에 보여지도록 구성했다.
     * 그러나,,,,
     * 이대로 실행하면 쿼리가 지연로딩의 동작으로 인해 N+N+1 번의 쿼리가 실행된다.
     * 이 문제를 해결 해야 한다.
     * @return
     */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<OrderDto> collect = orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());
        return collect;
    }



    /*
     페치 조인으로 SQL이 1번만 실행됨
     distinct 를 사용한 이유는 1대 다 조인이 있으므로 데이터베이스 row가 증가한다.
     그 결과 같은 order 엔티티의 조회 수도 증가하게 된다.
     JPA의 distinct는 SQL에 distinct를 추가하고, 더해서 같은 엔티티가 조회되면,
     애플리케이션에서 중복을 걸러준다.
     이 예에서 order가 컬렉션 페치 조인 때문에 중복 조회 되는 것을 막아준다.

     단점으로는 페이징 불가능하다.
     컬렉션 페치 조인을 사용하면 페이징이 불가능하다. 하이버네이트는 경고 로그를 남기면서 모든 데이터를 DB에서 읽어오고,
     메모리에서 페이징 해버린다(매우 위험하다). 자세한 내용은 자바 ORM 표준 JPA 프로그래밍의 페치 조인 부분을 참고하자.
     컬렉션 페치 조인은 1개만 사용할 수 있다. 컬렉션 둘 이상에 페치 조인을 사용하면 안된다. 데이터가 부정합하게 조회될 수 있다.
     자세한 내용은 자바 ORM 표준 JPA 프로그래밍을 참고하자.
  */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();
        List<OrderDto> collect = orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());
        return collect;
    }



    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(
            @RequestParam(value = "offset", defaultValue ="0") int offset,
            @RequestParam(value = "limit", defaultValue = "100") int limit)
    {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);
        List<OrderDto> collect = orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());
        return collect;
    }


    /**
     * Query : 루트 1번, 컬렉션 N번 실행
     * ToOne(N:1, 1:1) 관계들을 먼저 조회하고, ToMany(1:N) 관계는 각각 별도로 처리한다.
     *     이런 방식을 선택한 이유는 다음과 같다.
     *     1) ToOne 관계는 조인해도 데이터 row 수가 증가하지 않는다.
     *     2) ToMany(1:N) 관계는 조인하면 row 수가 증가한다.
     * row 수가 증가하지 않는 ToOne 관계는 조인으로 최적화 하기 쉬우므로 한번에 조회하고, ToMany 관계는
     * 최적화 하기 어려우므로 findOrderItems() 같은 별도의 메서드로 조회한다.
     *
     * @return
     */
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }


    /**
     * Query : 루트 1번, 컬렉션 1번
     * ToOne 관계들을 먼저 조회하고, 여기서 얻은 식별자 orderId로 ToMany 관계인 OrderItem 을 한꺼번에 조회
     * MAP을 사용해서 매칭 성능 향상(O(1))
     * @return
     */
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDto_optimization();
    }


    /**
     * Query : 한번의 쿼리로 원하는 데이터를 조회할 수 있다.
     * 단점
     *  1) 쿼리는 한번이지만 조인으로 인해 DB에서 애플리케이션에 전달하는 데이터에 중복 데이터가 추가되므로 상황에 따라 V5보다 더 느릴 수도 있다.
     *  2) 애플리케이션에서 추가 작업이 크다
     *  3) 페이징 불가능
     * @return
     */
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();

        return flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(), e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(), e.getKey().getAddress(), e.getValue()))
                .collect(toList());
    }



    @Data
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(OrderItemDto::new)
                    .collect(Collectors.toList());
        }
    }


    @Data
    static class OrderItemDto {

        private String itemName;
        private int orderPrice;
        private int count;

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }
}


