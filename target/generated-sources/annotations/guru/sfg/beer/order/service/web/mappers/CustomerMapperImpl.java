package guru.sfg.beer.order.service.web.mappers;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.domain.Customer.CustomerBuilder;
import guru.sfg.brewery.model.CustomerDto;
import guru.sfg.brewery.model.CustomerDto.CustomerDtoBuilder;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2021-11-05T23:34:55+0530",
    comments = "version: 1.3.1.Final, compiler: javac, environment: Java 11.0.12 (Oracle Corporation)"
)
@Component
public class CustomerMapperImpl implements CustomerMapper {

    @Autowired
    private DateMapper dateMapper;

    @Override
    public CustomerDto customerToDto(Customer customer) {
        if ( customer == null ) {
            return null;
        }

        CustomerDtoBuilder customerDto = CustomerDto.builder();

        customerDto.id( customer.getId() );
        if ( customer.getVersion() != null ) {
            customerDto.version( customer.getVersion().intValue() );
        }
        customerDto.createdDate( dateMapper.asOffsetDateTime( customer.getCreatedDate() ) );
        customerDto.lastModifiedDate( dateMapper.asOffsetDateTime( customer.getLastModifiedDate() ) );
        customerDto.customerName( customer.getCustomerName() );

        return customerDto.build();
    }

    @Override
    public Customer dtoToCustomer(Customer customer) {
        if ( customer == null ) {
            return null;
        }

        CustomerBuilder customer1 = Customer.builder();

        customer1.id( customer.getId() );
        customer1.version( customer.getVersion() );
        customer1.createdDate( customer.getCreatedDate() );
        customer1.lastModifiedDate( customer.getLastModifiedDate() );
        customer1.customerName( customer.getCustomerName() );
        customer1.apiKey( customer.getApiKey() );
        Set<BeerOrder> set = customer.getBeerOrders();
        if ( set != null ) {
            customer1.beerOrders( new HashSet<BeerOrder>( set ) );
        }

        return customer1.build();
    }
}
