package com.eureka.flowbuilder.core.saver;

import com.eureka.flowbuilder.ai.model.HtmlCodeResult;
import com.eureka.flowbuilder.exception.BusinessException;
import com.eureka.flowbuilder.exception.ErrorCode;
import com.eureka.flowbuilder.model.enums.CodeGenTypeEnum;
import com.github.xiaoymin.knife4j.core.util.StrUtil;

/**
 * HTML代码文件保存器
 *
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        // 保存 HTML 文件
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    }

    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        // HTML 代码不能为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空");
        }
    }
}
