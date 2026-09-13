#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

// ======= 【基础 MSDF 配置】 =======
const float pxRange = 4.0; // 必须匹配生成/渲染的清晰度像素范围

// ======= 【核心特效开关与参数调节】 =======
#define ENABLE_OUTLINE 1     // 1 开启描边，0 关闭
#define ENABLE_GLOW    1     // 1 开启动态发光，0 关闭

// 1. 描边微调参数
const float outlineWidth = 0.08;                 // 描边粗细 (0.0 到 0.2 之间微调)
const vec4  outlineColor = vec4(0.0, 0.0, 0.0, 1.0); // 描边颜色 (当前为纯黑 RGB:0,0,0)

// 2. 动态发光微调参数
const float glowWidth    = 0.25;                 // 发光渐变半径 (越大光晕越宽)
const vec3  glowColor    = vec3(0.0, 0.8, 1.0);  // 发光颜色 (当前为科技感青蓝色 RGB:0.0, 0.8, 1.0)
const float glowSpeed    = 3.0;                  // 动态呼吸闪烁速度

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    // 1. 采样 MSDF 纹理并计算中位数距离
    vec3 msd = texture(Sampler0, texCoord0).rgb;
    float sd = median(msd.r, msd.g, msd.b);

    // 2. 硬件偏导数抗锯齿
    vec2 msdfDims = textureSize(Sampler0, 0);
    vec2 dx = dFdx(texCoord0 * msdfDims);
    vec2 dy = dFdy(texCoord0 * msdfDims);
    float toPixels = pxRange * inversesqrt(dot(dx, dx) + dot(dy, dy));

    // 3. 计算文字主体的 Alpha 权重 (阈值设在 0.5)
    float sigDist = (sd - 0.5) * toPixels;
    float textAlpha = clamp(sigDist + 0.5, 0.0, 1.0);

    // 初始颜色赋予文字本身的颜色 (包含 Minecraft 原版 § 颜色码传入的颜色)
    vec4 finalColor = vertexColor * ColorModulator;
    float finalAlpha = textAlpha;

    // ======= 核心公式 A: 动态外发光 (Glow) =======
    #if ENABLE_GLOW == 1
    // 利用 gl_FragCoord.x 创造随屏幕像素位置波动的动态呼吸波（无需依赖外部时间变量）
    float wave = sin(gl_FragCoord.x * 0.02 + gl_FragCoord.y * 0.01) * 0.5 + 0.5;
    float currentGlowWidth = glowWidth * (0.6 + 0.4 * wave); // 动态改变发光半径

    // 距离场公式：在 0.5 之外的大范围平滑渐变
    float glowAlpha = clamp((sd - (0.5 - currentGlowWidth)) / currentGlowWidth, 0.0, 1.0);
    glowAlpha = pow(glowAlpha, 3.0); // 让发光更加柔和收敛

    // 混合发光颜色
    finalColor.rgb = mix(glowColor, finalColor.rgb, textAlpha);
    finalAlpha = max(textAlpha, glowAlpha * 0.7); // 0.7 控制发光的最大透明度
    #endif

    // ======= 核心公式 B: 字体描边 (Outline) =======
    #if ENABLE_OUTLINE == 1
    // 距离场公式：在外扩 outlineWidth 的区域内计算出描边的 Alpha 边缘
    float outlineAlpha = clamp((sd - (0.5 - outlineWidth)) * toPixels + 0.5, 0.0, 1.0);

    // 将描边颜色和文字主体颜色根据本体的 textAlpha 进行线性混合
    finalColor.rgb = mix(outlineColor.rgb, finalColor.rgb, textAlpha);
    // 最终的透明度由主体透明度与描边透明度取最大值决定
    finalAlpha = max(finalAlpha, outlineAlpha * outlineColor.a);
    #endif

    // 4. 应用最终的透明度
    finalColor.a *= finalAlpha;

    // 5. 剔除完全透明像素，优化光栅化性能
    if (finalColor.a < 0.01) discard;

    fragColor = finalColor;
}
