package com.hmoob.doc.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.doc.domain.KbSysConfig;
import com.hmoob.doc.domain.vo.KbSysConfigVo;

import java.util.List;

/**
 * KB系统配置 Mapper接口
 *
 * @author hmoob
 */
public interface KbSysConfigMapper extends BaseMapperPlus<KbSysConfig, KbSysConfigVo> {

    /**
     * 根据配置项编码查询配置
     *
     * @param itemCode 配置项编码
     * @return 配置Vo对象
     */
    default KbSysConfigVo selectByItemCode(String itemCode) {
        return this.selectVoOne(new LambdaQueryWrapper<KbSysConfig>()
            .eq(KbSysConfig::getItemCode, itemCode)
            .eq(KbSysConfig::getStatus, 1));
    }

    /**
     * 根据配置分组查询配置列表
     *
     * @param itemGroup 配置分组
     * @return 配置列表
     */
    default List<KbSysConfigVo> selectByItemGroup(String itemGroup) {
        return this.selectVoList(new LambdaQueryWrapper<KbSysConfig>()
            .eq(KbSysConfig::getItemGroup, itemGroup)
            .eq(KbSysConfig::getStatus, 1)
            .orderByAsc(KbSysConfig::getSortOrder));
    }

}