package com.yyds.feng.alpha.service;

import com.yyds.feng.alpha.entity.UserDefaultSourceVd;

import java.util.List;

public interface UserDefaultSourceVdService {
    void saveDefaultSource(UserDefaultSourceVd source);

    List<UserDefaultSourceVd> listAll();

    /**
     * Get default source for a user.
     *
     * @param username username
     * @return default source or null when missing
     */
    Integer getDefaultSource(String username);
}
