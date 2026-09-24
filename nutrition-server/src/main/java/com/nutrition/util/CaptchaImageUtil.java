package com.nutrition.util;

import com.nutrition.enums.CaptchaColorEnum;
import com.nutrition.enums.CaptchaConfigEnum;
import lombok.experimental.UtilityClass;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 验证码图片工具类
 * 负责把后端生成的数字验证码绘制为 PNG 图片，并以 Base64 Data URL 返回。
 */
@UtilityClass
public class CaptchaImageUtil {

    /** 安全随机数生成器 */
    private final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 创建验证码图片。
     *
     * @param code      验证码数字
     * @param width     图片宽度
     * @param height    图片高度
     * @param lineCount 干扰线数量
     * @return Base64 Data URL 格式的 PNG 图片
     * @throws IllegalStateException 图片编码失败时抛出
     */
    public String createBase64Image(String code, int width, int height, int lineCount) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            applyRenderHints(graphics);
            drawBackground(graphics, width, height);
            drawNoiseLines(graphics, width, height, lineCount);
            drawCode(graphics, code, width, height);
        } finally {
            graphics.dispose();
        }
        return encodeImage(image);
    }

    /**
     * 应用绘图抗锯齿配置。
     *
     * @param graphics 图形上下文
     */
    private void applyRenderHints(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /**
     * 绘制背景和浅色噪点。
     *
     * @param graphics 图形上下文
     * @param width    图片宽度
     * @param height   图片高度
     */
    private void drawBackground(Graphics2D graphics, int width, int height) {
        graphics.setColor(CaptchaColorEnum.BACKGROUND.toColor());
        graphics.fillRect(0, 0, width, height);
        for (int i = 0; i < width * height / 20; i++) {
            graphics.setColor(new Color(randomChannel(), randomChannel(), randomChannel(), 35));
            int x = SECURE_RANDOM.nextInt(width);
            int y = SECURE_RANDOM.nextInt(height);
            graphics.drawLine(x, y, x, y);
        }
    }

    /**
     * 绘制随机干扰线。
     *
     * @param graphics  图形上下文
     * @param width     图片宽度
     * @param height    图片高度
     * @param lineCount 干扰线数量
     */
    private void drawNoiseLines(Graphics2D graphics, int width, int height, int lineCount) {
        for (int i = 0; i < lineCount; i++) {
            graphics.setColor(new Color(randomChannel(), randomChannel(), randomChannel(), 90));
            graphics.drawLine(
                    SECURE_RANDOM.nextInt(width),
                    SECURE_RANDOM.nextInt(height),
                    SECURE_RANDOM.nextInt(width),
                    SECURE_RANDOM.nextInt(height)
            );
        }
    }

    /**
     * 绘制带轻微旋转的验证码数字。
     *
     * @param graphics 图形上下文
     * @param code     验证码数字
     * @param width    图片宽度
     * @param height   图片高度
     */
    private void drawCode(Graphics2D graphics, String code, int width, int height) {
        int fontSize = Math.max(24, height - 14);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));

        int characterWidth = width / (code.length() + 1);
        for (int i = 0; i < code.length(); i++) {
            graphics.setColor(randomTextColor());
            AffineTransform original = graphics.getTransform();
            int x = characterWidth / 2 + i * characterWidth;
            int y = height / 2 + fontSize / 3 + SECURE_RANDOM.nextInt(7) - 3;
            double angle = Math.toRadians(SECURE_RANDOM.nextInt(25) - 12);
            graphics.rotate(angle, x + fontSize / 2.0, y - fontSize / 2.0);
            graphics.drawString(String.valueOf(code.charAt(i)), x, y);
            graphics.setTransform(original);
        }
    }

    /**
     * 把图片编码为 Base64 Data URL。
     *
     * @param image 验证码图片
     * @return Base64 Data URL
     */
    private String encodeImage(BufferedImage image) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("验证码图片编码失败", e);
        }
    }

    /**
     * 生成适合验证码图片的随机颜色通道值。
     *
     * @return 0-255 的随机整数
     */
    private int randomChannel() {
        return CaptchaConfigEnum.COLOR_CHANNEL_MIN.getValue()
                + SECURE_RANDOM.nextInt(CaptchaConfigEnum.COLOR_CHANNEL_RANGE.getValue());
    }

    /**
     * 随机选择验证码数字颜色。
     *
     * @return 数字颜色
     */
    private Color randomTextColor() {
        CaptchaColorEnum[] colors = {
                CaptchaColorEnum.TEXT_PRIMARY,
                CaptchaColorEnum.TEXT_SECONDARY,
                CaptchaColorEnum.TEXT_TERTIARY
        };
        return colors[SECURE_RANDOM.nextInt(colors.length)].toColor();
    }
}
