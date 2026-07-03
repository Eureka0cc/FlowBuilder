package com.eureka.flowbuilder.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.eureka.flowbuilder.constant.CaptchaConstant;
import com.eureka.flowbuilder.model.dto.captcha.CaptchaVerifyRequest;
import com.eureka.flowbuilder.model.vo.CaptchaVO;
import com.eureka.flowbuilder.service.CaptchaService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现
 */
@Service
@Slf4j
public class CaptchaServiceImpl implements CaptchaService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @org.springframework.beans.factory.annotation.Value("${pexels.api-key}")
    private String pexelsApiKey;

    @Override
    public CaptchaVO generateCaptcha() {
        // 1. 生成唯一 ID
        String captchaId = IdUtil.randomUUID();

        // 2. 随机位置 + 随机凸起方向
        int x = RandomUtil.randomInt(CaptchaConstant.MIN_X, CaptchaConstant.MAX_X);
        int y = RandomUtil.randomInt(CaptchaConstant.MIN_Y,
                CaptchaConstant.IMAGE_HEIGHT - CaptchaConstant.SLIDER_HEIGHT - CaptchaConstant.MIN_Y);
        int bumpSide = RandomUtil.randomInt(0, 3); // 0=top, 1=right, 2=bottom, 3=left

        // 3. 生成图片
        CaptchaImages images = generateImages(x, y, bumpSide);

        // 4. 存入 Redis（只存 X 坐标，bumpSide/y 仅视觉效果，不影响验证）
        String key = CaptchaConstant.CAPTCHA_KEY_PREFIX + captchaId;
        CaptchaData data = new CaptchaData(x, System.currentTimeMillis());
        stringRedisTemplate.opsForValue().set(
                key,
                JSONUtil.toJsonStr(data),
                CaptchaConstant.CAPTCHA_EXPIRE_SECONDS,
                TimeUnit.SECONDS);

        // 5. 构建返回结果
        return CaptchaVO.builder()
                .captchaId(captchaId)
                .backgroundImage(images.backgroundBase64())
                .sliderImage(images.sliderBase64())
                .sliderY(y)
                .expiresIn(CaptchaConstant.CAPTCHA_EXPIRE_SECONDS)
                .build();
    }

    @Override
    public boolean verifyCaptcha(CaptchaVerifyRequest request) {
        if (request == null
                || StrUtil.isBlank(request.getCaptchaId())
                || request.getSliderX() == null) {
            return false;
        }

        String key = CaptchaConstant.CAPTCHA_KEY_PREFIX + request.getCaptchaId();
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(json)) {
            return false;
        }

        CaptchaData data = JSONUtil.toBean(json, CaptchaData.class);
        int correctX = data.getX();
        boolean verified = Math.abs(request.getSliderX() - correctX) <= CaptchaConstant.TOLERANCE;

        // 验证成功后删除，防止重复使用
        if (verified) {
            stringRedisTemplate.delete(key);
        }

        return verified;
    }

    // ======================== 图片生成 ========================

    /**
     * 下载背景照片，依次尝试 Pexels API → 程序生成丰富背景
     */
    private BufferedImage downloadBackgroundImage() {
        // 尝试 Pexels API 获取真实照片
        try (HttpResponse response = HttpRequest.get("https://api.pexels.com/v1/search")
                .header("Authorization", pexelsApiKey)
                .form("query", "nature")
                .form("per_page", 15)
                .form("page", RandomUtil.randomInt(1, 10))
                .timeout(5000)
                .execute()) {
            if (response.isOk()) {
                cn.hutool.json.JSONObject jsonResult =
                        cn.hutool.json.JSONUtil.parseObj(response.body());
                cn.hutool.json.JSONArray photos = jsonResult.getJSONArray("photos");
                if (photos != null && !photos.isEmpty()) {
                    // 随机取一张
                    int idx = RandomUtil.randomInt(0, photos.size());
                    cn.hutool.json.JSONObject photo = photos.getJSONObject(idx);
                    cn.hutool.json.JSONObject src = photo.getJSONObject("src");
                    String imageUrl = src.getStr("medium");
                    // 下载图片
                    try (HttpResponse imgResp = HttpRequest.get(imageUrl)
                            .timeout(5000)
                            .execute()) {
                        if (imgResp.isOk()) {
                            byte[] bytes = imgResp.bodyBytes();
                            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                            if (img != null) {
                                BufferedImage result = new BufferedImage(
                                        CaptchaConstant.IMAGE_WIDTH, CaptchaConstant.IMAGE_HEIGHT,
                                        BufferedImage.TYPE_INT_RGB);
                                Graphics2D g = result.createGraphics();
                                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                                g.drawImage(img, 0, 0, CaptchaConstant.IMAGE_WIDTH,
                                        CaptchaConstant.IMAGE_HEIGHT, null);
                                g.dispose();
                                log.debug("Pexels 背景图下载成功: {}", imageUrl);
                                return result;
                            }
                        }
                    }
                }
            }
            log.debug("Pexels API 不可用 ({}), 降级", response.getStatus());
        } catch (Exception e) {
            log.warn("Pexels 图片下载失败({}), 降级为程序生成背景", e.toString());
        }
        return generateRichBackground();
    }

    /**
     * 程序生成丰富背景（多色渐变 + 随机几何装饰）
     */
    private BufferedImage generateRichBackground() {
        int w = CaptchaConstant.IMAGE_WIDTH;
        int h = CaptchaConstant.IMAGE_HEIGHT;
        BufferedImage bg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bg.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // 随机选择一个配色主题
        Color[][] themes = {
                { new Color(72, 142, 245), new Color(108, 92, 231) },    // blue-purple
                { new Color(236, 72, 153), new Color(239, 68, 68) },     // pink-red
                { new Color(34, 197, 94), new Color(20, 184, 166) },     // green-teal
                { new Color(249, 115, 22), new Color(234, 88, 12) },     // orange-deepOrange
                { new Color(139, 92, 246), new Color(59, 130, 246) },    // violet-blue
        };
        Color[] theme = themes[RandomUtil.randomInt(0, themes.length)];

        // 斜向渐变
        GradientPaint gradient = new GradientPaint(
                0, 0, theme[0], w, h, theme[1]);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, w, h);

        // 随机大圆（叠加透明色块）
        for (int i = 0; i < 3; i++) {
            int cx = RandomUtil.randomInt(-40, w + 40);
            int cy = RandomUtil.randomInt(-40, h + 40);
            int r = RandomUtil.randomInt(60, 140);
            g2d.setColor(new Color(255, 255, 255, 20 + RandomUtil.randomInt(0, 25)));
            g2d.fillOval(cx - r, cy - r, r * 2, r * 2);
        }

        // 随机线条纹理
        g2d.setStroke(new BasicStroke(RandomUtil.randomInt(3, 8),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 2; i++) {
            g2d.setColor(new Color(255, 255, 255, 15 + RandomUtil.randomInt(0, 20)));
            int x1 = RandomUtil.randomInt(0, w);
            int y1 = RandomUtil.randomInt(0, h);
            int x2 = RandomUtil.randomInt(0, w);
            int y2 = RandomUtil.randomInt(0, h);
            g2d.drawLine(x1, y1, x2, y2);
        }

        g2d.dispose();
        return bg;
    }

    /**
     * 生成拼图形状（矩形 + 单侧圆形凸起）
     *
     * @param x         形状左上角 X
     * @param y         形状左上角 Y
     * @param w         矩形宽度
     * @param h         矩形高度
     * @param bumpR     凸起圆半径
     * @param bumpSide  凸起方向：0=top, 1=right, 2=bottom, 3=left
     */
    private Area createPuzzleShape(int x, int y, int w, int h,
                                    int bumpR, int bumpSide) {
        // 基础圆角矩形
        Area area = new Area(new Rectangle(x, y, w, h));

        // 凸起圆——圆心在边中点外侧，约 40% 与矩形重叠
        float bumpDiam = bumpR * 2f;
        float cx, cy;
        switch (bumpSide) {
            case 0: // top
                cx = x + w / 2f;
                cy = y - bumpR * 0.4f;
                break;
            case 1: // right
                cx = x + w + bumpR * 0.4f;
                cy = y + h / 2f;
                break;
            case 2: // bottom
                cx = x + w / 2f;
                cy = y + h + bumpR * 0.4f;
                break;
            default: // left
                cx = x - bumpR * 0.4f;
                cy = y + h / 2f;
                break;
        }
        Ellipse2D.Float bump = new Ellipse2D.Float(
                cx - bumpR, cy - bumpR, bumpDiam, bumpDiam);
        area.add(new Area(bump));

        return area;
    }

    /**
     * 生成滑块坐标系下的拼图形状（偏移 PADDING 以便凸起有空间）
     */
    private Area createPuzzleShapeForSlider(int bumpR, int bumpSide) {
        return createPuzzleShape(
                CaptchaConstant.BUMP_PADDING,
                CaptchaConstant.BUMP_PADDING,
                CaptchaConstant.SLIDER_WIDTH,
                CaptchaConstant.SLIDER_HEIGHT,
                bumpR, bumpSide);
    }

    /**
     * 生成验证码图片
     *
     * 流程：下载照片 → 裁剪滑块（拼图形状）→ 背景画暗色缺口 + 干扰
     */
    private CaptchaImages generateImages(int x, int y, int bumpSide) {
        int w = CaptchaConstant.IMAGE_WIDTH;
        int h = CaptchaConstant.IMAGE_HEIGHT;
        int sw = CaptchaConstant.SLIDER_WIDTH;
        int sh = CaptchaConstant.SLIDER_HEIGHT;
        int pad = CaptchaConstant.BUMP_PADDING;
        int bumpR = CaptchaConstant.PUZZLE_BUMP_RADIUS;

        // 1. 下载 / 降级背景照片
        BufferedImage photo = downloadBackgroundImage();

        // 2. 构建拼图形状
        Area bgShape = createPuzzleShape(x, y, sw, sh, bumpR, bumpSide);
        Area sliderShape = createPuzzleShapeForSlider(bumpR, bumpSide);

        // 3. 生成背景图：复制照片 → 暗色缺口 + 边框 + 干扰
        BufferedImage background = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D bgG = background.createGraphics();
        bgG.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        bgG.drawImage(photo, 0, 0, null);

        // 缺口阴影
        bgG.setColor(new Color(0, 0, 0, 110));
        bgG.fill(bgShape);
        // 缺口内边缘高亮（模拟立体凹陷）
        bgG.setColor(new Color(255, 255, 255, 160));
        bgG.setStroke(new BasicStroke(1.5f));
        bgG.draw(bgShape);

        // 添加干扰
        addInterference(bgG);
        bgG.dispose();

        // 4. 生成滑块图（带 padding 容纳凸起）
        int sliderImgW = sw + 2 * pad;
        int sliderImgH = sh + 2 * pad;
        BufferedImage slider = new BufferedImage(sliderImgW, sliderImgH,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D sliderG = slider.createGraphics();
        sliderG.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // clip 到拼图形状，从原图裁切对应区域
        sliderG.setClip(sliderShape);
        sliderG.drawImage(photo,
                0, 0, sliderImgW, sliderImgH,           // dest: 完整 canvas
                x - pad, y - pad,                        // src: 原图对应区域（含 padding）
                x - pad + sliderImgW, y - pad + sliderImgH,
                null);

        // 去除 clip，画白边框 + 阴影
        sliderG.setClip(null);
        // 外阴影
        sliderG.setColor(new Color(0, 0, 0, 30));
        sliderG.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        sliderG.draw(sliderShape);
        // 白色边框
        sliderG.setColor(Color.WHITE);
        sliderG.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        sliderG.draw(sliderShape);
        sliderG.dispose();

        return new CaptchaImages(toBase64(background), toBase64(slider));
    }

    // ======================== 干扰 / 工具方法 ========================

    /**
     * 添加干扰元素
     */
    private void addInterference(Graphics2D g2d) {
        // 干扰线
        g2d.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i < 4; i++) {
            g2d.setColor(new Color(255, 255, 255, 50 + RandomUtil.randomInt(0, 50)));
            int x1 = RandomUtil.randomInt(0, CaptchaConstant.IMAGE_WIDTH);
            int y1 = RandomUtil.randomInt(0, CaptchaConstant.IMAGE_HEIGHT);
            int x2 = RandomUtil.randomInt(0, CaptchaConstant.IMAGE_WIDTH);
            int y2 = RandomUtil.randomInt(0, CaptchaConstant.IMAGE_HEIGHT);
            g2d.drawLine(x1, y1, x2, y2);
        }

        // 干扰圆点
        for (int i = 0; i < 15; i++) {
            g2d.setColor(new Color(255, 255, 255, 80 + RandomUtil.randomInt(0, 80)));
            int cx = RandomUtil.randomInt(0, CaptchaConstant.IMAGE_WIDTH);
            int cy = RandomUtil.randomInt(0, CaptchaConstant.IMAGE_HEIGHT);
            int r = RandomUtil.randomInt(2, 4);
            g2d.fillOval(cx, cy, r, r);
        }
    }

    /**
     * BufferedImage 转 Base64
     */
    private String toBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            log.error("图片转 Base64 失败", e);
            throw new RuntimeException("生成验证码图片失败", e);
        }
    }

    /**
     * 验证码图片数据
     */
    private record CaptchaImages(String backgroundBase64, String sliderBase64) {
    }

    /**
     * Redis 中存储的验证码数据
     */
    @lombok.Data
    private static class CaptchaData {
        private Integer x;
        private Long createdAt;

        public CaptchaData(Integer x, Long createdAt) {
            this.x = x;
            this.createdAt = createdAt;
        }
    }
}
