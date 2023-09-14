#extension GL_OES_EGL_image_external:require
precision mediump float;
varying vec2 vTextureCoord;
varying float inAlpha;
uniform samplerExternalOES uTexture;
void main() {
    vec4 color = texture2D(uTexture, vCoordinate);
    gl_FragColor = vec4(color.r, color.g, color.b, inAlpha);
}
