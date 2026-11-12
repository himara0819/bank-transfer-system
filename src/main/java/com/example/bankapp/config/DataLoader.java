
package com.example.bankapp.config;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) {

        if (customerRepository.count() == 0) {

            Customer kamal = new Customer();
            kamal.setFullName("Kamal Perera");
            kamal.setAccountNumber("100001");
            kamal.setEmail("kamal@gmail.com");
            kamal.setBalance(new BigDecimal("50000.00"));

            Customer nimali = new Customer();
            nimali.setFullName("Nimali Silva");
            nimali.setAccountNumber("100002");
            nimali.setEmail("nimali@gmail.com");
            nimali.setBalance(new BigDecimal("25000.00"));

            Customer saman = new Customer();
            saman.setFullName("Saman Fernando");
            saman.setAccountNumber("100003");
            saman.setEmail("saman@gmail.com");
            saman.setBalance(new BigDecimal("10000.00"));

            customerRepository.saveAll(
                List.of(kamal, nimali, saman)
            );

            System.out.println("Sample customers inserted!");
        }
    }
}
