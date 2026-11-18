#version 330

// The exact vertex color: the color modulator is left out on purpose, so nothing tints the Configuration screens.
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    if (vertexColor.a == 0.0) {
        discard;
    }
    fragColor = vertexColor;
}
