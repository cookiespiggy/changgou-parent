package com.changgou.oauth.config;

import com.changgou.oauth.util.UserJwt;
import com.changgou.user.feign.UserFeign;
import entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/*****
 * 自定义授权认证类
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    ClientDetailsService clientDetailsService;

    @Autowired
    private UserFeign userFeign;

    /****
     * 自定义授权认证
     * @param username
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        //=========================客户端信息认证  开始=============================
        //取出身份，如果身份为空说明没有认证
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        //没有认证统一采用httpbasic认证，httpbasic中存储了client_id和client_secret，开始认证client_id和client_secret
        if(authentication==null){
            ClientDetails clientDetails = clientDetailsService.loadClientByClientId(username);
            if(clientDetails!=null){
                //秘钥
                String clientSecret = clientDetails.getClientSecret();
                //静态方式
                //return new User(username,new BCryptPasswordEncoder().encode(clientSecret), AuthorityUtils.commaSeparatedStringToAuthorityList(""));
                //数据库查找方式
                /*return new User(
                        username,  //客户端ID
                        new BCryptPasswordEncoder().encode(clientSecret),  //客户端密钥
                        AuthorityUtils.commaSeparatedStringToAuthorityList(""));  //权限*/
                //数据库查找方式
                return new User(
                        username,
                        clientSecret,  //数据库中已加密,不用再加密
                        AuthorityUtils.commaSeparatedStringToAuthorityList(""));
            }
        }
        //=========================客户端信息认证  结束=============================

        //=========================用户账户密码信息认证  开始=============================
        if (StringUtils.isEmpty(username)) {
            return null;
        }

        //从数据库中加载用户信息
        Result<com.changgou.user.pojo.User> userResult = userFeign.findById(username);

        if (userResult==null || userResult.getData()==null){
            return null;
        }

        //根据用户名查询用户信息
        String pwd = userResult.getData().getPassword();
        //创建User对象
        String permissions = "admin";  //给创建的用户授权
        UserJwt userDetails = new UserJwt(username,pwd,AuthorityUtils.commaSeparatedStringToAuthorityList(permissions));
        //=========================用户账户密码信息认证  结束=============================
        return userDetails;
    }
}
