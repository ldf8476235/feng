package com.yyds.feng.alpha.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yyds.feng.alpha.entity.UserDefaultSource;
import com.yyds.feng.alpha.mapper.UserDefaultSourceMapper;
import com.yyds.feng.alpha.service.UserDefaultSourceService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class UserDefaultSourceServiceImpl implements UserDefaultSourceService {

    @Resource
    private UserDefaultSourceMapper userDefaultSourceMapper;

    @Override
    public void saveDefaultSource(UserDefaultSource source) {
        QueryWrapper<UserDefaultSource> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", source.getUsername());

        UserDefaultSource exist = userDefaultSourceMapper.selectOne(queryWrapper);
        if (exist != null) {
            source.setId(exist.getId());
            userDefaultSourceMapper.updateById(source);
        } else {
            userDefaultSourceMapper.insert(source);
        }
    }

    @Override
    public List<UserDefaultSource> listAll() {
        return userDefaultSourceMapper.selectList(null);
    }

    @Override
    public Integer getDefaultSource(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        QueryWrapper<UserDefaultSource> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username.trim());

        UserDefaultSource record = userDefaultSourceMapper.selectOne(queryWrapper);
        return record != null ? record.getDefaultSource() : null;
    }
}
