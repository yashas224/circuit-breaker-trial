package com.example.circuitbreakertrial;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Service
public class CustomerService {

  Logger logger = LoggerFactory.getLogger(CustomerService.class);

  @Autowired
  private CircuitBreakerFactory circuitBreakerFactory;

  @Autowired
  CustomerRepository customerRepository;

  CircuitBreaker circuitBreaker;

  private String retryPrefix = "retryTrial";
  @PostConstruct
  public void initCircuitBreaker() {
    circuitBreaker = circuitBreakerFactory.create("trialDB");
  }

  public Optional<CustomerEntity> getCustomerByNameNormal(String name) {
    return Optional.ofNullable(customerRepository.findByName(name));
  }

  public Optional<CustomerEntity> getCustomerByNameCB1(String name) {

    return Optional.ofNullable(circuitBreaker.run(() -> customerRepository.findByName(name),
       e -> {
         logger.error("exception raised {}", e);
         logger.info("in fall back method of CB1  !!!!");
         return null;
       }));
  }

  @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "trialDB", fallbackMethod = "fallback")
  public Optional<CustomerEntity> getCustomerByNameCB2(String name) {
    return Optional.ofNullable(customerRepository.findByName(name));
  }

  public Optional<CustomerEntity> fallback(String name, Throwable e) {
    logger.info("in fall back method of CB2  !!!!");
    return Optional.ofNullable(null);
  }

  // retry and circuit breaker trial

  @Retry(name = "trialRetry", fallbackMethod = "retryFallback")
  @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "trialDB", fallbackMethod = "cbFallback")
  public Optional<CustomerEntity> getCustomerByNameCB3(String name) {
    logger.info("getCustomerByNameCB3  !!!!");
    return Optional.ofNullable(customerRepository.findByName(name));
  }

  @SneakyThrows
  public Optional<CustomerEntity> cbFallback(String name, Throwable e) {
    logger.info("{} in fall back method of Circuit Breaker   !!!!", retryPrefix);
    throw  e;
  }

  public Optional<CustomerEntity> retryFallback(String name, Throwable e) {
    logger.info("{} in fall back method of Retry  !!!!", retryPrefix);
    return Optional.ofNullable(null);
  }

  // circuit breaker fallback is throwing the exception
  // retry module by default would retry on all recorded  exceoptions
  // thus for a single failure by Circuit breaker  , retry module is triggered
  // If Circuit breaker does not throw exceptions , then Retry wonn't happen.


}
