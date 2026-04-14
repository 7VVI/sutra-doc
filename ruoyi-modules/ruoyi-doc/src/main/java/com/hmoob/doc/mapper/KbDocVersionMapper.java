package com.hmoob.doc.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.doc.domain.KbDocVersion;
import com.hmoob.doc.domain.vo.KbDocVersionVo;

import java.util.List;

/**
 * KB文档版本 Mapper接口
 *
 * @author hmoob
 */
public interface KbDocVersionMapper extends BaseMapperPlus<KbDocVersion, KbDocVersionVo> {

    /**
     * 根据文档ID和版本号查询版本
     *
     * @param docId    文档ID
     * @param versionNo 版本号
     * @return 版本Vo对象
     */
    default KbDocVersionVo selectByVersionNo(Long docId, Integer versionNo) {
        return this.selectVoOne(new LambdaQueryWrapper<KbDocVersion>()
            .eq(KbDocVersion::getDocId, docId)
            .eq(KbDocVersion::getVersionNo, versionNo));
    }

    /**
     * 根据文档ID查询所有版本列表
     *
     * @param docId 文档ID
     * @return 版本列表
     */
    default List<KbDocVersionVo> selectByDocId(Long docId) {
        return this.selectVoList(new LambdaQueryWrapper<KbDocVersion>()
            .eq(KbDocVersion::getDocId, docId)
            .orderByDesc(KbDocVersion::getVersionNo));
    }

}