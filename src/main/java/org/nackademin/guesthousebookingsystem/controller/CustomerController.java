package org.nackademin.guesthousebookingsystem.controller;

import lombok.RequiredArgsConstructor;
import org.nackademin.guesthousebookingsystem.client.CustomerClient;
import org.nackademin.guesthousebookingsystem.dto.CustomerDto;
import org.nackademin.guesthousebookingsystem.entity.AuditEvent;
import org.nackademin.guesthousebookingsystem.repository.AuditEventRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;

@Controller
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerClient customerClient;
    private final AuditEventRepository auditEventRepository;

    @GetMapping
    public String getCustomers(Model model) {
        try {
            model.addAttribute("customers", customerClient.getAllCustomers());
        } catch (RuntimeException e) {
            model.addAttribute("customers", Collections.emptyList());
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("customer", new CustomerDto());
        model.addAttribute("editMode", false);
        return "customers/list";
    }

    @GetMapping("/edit/{id}")
    public String editCustomer(@PathVariable Long id, Model model) {
        try {
            model.addAttribute("customers", customerClient.getAllCustomers());
            model.addAttribute("customer", customerClient.getCustomerById(id));
            model.addAttribute("editMode", true);
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("customers", Collections.emptyList());
            model.addAttribute("customer", new CustomerDto());
            model.addAttribute("editMode", false);
        }
        return "customers/list";
    }

    @PostMapping("/save")
    public String saveCustomer(@ModelAttribute CustomerDto customerDto, RedirectAttributes ra) {
        try {
            CustomerDto saved = customerClient.saveCustomer(customerDto);
            auditEventRepository.save(new AuditEvent(AuditEvent.Type.CUSTOMER, saved != null ? saved.getId() : null, AuditEvent.Action.CREATED));
            ra.addFlashAttribute("success", "Kunden sparades!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customers";
    }

    @PostMapping("/update/{id}")
    public String updateCustomer(@PathVariable Long id, @ModelAttribute CustomerDto customerDto, RedirectAttributes ra) {
        try {
            customerClient.updateCustomer(id, customerDto);
            auditEventRepository.save(new AuditEvent(AuditEvent.Type.CUSTOMER, id, AuditEvent.Action.UPDATED));
            ra.addFlashAttribute("success", "Kunden uppdaterades!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customers";
    }

    @GetMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable Long id, RedirectAttributes ra) {
        try {
            customerClient.deleteCustomer(id);
            auditEventRepository.save(new AuditEvent(AuditEvent.Type.CUSTOMER, id, AuditEvent.Action.DELETED));
            ra.addFlashAttribute("success", "Kunden togs bort.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customers";
    }
}