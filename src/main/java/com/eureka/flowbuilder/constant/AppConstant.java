package com.eureka.flowbuilder.constant;

public interface AppConstant {

    /**
     * 精选应用的优先级
     */
    Integer GOOD_APP_PRIORITY = 99;

    /**
     * 默认应用优先级
     */
    Integer DEFAULT_APP_PRIORITY = 0;

    /**
     * 应用生成目录
     */
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * Vue 工程模式下的项目目录名（与 FileWriteTool 等保持一致）
     */
    static String getVueProjectDirName(Long appId) {
        return "vue_project_" + appId;
    }

    /**
     * Vue 工程模式下的项目根目录绝对路径
     */
    static String getVueProjectDirPath(Long appId) {
        return CODE_OUTPUT_ROOT_DIR + "/" + getVueProjectDirName(appId);
    }

    /**
     * 应用部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_deploy";

    /**
     * 应用部署域名（包含端口和上下文路径）
     */
    String CODE_DEPLOY_HOST = "http://localhost:8123/api/deploy";

    /**
     * 部署应用入口文件
     */
    String DEPLOY_INDEX_FILE = "index.html";

}
