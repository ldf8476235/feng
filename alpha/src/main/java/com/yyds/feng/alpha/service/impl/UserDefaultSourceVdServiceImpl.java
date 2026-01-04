package com.yyds.feng.alpha.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yyds.feng.alpha.entity.UserDefaultSourceVd;
import com.yyds.feng.alpha.mapper.UserDefaultSourceVdMapper;
import com.yyds.feng.alpha.service.UserDefaultSourceVdService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class UserDefaultSourceVdServiceImpl implements UserDefaultSourceVdService {

    @Resource
    private UserDefaultSourceVdMapper userDefaultSourceMapper;

    @Override
    public void saveDefaultSource(UserDefaultSourceVd source) {
        QueryWrapper<UserDefaultSourceVd> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", source.getUsername());

        UserDefaultSourceVd exist = userDefaultSourceMapper.selectOne(queryWrapper);
        if (exist != null) {
            source.setId(exist.getId());
            userDefaultSourceMapper.updateById(source);
        } else {
            userDefaultSourceMapper.insert(source);
        }
    }

    @Override
    public List<UserDefaultSourceVd> listAll() {
        return userDefaultSourceMapper.selectList(null);
    }

    @Override
    public Integer getDefaultSource(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        QueryWrapper<UserDefaultSourceVd> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username.trim());

        UserDefaultSourceVd record = userDefaultSourceMapper.selectOne(queryWrapper);
        return record != null ? record.getDefaultSource() : null;
    }
}
