package com.hmoob.doc.service;

import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.doc.domain.bo.KbSysDbBackupBo;
import com.hmoob.doc.domain.vo.KbSysDbBackupVo;

import java.util.List;

/**
 * KB系统数据库备份 服务层
 *
 * @author hmoob
 */
public interface IKbSysDbBackupService {

    /**
     * 分页查询数据库备份数据
     *
     * @param bo        备份信息
     * @param pageQuery 分页对象
     * @return 备份信息集合
     */
    TableDataInfo<KbSysDbBackupVo> selectPageBackupList(KbSysDbBackupBo bo, PageQuery pageQuery);

    /**
     * 查询数据库备份数据
     *
     * @param bo 备份信息
     * @return 备份信息集合
     */
    List<KbSysDbBackupVo> selectBackupList(KbSysDbBackupBo bo);

    /**
     * 查询最新备份记录
     *
     * @return 最新备份信息
     */
    KbSysDbBackupVo selectLatestBackup();

    /**
     * 根据备份ID查询信息
     *
     * @param backupId 备份ID
     * @return 备份信息
     */
    KbSysDbBackupVo selectBackupById(Long backupId);

    /**
     * 新增保存备份记录信息
     *
     * @param bo 备份信息
     * @return 结果
     */
    int insertBackup(KbSysDbBackupBo bo);

    /**
     * 更新备份状态
     *
     * @param backupId 备份ID
     * @param status   备份状态: 0-进行中, 1-成功, 2-失败
     * @param errorMsg 错误信息
     * @return 结果
     */
    int updateBackupStatus(Long backupId, Integer status, String errorMsg);

    /**
     * 删除备份记录信息
     *
     * @param backupId 备份ID
     * @return 结果
     */
    int deleteBackupById(Long backupId);

}