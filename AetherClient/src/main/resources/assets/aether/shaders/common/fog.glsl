vec3 applyFog(vec3 color, float distanceToCamera) {
    float fog = clamp(exp(-distanceToCamera * 0.015), 0.0, 1.0);
    return mix(vec3(0.55, 0.62, 0.72), color, fog);
}
