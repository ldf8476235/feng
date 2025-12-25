package com.yyds.feng.op.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yyds.feng.common.entity.BscMatcherSyncStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BscMatcherSyncStatusMapper extends BaseMapper<BscMatcherSyncStatus> {
    @Select("SELECT id, last_block AS lastBlock, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM bsc_matcher_sync_status ORDER BY id DESC LIMIT 1")
    BscMatcherSyncStatus selectLatest();

    @Insert("INSERT INTO bsc_matcher_sync_status (last_block) VALUES (#{lastBlock})")
    int insertStatus(@Param("lastBlock") long lastBlock);

    @Update("UPDATE bsc_matcher_sync_status SET last_block = #{lastBlock} WHERE id = #{id}")
    int updateLastBlock(@Param("id") Long id, @Param("lastBlock") long lastBlock);
}
