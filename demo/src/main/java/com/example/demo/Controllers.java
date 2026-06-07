package com.example.demo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.Model;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;

@Controller
public class Controllers {

    @Autowired
    orderRepositry or;

    @Autowired
    singupRepositoy sr;

    @Autowired
    userRepository ur;

    @Autowired
    EntityRepository er;

    @Autowired
    prodectRepository pr;

    @Autowired
    JavaMailSender emailsender;

    @Value("${project.image}")
    private String uploadDir;

    @PostConstruct
    public void initAdmin() {
        if (sr.findByEmail("admin@ksleep.com").isEmpty()) {
            Entitysignup admin = new Entitysignup();
            admin.setName("pankaj");
            admin.setEmail("admin@ksleep.com");
            admin.setPassword("Pankaj@3287");
            admin.setRole("ADMIN");
            sr.save(admin);
        }
    }

    @GetMapping("starting")
    public String firstpage() {
        return "redirect:/index.html";
    }

    @PostMapping("/admin/sendOtp")
    @ResponseBody
    public ResponseEntity<String> sendAdminOtp(@RequestParam String email, HttpSession session) {
        if (!"pkumarsaini178@gmail.com".equals(email)) {
            return ResponseEntity.badRequest().body("Unauthorized email address!");
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", (int)(Math.random() * 1000000));
        session.setAttribute("adminOtp", otp);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("✦ KSleep Admin Login — OTP Verification");
            message.setText("Hello Admin,\n\n" +
                    "Your One-Time Password (OTP) for accessing the KSleep Admin Panel is:\n\n" +
                    "👉 " + otp + "\n\n" +
                    "This OTP is valid for this session only. Do not share it with anyone.\n\n" +
                    "Thank you,\nKSleep Security Team");
            emailsender.send(message);
            return ResponseEntity.ok("OTP sent successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send OTP: " + e.getMessage());
        }
    }

    @PostMapping("/adminLogin")
    public String adminLogin(
            @RequestParam String name,
            @RequestParam String password,
            @RequestParam String email,
            @RequestParam String otp,
            HttpSession session,
            jakarta.servlet.http.HttpServletResponse response) {

        String sessionOtp = (String) session.getAttribute("adminOtp");

        if ("pankaj".equals(name) && "Pankaj@3287".equals(password) && "pkumarsaini178@gmail.com".equals(email) && otp != null && otp.equals(sessionOtp)) {
            session.removeAttribute("adminOtp");

            session.setAttribute("userEmail", "admin@ksleep.com");
            session.setAttribute("isAdmin", true);
            session.setAttribute("userRole", "ADMIN");

            // Generate JWT token with ADMIN role
            String token = JwtUtil.generateToken("admin@ksleep.com", "ADMIN");

            // Store in HttpOnly Cookie (3 days expiration)
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwt", token);
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // Can be set to true if deployed on HTTPS
            cookie.setPath("/");
            cookie.setMaxAge(3 * 24 * 60 * 60); // 3 days in seconds
            response.addCookie(cookie);

            return "redirect:/admin/dashboard";
        } else {
            return "redirect:/admin.html?error=invalid";
        }
    }

