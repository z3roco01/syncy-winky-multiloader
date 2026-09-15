package z3roco01.syncywinky.loader;

//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

public class SyncyWinkyFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SyncyWinkyCommon.init();
    }
}
//?}