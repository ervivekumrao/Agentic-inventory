package com.smartinventory.inventory.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AiQueryResponse {

    private String answer;
    private List<String> recommendations;

    @JsonProperty("riskLevel")
    @JsonAlias({"risk_level", "riskLevel"})
    private String riskLevel;

    public AiQueryResponse() {
    }

    public AiQueryResponse(String answer, List<String> recommendations, String riskLevel) {
        this.answer = answer;
        this.recommendations = recommendations;
        this.riskLevel = riskLevel;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
}
