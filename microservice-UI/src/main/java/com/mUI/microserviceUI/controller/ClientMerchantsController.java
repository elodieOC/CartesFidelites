package com.mUI.microserviceUI.controller;

import com.mUI.microserviceUI.beans.AddShopDTO;
import com.mUI.microserviceUI.beans.MerchantBean;
import com.mUI.microserviceUI.beans.RewardBean;
import com.mUI.microserviceUI.exceptions.CannotAddException;
import com.mUI.microserviceUI.proxies.MicroserviceMerchantsProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * <h2>Controller linking with microservice-merchants</h2>
 */
@Controller
public class ClientMerchantsController {

    @Autowired
    private MicroserviceMerchantsProxy merchantsProxy;


    /*
    ***************************************
    * Merchant List
    * *************************************
     */

    /**
     * <p>show list of possible merchants</p>
     * @param model
     * @return merchants.html
     */
    @GetMapping("/Marchands")
    public String listMerchants(Model model){
        List<MerchantBean> merchantBeanList = merchantsProxy.listMerchants();
        model.addAttribute("merchants", merchantBeanList);
        return "merchants";
    }
    /*
     **************************************
     * Merchant register
     * ************************************
     */


    /**
     * <p>Page that displays a form to register a merchant</p>
     * @param model attribute passed to jsp page
     * @return login page
     */
    @GetMapping("/Marchands/nouvelle-boutique")
    public String registerPage(Model model, HttpServletRequest request) {
        AddShopDTO addShopDTO = new AddShopDTO();
        HttpSession session = request.getSession();
        model.addAttribute("sessionId", session.getAttribute("loggedInUserId"));
        model.addAttribute("addShopDTO", addShopDTO);
        return "register-shop";
    }
//TODO voir comment gérer les catégories (prédéfinies? liste déroulante?
    /**
     * <p>Process called after the submit button is clicked on register page</p>
     * @param addShopDTO merchant being created by DTO
     * @param model attribute passed to jsp page
     * @return page to show depending on result of process
     */
    @PostMapping("/Marchands/add-shop")
    public String saveMerchant(@ModelAttribute("addShopDTO")AddShopDTO addShopDTO, ModelMap model) {
        String toBeReturned;
        try {
            Integer maxP = Integer.parseInt(addShopDTO.getMaxPoints());
            MerchantBean theMerchant = new MerchantBean();
            theMerchant.setUserId(addShopDTO.getUserId());
            theMerchant.setAddress(addShopDTO.getAddress());
            theMerchant.setCategory(addShopDTO.getCategory());
            theMerchant.setEmail(addShopDTO.getEmail());
            theMerchant.setMaxPoints(maxP);
            theMerchant.setMerchantName(addShopDTO.getMerchantName());
            MerchantBean merchantToRegister = merchantsProxy.addShop(theMerchant);
            toBeReturned = "redirect:/Marchands/"+merchantToRegister.getId();
        }
        catch (Exception e){
            e.printStackTrace();
            if(e instanceof CannotAddException){
                model.addAttribute("errorMessage", e.getMessage());
            }
            toBeReturned = "register-shop";
        }
        return toBeReturned;
    }

    /**
     * shows details of particular merchant with its id
     * @param shopId
     * @param model
     * @return merchant-details.html
     */
    @RequestMapping("/Marchands/{shopId}")
    public String merchantDetails(@PathVariable Integer shopId, Model model, HttpServletRequest request){
        HttpSession session = request.getSession();
        MerchantBean merchant = merchantsProxy.showShop(shopId);
        RewardBean rewardCard = new RewardBean();
        rewardCard.setIdMerchant(merchant.getId());
        rewardCard.setMaxPoints(merchant.getMaxPoints());
        Integer userId = (Integer)request.getSession().getAttribute("loggedInUserId");
        rewardCard.setIdUser(userId);
        model.addAttribute("merchant", merchant);
        model.addAttribute("rewardCard", rewardCard);
        model.addAttribute("sessionId", session.getAttribute("loggedInUserId"));
        return "shop-profile";
    }
    /**
     * shows lists of shop with its owner id
     * @param model
     * @return merchant-details.html
     */
    @RequestMapping("/Marchands/MesBoutiques")
    public String shopListByOwner(Model model, HttpServletRequest request){
        List<MerchantBean> allShops = merchantsProxy.listMerchants();
        Integer loggedInUserId = (Integer)request.getSession().getAttribute("loggedInUserId");
        List<MerchantBean> list = new ArrayList<>();
        for(MerchantBean shop:allShops){
            if(loggedInUserId.equals(shop.getUserId())){
                list.add(shop);
            }
        }
        model.addAttribute("shopList", list);
        return "myshops";
    }


    /*
     **************************************
     * Merchant delete
     * ************************************
     */

    /**
     * <p>deletes a merchant when merchant clicks on suppress button</p>
     * @param id
     * @return url depending on function result
     */
    @RequestMapping(value = "/Marchands/delete/{id}", method = RequestMethod.POST)
    public String deleteMerchant(@PathVariable("id") Integer id, HttpServletRequest request){
        HttpSession session = request.getSession();
        Integer userId = (Integer) session.getAttribute("loggedInUserId");
        merchantsProxy.deleteShop(id);
        return "redirect:/Utilisateurs/MonProfil/"+userId;
    }


    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ModelAndView handleMissingParams(MissingServletRequestParameterException ex){
        return new ModelAndView("redirect:/Accueil");
    }

}