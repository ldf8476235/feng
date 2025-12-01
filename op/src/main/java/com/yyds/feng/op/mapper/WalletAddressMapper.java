package com.yyds.feng.op.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yyds.feng.common.entity.WalletAddress;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WalletAddressMapper extends BaseMapper<WalletAddress> {

    // MySQL INSERT IGNORE 防止重复
    @Insert("INSERT IGNORE INTO wallet_address(wallet) VALUES(#{wallet})")
    int insertIgnore(String wallet);

    @Select("SELECT wallet FROM wallet_address")
    List<String> selectAllWallets();

    int insertBatch(List<String> wallets);
}