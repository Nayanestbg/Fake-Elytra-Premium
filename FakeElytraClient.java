package nevercry.larp;

import net.fabricmc.api.ClientModInitializer;

import nevercry.larp.flight.ElytraCam;

public class FakeElytraClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ElytraCam.init();
	}
}
