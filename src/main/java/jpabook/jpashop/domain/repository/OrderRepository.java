package jpabook.jpashop.domain.repository;

import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.repository.order.simplequery.OrderSimpleQueryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepository {
    private final EntityManager em;

    public void save(Order order) {
        em.persist(order);
    }

    public Order findOne(Long id) {
        return em.find(Order.class, id);
    }


//    public List<Order> findAll(OrderSearch orderSearch) {
//        QOrder order = QOrder.order;
//    }



    /**
     * JPA 동적 쿼리 대응, 실무에서 안씀
     * @param orderSearch
     * @return
     */
    public List<Order> findAllByString(OrderSearch orderSearch) {
        //정적 쿼리인 경우...
//        return em.createQuery("select o from Order o join o.member m" +
//                " where o.status = :status " +
//                " and m.name like :name", Order.class)
//                .setParameter("status", orderSearch.getOrderStatus())
//                .setParameter("name", orderSearch.getMemberName())
//                .setMaxResults(1000)    //최대 1000줄까지만 조회
//                .getResultList();

        //동적 쿼리 작성 예시1
        String jpql = "select o from Order o join o.member m";

        boolean isFirstCondition = true;

        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where ";
                isFirstCondition = false;
            } else {
                jpql += " and ";
            }
            jpql += " o.status = :status";
        }

        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where ";
                isFirstCondition = false;
            } else {
                jpql += " and ";
            }
            jpql += " m.name like :name ";
        }

        TypedQuery<Order> query = em.createQuery(jpql, Order.class)
                .setMaxResults(1000);

        if (orderSearch.getOrderStatus() != null) {
            query = query.setParameter("status", orderSearch.getOrderStatus());
        }
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            query = query.setParameter("name", orderSearch.getMemberName());
        }

        return query.getResultList();
    }


    /**
     * JPA CRITERIA 실무에서 안씀
     * @param orderSearch
     * @return
     */
    public List<Order> findAllByCriteria(OrderSearch orderSearch) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> o = cq.from(Order.class);
        Join<Object, Object> m = o.join("member", JoinType.INNER);

        List<Predicate> criteria = new ArrayList<>();

        // 주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            Predicate status = cb.equal(o.get("status"), orderSearch.getOrderStatus());
            criteria.add(status);
        }

        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            Predicate name = cb.like(m.<String>get("name"), "%" + orderSearch.getMemberName() + "%");
            criteria.add(name);
        }

        cq.where(cb.and(criteria.toArray(new Predicate[criteria.size()])));
        TypedQuery<Order> query = em.createQuery(cq).setMaxResults(1000);
        return query.getResultList();
    }

    public List<Order> findAllWithMemberDelivery() {
        //fetch 조인으로 LAZY로 설정되어 있더라도 한번에 일괄 조인하여 결과를 모두 리턴하게 만든다.
        return em.createQuery(
                "select o from Order o" +
                        " left join fetch o.member m" +
                        " left join fetch o.delivery d", Order.class)
                .getResultList();

    }


    public List<Order> findAllWithMemberDelivery(int offset, int limit) {
        //fetch 조인으로 LAZY로 설정되어 있더라도 한번에 일괄 조인하여 결과를 모두 리턴하게 만든다.
        return em.createQuery(
                        "select o from Order o" +
                                " left join fetch o.member m" +
                                " left join fetch o.delivery d", Order.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();

    }


    public List<Order> findAllWithItem() {
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
        return em.createQuery(
                "select distinct o from Order o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d" +
                        " join fetch o.orderItems oi" +
                        " join fetch oi.item i", Order.class
        ).getResultList();


    }
}
