package com.terraforming.ares.model.turn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraforming.ares.TerraformingAresApplication;
import com.terraforming.ares.model.StandardProjectType;
import com.terraforming.ares.model.payments.AnaerobicMicroorganismsPayment;
import com.terraforming.ares.model.payments.HeatPayment;
import com.terraforming.ares.model.payments.MegacreditsPayment;
import com.terraforming.ares.model.payments.RestructuredResourcesPayment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by oleksii.nikitin
 * Creation date 19.05.2022
 */
class TestTurnSerialization {

    private final ObjectMapper objectMapper = new TerraformingAresApplication().objectMapper();

    @Test
    void testBlueRedProjectTurn() throws Exception {
        BuildBlueRedProjectTurn expectedTurn = new BuildBlueRedProjectTurn(
                "uuid",
                5,
                List.of(
                        new MegacreditsPayment(5),
                        new AnaerobicMicroorganismsPayment(1),
                        new HeatPayment(6),
                        new RestructuredResourcesPayment(3)
                ),
                Map.of(6, List.of(7, 8, 9))
        );

        BuildBlueRedProjectTurn actualTurn = serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getProjectId(), actualTurn.getProjectId());
        assertEquals(expectedTurn.getPayments(), actualTurn.getPayments());
        assertEquals(expectedTurn.getInputParams(), actualTurn.getInputParams());
    }

    @Test
    void testGreenProjectTurn() throws Exception {
        BuildGreenProjectTurn expectedTurn = new BuildGreenProjectTurn(
                "uuid",
                5,
                List.of(
                        new MegacreditsPayment(5),
                        new AnaerobicMicroorganismsPayment(1),
                        new HeatPayment(6),
                        new RestructuredResourcesPayment(3)
                ),
                Map.of(6, List.of(7, 8, 9))
        );

        BuildGreenProjectTurn actualTurn = serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getProjectId(), actualTurn.getProjectId());
        assertEquals(expectedTurn.getPayments(), actualTurn.getPayments());
        assertEquals(expectedTurn.getInputParams(), actualTurn.getInputParams());
    }

    @Test
    void testCollectIncomeTurn() throws Exception {
        CollectIncomeTurn expectedTurn = new CollectIncomeTurn("uuid");
        CollectIncomeTurn actualTurn = serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
    }

    @Test
    void testCorporationChoiceTurn() throws Exception {
        CorporationChoiceTurn expectedTurn = new CorporationChoiceTurn("uuid", 5);
        CorporationChoiceTurn actualTurn = serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getCorporationCardId(), actualTurn.getCorporationCardId());
    }

    @Test
    void testDiscardCardsTurn() throws Exception {
        DiscardCardsTurn expectedTurn = new DiscardCardsTurn(
                "uuid",
                List.of(1, 2, 3, 4, 5),
                3,
                true
        );

        DiscardCardsTurn actualTurn = serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getCards(), actualTurn.getCards());
        assertEquals(expectedTurn.getSize(), actualTurn.getSize());
        assertEquals(expectedTurn.isOnlyFromSelectedCards(), actualTurn.isOnlyFromSelectedCards());
    }

    @Test
    void testDraftCardsTurn() throws Exception {
        DraftCardsTurn expectedTurn = new DraftCardsTurn("uuid");
        DraftCardsTurn actualTurn = serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
    }

    @Test
    void testExchangeHeatTurn() throws Exception {
        ExchangeHeatTurn expectedTurn = new ExchangeHeatTurn("uuid", 10);
        ExchangeHeatTurn actualTurn = serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getValue(), actualTurn.getValue());
    }

    @Test
    void testGameEndConfirmTurn() throws Exception {
        GameEndConfirmTurn expectedTurn = new GameEndConfirmTurn("uuid");
        GameEndConfirmTurn actualTurn = serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
    }

    @Test
    void testIncreaseTemperatureTurn() throws Exception {
        IncreaseTemperatureTurn expectedTurn = new IncreaseTemperatureTurn("uuid");
        IncreaseTemperatureTurn actualTurn = serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
    }

    @Test
    void testPerformBlueActionTurn() throws Exception {
        PerformBlueActionTurn expectedTurn = new PerformBlueActionTurn(
                "uuid",
                6,
                List.of(7, 8)

        );
        PerformBlueActionTurn actualTurn = serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getProjectId(), actualTurn.getProjectId());
        assertEquals(expectedTurn.getInputParams(), actualTurn.getInputParams());
    }

    @Test
    void testPhaseChoiceTurn() throws Exception {
        PhaseChoiceTurn expectedTurn = new PhaseChoiceTurn(
                "uuid",
                20

        );
        PhaseChoiceTurn actualTurn = serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getPhaseId(), actualTurn.getPhaseId());
    }

    @Test
    void testPickExtraCardTurn() throws Exception {
        PickExtraCardTurn expectedTurn = new PickExtraCardTurn("uuid");
        PickExtraCardTurn actualTurn = serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
    }

    @Test
    void testPlantForestTurn() throws Exception {
        PlantForestTurn expectedTurn = new PlantForestTurn("uuid");
        PlantForestTurn actualTurn = serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
    }

    @Test
    void testSellCardsTurn() throws Exception {
        SellCardsTurn expectedTurn = new SellCardsTurn(
                "uuid",
                List.of(100, 101, 102)
        );
        SellCardsTurn actualTurn = serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getCards(), actualTurn.getCards());
    }

    @Test
    void testSkipTurn() throws Exception {
        SkipTurn expectedTurn = new SkipTurn("uuid");
        SkipTurn actualTurn = serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
    }

    @Test
    void testStandardProjectTurn() throws Exception {
        StandardProjectTurn expectedTurn = new StandardProjectTurn(
                "uuid",
                StandardProjectType.FOREST
        );
        StandardProjectTurn actualTurn = serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getProjectType(), actualTurn.getProjectType());
    }

    @SuppressWarnings("unchecked")
    private <T> T serializeDeserialize(T t) throws Exception {
        String json = objectMapper.writeValueAsString(t);
        return (T) objectMapper.readValue(json, t.getClass());
    }
}
