package com.hmoob.doc.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import com.hmoob.common.core.exception.ServiceException;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.doc.domain.KbDocVersion;
import com.hmoob.doc.domain.bo.KbDocVersionBo;
import com.hmoob.doc.domain.vo.KbDocVersionVo;
import com.hmoob.doc.mapper.KbDocVersionMapper;
import com.hmoob.doc.service.IKbDocVersionService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * KB文档版本 服务实现
 *
 * @author hmoob
 */
@RequiredArgsConstructor
@Service
public class KbDocVersionServiceImpl implements IKbDocVersionService {

    private final KbDocVersionMapper baseMapper;

    /**
     * 根据文档ID查询版本列表
     *
     * @param docId 文档ID
     * @return 版本列表(按版本号降序)
     */
    @Override
    public List<KbDocVersionVo> selectVersionList(Long docId) {
        return baseMapper.selectByDocId(docId);
    }

    /**
     * 根据文档ID和版本号查询版本信息
     *
     * @param docId    文档ID
     * @param versionNo 版本号
     * @return 版本信息
     */
    @Override
    public KbDocVersionVo selectVersionByVersionNo(Long docId, Integer versionNo) {
        return baseMapper.selectByVersionNo(docId, versionNo);
    }

    /**
     * 根据版本ID查询版本信息
     *
     * @param versionId 版本ID
     * @return 版本信息
     */
    @Override
    public KbDocVersionVo selectVersionById(Long versionId) {
        return baseMapper.selectVoById(versionId);
    }

    /**
     * 创建新版本
     *
     * @param bo 版本业务对象
     * @return 影响行数
     */
    @Override
    public int createNewVersion(KbDocVersionBo bo) {
        // 获取当前最新版本号
        Integer latestVersionNo = getLatestVersionNo(bo.getDocId());
        Integer newVersionNo = latestVersionNo + 1;
        // 设置版本号
        KbDocVersion version = MapstructUtils.convert(bo, KbDocVersion.class);
        version.setVersionNo(newVersionNo);
        return baseMapper.insert(version);
    }

    /**
     * 回滚到指定版本
     * 将文档恢复到指定版本状态
     * 实际上是创建一个新的版本记录,内容为目标版本的内容
     *
     * @param docId    文档ID
     * @param versionNo 目标版本号
     * @return 影响行数
     */
    @Override
    public int rollbackToVersion(Long docId, Integer versionNo) {
        // 查询目标版本
        KbDocVersionVo targetVersion = selectVersionByVersionNo(docId, versionNo);
        if (ObjectUtil.isNull(targetVersion)) {
            throw new ServiceException("目标版本不存在");
        }
        // 创建新版本记录(内容为目标版本内容)
        KbDocVersionBo newVersionBo = new KbDocVersionBo();
        newVersionBo.setDocId(docId);
        newVersionBo.setFileId(targetVersion.getFileId());
        newVersionBo.setFileSize(targetVersion.getFileSize());
        newVersionBo.setVersionComment("回滚到版本V" + versionNo);
        return createNewVersion(newVersionBo);
    }

    /**
     * 删除版本
     *
     * @param versionId 版本ID
     * @return 影响行数
     */
    @Override
    public int deleteVersion(Long versionId) {
        // 校验版本是否存在
        KbDocVersionVo version = selectVersionById(versionId);
        if (ObjectUtil.isNull(version)) {
            throw new ServiceException("版本不存在");
        }
        return baseMapper.deleteById(versionId);
    }

    /**
     * 获取文档的最新版本号
     *
     * @param docId 文档ID
     * @return 最新版本号, 无版本时返回0
     */
    @Override
    public Integer getLatestVersionNo(Long docId) {
        List<KbDocVersionVo> versions = selectVersionList(docId);
        if (versions.isEmpty()) {
            return 0;
        }
        // 版本列表已按版本号降序排列,第一个即为最新版本
        return versions.get(0).getVersionNo();
    }

}