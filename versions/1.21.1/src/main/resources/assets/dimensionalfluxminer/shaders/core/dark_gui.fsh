#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float Darkness;

in vec2 texCoord0;

out vec4 fragColor;

// Dark look of the machine screens: gray pixels keep only Darkness of their brightness, colored pixels keep their
// color, with a smooth step between the two so tinted parts never lose their hue.
void main() {
    vec4 color = texture(Sampler0, texCoord0) * ColorModulator;
    if (color.a == 0.0) {
        discard;
    }
    float chroma = max(color.r, max(color.g, color.b)) - min(color.r, min(color.g, color.b));
    color.rgb *= mix(Darkness, 1.0, smoothstep(0.12, 0.35, chroma));
    fragColor = color;
}
