package com.yyds.feng.op.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yyds.feng.common.entity.WalletWeeklySnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WalletWeeklySnapshotMapper extends BaseMapper<WalletWeeklySnapshot> {
    int insertBatch(@Param("list") List<WalletWeeklySnapshot> list);
}
