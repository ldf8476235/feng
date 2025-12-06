package com.yyds.feng.alpha.service;

import com.yyds.feng.alpha.entity.UserDefaultSource;

import java.util.List;

public interface UserDefaultSourceService {
    void saveDefaultSource(UserDefaultSource source);

    List<UserDefaultSource> listAll();
}
