package com.example.ecommercewebapp.dto;

public class JointProbabilityDto {
    private String eventA;          // e.g., product name
    private String eventB;          // e.g., city
    private Long count;             // number of times A and B appear together
    private Double jointProbability; // count / total orders

    // Constructors
    public JointProbabilityDto() {}

    public JointProbabilityDto(String eventA, String eventB, Long count, Double jointProbability) {
        this.eventA = eventA;
        this.eventB = eventB;
        this.count = count;
        this.jointProbability = jointProbability;
    }

    // Getters and setters (required for JSON serialisation)
    public String getEventA() { return eventA; }
    public void setEventA(String eventA) { this.eventA = eventA; }

    public String getEventB() { return eventB; }
    public void setEventB(String eventB) { this.eventB = eventB; }

    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }

    public Double getJointProbability() { return jointProbability; }
    public void setJointProbability(Double jointProbability) { this.jointProbability = jointProbability; }
}