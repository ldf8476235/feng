package com.yyds.feng.alpha.service;

import com.yyds.feng.alpha.entity.UserDefaultSource;

import java.util.List;

public interface UserDefaultSourceService {
    void saveDefaultSource(UserDefaultSource source);

    List<UserDefaultSource> listAll();

    /**
     * 获取用户的默认分数
     *
     * @param username 用户名
     * @return 默认分数，未设置则返回 null
     */
    Integer getDefaultSource(String username);
}