    // value = "/CompanyDeatailes",
    // consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    @PostMapping(value = "/insertproductdata", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String productDetail(
            @RequestParam String productName,
            @RequestParam double price,
            @RequestParam String material,
            @RequestParam String comfort,
            @RequestParam String description,
            @RequestParam(value = "image1", required = false) MultipartFile file1,
            @RequestParam(value = "image2", required = false) MultipartFile file2,
            @RequestParam(value = "image3", required = false) MultipartFile file3,
            @RequestParam(value = "image4", required = false) MultipartFile file4,
            @RequestParam(value = "image5", required = false) MultipartFile file5,
            @RequestParam(value = "imageUrl1", required = false) String imageUrl1,
            @RequestParam(value = "imageUrl2", required = false) String imageUrl2,
            @RequestParam(value = "imageUrl3", required = false) String imageUrl3,
            @RequestParam(value = "imageUrl4", required = false) String imageUrl4,
            @RequestParam(value = "imageUrl5", required = false) String imageUrl5,
            HttpSession session) throws IOException {

        Files.createDirectories(Paths.get(uploadDir));

        String img1Name = processImageInput(file1, imageUrl1);
        String img2Name = processImageInput(file2, imageUrl2);
        String img3Name = processImageInput(file3, imageUrl3);
        String img4Name = processImageInput(file4, imageUrl4);
        String img5Name = processImageInput(file5, imageUrl5);

        prodectentity pe = new prodectentity();
        pe.setProductName(productName);
        pe.setPrice(price);
        pe.setMaterial(material);
        pe.setComfortLevel(comfort);
        pe.setProductDescription(description);

        if (img1Name != null) pe.setImage1(img1Name);
        if (img2Name != null) pe.setImage2(img2Name);
        if (img3Name != null) pe.setImage3(img3Name);
        if (img4Name != null) pe.setImage4(img4Name);
        if (img5Name != null) pe.setImage5(img5Name);

        pr.save(pe);

        String role = (String) session.getAttribute("userRole");
        if ("ADMIN".equals(role)) {
            return "redirect:/admin/products";
        }
        return "redirect:/prodectlist.html";
    }

    // ===============================
    // FETCH ALL PRODUCTS
    // ===============================
    @CrossOrigin(origins = "*")
    @GetMapping("/fechdata")
    @ResponseBody
    public List<prodectentity> fechdata() {

        return pr.findAll().stream().map(product -> {

            if (product.getImage1() != null)
                product.setImage1("/images/" + product.getImage1());
            if (product.getImage2() != null)
                product.setImage2("/images/" + product.getImage2());
            if (product.getImage3() != null)
                product.setImage3("/images/" + product.getImage3());
            if (product.getImage4() != null)
                product.setImage4("/images/" + product.getImage4());
            if (product.getImage5() != null)
                product.setImage5("/images/" + product.getImage5());

            return product;

        }).toList();
    }

    @GetMapping("/fechdata/{id}")
    @ResponseBody
    public prodectentity getProductById(@PathVariable Long id) {

        prodectentity product = pr.findById(id).orElse(null);

        if (product != null) {
            product.setImage1("/images/" + product.getImage1());
            product.setImage2("/images/" + product.getImage2());
            product.setImage3("/images/" + product.getImage3());
            product.setImage4("/images/" + product.getImage4());
            product.setImage5("/images/" + product.getImage5());
        }

        return product;
    }

    // ===============================
    // FETCH SINGLE IMAGE
    // ===============================
    @GetMapping("/images/{imageName}")
    public ResponseEntity<Resource> getImage(@PathVariable String imageName) throws IOException {

        Path imagePath = Paths.get(uploadDir).resolve(imageName);
        Resource resource = new UrlResource(imagePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(imagePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))

                .body(resource);
    }

    @PostMapping("/senddeatailEmail")
    public String paymentgateway(
            @RequestParam String username,
            @RequestParam String mobile,
            @RequestParam String address,
            @RequestParam String state,
            @RequestParam String city,
            @RequestParam String pincode,
            @RequestParam String product_id,
            @RequestParam String pprice,
            @RequestParam String pName,
            HttpSession session) {

        try {

            String loginEmail = (String) session.getAttribute("userEmail");

            if (loginEmail == null) {
                return "redirect:/login.html";
            }

            userEntity ue = new userEntity();

            ue.setUser_name(username);
            ue.setEmail(loginEmail); // session email
            ue.setMobile_number(mobile);
            ue.setAddress(address);
            ue.setPincode(pincode);

            ur.save(ue);

            SimpleMailMessage m = new SimpleMailMessage();

            String detail = "Username: " + username +
                    "\nEmail: " + loginEmail +
                    "\nMobile: " + mobile +
                    "\nAddress: " + address +
                    "\nState: " + state +
                    "\nCity: " + city +
                    "\nPincode: " + pincode +
                    "\nProduct_id: " + product_id +
                    "\nProduct_price: " + pprice +
                    "\nProduct_name: " + pName;

            m.setFrom(loginEmail);
            m.setTo("pkumarsaini178@gmail.com");
            m.setSubject("Order Of Customer");
            m.setText(detail);

            emailsender.send(m);

            return "redirect:/index.html";

        } catch (Exception e) {

            return "redirect:/index.html";
        }
    }

    @PostMapping("signup")
    public String sinuppage(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String id) {

        Entitysignup es = new Entitysignup();
        es.setName(username);
        es.setEmail(email);
        es.setPassword(password);
        es.setRole("CUSTOMER"); // Default role
        sr.save(es);
        return "redirect:/login.html" + (id != null && !id.isEmpty() ? "?id=" + id + "&email=" + email : "");
    }

    @PostMapping("/loginpage")
    public String loginpage(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String id,
            HttpSession session,
            jakarta.servlet.http.HttpServletResponse response) {
        try {

            List<Entitysignup> user = sr.findByEmailAndPassword(email, password);

            if (!user.isEmpty()) {
                Entitysignup member = user.get(0);
                String role = member.getRole();
                if (role == null || role.isEmpty()) {
                    role = "CUSTOMER";
                }

                session.setAttribute("userEmail", email);
                session.setAttribute("userRole", role);

                // Generate JWT token with user's role
                String token = JwtUtil.generateToken(email, role);

                // Store in HttpOnly Cookie (3 days expiration)
                jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwt", token);
                cookie.setHttpOnly(true);
                cookie.setSecure(false); // Can be set to true if deployed on HTTPS
                cookie.setPath("/");
                cookie.setMaxAge(3 * 24 * 60 * 60); // 3 days in seconds
                response.addCookie(cookie);

                if ("ADMIN".equals(role)) {
                    return "redirect:/login.html?error=invalid";
                }

                if (id != null && !id.isEmpty() && !id.equals("null")) {
                    return "redirect:/fulldeatailprodect.html?id=" + id + "&email=" + email;
                } else {
                    return "redirect:/index.html";
                }
            } else {
                return "redirect:/login.html?error=invalid";
            }

        } catch (Exception e) {
            return "redirect:/login.html";
        }
    }

    @GetMapping("/getUserName")
    @ResponseBody
    public ResponseEntity<String> getUserName(@RequestParam String email) {
        List<Entitysignup> users = sr.findByEmail(email);
        if (!users.isEmpty()) {
            return ResponseEntity.ok(users.get(0).getName());
        }
        return ResponseEntity.ok("");
    }

    @PostMapping("/placeOrder")
    public String placeOrder(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String mobile,
            @RequestParam String address,
            @RequestParam String state,
            @RequestParam String city,
            @RequestParam String pincode,
            @RequestParam String pName,
            @RequestParam double pprice,
            @RequestParam Long product_id,
            @RequestParam String image,
            @RequestParam int qty,
            HttpSession session) {

        // ✅ KEY LINE — read email from session (set during login)
        String sessionEmail = (String) session.getAttribute("userEmail");
        String finalEmail = sessionEmail != null ? sessionEmail : email;

        orderEntity order = new orderEntity();
        order.setCustomerName(username);
        order.setEmail(finalEmail); // ✅ session email links to My Orders
        order.setMobile_No(mobile);
        order.setAddress(address + ", " + city + ", " + state + " - " + pincode);
        order.setProductName(pName);
        order.setPrice(pprice);
        order.setImage(image); // ✅ saves image URL/filename
        order.setProductId(product_id);
        order.setQuantity(qty); // ✅ fixed field name
        order.setStatus("confirmed"); // ✅ default status

        or.save(order); // ✅ save to DB

        // Save delivery profile if not already present
        try {
            List<userEntity> existingProfile = ur.findByEmail(finalEmail);
            if (existingProfile.isEmpty()) {
                userEntity ue = new userEntity();
                ue.setUser_name(username);
                ue.setEmail(finalEmail);
                ue.setMobile_number(mobile);
                ue.setAddress(address + ", " + city + ", " + state);
                ue.setPincode(pincode);
                ur.save(ue);
            }
        } catch (Exception ex) {
            System.out.println("Error saving customer profile: " + ex.getMessage());
        }

        // ✅ send confirmation email AFTER save
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("✦ Order Confirmed — KSleep");
            message.setText(
                    "Hello " + username + ",\n\n" +
                            "Your order has been placed successfully!\n\n" +
                            "Product  : " + pName + "\n" +
                            "Price    : ₹" + pprice + "\n" +
                            "Quantity : " + qty + "\n" +
                            "Address  : " + address + ", " + city + ", " + state + " - " + pincode + "\n\n" +
                            "Thank you for choosing KSleep ✦\n" +
                            "Track your order at: yourdomain.com/myorder.html");
            emailsender.send(message);

            // Send notification to admin
            SimpleMailMessage adminMessage = new SimpleMailMessage();
            adminMessage.setTo("pkumarsaini178@gmail.com");
            adminMessage.setSubject("✦ New Order Received — KSleep Admin");
            adminMessage.setText(
                    "Hello Admin,\n\n" +
                            "A new order has been placed successfully!\n\n" +
                            "Customer Name   : " + username + "\n" +
                            "Customer Email  : " + finalEmail + "\n" +
                            "Mobile Number   : " + mobile + "\n" +
                            "Product Name    : " + pName + "\n" +
                            "Price           : ₹" + pprice + "\n" +
                            "Quantity        : " + qty + "\n" +
                            "Delivery Address: " + address + ", " + city + ", " + state + " - " + pincode + "\n\n" +
                            "Manage all orders at: http://localhost:1234/admin/orders"
            );
            emailsender.send(adminMessage);
        } catch (Exception e) {
            System.out.println("Email error: " + e.getMessage());
        }

        return "redirect:/order.html"; // ✅ go to My Orders page
    }

    @GetMapping("/orderDeatail")
    @ResponseBody
    public List<orderEntity> orderDetail(HttpSession session) {

        // ✅ reads email that was set during login
        String email = (String) session.getAttribute("userEmail");

        if (email == null) {
            return List.of(); // not logged in — return empty
        }

        // ✅ returns ONLY this user's orders
        return or.findByEmail(email);
    }

    @PostMapping("/cancelOrder")
    @ResponseBody
    public String cancelOrder(@RequestParam Long id,
            @RequestParam String reason,
            @RequestParam(required = false) String comment) {

        orderEntity order = or.findById(id).orElse(null);

        if (order == null) {
            return "Order not found";
        }

        /* order details */

        String name = order.getCustomerName();
        String email = order.getEmail();
        String productname = order.getProductName();
        Long productid = order.getProductId();
        String address = order.getAddress();
        double price = order.getPrice();
        String mobileno = order.getMobile_No();
        String date = order.getOrderDate().toString();

        /* mail content */

        String detail = "Hello " + name + ",\n\n" +
                "Your order has been cancelled successfully.\n\n" +

                "Product Details:\n" +
                "Product Name : " + productname + "\n" +
                "Product ID : " + productid + "\n" +
                "Price : ₹" + price + "\n\n" +

                "Delivery Address:\n" +
                address + "\n\n" +
                "Mobile_no :" + mobileno +

                "Order Date : " + date + "\n\n" +

                "Cancellation Reason : " + reason + "\n" +
                "Additional Comment : " + comment + "\n\n" +

                "";

        /* send mail */

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Order Cancellation Confirmation");
        message.setText(detail);

        emailsender.send(message);

        /* delete order */

        or.deleteById(id);

        return "Order cancelled successfully";
    }

    @PostMapping("/sendContactEmail")
    public String sendemailforcontect(@RequestParam String name, @RequestParam String email, @RequestParam long phone,
            @RequestParam String type, @RequestParam String message) {
        SimpleMailMessage ms = new SimpleMailMessage();

        String contectDeatails = ""
                + "Customer_Name:" + name +
                "Customer_email" + email +
                "Customer_phone_no." + phone +
                "Question Type" + type +
                "Message of Customer " + message +
                "";
        ms.setTo("pkumarsaini178@gmail.com");
        ms.setSubject(type + " Information regarding ");
        ms.setText(contectDeatails);
        emailsender.send(ms);

        // redirect:/myorder.html"
        return "redirect:/index.html";
    }

    // ===============================
    // ADMIN DASHBOARD & MANAGEMENT PAGES (THYMELEAF)
    // ===============================

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        long totalCustomers = sr.countByRole("CUSTOMER");
        long totalOrders = or.count();
        double totalRevenue = or.findAll().stream()
                .filter(o -> !"cancelled".equalsIgnoreCase(o.getStatus()))
                .mapToDouble(o -> o.getPrice() * o.getQuantity())
                .sum();
        
        List<orderEntity> recentOrders = or.findAll().stream()
                .sorted((o1, o2) -> {
                    if (o1.getOrderDate() == null || o2.getOrderDate() == null) return 0;
                    return o2.getOrderDate().compareTo(o1.getOrderDate());
                })
                .limit(5)
                .toList();

        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("recentOrders", recentOrders);
        return "admin/dashboard";
    }

    @GetMapping("/admin/products")
    public String adminProducts(Model model) {
        model.addAttribute("products", pr.findAll());
        return "admin/products";
    }

    @GetMapping("/admin/orders")
    public String adminOrders(Model model) {
        model.addAttribute("orders", or.findAll());
        return "admin/orders";
    }

    @GetMapping("/admin/customers")
    public String adminCustomers(Model model) {
        List<Entitysignup> customers = sr.findByRole("CUSTOMER");
        List<userEntity> profiles = ur.findAll();
        
        // Map email to userEntity profile (for reliable, case-insensitive matching in Thymeleaf)
        Map<String, userEntity> profileMap = new HashMap<>();
        for (userEntity profile : profiles) {
            if (profile.getEmail() != null) {
                profileMap.put(profile.getEmail().toLowerCase(), profile);
            }
        }
        
        model.addAttribute("customers", customers);
        model.addAttribute("profileMap", profileMap);
        return "admin/customers";
    }

    @GetMapping("/admin/analytics")
    public String adminAnalytics() {
        return "admin/analytics";
    }

    @GetMapping("/admin/reports")
    public String adminReports(Model model) {
        long totalCustomers = sr.countByRole("CUSTOMER");
        long totalOrders = or.count();
        double totalRevenue = or.findAll().stream()
                .filter(o -> !"cancelled".equalsIgnoreCase(o.getStatus()))
                .mapToDouble(o -> o.getPrice() * o.getQuantity())
                .sum();
        
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("orders", or.findAll());
        return "admin/reports";
    }

    // ===============================
    // PRODUCT CRUD OPERATIONS (ADMIN)
    // ===============================

    @PostMapping("/admin/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        pr.deleteById(id);
        return "redirect:/admin/products";
    }

    @PostMapping("/admin/products/update")
    public String updateProduct(
            @RequestParam Long id,
            @RequestParam String productName,
            @RequestParam double price,
            @RequestParam String material,
            @RequestParam String comfort,
            @RequestParam String description,
            @RequestParam(value = "image1", required = false) MultipartFile file1,
            @RequestParam(value = "image2", required = false) MultipartFile file2,
            @RequestParam(value = "image3", required = false) MultipartFile file3,
            @RequestParam(value = "image4", required = false) MultipartFile file4,
            @RequestParam(value = "image5", required = false) MultipartFile file5,
            @RequestParam(value = "imageUrl1", required = false) String imageUrl1,
            @RequestParam(value = "imageUrl2", required = false) String imageUrl2,
            @RequestParam(value = "imageUrl3", required = false) String imageUrl3,
            @RequestParam(value = "imageUrl4", required = false) String imageUrl4,
            @RequestParam(value = "imageUrl5", required = false) String imageUrl5) throws IOException {

        prodectentity pe = pr.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid product Id:" + id));
        pe.setProductName(productName);
        pe.setPrice(price);
        pe.setMaterial(material);
        pe.setComfortLevel(comfort);
        pe.setProductDescription(description);

        Files.createDirectories(Paths.get(uploadDir));

        String img1Name = processImageInput(file1, imageUrl1);
        if (img1Name != null) pe.setImage1(img1Name);

        String img2Name = processImageInput(file2, imageUrl2);
        if (img2Name != null) pe.setImage2(img2Name);

        String img3Name = processImageInput(file3, imageUrl3);
        if (img3Name != null) pe.setImage3(img3Name);

        String img4Name = processImageInput(file4, imageUrl4);
        if (img4Name != null) pe.setImage4(img4Name);

        String img5Name = processImageInput(file5, imageUrl5);
        if (img5Name != null) pe.setImage5(img5Name);

        pr.save(pe);
        return "redirect:/admin/products";
    }

    // ===============================
    // ORDER LIFE CYCLE (ADMIN)
    // ===============================

    @PostMapping("/admin/orders/update-status")
    public String updateOrderStatus(@RequestParam Long id, @RequestParam String status) {
        orderEntity order = or.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid order Id:" + id));
        order.setStatus(status);
        or.save(order);
        return "redirect:/admin/orders";
    }

    // ===============================
    // BUSINESS ANALYTICS REST APIS
    // ===============================

    @GetMapping("/admin/api/analytics/summary")
    @ResponseBody
    public Map<String, Object> getAnalyticsSummary() {
        Map<String, Object> data = new HashMap<>();
        
        long totalCustomers = sr.countByRole("CUSTOMER");
        long totalOrders = or.count();
        double totalRevenue = or.findAll().stream()
                .filter(o -> !"cancelled".equalsIgnoreCase(o.getStatus()))
                .mapToDouble(o -> o.getPrice() * o.getQuantity())
                .sum();
            
        data.put("totalCustomers", totalCustomers);
        data.put("totalOrders", totalOrders);
        data.put("totalRevenue", totalRevenue);
        
        // Group by product name for quantities
        Map<String, Integer> productQuantities = or.findAll().stream()
                .collect(Collectors.groupingBy(orderEntity::getProductName, Collectors.summingInt(orderEntity::getQuantity)));

        // Sort to get top 5 products (most purchased)
        List<Map<String, Object>> topProducts = productQuantities.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("productName", entry.getKey());
                    m.put("quantity", entry.getValue());
                    return m;
                }).toList();
        data.put("mostPurchasedProducts", topProducts);

        // Sort to get bottom 5 products (least purchased)
        List<Map<String, Object>> leastProducts = productQuantities.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("productName", entry.getKey());
                    m.put("quantity", entry.getValue());
                    return m;
                }).toList();
        data.put("leastPurchasedProducts", leastProducts);
        
        return data;
    }

    @GetMapping("/admin/api/analytics/monthly-sales")
    @ResponseBody
    public List<Map<String, Object>> getMonthlySales() {
        Map<String, Double> monthlyRevenue = or.findAll().stream()
                .filter(o -> o.getOrderDate() != null && !"cancelled".equalsIgnoreCase(o.getStatus()))
                .collect(Collectors.groupingBy(
                        o -> o.getOrderDate().getMonth().toString(),
                        Collectors.summingDouble(o -> o.getPrice() * o.getQuantity())
                ));
            
        return monthlyRevenue.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("month", entry.getKey());
                    m.put("revenue", entry.getValue());
                    return m;
                }).toList();
    }

    private String processImageInput(MultipartFile file, String imageUrl) throws IOException {
        if (file != null && !file.isEmpty()) {
            String imgName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            file.transferTo(Paths.get(uploadDir + imgName));
            return imgName;
        } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return saveImageFromUrl(imageUrl);
        }
        return null;
    }

    private String saveImageFromUrl(String urlString) {
        try {
            if (urlString == null || urlString.trim().isEmpty()) {
                return null;
            }
            URL url = new URL(urlString);
            String extension = ".jpg";
            
            String path = url.getPath();
            if (path.contains(".")) {
                String ext = path.substring(path.lastIndexOf(".")).toLowerCase();
                if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".gif") || ext.equals(".webp")) {
                    extension = ext;
                }
            }
            
            String imgName = UUID.randomUUID() + extension;
            Files.createDirectories(Paths.get(uploadDir));
            
            try (InputStream in = url.openStream()) {
                Files.copy(in, Paths.get(uploadDir + imgName), StandardCopyOption.REPLACE_EXISTING);
            }
            return imgName;
        } catch (Exception e) {
            System.out.println("Error downloading image from URL: " + e.getMessage());
            return null;
        }
    }
}
