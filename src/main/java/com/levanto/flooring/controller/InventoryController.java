package com.levanto.flooring.controller;

import com.levanto.flooring.dto.ApiResponse;
import com.levanto.flooring.entity.InventoryItem;
import com.levanto.flooring.enums.ItemType;
import com.levanto.flooring.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller @RequestMapping("/admin/inventory") @RequiredArgsConstructor
public class InventoryController {

    private final InventoryService service;

    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page,
                       @RequestParam(defaultValue="20") int size,
                       @RequestParam(required=false) String search,
                       Model m) {
        m.addAttribute("itemsPage",   service.paged(page, size, search));
        m.addAttribute("search",      search);
        m.addAttribute("currentPage", page);
        m.addAttribute("lowStock",    service.getLowStock(10));
        m.addAttribute("ItemType",    ItemType.values());
        return "inventory/list";
    }

    @GetMapping("/new")
    public String newForm(Model m) {
        m.addAttribute("item",     new InventoryItem());
        m.addAttribute("ItemType", ItemType.values());
        return "inventory/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model m) {
        m.addAttribute("item",     service.getById(id));
        m.addAttribute("ItemType", ItemType.values());
        return "inventory/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute InventoryItem item, RedirectAttributes ra) {
        service.save(item);
        ra.addFlashAttribute("success", "Item saved successfully.");
        return "redirect:/admin/inventory";
    }

    @PostMapping("/{id}/stock") @ResponseBody
    public ResponseEntity<ApiResponse<?>> addStock(@PathVariable Long id, @RequestParam int qty) {
        service.addStock(id, qty);
        return ResponseEntity.ok(ApiResponse.ok("Stock updated."));
    }

    @PostMapping("/{id}/price") @ResponseBody
    public ResponseEntity<ApiResponse<?>> updatePrice(@PathVariable Long id, @RequestParam BigDecimal price) {
        service.updatePrice(id, price);
        return ResponseEntity.ok(ApiResponse.ok("Price updated."));
    }

    @PostMapping("/{id}/toggle") @ResponseBody
    public ResponseEntity<ApiResponse<?>> toggle(@PathVariable Long id) {
        service.toggleActive(id);
        return ResponseEntity.ok(ApiResponse.ok("Status toggled."));
    }

    @DeleteMapping("/{id}") @ResponseBody
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Deleted."));
    }
}
