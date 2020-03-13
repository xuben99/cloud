package cn.xuben99.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * (User)用户实体类
 *
 * @author 莫提
 * @since 2020-02-25 17:19:04
 */
@AllArgsConstructor
@Data
@Builder
public class User implements Serializable {
    /**
    * 用户ID
    */
    private Integer userId;
    /**
    * 用户的账号
    */
    private String user_account;
    /**
     * 用户的密码
     */
    private String user_password;
    /**
    * 文件仓库ID
    */
    private Integer fileStoreId;
    /**
    * 用户名
    */
    private String userName;
    /**
    * 注册时间
    */
    private Date registerTime;


}