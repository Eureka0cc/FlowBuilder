package com.eureka.flowbuilder.controller;

import com.eureka.flowbuilder.common.BaseResponse;
import com.eureka.flowbuilder.common.DeleteRequest;
import com.eureka.flowbuilder.common.ResultUtils;
import com.eureka.flowbuilder.exception.ErrorCode;
import com.eureka.flowbuilder.exception.ThrowUtils;
import com.eureka.flowbuilder.model.dto.team.TeamAssetQueryRequest;
import com.eureka.flowbuilder.model.dto.team.TeamAssetUpdateRequest;
import com.eureka.flowbuilder.model.dto.team.TeamCreateRequest;
import com.eureka.flowbuilder.model.entity.TeamAsset;
import com.eureka.flowbuilder.model.entity.User;
import com.eureka.flowbuilder.model.vo.TeamMemberVO;
import com.eureka.flowbuilder.model.vo.TeamVO;
import com.eureka.flowbuilder.service.TeamAssetService;
import com.eureka.flowbuilder.service.TeamService;
import com.eureka.flowbuilder.service.UserService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/team")
public class TeamController {
    @Resource
    private TeamService teamService;
    @Resource
    private TeamAssetService teamAssetService;
    @Resource
    private UserService userService;

    @PostMapping("/create")
    public BaseResponse<Long> create(@RequestBody TeamCreateRequest request, HttpServletRequest servletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(teamService.createTeam(request.getName(), user, false));
    }

    @GetMapping("/my")
    public BaseResponse<List<TeamVO>> listMyTeams(HttpServletRequest request) {
        return ResultUtils.success(teamService.listMyTeams(userService.getLoginUser(request)));
    }

    @GetMapping("/{teamId}/members")
    public BaseResponse<List<TeamMemberVO>> listMembers(@PathVariable Long teamId, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(teamService.listMembers(teamId, user));
    }

    @PostMapping(value = "/asset/upload", consumes = "multipart/form-data")
    public BaseResponse<TeamAsset> uploadAsset(@RequestParam Long teamId,
                                               @RequestPart("file") MultipartFile file,
                                               @RequestParam(required = false) String name,
                                               @RequestParam(required = false) String tags,
                                               HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(teamAssetService.upload(teamId, file, name, tags, user));
    }

    @PostMapping("/asset/list/page")
    public BaseResponse<Page<TeamAsset>> listAssets(@RequestBody TeamAssetQueryRequest query,
                                                    HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(teamAssetService.listAssets(query, user));
    }

    @PostMapping("/asset/update")
    public BaseResponse<Boolean> updateAsset(@RequestBody TeamAssetUpdateRequest update,
                                             HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(teamAssetService.updateAsset(update, user));
    }

    @PostMapping("/asset/delete")
    public BaseResponse<Boolean> deleteAsset(@RequestBody DeleteRequest delete,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(delete == null || delete.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getLoginUser(request);
        return ResultUtils.success(teamAssetService.deleteAsset(delete.getId(), user));
    }
}
