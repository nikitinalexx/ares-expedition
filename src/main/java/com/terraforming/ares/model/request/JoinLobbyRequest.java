package com.terraforming.ares.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Created by oleksii.nikitin
 * Creation date 07.06.2022
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class JoinLobbyRequest {
    private String nickname;
}
