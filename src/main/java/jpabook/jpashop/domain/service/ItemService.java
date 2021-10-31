package jpabook.jpashop.domain.service;

import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.domain.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;

    @Transactional
    public void saveItem(Item item) {
        itemRepository.save(item);
    }

    /**
     * 변경 감지를 통해 자동으로 DB 업데이트가 발생하는 경우임
     * 즉, @Transactional 애노테이션으로 인해 트랜잭션 종료 시
     * DB에서 조회한 데이터를 저장하고 있는 findItem 인스턴스가 영속성에 관리되고 있으므로
     * findItem 인스턴스 내에 값의 변경이 있는지 JPA가 관리하여 변경점 발생 시 자동으로 update가 발생되는 구조이다.
     */
    @Transactional
    public void updateItem(Long itemId, String name, int price, int stockQuantity) {
//    public void updateItem(Long itemId, Book bookParam) {
//        Item findItem = itemRepository.findOne(itemId);
//        findItem.setPrice(bookParam.getPrice());
//        findItem.setName(bookParam.getName());
//        findItem.setStockQuantity(bookParam.getStockQuantity());

        //영속성으로 관리되도록 DB에서 조회하여 findItem 인스턴스를 생성하고,
        //생성된 findItem 인스턴스의 값을 변경시킴으로 JPA가 변경점이 발생하였음을 인지하게 만듦으로
        //트랜잭션 커밋 시점에 자동으로 update가 발생되도록 유도한다.
        //이것이 유지보수하기에 편리하고, merge(병합)을 통한 데이터 갱신으로 원치 않는 필드의 값 갱신 발생 오류를 막을 수 있다.
        Item findItem = itemRepository.findOne(itemId);
        findItem.changeItemInfo(name, price, stockQuantity);
    }

    public List<Item> findItems() {
        return itemRepository.findAll();
    }

    public Item findOne(Long id) {
        return itemRepository.findOne(id);
    }
}
