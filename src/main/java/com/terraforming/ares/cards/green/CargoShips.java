package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 04.12.2023
 */
@RequiredArgsConstructor
@Getter
public class CargoShips implements InfrastructureExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public CargoShips(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Cargo Ships")
                .description("Effect: When you raise infrastructure, gain 2 heat or 2 plants.")
                .cardAction(CardAction.CARGO_SHIPS)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void onInfrastructureChangedEffect(MarsContext context) {
        MarsGame game = context.getGame();
        Player player = context.getPlayer();

        boolean takePlants = true;

        if (context.getInputParams() != null) {
            Map<Integer, List<Integer>> inputParams = context.getInputParams();

            List<Integer> cargoShipsInput = inputParams.getOrDefault(InputFlag.CARGO_SHIPS.getId(), List.of());

            if (!CollectionUtils.isEmpty(cargoShipsInput) && cargoShipsInput.get(0) == InputFlag.CARGO_SHIPS_HEAT.getId()) {
                takePlants = false;
            }
        }

        if (player.isComputer()) {
            Planet planet = game.getPlanetAtTheStartOfThePhase();
            if (planet.isOxygenMax() && !planet.isTemperatureMax()) {
                takePlants = false;
            }
        }

        if (takePlants) {
            player.setPlants(player.getPlants() + 2);
        } else {
            player.setHeat(player.getHeat() + 2);
        }
    }

    @Override
    public int getPrice() {
        return 15;
    }

}
