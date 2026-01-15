package com.terraforming.ares.services.ai.network2.buildParams;

import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.ai.advanced.IDataCollect;
import com.terraforming.ares.services.ai.turnProcessors.AiUtility;
import lombok.Data;

@Data
public class DataCollectContext {
    private final IDataCollect dataCollect;
    private final CardService cardService;
    private final MarsContextProvider contextProvider;
    private final AiUtility aiUtility;
}
