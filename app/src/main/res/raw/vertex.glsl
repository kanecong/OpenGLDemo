attribute vec4 aPosition;
attribute vec2 aTextureCoord;
uniform mat4 uMatrix;
varying vec2 vTextureCoord;
attribute float alpha;
varying float inAlpha;
void main() {
    gl_Position = uMatrix * aPosition;
    vTextureCoord = aTextureCoord;
    inAlpha = alpha;
}
