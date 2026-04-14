package com.hmoob.doc.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.doc.domain.KbSearchRecord;
import com.hmoob.doc.domain.vo.KbSearchRecordVo;

import java.util.List;

/**
 * KB搜索记录 Mapper接口
 *
 * @author hmoob
 */
public interface KbSearchRecordMapper extends BaseMapperPlus<KbSearchRecord, KbSearchRecordVo> {

    /**
     * 根据用户ID查询搜索记录列表
     *
     * @param userId 用户ID
     * @return 搜索记录列表
     */
    default List<KbSearchRecordVo> selectByUserId(Long userId) {
        return this.selectVoList(new LambdaQueryWrapper<KbSearchRecord>()
            .eq(KbSearchRecord::getUserId, userId)
            .orderByDesc(KbSearchRecord::getCreateTime));
    }

    /**
     * 查询热门搜索关键词
     *
     * @param limit 数量限制
     * @return 搜索记录列表
     */
    default List<KbSearchRecordVo> selectHotKeywords(int limit) {
        return this.selectVoList(new LambdaQueryWrapper<KbSearchRecord>()
            .isNotNull(KbSearchRecord::getKeywords)
            .groupBy(KbSearchRecord::getKeywords)
            .orderByDesc(KbSearchRecord::getCreateTime)
            .last("LIMIT " + limit));
    }

}