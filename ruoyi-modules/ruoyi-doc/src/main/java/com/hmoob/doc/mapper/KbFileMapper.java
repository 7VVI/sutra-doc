package com.hmoob.doc.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.doc.domain.KbFile;
import com.hmoob.doc.domain.vo.KbFileVo;

/**
 * KB文件 Mapper接口
 *
 * @author hmoob
 */
public interface KbFileMapper extends BaseMapperPlus<KbFile, KbFileVo> {

    /**
     * 根据SHA256哈希值查询文件
     *
     * @param sha256 SHA256哈希值
     * @return 文件Vo对象
     */
    default KbFileVo selectBySha256(String sha256) {
        return this.selectVoOne(new LambdaQueryWrapper<KbFile>()
            .eq(KbFile::getSha256, sha256)
            .eq(KbFile::getStatus, 1));
    }

}