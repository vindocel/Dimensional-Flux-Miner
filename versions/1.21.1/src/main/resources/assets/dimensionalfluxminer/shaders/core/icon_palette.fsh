#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float Monochrome;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a == 0.0) {
        discard;
    }
    // Keep colored details intact; multiplying neutral pixels retains the artwork's shading.
    float chroma = max(color.r, max(color.g, color.b)) - min(color.r, min(color.g, color.b));
    float mask = 1.0 - step(0.5 / 255.0, chroma);
    if (Monochrome > 0.5) {
        color.rgb = vec3(max(color.r, max(color.g, color.b))) * ColorModulator.rgb;
    } else {
        color.rgb = mix(color.rgb, color.rgb * ColorModulator.rgb, mask);
    }
    color.a *= ColorModulator.a;
    fragColor = color;
}
