package com.terraforming.ares.services.ai.turnProcessors.network2.buildParams;

import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.ai.advanced.IDataCollect;
import com.terraforming.ares.services.ai.turnProcessors.AiUtility;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
@Getter
public class DataCollectContextProvider {
    private final DataCollectContext dataCollectContext;

    public DataCollectContextProvider(IDataCollect dataCollect, CardService cardService, MarsContextProvider contextProvider, AiUtility aiUtility) {
        this.dataCollectContext = new DataCollectContext(dataCollect, cardService, contextProvider, aiUtility);
    }

}
