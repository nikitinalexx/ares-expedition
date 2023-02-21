package com.terraforming.ares.model.request;

import com.terraforming.ares.model.Expansion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 10.06.2022
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class AllProjectsRequest {
    private List<Expansion> expansions;
}
