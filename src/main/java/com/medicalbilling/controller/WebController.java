package com.medicalbilling.controller;

import com.medicalbilling.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final DashboardService dashboardService;
    private final CategoryService categoryService;
    private final SupplierService supplierService;
    private final CustomerService customerService;
    private final MedicineService medicineService;
    private final InventoryService inventoryService;
    private final SettingsService settingsService;
    private final UserService userService;
    private final WebFeatureServices featureServices;

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        Model model) {
        if (error != null) model.addAttribute("error", "Invalid username or password");
        if (logout != null) model.addAttribute("message", "You have been logged out successfully");
        return "login";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @GetMapping("/logout-success")
    public String logoutSuccess() {
        return "logout-success";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal UserDetails user) {
        model.addAttribute("dashboard", dashboardService.getDashboardData());
        model.addAttribute("username", user != null ? user.getUsername() : "");
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activePage", "dashboard");
        return "dashboard";
    }

    @GetMapping("/medicines")
    public String medicines(Model model, @RequestParam(required = false) String search) {
        model.addAttribute("medicines", medicineService.getAll(search));
        model.addAttribute("categories", categoryService.getAll(null));
        model.addAttribute("suppliers", supplierService.getAll(null));
        model.addAttribute("pageTitle", "Medicine Master");
        model.addAttribute("activePage", "medicines");
        return "medicines";
    }

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categoryService.getAll(null));
        model.addAttribute("pageTitle", "Category Master");
        model.addAttribute("activePage", "categories");
        return "categories";
    }

    @GetMapping("/suppliers")
    public String suppliers(Model model) {
        model.addAttribute("suppliers", supplierService.getAll(null));
        model.addAttribute("pageTitle", "Supplier Master");
        model.addAttribute("activePage", "suppliers");
        return "suppliers";
    }

    @GetMapping("/customers")
    public String customers(Model model) {
        model.addAttribute("customers", customerService.getAll(null));
        model.addAttribute("pageTitle", "Customer Master");
        model.addAttribute("activePage", "customers");
        return "customers";
    }

    @GetMapping("/billing")
    public String billing(Model model) {
        model.addAttribute("customers", customerService.getAll(null));
        model.addAttribute("pageTitle", "Billing");
        model.addAttribute("activePage", "billing");
        return "billing";
    }

    @GetMapping("/purchases")
    public String purchases(Model model) {
        model.addAttribute("suppliers", supplierService.getAll(null));
        model.addAttribute("medicines", medicineService.getAll(null));
        model.addAttribute("pageTitle", "Purchase");
        model.addAttribute("activePage", "purchases");
        return "purchases";
    }

    @GetMapping("/inventory")
    public String inventory(Model model, @RequestParam(required = false) String filter) {
        model.addAttribute("inventory", inventoryService.getInventorySummary(filter));
        model.addAttribute("pageTitle", "Inventory");
        model.addAttribute("activePage", "inventory");
        return "inventory";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("pageTitle", "Reports");
        model.addAttribute("activePage", "reports");
        return "reports";
    }

    @GetMapping("/returns")
    public String returns(Model model) {
        model.addAttribute("medicines", medicineService.getAll(null));
        model.addAttribute("pageTitle", "Returns");
        model.addAttribute("activePage", "returns");
        return "returns";
    }

    @GetMapping("/prescriptions")
    public String prescriptions(Model model) {
        model.addAttribute("customers", customerService.getAll(null));
        model.addAttribute("pageTitle", "Prescriptions");
        model.addAttribute("activePage", "prescriptions");
        return "prescriptions";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("settings", settingsService.getSettings());
        model.addAttribute("pageTitle", "Settings");
        model.addAttribute("activePage", "settings");
        return "settings";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("pageTitle", "User Management");
        model.addAttribute("activePage", "users");
        return "users";
    }

    @GetMapping("/branches")
    public String showBranches(Model model) {
        model.addAttribute("branches", featureServices.getBranchService().getAll());
        model.addAttribute("pageTitle", "Branch Management");
        model.addAttribute("activePage", "branches");
        return "branches";
    }

    @GetMapping("/online-orders")
    public String onlineOrders(Model model) {
        model.addAttribute("orders", featureServices.getOnlineOrderService().getAll());
        model.addAttribute("customers", customerService.getAll(null));
        model.addAttribute("medicines", medicineService.getAll(null));
        model.addAttribute("branches", featureServices.getBranchService().getActive());
        model.addAttribute("pageTitle", "Online Orders");
        model.addAttribute("activePage", "online-orders");
        return "online-orders";
    }

    @GetMapping("/reorder-suggestions")
    public String reorderSuggestions(Model model) {
        model.addAttribute("suggestions", featureServices.getReorderSuggestionService().getSuggestions());
        model.addAttribute("pageTitle", "AI Reorder Suggestions");
        model.addAttribute("activePage", "reorder");
        return "reorder-suggestions";
    }

    @GetMapping("/audit-logs")
    public String auditLogs(Model model) {
        model.addAttribute("logs", featureServices.getAuditLogService().getRecent(100));
        model.addAttribute("pageTitle", "Audit Logs");
        model.addAttribute("activePage", "audit");
        return "audit-logs";
    }
}