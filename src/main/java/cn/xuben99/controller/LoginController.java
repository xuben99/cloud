package cn.xuben99.controller;

import cn.xuben99.entity.User;
import cn.xuben99.utils.LogUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;


/**
 * @Description 登录控制器
 * @Author xw
 * @Date 18:19 2020/2/25
 * @Param  * @param null
 * @return
 **/
@Controller
public class LoginController extends BaseController {

    private Logger logger = LogUtils.getInstance(LoginController.class);

    /**
     * @Description 免登陆用户入口
     * @Author xw
     * @Date 15:17 2020/2/26
     * @Param []
     * @return java.lang.String
     **/
    @GetMapping("/admin")
    public String adminLogin(){
        User user = userService.getUserByOpenId("123456789");
        logger.info("登录成功！"+user);
        session.setAttribute("loginUser", user);
        return "redirect:/index";
    }

    /**
     * @Description 请求QQ登录
     * @Author xw
     * @Date 18:27 2020/2/25
     * @Param []
     * @return void
     **/
    @RequestMapping("/login")
    public String login(User u){

        response.setContentType("text/html;charset=utf-8");
            logger.info("请求登录");
            User user =  null;
            user = userService.cheakUser(u.getUser_account(),u.getUser_password());
          if(user!=null){
              logger.info("用户登录成功！"+user);
              session.setAttribute("loginUser", user);
              return "redirect:/index";
          }

        return "redirect:/";

    }

    /**
     * @Description QQ登录回调地址
     * @Author xw
     * @Date 18:27 2020/2/25
     * @Param []
     * @return java.lang.String
     **/



    /**
     * @Description 退出登录，清空session
     * @Author xuben
     * @Date 18:26 2020/2/25
     * @Param []
     * @return java.lang.String
     **/
    @GetMapping("/logout")
    public String logout() {
        logger.info("用户退出登录！");
        session.invalidate();
        return "redirect:/";
    }

}
