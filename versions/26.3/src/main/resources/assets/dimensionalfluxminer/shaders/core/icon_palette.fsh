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

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a == 0.0) {
        discard;
    }
    // Keep colored details intact; multiplying neutral pixels retains the artwork's shading.
    float chroma = max(color.r, max(color.g, color.b)) - min(color.r, min(color.g, color.b));
    float mask = 1.0 - step(0.5 / 255.0, chroma);
#ifdef MONOCHROME_ICON
    color.rgb = vec3(max(color.r, max(color.g, color.b))) * vertexColor.rgb;
#else
    color.rgb = mix(color.rgb, color.rgb * vertexColor.rgb, mask);
#endif
    color.a *= vertexColor.a;
    fragColor = color * ColorModulator;
}
