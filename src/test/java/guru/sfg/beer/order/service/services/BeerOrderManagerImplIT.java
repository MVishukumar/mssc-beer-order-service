package guru.sfg.beer.order.service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jenspiegsa.wiremockextension.ManagedWireMockServer;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.services.beer.BeerServiceImpl;
import guru.sfg.brewery.model.BeerDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(WireMockExtension.class)
@SpringBootTest
class BeerOrderManagerImplIT {

  @Autowired
  BeerOrderManager beerOrderManager;

  @Autowired
  BeerOrderRepository beerOrderRepository;

  @Autowired
  CustomerRepository customerRepository;

  @Autowired
  WireMockServer wireMockServer;

  @Autowired
  ObjectMapper objectMapper;

  Customer testCustomer;

  UUID beerId = UUID.randomUUID();

  @TestConfiguration
  static class RestTemplateBuilderProvider {

    @Bean(destroyMethod = "stop")
    public WireMockServer wireMockServer() {

      WireMockServer server = ManagedWireMockServer.with(wireMockConfig().port(8083));
      server.start();
      return server;
    }
  }

  @BeforeEach
  void setUp() {
    testCustomer = customerRepository.save(Customer.builder()
            .customerName("Test Customer")
        .build());
  }

  public BeerOrder createBeerOrder() {
    BeerOrder beerOrder = BeerOrder.builder()
        .customer(testCustomer)
        .build();

    Set<BeerOrderLine> lines = new HashSet<>();
    lines.add(BeerOrderLine.builder()
            .beerId(beerId)
            .upc("12345")
            .orderQuantity(1)
            .beerOrder(beerOrder)
            .build());

    beerOrder.setBeerOrderLines(lines);

    return beerOrder;
  }

  @Test
  void testNewToAllocated() throws JsonProcessingException, InterruptedException {

    BeerDto beerDto = BeerDto.builder()
        .id(beerId)
        .upc("12345")
        .build();

    wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + "12345")
        .willReturn(okJson(objectMapper.writeValueAsString(beerDto)))
    );

    BeerOrder beerOrder = createBeerOrder();

    BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

    await().untilAsserted( () -> {
      BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

      assertEquals(BeerOrderStatusEnum.ALLOCATED, foundOrder.getOrderStatus());
    });

    savedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

    assertNotNull(savedBeerOrder);
    assertEquals(BeerOrderStatusEnum.ALLOCATED, savedBeerOrder.getOrderStatus());
  }


}