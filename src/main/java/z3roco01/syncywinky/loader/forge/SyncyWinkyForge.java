package z3roco01.syncywinky.loader.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import z3roco01.syncywinky.loader.SyncyWinkyCommon;

//? if forge {
@Mod(SyncyWinkyCommon.MOD_ID)
public class SyncyWinkyForge {
    public SyncyWinkyForge() {
        if(FMLEnvironment.dist.isClient()) {
            SyncyWinkyCommon.init();
        }
    }
}
//? }
