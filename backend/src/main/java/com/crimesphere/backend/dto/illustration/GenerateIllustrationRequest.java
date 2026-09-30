package com.crimesphere.backend.dto.illustration;

public class GenerateIllustrationRequest {

    private String prompt;
    private String aspectRatio = "1:1";

    public GenerateIllustrationRequest() {
    }

    public GenerateIllustrationRequest(String prompt) {
        this.prompt = prompt;
    }

    public GenerateIllustrationRequest(String prompt, String aspectRatio) {
        this.prompt = prompt;
        this.aspectRatio = aspectRatio;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
    }
}
