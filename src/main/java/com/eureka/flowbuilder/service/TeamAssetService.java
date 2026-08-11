package com.eureka.flowbuilder.service;

import com.eureka.flowbuilder.model.dto.team.TeamAssetQueryRequest;
import com.eureka.flowbuilder.model.dto.team.TeamAssetUpdateRequest;
import com.eureka.flowbuilder.model.entity.TeamAsset;
import com.eureka.flowbuilder.model.entity.User;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TeamAssetService extends IService<TeamAsset> {
    TeamAsset upload(Long teamId, MultipartFile file, String name, String tags, User user);

    Page<TeamAsset> listAssets(TeamAssetQueryRequest request, User user);

    boolean updateAsset(TeamAssetUpdateRequest request, User user);

    boolean deleteAsset(Long id, User user);

    List<TeamAsset> listPromptAssets(Long teamId, int limit);
}
