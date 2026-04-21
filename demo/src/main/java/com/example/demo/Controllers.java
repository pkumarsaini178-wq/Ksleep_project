package com.example.demo;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

     
         public static final String UPLOAD_DIR =
            "C:/Users/pankaj kumar saini/Documents/Ecommerce/images/";

     @GetMapping("starting")
     public String firstpage(){
        return "redirect:/index.html";
     }
        @GetMapping("/saveAdmin")
    public String insetdata(@RequestParam String name,
                            @RequestParam String email,
                            @RequestParam Long mo) {

        List<EntityFile> username = er.findByName(name);
        List<EntityFile> e = er.findByEmail(email);

        if(!username.isEmpty() && !e.isEmpty()) {
            return "redirect:/prodectadd.html";
        }

        return "index.html";
    }
//value = "/CompanyDeatailes",
       // consumes = MediaType.MULTIPART_FORM_DATA_VALUE
     @PostMapping(value = "/insertproductdata",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String productDetail(
            @RequestParam String productName,
            @RequestParam double price,
            @RequestParam String material,
            @RequestParam String comfort,
            @RequestParam String description,
            @RequestParam("image1") MultipartFile file1,
            @RequestParam("image2") MultipartFile file2,
            @RequestParam("image3") MultipartFile file3,
            @RequestParam("image4") MultipartFile file4,
            @RequestParam("image5") MultipartFile file5
    ) throws IOException {

        Files.createDirectories(Paths.get(UPLOAD_DIR));

        String img1Name = UUID.randomUUID() + "_" + file1.getOriginalFilename();
        String img2Name = UUID.randomUUID() + "_" + file2.getOriginalFilename();
        String img3Name = UUID.randomUUID() + "_" + file3.getOriginalFilename();
        String img4Name = UUID.randomUUID() + "_" + file4.getOriginalFilename();
        String img5Name = UUID.randomUUID() + "_" + file5.getOriginalFilename();

        file1.transferTo(Paths.get(UPLOAD_DIR + img1Name));
        file2.transferTo(Paths.get(UPLOAD_DIR + img2Name));
        file3.transferTo(Paths.get(UPLOAD_DIR + img3Name));
        file4.transferTo(Paths.get(UPLOAD_DIR + img4Name));
        file5.transferTo(Paths.get(UPLOAD_DIR + img5Name));

        prodectentity pe = new prodectentity();
        pe.setProductName(productName);
        pe.setPrice(price);
        pe.setMaterial(material);
        pe.setComfortLevel(comfort);
        pe.setProductDescription(description);

        pe.setImage1(img1Name);
        pe.setImage2(img2Name);
        pe.setImage3(img3Name);
        pe.setImage4(img4Name);
        pe.setImage5(img5Name);

        pr.save(pe);

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

        product.setImage1("http://localhost:1234/images/" + product.getImage1());

        return product;

    }).toList();
}@GetMapping("/fechdata/{id}")
@ResponseBody
public prodectentity getProductById(@PathVariable Long id) {

    prodectentity product = pr.findById(id).orElse(null);

    if (product != null) {
        product.setImage1("http://localhost:1234/images/" + product.getImage1());
        product.setImage2("http://localhost:1234/images/" + product.getImage2());
        product.setImage3("http://localhost:1234/images/" + product.getImage3());
        product.setImage4("http://localhost:1234/images/" + product.getImage4());
        product.setImage5("http://localhost:1234/images/" + product.getImage5());
    }

    return product;
}

    // ===============================
    // FETCH SINGLE IMAGE
    // ===============================
   @GetMapping("/images/{imageName}")
public ResponseEntity<Resource> getImage(@PathVariable String imageName) throws IOException {

    Path imagePath = Paths.get(UPLOAD_DIR).resolve(imageName);
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
}@PostMapping("/senddeatailEmail")
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

        if(loginEmail == null)
        {
            return "redirect:/login.html";
        }

        userEntity ue = new userEntity();

        ue.setUser_name(username);
        ue.setEmail(loginEmail);   // session email
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

    } catch(Exception e) {

        return "redirect:/index.html";
    }
}
  @PostMapping("signup")
  public String sinuppage(@RequestParam String username,@RequestParam String email, @RequestParam String password){

     Entitysignup es=new Entitysignup();
     es.setName(username);
     es.setEmail(email);
     es.setPassword(password);
     sr.save(es);
     return "redirect:/login.html";
  }
@PostMapping("/loginpage")
public String loginpage(
        @RequestParam String email,
        @RequestParam String password,
        @RequestParam int id,
        HttpSession session)
{
    try {

        List<Entitysignup> user = sr.findByEmailAndPassword(email, password);

        if(!user.isEmpty())
        {
            session.setAttribute("userEmail", email);

            return "redirect:/fulldeatailprodect.html?id=" + id;
        }
        else
        {
            return "redirect:/login.html";
        }

    } 
    catch(Exception e) {

        return "redirect:/login.html";
    }
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

    orderEntity order = new orderEntity();
    order.setCustomerName(username);
    order.setEmail(sessionEmail);          // ✅ session email links to My Orders
    order.setMobile_No(mobile);
    order.setAddress(address + ", " + city + ", " + state + " - " + pincode);
    order.setProductName(pName);
    order.setPrice(pprice);
    order.setImage(image);                 // ✅ saves image URL/filename
    order.setProductId(product_id);
    order.setQuantity(qty);                // ✅ fixed field name
    order.setStatus("confirmed");          // ✅ default status

    or.save(order);                        // ✅ save to DB

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
            "Address  : " + address + ", " + city + ", " + state + "\n\n" +
            "Thank you for choosing KSleep ✦\n" +
            "Track your order at: yourdomain.com/myorder.html"
        );
        emailsender.send(message);
    } catch(Exception e) {
        System.out.println("Email error: " + e.getMessage());
    }

    return "redirect:/myorder.html";       // ✅ go to My Orders page
}

   @GetMapping("/orderDeatail")
@ResponseBody
public List<orderEntity> orderDetail(HttpSession session) {

    // ✅ reads email that was set during login
    String email = (String) session.getAttribute("userEmail");

    if (email == null) {
        return List.of();   // not logged in — return empty
    }

    // ✅ returns ONLY this user's orders
    return or.findByEmail(email);
}
  @PostMapping("/cancelOrder")
@ResponseBody
public String cancelOrder(@RequestParam Long id,
                          @RequestParam String reason,
                          @RequestParam(required = false) String comment){

    orderEntity order = or.findById(id).orElse(null);

    if(order == null){
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

    String detail =
            "Hello " + name + ",\n\n" +
            "Your order has been cancelled successfully.\n\n" +

            "Product Details:\n" +
            "Product Name : " + productname + "\n" +
            "Product ID : " + productid + "\n" +
            "Price : ₹" + price + "\n\n" +

            "Delivery Address:\n" +
            address + "\n\n" +
            "Mobile_no :"+mobileno+

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
  public String  sendemailforcontect(@RequestParam String name ,@RequestParam String email,@RequestParam long phone,@RequestParam String type,@RequestParam String message ){
    SimpleMailMessage ms=new SimpleMailMessage();

    String contectDeatails=""
           + "Customer_Name:"+name+
           "Customer_email"+email+
           "Customer_phone_no."+phone+
           "Question Type"+type+
           "Message of Customer "+message+
            "";
    ms.setTo("pkumarsaini178@gmail.com");
    ms.setSubject(type+" Information regarding ");
    ms.setText(contectDeatails);
    emailsender.send(ms);

    //redirect:/myorder.html"
    return "//redirect:/index.html";
}
}

