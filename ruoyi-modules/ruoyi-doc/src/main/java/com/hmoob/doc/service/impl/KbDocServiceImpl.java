package com.hmoob.doc.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import com.hmoob.common.core.constant.SystemConstants;
import com.hmoob.common.core.exception.ServiceException;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.common.core.utils.StringUtils;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.doc.domain.KbDoc;
import com.hmoob.doc.domain.bo.KbDocBo;
import com.hmoob.doc.domain.vo.KbDocVo;
import com.hmoob.doc.enums.DocStatusEnum;
import com.hmoob.doc.mapper.KbDocMapper;
import com.hmoob.doc.service.IKbDocService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * KB文档管理 服务实现
 *
 * @author hmoob
 */
@RequiredArgsConstructor
@Service
public class KbDocServiceImpl implements IKbDocService {

    private final KbDocMapper baseMapper;

    /**
     * 分页查询文档管理数据
     *
     * @param bo        文档信息
     * @param pageQuery 分页对象
     * @return 文档信息集合
     */
    @Override
    public TableDataInfo<KbDocVo> selectPageDocList(KbDocBo bo, PageQuery pageQuery) {
        Page<KbDocVo> page = baseMapper.selectPageDocList(pageQuery.build(), buildQueryWrapper(bo));
        return TableDataInfo.build(page);
    }

    /**
     * 查询文档管理数据
     *
     * @param bo 文档信息
     * @return 文档信息集合
     */
    @Override
    public List<KbDocVo> selectDocList(KbDocBo bo) {
        return baseMapper.selectDocList(buildQueryWrapper(bo));
    }

    /**
     * 构建查询条件
     *
     * @param bo 文档信息
     * @return 查询条件
     */
    private LambdaQueryWrapper<KbDoc> buildQueryWrapper(KbDocBo bo) {
        LambdaQueryWrapper<KbDoc> lqw = Wrappers.lambdaQuery();
        lqw.eq(KbDoc::getDelFlag, SystemConstants.NORMAL);
        lqw.eq(ObjectUtil.isNotNull(bo.getDocId()), KbDoc::getDocId, bo.getDocId());
        lqw.eq(ObjectUtil.isNotNull(bo.getFolderId()), KbDoc::getFolderId, bo.getFolderId());
        lqw.like(StringUtils.isNotBlank(bo.getDocName()), KbDoc::getDocName, bo.getDocName());
        lqw.like(StringUtils.isNotBlank(bo.getDocTitle()), KbDoc::getDocTitle, bo.getDocTitle());
        lqw.eq(StringUtils.isNotBlank(bo.getSerialNumber()), KbDoc::getSerialNumber, bo.getSerialNumber());
        lqw.eq(StringUtils.isNotBlank(bo.getFileType()), KbDoc::getFileType, bo.getFileType());
        lqw.eq(StringUtils.isNotBlank(bo.getCategory()), KbDoc::getCategory, bo.getCategory());
        lqw.like(StringUtils.isNotBlank(bo.getKeywords()), KbDoc::getKeywords, bo.getKeywords());
        lqw.eq(ObjectUtil.isNotNull(bo.getStatus()), KbDoc::getStatus, bo.getStatus());
        lqw.eq(ObjectUtil.isNotNull(bo.getReleaseFlag()), KbDoc::getReleaseFlag, bo.getReleaseFlag());
        lqw.orderByDesc(KbDoc::getCreateTime);
        return lqw;
    }

    /**
     * 根据文档ID查询信息
     *
     * @param docId 文档ID
     * @return 文档信息
     */
    @Override
    public KbDocVo selectDocById(Long docId) {
        return baseMapper.selectVoById(docId);
    }

    /**
     * 根据文档编号查询信息
     *
     * @param serialNumber 文档编号
     * @return 文档信息
     */
    @Override
    public KbDocVo selectDocBySerialNumber(String serialNumber) {
        return baseMapper.selectBySerialNumber(serialNumber);
    }

