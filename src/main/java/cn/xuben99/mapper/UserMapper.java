package cn.xuben99.mapper;

import cn.xuben99.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @InterfaceName: MyFileMapper
 * @Description: 与用户相关的数据库操作
 * @author: xw
 * @date 2020/1/26 21:55
 * @Version: 1.0
 **/
@Mapper
public interface UserMapper {

    /**
     * @Description 添加User
     * @author 莫提
     * @date 2020-02-25 17:19:31
     * @param user 实例对象
     * @return 影响行数
     */
    int insert(User user);
    
    /**
     * @Description 删除User
     * @author 莫提
     * @date 2020-02-25 17:19:31
     * @param userId 主键
     * @return 影响行数
     */
    int deleteById(Integer userId);

    /**
     * @Description 通过ID查询单条数据
     * @author 莫提
     * @date 2020-02-25 17:19:31
     * @param userId 主键
     * @return 实例对象
     */
    User queryById(Integer userId);

    /**
     * @Description  通过openID查询单条数据
     * @Author xw
     * @Date 18:29 2020/2/25
     * @Param [userId]
     * @return com.moti.entity.User
     **/
    User getUserByOpenId(String openId);

    /**
     * @Description 查询全部数据
     * @author 莫提
     * @date 2020-02-25 17:19:31
     * 分页使用MyBatis的插件实现
     * @return 对象列表
     */
    List<User> queryAll();

    /**
     * @Description 实体作为筛选条件查询数据
     * @author 莫提
     * @date 2020-02-25 17:19:31
     * @param user 实例对象
     * @return 对象列表
     */
    List<User> queryAll(User user);

    /**
     * @Description 修改User
     * @author 莫提
     * @date 2020-02-25 17:19:31
     * @return 影响行数
     */
    int update(User user);

    User cheakUser(String user_account,String user_password);

}