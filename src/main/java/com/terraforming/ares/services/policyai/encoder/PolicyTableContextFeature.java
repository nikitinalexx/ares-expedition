package com.terraforming.ares.services.policyai.encoder;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildDto;
import com.terraforming.ares.model.BuildType;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PolicyTableContextFeature implements PolicyFeatureBlock {

    public static final List<BuildDto> ALL_BUILD_DTOS = List.of(
            new BuildDto(BuildType.BLUE_RED),
            new BuildDto(BuildType.BLUE_RED),
            new BuildDto(BuildType.BLUE_RED, 11),

            new BuildDto(BuildType.BLUE_RED_OR_CARD),
            new BuildDto(BuildType.BLUE_RED_OR_MC),


            new BuildDto(BuildType.GREEN_OR_BLUE, 2),
            new BuildDto(BuildType.GREEN_OR_BLUE, 25),
            new BuildDto(BuildType.GREEN_OR_BLUE, 12, 12),

            new BuildDto(BuildType.GREEN),
            new BuildDto(BuildType.GREEN, 3),
            new BuildDto(BuildType.GREEN, 6),
            new BuildDto(BuildType.GREEN, 9, 9),
            new BuildDto(BuildType.GREEN, 0, 12)
    );

    @Override
    public void encode(TableContext ctx, PolicyRecord out) {
        MarsGame game = ctx.getGame();
        CardService cardService = ctx.getCardService();
        Player player = ctx.getPlayer();
        Player opponent = ctx.getOpponent();

        int currentPhase = game.getCurrentPhase();
        if (currentPhase == 3) {
            Set<Class<?>> activatedOnce =
                    player.getActivatedBlueCards()
                            .getCards()
                            .stream()
                            .map(id -> cardService.getCard(id).getClass())
                            .collect(Collectors.toSet());

            Set<Class<?>> activatedTwice =
                    player.getActivatedBlueCardsTwice()
                            .getCards()
                            .stream()
                            .map(id -> cardService.getCard(id).getClass())
                            .collect(Collectors.toSet());

            out.blueActionFirstMask = 0L;
            out.blueActionSecondMask = 0L;
            out.blueActionSingleMask = 0;

            for (int i = 0; i < AiConstants.WITH_PAY_BLUE_ACTIONS.size(); i++) {
                Class<?> clazz = AiConstants.WITH_PAY_BLUE_ACTIONS.get(i);

                if (!activatedOnce.contains(clazz)) {
                    out.blueActionFirstMask |= (1L << i);
                }

                if (!activatedTwice.contains(clazz)) {
                    out.blueActionSecondMask |= (1L << i);
                }
            }

            for (int i = 0; i < AiConstants.NO_PAYMENT_BLUE_ACTIONS.size(); i++) {
                Class<?> clazz = AiConstants.NO_PAYMENT_BLUE_ACTIONS.get(i);

                if (!activatedTwice.contains(clazz)) {
                    out.blueActionSingleMask |= (1 << i);
                }
            }
        }

        out.blueExtraActivationsLeft =
                (byte) (currentPhase == 3
                        ? player.getBlueActionExtraActivationsLeft()
                        : 0);

        out.builtSpecialDesignLastTurn = player.isBuiltSpecialDesignLastTurn();
        out.unmiTurnAvailable = player.isHasUnmiAction();

        if (currentPhase >= 1 && currentPhase <= 5) {
            out.currentPhaseMask = (byte) (1 << (currentPhase - 1));
        }


        if (!player.getBuilds().isEmpty()) {
            List<BuildDto> playerBuilds = player.getBuilds();

            int ownedBuildMask = 0;

            Map<BuildDto, Long> counts =
                    playerBuilds.stream()
                            .collect(Collectors.groupingBy(
                                    b -> b,
                                    Collectors.counting()
                            ));

            for (int i = 0; i < ALL_BUILD_DTOS.size(); i++) {

                BuildDto dto = ALL_BUILD_DTOS.get(i);
                long count = counts.getOrDefault(dto, 0L);

                if (i == 0 && count >= 1)
                    ownedBuildMask |= (1 << i);

                else if (i == 1 && count >= 2)
                    ownedBuildMask |= (1 << i);

                else if (count > 0)
                    ownedBuildMask |= (1 << i);
            }
            out.ownedBuildsMask = (short) ownedBuildMask;
        }

        out.dummyStateMask = (byte) getDummyStateMask(game);

        out.chosenPhaseMask[0] = phaseToMask(player.getChosenPhase());
        out.chosenPhaseMask[1] = phaseToMask(opponent.getChosenPhase());

        out.previousPhaseMask[0] = phaseToMask(player.getPreviousChosenPhase());
        out.previousPhaseMask[1] = phaseToMask(opponent.getPreviousChosenPhase());
    }

    private static byte phaseToMask(Integer phase) {
        if (phase == null || phase < 1) {
            return 0;
        }
        return (byte) (1 << (phase - 1));
    }

    public int getDummyStateMask(MarsGame game) {

        if (!game.isDummyHandMode() || game.getUsedDummyHand() == null) {
            return 0;
        }

        int mask = 0;

        for (int phase : game.getUsedDummyHand()) {
            mask |= (1 << (phase - 1));
        }

        return mask;
    }


}
