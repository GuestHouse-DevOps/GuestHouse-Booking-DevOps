package org.nackademin.guesthousebookingsystem.controller;

import lombok.RequiredArgsConstructor;
import org.nackademin.guesthousebookingsystem.client.CustomerClient;
import org.nackademin.guesthousebookingsystem.dto.BookingDto;
import org.nackademin.guesthousebookingsystem.dto.RoomDto;
import org.nackademin.guesthousebookingsystem.entity.BookingStatus;
import org.nackademin.guesthousebookingsystem.service.BookingService;
import org.nackademin.guesthousebookingsystem.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final CustomerClient customerClient;
    private final RoomService roomService;

    private void populateModel(Model model) {
        model.addAttribute("bookings", bookingService.getAllBookings());
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("statuses", BookingStatus.values());
        try {
            model.addAttribute("customers", customerClient.getAllCustomers());
        } catch (RuntimeException e) {
            model.addAttribute("customers", java.util.Collections.emptyList());
            model.addAttribute("customerServiceError", e.getMessage());
        }
    }

    @GetMapping
    public String getBookings(Model model) {
        populateModel(model);
        BookingDto booking = new BookingDto();
        booking.setRoom(new RoomDto());
        model.addAttribute("booking", booking);
        model.addAttribute("editMode", false);
        return "bookings/list";
    }

    @GetMapping("/edit/{id}")
    public String editBooking(@PathVariable Long id, Model model) {
        populateModel(model);
        model.addAttribute("booking", bookingService.getBookingById(id));
        model.addAttribute("editMode", true);
        return "bookings/list";
    }

    @PostMapping("/save")
    public String saveBooking(@ModelAttribute BookingDto bookingDto, RedirectAttributes ra) {
        try {
            bookingService.saveBooking(bookingDto);
            ra.addFlashAttribute("success", "Bokning sparad!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bookings";
    }

    @PostMapping("/update/{id}")
    public String updateBooking(@PathVariable Long id, @ModelAttribute BookingDto bookingDto, RedirectAttributes ra) {
        try {
            bookingService.updateBooking(id, bookingDto);
            ra.addFlashAttribute("success", "Bokning uppdaterad!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bookings";
    }

    @GetMapping("/cancel/{id}")
    public String cancelBooking(@PathVariable Long id, RedirectAttributes ra) {
        try {
            bookingService.cancelBooking(id);
            ra.addFlashAttribute("success", "Bokning avbokad.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Bokningen kunde inte avbokas.");
        }
        return "redirect:/bookings";
    }

    @GetMapping("/customer/{customerId}/exists")
    @ResponseBody
    public ResponseEntity<Boolean> customerHasBookings(@PathVariable Long customerId) {
        return ResponseEntity.ok(bookingService.customerHasActiveBookings(customerId));
    }

    @GetMapping("/customer/{customerId}/room/{roomId}/exists")
    @ResponseBody
    public ResponseEntity<Boolean> customerHasBookedRoom(@PathVariable Long customerId, @PathVariable Long roomId) {
        return ResponseEntity.ok(bookingService.customerHasBookedRoom(customerId, roomId));
    }
}