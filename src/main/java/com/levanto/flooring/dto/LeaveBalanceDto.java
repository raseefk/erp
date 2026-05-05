package com.levanto.flooring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveBalanceDto {
    private int allocatedSickLeaves;
    private int usedSickLeaves;
    private int remainingSickLeaves;
    private int allocatedCasualLeaves;
    private int usedCasualLeaves;
    private int remainingCasualLeaves;
}
