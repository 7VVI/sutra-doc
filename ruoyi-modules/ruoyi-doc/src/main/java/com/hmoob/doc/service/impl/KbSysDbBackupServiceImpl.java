package com.hmoob.doc.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import com.hmoob.common.core.constant.SystemConstants;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.common.core.utils.StringUtils;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.doc.domain.KbSysDbBackup;
import com.hmoob.doc.domain.bo.KbSysDbBackupBo;
import com.hmoob.doc.domain.vo.KbSysDbBackupVo;
import com.hmoob.doc.mapper.KbSysDbBackupMapper;
import com.hmoob.doc.service.IKbSysDbBackupService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * KB系统数据库备份 服务实现
 *
 * @author hmoob
 */
@RequiredArgsConstructor
@Service
public class KbSysDbBackupServiceImpl implements IKbSysDbBackupService {

    private final KbSysDbBackupMapper baseMapper;

    /**
     * 分页查询数据库备份数据
     *
     * @param bo        备份信息
     * @param pageQuery 分页对象
     * @return 备份信息集合
     */
    @Override
    public TableDataInfo<KbSysDbBackupVo> selectPageBackupList(KbSysDbBackupBo bo, PageQuery pageQuery) {
        Page<KbSysDbBackupVo> page = baseMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return TableDataInfo.build(page);
    }

    /**
     * 查询数据库备份数据
     *
     * @param bo 备份信息
     * @return 备份信息集合
     */
    @Override
    public List<KbSysDbBackupVo> selectBackupList(KbSysDbBackupBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 构建查询条件
     *
     * @param bo 备份信息
     * @return 查询条件
     */
    private LambdaQueryWrapper<KbSysDbBackup> buildQueryWrapper(KbSysDbBackupBo bo) {
        LambdaQueryWrapper<KbSysDbBackup> lqw = Wrappers.lambdaQuery();
        lqw.eq(KbSysDbBackup::getDelFlag, SystemConstants.NORMAL);
        lqw.eq(ObjectUtil.isNotNull(bo.getBackupId()), KbSysDbBackup::getBackupId, bo.getBackupId());
        lqw.like(StringUtils.isNotBlank(bo.getFileName()), KbSysDbBackup::getFileName, bo.getFileName());
        lqw.eq(ObjectUtil.isNotNull(bo.getBackupType()), KbSysDbBackup::getBackupType, bo.getBackupType());
        lqw.eq(ObjectUtil.isNotNull(bo.getBackupStatus()), KbSysDbBackup::getBackupStatus, bo.getBackupStatus());
        lqw.orderByDesc(KbSysDbBackup::getCreateTime);
        return lqw;
    }

    /**
     * 查询最新备份记录
     *
     * @return 最新备份信息
     */
    @Override
    public KbSysDbBackupVo selectLatestBackup() {
        return baseMapper.selectLatestBackup();
    }

    /**
     * 根据备份ID查询信息
     *
     * @param backupId 备份ID
     * @return 备份信息
     */
    @Override
    public KbSysDbBackupVo selectBackupById(Long backupId) {
        return baseMapper.selectVoById(backupId);
    }

    /**
     * 新增保存备份记录信息
     *
     * @param bo 备份信息
     * @return 结果
     */
    @Override
    public int insertBackup(KbSysDbBackupBo bo) {
        KbSysDbBackup backup = MapstructUtils.convert(bo, KbSysDbBackup.class);
        // 设置初始值
        if (backup.getBackupStatus() == null) {
            backup.setBackupStatus(0); // 默认进行中
        }
        return baseMapper.insert(backup);
    }

    /**
     * 更新备份状态
     *
     * @param backupId 备份ID
     * @param status   备份状态: 0-进行中, 1-成功, 2-失败
     * @param errorMsg 错误信息
     * @return 结果
     */
    @Override
    public int updateBackupStatus(Long backupId, Integer status, String errorMsg) {
        LambdaUpdateWrapper<KbSysDbBackup> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(KbSysDbBackup::getBackupId, backupId);
        wrapper.set(KbSysDbBackup::getBackupStatus, status);
        wrapper.set(StringUtils.isNotBlank(errorMsg), KbSysDbBackup::getErrorMsg, errorMsg);
        return baseMapper.update(null, wrapper);
    }

    /**
     * 删除备份记录信息
     *
     * @param backupId 备份ID
     * @return 结果
     */
    @Override
    public int deleteBackupById(Long backupId) {
        return baseMapper.deleteById(backupId);
    }

}