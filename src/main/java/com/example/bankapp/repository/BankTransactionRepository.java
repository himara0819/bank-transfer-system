
package com.example.bankapp.repository;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;

import java.util.List;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    // 1. Find all transactions, newest first
    @Query("SELECT t FROM BankTransaction t ORDER BY t.createdAt DESC")
    List<BankTransaction> findAllLatest();

    // 2. Find transaction history of an account
    @Query("SELECT t FROM BankTransaction t WHERE t.fromAccount = :accountNumber OR t.toAccount = :accountNumber ORDER BY t.createdAt DESC")
    List<BankTransaction> findHistory(
            @Param("accountNumber") String accountNumber
    );

    // 3. Find transactions by status, newest first
    @Query("SELECT t FROM BankTransaction t WHERE t.status = :status ORDER BY t.createdAt DESC")
    List<BankTransaction> findByStatusLatest(
            @Param("status") TransactionStatus status
    );

    // 4. Count transactions by status
    @Query("SELECT COUNT(t) FROM BankTransaction t WHERE t.status = :status")
    long countByStatus(
            @Param("status") TransactionStatus status
    );

    

// Find successful large transfers, newest first
        List<BankTransaction>
        findByStatusAndAmountGreaterThanEqualOrderByCreatedAtDesc(
        TransactionStatus status,
        BigDecimal minAmount
        );

        @Query("""
    SELECT COALESCE(SUM(t.amount), 0)
    FROM BankTransaction t
    WHERE t.fromAccount = :accountNumber
    AND t.status = :status
""")
BigDecimal calculateTotalSent(
        @Param("accountNumber") String accountNumber,
        @Param("status") TransactionStatus status
);
}
