package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.statemachine.BeerOrderStateChangeInterceptor;
import guru.sfg.brewery.model.BeerOrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class BeerOrderManagerImpl implements BeerOrderManager {

  public static final String ORDER_ID_HEADER = "ORDER_ID_HEADER";

  private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
  private final BeerOrderRepository beerOrderRepository;
  private final BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;

  @Transactional
  @Override
  public BeerOrder newBeerOrder(BeerOrder beerOrder) {
    beerOrder.setId(null);
    beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);

    BeerOrder savedBeer = beerOrderRepository.save(beerOrder);
    sendBeerOrderEvent(savedBeer, BeerOrderEventEnum.E_VALIDATE_ORDER);
    return savedBeer;
  }


  private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum eventEnum) {
    StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = build(beerOrder);

    Message msg = MessageBuilder.withPayload(eventEnum)
        .setHeader(ORDER_ID_HEADER, beerOrder.getId().toString())
        .build();

    sm.sendEvent(msg);
  }

  @Transactional
  @Override
  public void processValidationResult(UUID beerOrderId, Boolean isValid) {
    Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderId);

    beerOrderOptional.ifPresentOrElse(beerOrder -> {
      if(isValid){
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.E_VALIDATION_PASSED);

        BeerOrder validatedOrder = beerOrderRepository.findById(beerOrderId).get();

        sendBeerOrderEvent(validatedOrder, BeerOrderEventEnum.E_ALLOCATE_ORDER);

      } else {
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.E_VALIDATION_FAILED);
      }
    }, () -> log.error("Order Not Found. Id: " + beerOrderId));



  }

  @Override
  public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto) {

    BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
    sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.E_ALLOCATION_SUCCESS);
    updateAllocationQty(beerOrderDto);

  }

  @Override
  public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto) {

    BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
    sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.E_ALLOCATION_NO_INVENTORY);
    updateAllocationQty(beerOrderDto);
  }

  private void updateAllocationQty(BeerOrderDto beerOrderDto) {

    Optional<BeerOrder> allocatedOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());

    allocatedOrderOptional.ifPresentOrElse(allocatedOrder -> {
      allocatedOrder.getBeerOrderLines()
          .forEach(beerOrderLine -> {
            beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
              if(beerOrderLine.getId().equals(beerOrderDto.getId())) {
                beerOrderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
              }
            });
          });

          beerOrderRepository.saveAndFlush(allocatedOrder);
    }, () -> log.error("Order Not Found. Id:" + beerOrderDto.getId()));



  }

  @Override
  public void beerOrderAllocationFailed(BeerOrderDto beerOrderDto) {

    BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
    sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.E_ALLOCATION_FAILED);

  }

  @Override
  public void beerOrderPickedUp(UUID id) {
    Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(id);

    beerOrderOptional.ifPresentOrElse(beerOrder -> {
      //do process
      sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.E_BEER_ORDER_PICKED_UP);
    }, () -> log.error("Order Not Found. Id: " + id));
  }

  @Override
  public void cancelOrder(UUID id) {
    beerOrderRepository.findById(id).ifPresentOrElse(beerOrder -> {
      sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.E_CANCEL_ORDER);
    }, () -> log.error("Order Not Found. Id: " + id));
  }

  private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build(BeerOrder beerOrder) {
    StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = stateMachineFactory.getStateMachine(beerOrder.getId());

    sm.stop();

    sm.getStateMachineAccessor()
        .doWithAllRegions(sma -> {
          sma.addStateMachineInterceptor(beerOrderStateChangeInterceptor);
          sma.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null, null, null));
        });

    sm.start();

    return sm;
  }
}
