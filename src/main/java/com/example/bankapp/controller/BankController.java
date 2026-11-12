
package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.CustomerRepository;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;
    private final CustomerRepository customerRepository;

    // Dashboard
    @GetMapping("/")
    public String dashboard(
            @RequestParam(required = false, defaultValue = "") String keyword,
            Model model) {

        populateDashboard(model, keyword);
        return "bank";
    }

    // Add customer
    @PostMapping("/customers")
    public String addCustomer(
            @Valid @ModelAttribute("customer") Customer customer,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            populateDashboard(model, "");
            model.addAttribute("customer", customer);
            return "bank";
        }

        try {
            bankService.createCustomer(customer);
            redirectAttributes.addFlashAttribute(
                    "success", "Customer added successfully!");

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "error", e.getMessage());
        }

        return "redirect:/";
    }

    // Transfer money
    @PostMapping("/transfer")
    public String transfer(
            @Valid @ModelAttribute("transferRequest")
            TransferRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

      if (result.hasErrors()) {
    populateDashboard(model, "");

    model.addAttribute("transferRequest", request);

    model.addAttribute(
        org.springframework.validation.BindingResult.MODEL_KEY_PREFIX
        + "transferRequest",
        result
    );

    return "bank";
}

        try {
            bankService.transfer(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    request.isSimulateFailure()
            );

            redirectAttributes.addFlashAttribute(
                    "success", "Transfer completed successfully!");

        } catch (RuntimeException e) {

            // Record failure in a separate transaction
            try {
                auditService.recordFailure(
                        request.getFromAccount(),
                        request.getToAccount(),
                        request.getAmount(),
                        e.getMessage()
                );
            } catch (RuntimeException auditError) {
                redirectAttributes.addFlashAttribute(
                        "error", "Transfer failed. Audit recording also failed.");
                return "redirect:/";
            }

            redirectAttributes.addFlashAttribute(
                    "error", e.getMessage());
        }

        return "redirect:/";
    }

    // Deposit page
    @GetMapping("/deposit")
    public String depositPage(Model model) {
        model.addAttribute("customers", bankService.getCustomers());
        return "deposit";
    }

    // Process deposit
    @PostMapping("/deposit")
    public String deposit(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes) {

        try {
            bankService.deposit(accountNumber, amount);
            redirectAttributes.addFlashAttribute(
                    "success", "Deposit completed successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/deposit";
    }

    // Withdraw page
    @GetMapping("/withdraw")
    public String withdrawPage(Model model) {
        model.addAttribute("customers", bankService.getCustomers());
        return "withdraw";
    }

    // Process withdrawal
    @PostMapping("/withdraw")
    public String withdraw(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes) {

        try {
            bankService.withdraw(accountNumber, amount);
            redirectAttributes.addFlashAttribute(
                    "success", "Withdrawal completed successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/withdraw";
    }

    // Account transaction history
    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(
            @PathVariable String accountNumber,
            Model model) {

        Customer customer = customerRepository
                .getByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException(
                        "Account not found"));

        model.addAttribute("customer", customer);

        model.addAttribute(
                "transactions",
                bankService.getAccountHistory(accountNumber));

        model.addAttribute(
        "totalSent",
        bankService.getTotalSent(accountNumber)
);

        return "history";
    }

    // Large transfers page
@GetMapping("/large-transfers")
public String largeTransfers(
        @RequestParam(defaultValue = "10000") BigDecimal min,
        Model model) {

    model.addAttribute(
            "transfers",
            bankService.getLargeTransfers(min)
    );

    model.addAttribute("min", min);

    return "large-transfers";
}

    // Populate dashboard data
    private void populateDashboard(Model model, String keyword) {

        model.addAttribute(
                "customers",
                bankService.searchCustomers(keyword));

        model.addAttribute(
                "allCustomers",
                bankService.getCustomers());

        model.addAttribute(
                "transactions",
                bankService.getLatestTransactions());

        model.addAttribute(
                "totalCustomers",
                bankService.getCustomerCount());

        model.addAttribute(
                "totalBalance",
                bankService.getTotalBalance());

        model.addAttribute(
                "successCount",
                bankService.getTransactionCount(
                        TransactionStatus.SUCCESS));

        model.addAttribute(
                "failedCount",
                bankService.getTransactionCount(
                        TransactionStatus.FAILED));

        model.addAttribute(
                "transferRequest", new TransferRequest());

        model.addAttribute("customer", new Customer());
        model.addAttribute("keyword", keyword);
    }
}