    /**
     * 发布文档
     *
     * @param docId 文档ID
     * @return 结果
     */
    @Override
    public int publishDoc(Long docId) {
        KbDoc doc = baseMapper.selectById(docId);
        if (ObjectUtil.isNull(doc)) {
            throw new ServiceException("文档不存在，无法发布");
        }
        // 校验文档状态是否允许发布
        if (doc.getStatus().equals(DocStatusEnum.PUBLISHED.getCode())) {
            throw new ServiceException("文档已发布，请勿重复操作");
        }
        if (doc.getStatus().equals(DocStatusEnum.ARCHIVED.getCode())) {
            throw new ServiceException("文档已归档，不允许发布");
        }
        return baseMapper.update(null, new LambdaUpdateWrapper<KbDoc>()
            .set(KbDoc::getStatus, DocStatusEnum.PUBLISHED.getCode())
            .set(KbDoc::getReleaseFlag, 1)
            .eq(KbDoc::getDocId, docId));
    }

    /**
     * 撤回文档
     *
     * @param docId 文档ID
     * @return 结果
     */
    @Override
    public int withdrawDoc(Long docId) {
        KbDoc doc = baseMapper.selectById(docId);
        if (ObjectUtil.isNull(doc)) {
            throw new ServiceException("文档不存在，无法撤回");
        }
        // 校验文档状态是否允许撤回
        if (!doc.getStatus().equals(DocStatusEnum.PUBLISHED.getCode())) {
            throw new ServiceException("只有已发布的文档才能撤回");
        }
        return baseMapper.update(null, new LambdaUpdateWrapper<KbDoc>()
            .set(KbDoc::getStatus, DocStatusEnum.WITHDRAWN.getCode())
            .set(KbDoc::getReleaseFlag, 0)
            .eq(KbDoc::getDocId, docId));
    }

    /**
     * 增加浏览次数
     *
     * @param docId 文档ID
     * @return 结果
     */
    @Override
    public int incrementViewCount(Long docId) {
        return baseMapper.update(null, new LambdaUpdateWrapper<KbDoc>()
            .setSql("view_count = view_count + 1")
            .eq(KbDoc::getDocId, docId));
    }

    /**
     * 增加下载次数
     *
     * @param docId 文档ID
     * @return 结果
     */
    @Override
    public int incrementDownloadCount(Long docId) {
        return baseMapper.update(null, new LambdaUpdateWrapper<KbDoc>()
            .setSql("download_count = download_count + 1")
            .eq(KbDoc::getDocId, docId));
    }

    /**
     * 校验文档名称是否唯一
     *
     * @param bo 文档信息
     * @return 结果
     */
    @Override
    public boolean checkDocNameUnique(KbDocBo bo) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<KbDoc>()
            .eq(KbDoc::getDocName, bo.getDocName())
            .eq(KbDoc::getFolderId, bo.getFolderId())
            .ne(ObjectUtil.isNotNull(bo.getDocId()), KbDoc::getDocId, bo.getDocId()));
        return !exist;
    }

    /**
     * 新增保存文档信息
     *
     * @param bo 文档信息
     * @return 结果
     */
    @Override
    public int insertDoc(KbDocBo bo) {
        KbDoc doc = MapstructUtils.convert(bo, KbDoc.class);
        // 设置初始值
        if (doc.getStatus() == null) {
            doc.setStatus(DocStatusEnum.PENDING.getCode());
        }
        if (doc.getReleaseFlag() == null) {
            doc.setReleaseFlag(0);
        }
        if (doc.getCurrentVersion() == null) {
            doc.setCurrentVersion(1);
        }
        if (doc.getViewCount() == null) {
            doc.setViewCount(0L);
        }
        if (doc.getDownloadCount() == null) {
            doc.setDownloadCount(0L);
        }
        if (doc.getCommentCount() == null) {
            doc.setCommentCount(0L);
        }
        if (doc.getFavouriteCount() == null) {
            doc.setFavouriteCount(0L);
        }
        if (doc.getFtiFlag() == null) {
            doc.setFtiFlag(0);
        }
        if (doc.getConvertFlag() == null) {
            doc.setConvertFlag(0);
        }
        return baseMapper.insert(doc);
    }

    /**
     * 修改保存文档信息
     *
     * @param bo 文档信息
     * @return 结果
     */
    @Override
    public int updateDoc(KbDocBo bo) {
        KbDoc doc = MapstructUtils.convert(bo, KbDoc.class);
        KbDoc oldDoc = baseMapper.selectById(doc.getDocId());
        if (ObjectUtil.isNull(oldDoc)) {
            throw new ServiceException("文档不存在，无法修改");
        }
        return baseMapper.updateById(doc);
    }

    /**
     * 删除文档管理信息
     *
     * @param docId 文档ID
     * @return 结果
     */
    @Override
    public int deleteDocById(Long docId) {
        return baseMapper.deleteById(docId);
    }

}