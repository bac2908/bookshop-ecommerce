package vn.fs.controller;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.fs.commom.CommomDataService;
import vn.fs.dto.SessionUser;
import vn.fs.entities.*;
import vn.fs.repository.*;

import jakarta.servlet.http.HttpSession;

@Controller
public class ShopController {

    @Autowired private ProductRepository productRepository;
    @Autowired private FavoriteRepository favoriteRepository;
    @Autowired private CommomDataService commomDataService;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private NxbRepository nxbRepository;

    private User getCurrentUser(HttpSession session) {
        Object customer = (session != null) ? session.getAttribute("customer") : null;
        if (customer instanceof User) return (User) customer; //Đoạn này phải là  if (customer instanceof SessionUser) return (SessionUser) customer;
                                                                // vì tại đã lưu SessionUser vào Session
/**
       // Đặt "customer" vào session dưới dạng DTO an toàn
       String loginId = auth.getName();
       User me = userRepository.findByUsername(loginId);
       if (me == null) {
           me = userRepository.findByEmail(loginId);
       }
        if (me != null) {
           SessionUser su = SessionUser.from(me);
           req.getSession().setAttribute("customer", su);  ***điểm đáng chú ý***
       }
 ví dụ như đoạn này trong SuccessHandler thì đã "convert" User sang SessionUser, sau đó lưu vào Session với key là "customer"
**/
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String loginId = auth.getName();
            User byUsername = userRepository.findByUsername(loginId);
            return (byUsername != null) ? byUsername : userRepository.findByEmail(loginId);
        }
        return null;
    }

    @GetMapping(value = "/products")
    public String shop(Model model,
                       Pageable pageable,
                       @RequestParam("page") Optional<Integer> page,
                       @RequestParam("size") Optional<Integer> size,
                       HttpSession session) {
        User current = getCurrentUser(session);
        int currentPage = page.orElse(1);
        int pageSize   = size.orElse(12);

        Page<Product> productPage = findPaginatedActive(PageRequest.of(currentPage - 1, pageSize));
        markFavorite(productPage.getContent(), current);

        int totalPages = productPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }
        commomDataService.commonData(model, current);

        model.addAttribute("products", productPage);
        return "web/shop";
    }

    public Page<Product> findPaginatedActive(Pageable pageable) {
        List<Product> all = productRepository.findAllActive();
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;

        List<Product> list;
        if (all.size() < startItem) {
            list = Collections.emptyList();
        } else {
            int toIndex = Math.min(startItem + pageSize, all.size());
            list = all.subList(startItem, toIndex);
        }
        return new PageImpl<>(list, PageRequest.of(currentPage, pageSize), all.size());
    }

    @GetMapping(value = "/searchProduct")
    public String showsearch(Model model,
                             Pageable pageable,
                             @RequestParam("keyword") String keyword,
                             @RequestParam("size") Optional<Integer> size,
                             @RequestParam("page") Optional<Integer> page,
                             HttpSession session) {

        User current = getCurrentUser(session);
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(12);

        Page<Product> productPage = findPaginatedSearchActive(PageRequest.of(currentPage - 1, pageSize), keyword);
        markFavorite(productPage.getContent(), current);

        int totalPages = productPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }
        commomDataService.commonData(model, current);

        model.addAttribute("products", productPage);
        model.addAttribute("keyword", keyword);
        return "web/shop";
    }

    public Page<Product> findPaginatedSearchActive(Pageable pageable, String keyword) {
        List<Product> all = productRepository.searchProductActive(keyword);

        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;

        List<Product> list;
        if (all.size() < startItem) {
            list = Collections.emptyList();
        } else {
            int toIndex = Math.min(startItem + pageSize, all.size());
            list = all.subList(startItem, toIndex);
        }
        return new PageImpl<>(list, PageRequest.of(currentPage, pageSize), all.size());
    }

    @GetMapping(value = "/productByCategory")
    public String listProductByCategory(Model model,
                                        @RequestParam("id") Long categoryId,
                                        HttpSession session,
                                        RedirectAttributes ra) {

        User current = getCurrentUser(session);
        Long currentUserId = (current != null) ? current.getUserId() : null;

        Category cat = categoryRepository.findRaw(categoryId);
        if (cat == null || Boolean.FALSE.equals(cat.getStatus())) {
            ra.addFlashAttribute("toast", "Thể loại này đang ngừng kinh doanh!");
            return "redirect:/products";
        }

        List<Product> products = productRepository.listProductByCategoryActive(categoryId);
        List<Product> viewList = new ArrayList<>();

        for (Product product : products) {
            Product copy = new Product();
            BeanUtils.copyProperties(product, copy);

            Favorite save = (currentUserId == null) ? null
                    : favoriteRepository.selectSaves(copy.getProductId(), currentUserId);
            copy.favorite = (save != null);
            viewList.add(copy);
        }

        model.addAttribute("products", viewList);
        commomDataService.commonData(model, current);
        return "web/shop";
    }

    @GetMapping(value = "/productByPublisher")
    public String listProductByPublisher(Model model,
                                         @RequestParam("id") Long nxbId,
                                         HttpSession session,
                                         RedirectAttributes ra) {

        User current = getCurrentUser(session);
        Long currentUserId = (current != null) ? current.getUserId() : null;

        NXB nxb = nxbRepository.findRaw(nxbId);
        if (nxb == null || Boolean.FALSE.equals(nxb.getStatus())) {
            ra.addFlashAttribute("toast", "NXB này đang ngừng kinh doanh!");
            return "redirect:/products";
        }

        List<Product> products = productRepository.listProductByNxbActive(nxbId);
        List<Product> view = new ArrayList<>();
        for (Product p : products) {
            Product copy = new Product();
            BeanUtils.copyProperties(p, copy);

            Favorite save = (currentUserId == null) ? null
                    : favoriteRepository.selectSaves(copy.getProductId(), currentUserId);
            copy.favorite = (save != null);
            view.add(copy);
        }

        model.addAttribute("products", view);
        commomDataService.commonData(model, current);
        return "web/shop";
    }

    private void markFavorite(List<Product> products, User user) {
        if (user == null) return;
        for (Product p : products) {
            Favorite f = favoriteRepository.selectSaves(p.getProductId(), user.getUserId());
            p.favorite = (f != null);
        }
    }
}
