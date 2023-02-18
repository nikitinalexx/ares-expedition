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

import static org.junit.jupiter.api.Assertions.*;

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
                        new AnaerobicMicroorganismsPayment(),
                        new HeatPayment(6),
                        new RestructuredResourcesPayment()
                ),
                Map.of(6, List.of(7, 8, 9))
        );

        BuildBlueRedProjectTurn actualTurn = (BuildBlueRedProjectTurn) serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getProjectId(), actualTurn.getProjectId());
        assertEquals(expectedTurn.getPayments(), actualTurn.getPayments());
        assertEquals(expectedTurn.getInputParams(), actualTurn.getInputParams());
        assertFalse(expectedTurn.expectedAsNextTurn());
    }

    @Test
    void testGreenProjectTurn() throws Exception {
        BuildGreenProjectTurn expectedTurn = new BuildGreenProjectTurn(
                "uuid",
                5,
                List.of(
                        new MegacreditsPayment(5),
                        new AnaerobicMicroorganismsPayment(),
                        new HeatPayment(6),
                        new RestructuredResourcesPayment()
                ),
                Map.of(6, List.of(7, 8, 9))
        );

        BuildGreenProjectTurn actualTurn = (BuildGreenProjectTurn) serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getProjectId(), actualTurn.getProjectId());
        assertEquals(expectedTurn.getPayments(), actualTurn.getPayments());
        assertEquals(expectedTurn.getInputParams(), actualTurn.getInputParams());
        assertFalse(expectedTurn.expectedAsNextTurn());
    }

    @Test
    void testCollectIncomeTurn() throws Exception {
        CollectIncomeTurn expectedTurn = new CollectIncomeTurn("uuid", null);
        CollectIncomeTurn actualTurn = (CollectIncomeTurn) serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertFalse(expectedTurn.expectedAsNextTurn());
    }

    @Test
    void testCorporationChoiceTurn() throws Exception {
        CorporationChoiceTurn expectedTurn = new CorporationChoiceTurn("uuid", 5);
        CorporationChoiceTurn actualTurn = (CorporationChoiceTurn) serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getCorporationCardId(), actualTurn.getCorporationCardId());
        assertFalse(expectedTurn.expectedAsNextTurn());
    }

    @Test
    void testDiscardCardsTurn() throws Exception {
        DiscardCardsTurn expectedTurn = new DiscardCardsTurn(
                "uuid",
                List.of(1, 2, 3, 4, 5),
                3,
                true,
                true
        );

        DiscardCardsTurn actualTurn = (DiscardCardsTurn) serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getCards(), actualTurn.getCards());
        assertEquals(expectedTurn.getSize(), actualTurn.getSize());
        assertEquals(expectedTurn.isOnlyFromSelectedCards(), actualTurn.isOnlyFromSelectedCards());
        assertTrue(expectedTurn.expectedAsNextTurn());
    }

    @Test
    void testDraftCardsTurn() throws Exception {
        DraftCardsTurn expectedTurn = new DraftCardsTurn("uuid");
        DraftCardsTurn actualTurn = (DraftCardsTurn) serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertFalse(expectedTurn.expectedAsNextTurn());
    }

    @Test
    void testExchangeHeatTurn() throws Exception {
        ExchangeHeatTurn expectedTurn = new ExchangeHeatTurn("uuid", 10);
        ExchangeHeatTurn actualTurn = (ExchangeHeatTurn) serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getValue(), actualTurn.getValue());
        assertFalse(expectedTurn.expectedAsNextTurn());
    }

    @Test
    void testGameEndConfirmTurn() throws Exception {
        GameEndConfirmTurn expectedTurn = new GameEndConfirmTurn("uuid");
        GameEndConfirmTurn actualTurn = (GameEndConfirmTurn) serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertFalse(expectedTurn.expectedAsNextTurn());
    }

    @Test
    void testUnmiRtTurn() throws Exception {
        UnmiRtTurn expectedTurn = new UnmiRtTurn("uuid");
        UnmiRtTurn actualTurn = (UnmiRtTurn) serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertFalse(expectedTurn.expectedAsNextTurn());
    }

    @Test
    void testIncreaseTemperatureTurn() throws Exception {
        IncreaseTemperatureTurn expectedTurn = new IncreaseTemperatureTurn("uuid");
        IncreaseTemperatureTurn actualTurn = (IncreaseTemperatureTurn) serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertFalse(expectedTurn.expectedAsNextTurn());
    }

    @Test
    void testPerformBlueActionTurn() throws Exception {
        PerformBlueActionTurn expectedTurn = new PerformBlueActionTurn(
                "uuid",
                6,
                List.of(7, 8)

        );
        PerformBlueActionTurn actualTurn = (PerformBlueActionTurn) serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getProjectId(), actualTurn.getProjectId());
        assertEquals(expectedTurn.getInputParams(), actualTurn.getInputParams());
        assertFalse(expectedTurn.expectedAsNextTurn());
    }

    @Test
    void testPhaseChoiceTurn() throws Exception {
        PhaseChoiceTurn expectedTurn = new PhaseChoiceTurn(
                "uuid",
                20

        );
        PhaseChoiceTurn actualTurn = (PhaseChoiceTurn) serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getPhaseId(), actualTurn.getPhaseId());
        assertFalse(expectedTurn.expectedAsNextTurn());
    }

    @Test
    void testPickExtraCardTurn() throws Exception {
        PickExtraBonusSecondPhase expectedTurn = new PickExtraBonusSecondPhase("uuid");
        PickExtraBonusSecondPhase actualTurn = (PickExtraBonusSecondPhase) serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertFalse(expectedTurn.expectedAsNextTurn());
    }

    @Test
    void testPlantForestTurn() throws Exception {
        PlantForestTurn expectedTurn = new PlantForestTurn("uuid");
        PlantForestTurn actualTurn = (PlantForestTurn) serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertFalse(expectedTurn.expectedAsNextTurn());
    }

    @Test
    void testSellCardsTurn() throws Exception {
        SellCardsTurn expectedTurn = new SellCardsTurn(
                "uuid",
                List.of(100, 101, 102)
        );
        SellCardsTurn actualTurn = (SellCardsTurn) serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getCards(), actualTurn.getCards());
        assertFalse(expectedTurn.expectedAsNextTurn());
    }

    @Test
    void testMulliganTurn() throws Exception {
        MulliganTurn expectedTurn = new MulliganTurn(
                "uuid",
                List.of(100, 101, 102)
        );
        MulliganTurn actualTurn = (MulliganTurn) serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getCards(), actualTurn.getCards());
        assertFalse(expectedTurn.expectedAsNextTurn());
    }

    @Test
    void testSellCardsLastRoundTurn() throws Exception {
        SellCardsLastRoundTurn expectedTurn = new SellCardsLastRoundTurn(
                "uuid",
                List.of(100, 101, 102)
        );
        SellCardsLastRoundTurn actualTurn = (SellCardsLastRoundTurn) serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getCards(), actualTurn.getCards());
        assertFalse(expectedTurn.expectedAsNextTurn());
    }

    @Test
    void testSkipTurn() throws Exception {
        SkipTurn expectedTurn = new SkipTurn("uuid");
        SkipTurn actualTurn = (SkipTurn) serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertFalse(expectedTurn.expectedAsNextTurn());
    }

    @Test
    void testStandardProjectTurn() throws Exception {
        StandardProjectTurn expectedTurn = new StandardProjectTurn(
                "uuid",
                StandardProjectType.FOREST
        );
        StandardProjectTurn actualTurn = (StandardProjectTurn) serializeDeserialize(expectedTurn);

        assertEquals(expectedTurn.getPlayerUuid(), actualTurn.getPlayerUuid());
        assertEquals(expectedTurn.getProjectType(), actualTurn.getProjectType());
        assertFalse(expectedTurn.expectedAsNextTurn());
    }

    @SuppressWarnings("unchecked")
    private <T> Turn serializeDeserialize(T t) throws Exception {
        String json = objectMapper.writeValueAsString(t);
        return objectMapper.readValue(json, Turn.class);
    }
}
