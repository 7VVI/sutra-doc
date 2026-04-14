package com.hmoob.doc.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.doc.domain.KbSysDbBackup;
import com.hmoob.doc.domain.vo.KbSysDbBackupVo;

import java.util.List;

/**
 * KB系统数据库备份 Mapper接口
 *
 * @author hmoob
 */
public interface KbSysDbBackupMapper extends BaseMapperPlus<KbSysDbBackup, KbSysDbBackupVo> {

    /**
     * 查询最新的备份记录
     *
     * @return 备份Vo对象
     */
    default KbSysDbBackupVo selectLatestBackup() {
        return this.selectVoOne(new LambdaQueryWrapper<KbSysDbBackup>()
            .eq(KbSysDbBackup::getBackupStatus, 1)
            .orderByDesc(KbSysDbBackup::getCreateTime)
            .last("LIMIT 1"));
    }

    /**
     * 根据备份状态查询备份列表
     *
     * @param backupStatus 备份状态
     * @return 备份列表
     */
    default List<KbSysDbBackupVo> selectByBackupStatus(Integer backupStatus) {
        return this.selectVoList(new LambdaQueryWrapper<KbSysDbBackup>()
            .eq(KbSysDbBackup::getBackupStatus, backupStatus)
            .orderByDesc(KbSysDbBackup::getCreateTime));
    }

}