package com.example.BE.issue.dto;

import com.example.BE.label.Label;

public class IssueLabelMap {

    private Integer issueNumber;
    private Label label;

    public IssueLabelMap() {
    }

    public IssueLabelMap(Integer issueNumber, Label label) {
        this.issueNumber = issueNumber;
        this.label = label;
    }

    public Integer getIssueNumber() {
        return issueNumber;
    }

    public void setIssueNumber(Integer issueNumber) {
        this.issueNumber = issueNumber;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "IssueLabelMap{" +
                "issueNumber=" + issueNumber +
                ", label=" + label +
                '}';
    }
}
