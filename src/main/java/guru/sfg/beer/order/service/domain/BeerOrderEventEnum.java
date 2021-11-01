package guru.sfg.beer.order.service.domain;

public enum BeerOrderEventEnum {
  E_VALIDATE_ORDER, E_VALIDATION_PASSED, E_VALIDATION_FAILED,
  E_ALLOCATE_ORDER, E_ALLOCATION_SUCCESS, E_ALLOCATION_NO_INVENTORY, E_ALLOCATION_FAILED,
  E_BEER_ORDER_PICKED_UP,
  E_CANCEL_ORDER
}
