package com.eureka.flowbuilder.service;

import com.eureka.flowbuilder.model.dto.app.AppQueryRequest;
import com.eureka.flowbuilder.model.entity.User;
import com.eureka.flowbuilder.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.eureka.flowbuilder.model.entity.App;
import reactor.core.publisher.Flux;

import java.io.Serializable;
import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/Eureka0cc">Eureka</a>
 */
public interface AppService extends IService<App> {

    /**
     * 根据实体对象获取VO对象。
     *
     * @param app 实体对象
     * @return VO对象
     */
    AppVO getAppVO(App app);

    /**
     * 根据查询条件获取查询包装器。
     *
     * @param appQueryRequest 查询条件
     * @return 查询包装器
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 根据实体列表获取VO列表。
     *
     * @param appList 实体列表
     * @return VO列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 聊天生成代码
     *
     * @param appId     应用id
     * @param message   消息
     * @param loginUser 登录用户
     * @return 代码
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 部署应用
     *
     * @param appId     应用id
     * @param loginUser 登录用户
     * @return 部署结果
     */
    String deployApp(Long appId, User loginUser);
}
