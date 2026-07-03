package com.eureka.flowbuilder.constant;

/**
 * 验证码常量
 */
public interface CaptchaConstant {

    /**
     * Redis Key 前缀
     */
    String CAPTCHA_KEY_PREFIX = "captcha:";

    /**
     * 验证码有效期（秒）
     */
    long CAPTCHA_EXPIRE_SECONDS = 300L;

    /**
     * 背景图宽度
     */
    int IMAGE_WIDTH = 320;

    /**
     * 背景图高度
     */
    int IMAGE_HEIGHT = 160;

    /**
     * 滑块宽度
     */
    int SLIDER_WIDTH = 50;

    /**
     * 滑块高度
     */
    int SLIDER_HEIGHT = 40;

    /**
     * 滑块 X 坐标最小值（防止贴边）
     */
    int MIN_X = 50;

    /**
     * 滑块 X 坐标最大值
     */
    int MAX_X = IMAGE_WIDTH - SLIDER_WIDTH - 50;

    /**
     * 滑块 Y 坐标最小值
     */
    int MIN_Y = 10;

    /**
     * 验证容差（像素）
     */
    int TOLERANCE = 5;

    /**
     * 拼图凸起半径（像素）
     */
    int PUZZLE_BUMP_RADIUS = 10;

    /**
     * 滑块图片 padding（每侧像素数，容纳凸起）
     */
    int BUMP_PADDING = PUZZLE_BUMP_RADIUS + 4;
}
