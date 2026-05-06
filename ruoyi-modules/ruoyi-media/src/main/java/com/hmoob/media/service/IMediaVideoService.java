package com.hmoob.media.service;

import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.media.domain.bo.MediaVideoQuery;
import com.hmoob.media.domain.bo.MediaVideoUpdateBo;
import com.hmoob.media.domain.bo.MediaVideoUploadBo;
import com.hmoob.media.domain.vo.MediaVideoDetailVo;
import com.hmoob.media.domain.vo.MediaVideoVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 视频管理服务接口
 *
 * @author hmoob
 */
public interface IMediaVideoService {

    /**
     * 上传视频
     *
     * @param file       视频文件
     * @param thumbnail  缩略图文件（可选）
     * @param bo         上传参数
     * @return 视频ID
     */
    Long uploadVideo(MultipartFile file, MultipartFile thumbnail, MediaVideoUploadBo bo);

    /**
     * 分页查询视频列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    TableDataInfo<MediaVideoVo> selectVideoPage(MediaVideoQuery query);

    /**
     * 查询视频详情
     *
     * @param videoId 视频ID
     * @return 视频详情
     */
    MediaVideoDetailVo selectVideoDetail(Long videoId);

    /**
     * 删除视频
     *
     * @param videoId 视频ID
     * @return 影响行数
     */
    int deleteVideo(Long videoId);

    /**
     * 更新视频信息
     *
     * @param videoId 视频ID
     * @param bo      更新参数
     */
    void updateVideo(Long videoId, MediaVideoUpdateBo bo);

    /**
     * Range 流式播放视频
     *
     * @param videoId 视频ID
     * @param request  HTTP请求
     * @param response HTTP响应
     */
    void playVideo(Long videoId, HttpServletRequest request, HttpServletResponse response);

    /**
     * 点赞视频
     *
     * @param videoId 视频ID
     * @return 是否成功
     */
    boolean likeVideo(Long videoId);

    /**
     * 取消点赞
     *
     * @param videoId 视频ID
     * @return 是否成功
     */
    boolean unlikeVideo(Long videoId);
}
