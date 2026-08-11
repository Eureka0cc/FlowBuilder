package com.eureka.flowbuilder.service.impl;

import cn.hutool.core.util.StrUtil;
import com.eureka.flowbuilder.exception.ErrorCode;
import com.eureka.flowbuilder.exception.ThrowUtils;
import com.eureka.flowbuilder.manager.TosManager;
import com.eureka.flowbuilder.mapper.TeamAssetMapper;
import com.eureka.flowbuilder.model.dto.team.TeamAssetQueryRequest;
import com.eureka.flowbuilder.model.dto.team.TeamAssetUpdateRequest;
import com.eureka.flowbuilder.model.entity.TeamAsset;
import com.eureka.flowbuilder.model.entity.User;
import com.eureka.flowbuilder.service.TeamAssetService;
import com.eureka.flowbuilder.service.TeamAuthorizationService;
import com.eureka.flowbuilder.service.TeamService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class TeamAssetServiceImpl extends ServiceImpl<TeamAssetMapper, TeamAsset> implements TeamAssetService {
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;

    @Resource
    private TeamAuthorizationService authorizationService;
    @Resource
    private TeamService teamService;
    @Resource
    private TosManager tosManager;

    @Override
    public TeamAsset upload(Long teamId, MultipartFile file, String name, String tags, User user) {
        ThrowUtils.throwIf(teamId == null || teamId <= 0, ErrorCode.PARAMS_ERROR, "团队 ID 无效");
        ThrowUtils.throwIf(teamService.getById(teamId) == null, ErrorCode.NOT_FOUND_ERROR, "团队不存在");
        authorizationService.requireEdit(teamId, user);
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "图片不能为空");
        ThrowUtils.throwIf(file.getSize() > MAX_IMAGE_SIZE, ErrorCode.PARAMS_ERROR, "图片不能超过 10MB");
        String contentType = file.getContentType();
        String suffix = extensionForContentType(contentType);
        ThrowUtils.throwIf(suffix == null, ErrorCode.PARAMS_ERROR, "仅支持 JPEG、PNG、GIF 图片");

        File tempFile = null;
        TeamAsset asset = new TeamAsset();
        try {
            String originalName = safeFileName(file.getOriginalFilename());
            tempFile = File.createTempFile("team-asset-", suffix);
            file.transferTo(tempFile);
            BufferedImage image = ImageIO.read(tempFile);
            ThrowUtils.throwIf(image == null, ErrorCode.PARAMS_ERROR, "无法解析图片内容");

            asset.setTeamId(teamId);
            asset.setUploaderId(user.getId());
            asset.setName(StrUtil.blankToDefault(name, originalName));
            asset.setTags(tags);
            asset.setContentType(contentType);
            asset.setSize(file.getSize());
            asset.setWidth(image.getWidth());
            asset.setHeight(image.getHeight());
            ThrowUtils.throwIf(!save(asset), ErrorCode.OPERATION_ERROR, "保存资产失败");

            String baseName = originalName.contains(".")
                    ? originalName.substring(0, originalName.lastIndexOf('.')) : originalName;
            String key = String.format("teams/%d/assets/%d/%s%s",
                    teamId, asset.getId(), baseName, suffix);
            String url = tosManager.uploadFile(key, tempFile);
            if (StrUtil.isBlank(url)) {
                removeById(asset.getId());
                throw new com.eureka.flowbuilder.exception.BusinessException(
                        ErrorCode.OPERATION_ERROR, "图片上传失败");
            }
            asset.setTosKey(key);
            asset.setUrl(url);
            ThrowUtils.throwIf(!updateById(asset), ErrorCode.OPERATION_ERROR, "更新资产地址失败");
            return asset;
        } catch (IOException e) {
            throw new com.eureka.flowbuilder.exception.BusinessException(
                    ErrorCode.OPERATION_ERROR, "处理图片失败：" + e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                tempFile.deleteOnExit();
            }
        }
    }

    @Override
    public Page<TeamAsset> listAssets(TeamAssetQueryRequest request, User user) {
        ThrowUtils.throwIf(request == null || request.getTeamId() == null,
                ErrorCode.PARAMS_ERROR, "团队 ID 不能为空");
        ThrowUtils.throwIf(request.getPageSize() <= 0 || request.getPageSize() > 50,
                ErrorCode.PARAMS_ERROR, "每页数量应为 1-50");
        authorizationService.requireView(request.getTeamId(), user);
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("teamId", request.getTeamId())
                .like("name", request.getSearchText())
                .like("tags", request.getTag())
                .orderBy("createTime", false);
        return page(Page.of(request.getPageNum(), request.getPageSize()), wrapper);
    }

    @Override
    public boolean updateAsset(TeamAssetUpdateRequest request, User user) {
        ThrowUtils.throwIf(request == null || request.getId() == null,
                ErrorCode.PARAMS_ERROR, "资产 ID 不能为空");
        TeamAsset existing = getById(request.getId());
        ThrowUtils.throwIf(existing == null, ErrorCode.NOT_FOUND_ERROR, "资产不存在");
        authorizationService.requireEdit(existing.getTeamId(), user);
        ThrowUtils.throwIf(StrUtil.isBlank(request.getName()) || request.getName().length() > 120,
                ErrorCode.PARAMS_ERROR, "资产名称长度应为 1-120");
        TeamAsset update = new TeamAsset();
        update.setId(existing.getId());
        update.setName(request.getName().trim());
        update.setTags(request.getTags());
        return updateById(update);
    }

    @Override
    public boolean deleteAsset(Long id, User user) {
        TeamAsset asset = getById(id);
        ThrowUtils.throwIf(asset == null, ErrorCode.NOT_FOUND_ERROR, "资产不存在");
        authorizationService.requireEdit(asset.getTeamId(), user);
        return removeById(id);
    }

    @Override
    public List<TeamAsset> listPromptAssets(Long teamId, int limit) {
        if (teamId == null || limit <= 0) {
            return List.of();
        }
        return list(QueryWrapper.create()
                .eq("teamId", teamId)
                .orderBy("updateTime", false)
                .limit(limit));
    }

    private String safeFileName(String originalName) {
        String fileName = StrUtil.blankToDefault(originalName, "image");
        fileName = new File(fileName).getName().replaceAll("[^a-zA-Z0-9._-]", "_");
        return StrUtil.isBlank(fileName) ? "image" : fileName;
    }

    private String extensionForContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            default -> null;
        };
    }
}
