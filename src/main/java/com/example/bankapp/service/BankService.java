
package com.example.bankapp.service;

import com.example.bankapp.model.*;
import com.example.bankapp.repository.*;
import com.example.bankapp.exception.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    // Get all customers
    public List<Customer> getCustomers() {
        return customerRepository.findAllOrdered();
    }

    // Search customers
    public List<Customer> searchCustomers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getCustomers();
        }
        return customerRepository.searchByName(keyword);
    }

    // Get total balance
    public BigDecimal getTotalBalance() {
        BigDecimal total = customerRepository.getTotalBalance();
        return total != null ? total : BigDecimal.ZERO;
    }

    // Get total customer count
    public long getCustomerCount() {
        return customerRepository.countCustomersNative();
    }

    // Get latest transactions
    public List<BankTransaction> getLatestTransactions() {
        return transactionRepository.findAllLatest();
    }

    // Get account history
    public List<BankTransaction> getAccountHistory(String accountNumber) {
        return transactionRepository.findHistory(accountNumber);
    }
    // Get successful large transfers
public List<BankTransaction> getLargeTransfers(BigDecimal minAmount) {

    return transactionRepository
            .findByStatusAndAmountGreaterThanEqualOrderByCreatedAtDesc(
                    TransactionStatus.SUCCESS,
                    minAmount
            );
}

    // Get transaction counts
    public long getTransactionCount(TransactionStatus status) {
        return transactionRepository.countByStatus(status);
    }

    // Create customer
    @Transactional
    public Customer createCustomer(Customer customer) {

        if (customerRepository.countAccounts(
                customer.getAccountNumber()) > 0) {
            throw new BankException("Account number already exists");
        }

        if (customerRepository.countEmails(
                customer.getEmail()) > 0) {
            throw new BankException("Email already exists");
        }

        return customerRepository.save(customer);
    }


    // Deposit money
    @Transactional
    public BankTransaction deposit(
            String accountNumber,
            BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Deposit amount must be greater than zero");
        }

        customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found"));

        int credited = customerRepository.credit(accountNumber, amount);

        if (credited == 0) {
            throw new AccountNotFoundException("Account not found");
        }

        BankTransaction transaction = new BankTransaction();
        transaction.setAmount(amount);
        transaction.setFromAccount("999999");
        transaction.setToAccount(accountNumber);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Money deposited successfully");
        transaction.setCreatedAt(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    // Withdraw money
    @Transactional
    public BankTransaction withdraw(
            String accountNumber,
            BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Withdrawal amount must be greater than zero");
        }

        customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found"));

        int debited = customerRepository.debit(accountNumber, amount);

        if (debited == 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance");
        }

        BankTransaction transaction = new BankTransaction();
        transaction.setAmount(amount);
        transaction.setFromAccount(accountNumber);
        transaction.setToAccount("999999");
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Money withdrawn successfully");
        transaction.setCreatedAt(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }


    // Transfer money
    
    @Transactional(rollbackFor = Exception.class)
    
    public BankTransaction transfer(
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            boolean simulateFailure) {

        if (fromAccount.equals(toAccount)) {
            throw new BankException(
                    "Sender and receiver must be different accounts");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Invalid transfer amount");
        }

        Customer sender = customerRepository
                .getByAccountNumber(fromAccount)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Sender account not found"));

        customerRepository.getByAccountNumber(toAccount)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Receiver account not found"));

        // Step 1: Debit sender
        int debited = customerRepository.debit(fromAccount, amount);

        if (debited == 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance");
        }

        // Step 2: Simulate system failure
        if (simulateFailure) {
            throw new BankException(
                    "All changes were rolled back");
        }

        // Step 3: Credit receiver
        int credited = customerRepository.credit(toAccount, amount);

        if (credited == 0) {
            throw new AccountNotFoundException(
                    "Receiver account not found");
        }

        // Step 4: Save successful transaction
        BankTransaction transaction = new BankTransaction();

        transaction.setAmount(amount);
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Bank transfer successful");
        transaction.setCreatedAt(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }
// Calculate total successful outgoing transactions

    public BigDecimal getTotalSent(String accountNumber) {
    return transactionRepository.calculateTotalSent(
            accountNumber,
            TransactionStatus.SUCCESS
        );
    }
}
