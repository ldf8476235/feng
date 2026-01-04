package com.yyds.feng.op.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yyds.feng.common.entity.BscMatcherTx;
import com.yyds.feng.op.dto.BscMatcherTxDailySummaryRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BscMatcherTxMapper extends BaseMapper<BscMatcherTx> {
    int insertBatchIgnore(@Param("list") List<BscMatcherTx> list);

    List<BscMatcherTxDailySummaryRow> selectDailySummaryByAddresses(@Param("addresses") List<String> addresses);
}
