#version 330
#extension GL_ARB_separate_shader_objects : require

// The exact vertex color: the color modulator is left out on purpose, so nothing tints the Configuration screens.
layout(location = 0) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    if (vertexColor.a == 0.0) {
        discard;
    }
    fragColor = vertexColor;
}
