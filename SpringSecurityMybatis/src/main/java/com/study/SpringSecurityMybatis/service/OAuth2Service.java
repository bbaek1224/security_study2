package com.study.SpringSecurityMybatis.service;

import com.study.SpringSecurityMybatis.dto.request.ReqOAuth2SignupDto;
import com.study.SpringSecurityMybatis.dto.response.RespSignupDto;
import com.study.SpringSecurityMybatis.entity.Role;
import com.study.SpringSecurityMybatis.entity.User;
import com.study.SpringSecurityMybatis.entity.UserRoles;
import com.study.SpringSecurityMybatis.exception.SignupException;
import com.study.SpringSecurityMybatis.repository.OAuth2UserMapper;
import com.study.SpringSecurityMybatis.repository.RoleMapper;
import com.study.SpringSecurityMybatis.repository.UserMapper;
import com.study.SpringSecurityMybatis.repository.UserRolesMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class OAuth2Service implements OAuth2UserService {

    @Autowired
    private DefaultOAuth2UserService defaultOAuth2UserService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRolesMapper userRolesMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private OAuth2UserMapper oAuth2UserMapper;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = defaultOAuth2UserService.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();
        Map<String, Object> OAuth2Attributes = new HashMap<>();
        OAuth2Attributes.put("provider", userRequest.getClientRegistration().getClientName());

        switch(userRequest.getClientRegistration().getClientName()) {
            case "Google":
                OAuth2Attributes.put("id", attributes.get("sub").toString());
                break;
            case "Naver":
                attributes = (Map<String, Object>) attributes.get("response");
                OAuth2Attributes.put("id", attributes.get("id").toString());
                break;
            case "Kakao":
                OAuth2Attributes.put("id", attributes.get("id").toString());
                break;
        }

        return new DefaultOAuth2User(new HashSet<>(), OAuth2Attributes, "id");
    }

    public void merge(com.study.SpringSecurityMybatis.entity.OAuth2User oAuth2User) {
        oAuth2UserMapper.save(oAuth2User);
    }

    @Transactional(rollbackFor = SignupException.class) // 예외가 터지면 바로 rollback 한다.
    public void signup(ReqOAuth2SignupDto dto) throws SignupException {
        User user = null;

        user = dto.toEntity(passwordEncoder);
        userMapper.save(user);

        Role role = roleMapper.findByName("ROLE_USER");
        if(role == null) {
            role = Role.builder().name("ROLE_USER").build();
            roleMapper.save(role);
        }

        UserRoles userRoles = UserRoles.builder()
                .userId(user.getId())
                .roleId(role.getId())
                .build();

        userRolesMapper.save(userRoles);

        com.study.SpringSecurityMybatis.entity.OAuth2User oAuth2User = com.study.SpringSecurityMybatis.entity.OAuth2User.builder()
                .userId(user.getId())
                .oAuth2Name(dto.getOauth2Name())
                .provider(dto.getProvider())
                .build();

        oAuth2UserMapper.save(oAuth2User);
    }


}
