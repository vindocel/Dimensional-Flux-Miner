#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

uniform sampler2D Sampler0;

layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

// Dark look of the machine screens: gray pixels keep only DARKNESS of their brightness, colored pixels keep their
// color, with a smooth step between the two so tinted parts never lose their hue. DARKNESS is 1.0 in the light look.
void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    if (color.a == 0.0) {
        discard;
    }
    float chroma = max(color.r, max(color.g, color.b)) - min(color.r, min(color.g, color.b));
    color.rgb *= mix(DARKNESS, 1.0, smoothstep(0.12, 0.35, chroma));
    fragColor = color * ColorModulator;
}
