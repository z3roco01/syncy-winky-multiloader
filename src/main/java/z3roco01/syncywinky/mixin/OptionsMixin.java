package z3roco01.syncywinky.mixin;

import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
//? if >=1.19.2 {
import net.minecraft.client.OptionInstance;
//? }
import net.minecraft.client.Options;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.packs.repository.PackRepository;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import z3roco01.syncywinky.ResourcePackUtil;
import z3roco01.syncywinky.loader.SyncyWinkyCommon;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

@Mixin(Options.class)
public abstract class OptionsMixin {
    @Shadow
    @Final
    private File optionsFile;

    @Shadow
    public List<String> resourcePacks;
    @Shadow
    public List<String> incompatibleResourcePacks;
    @Shadow
    protected Minecraft minecraft;

    @Shadow
    public abstract void save();

    @Shadow
    public abstract void broadcastOptions();

    @Shadow
    @Final
    private static Splitter OPTION_SPLITTER;

    //? if >=26.1 {
    @Shadow
    protected abstract String getFullscreenVideoModeString();
    //? }

    @Shadow
    protected abstract void processOptions(Options.FieldAccess fieldAccess);

    @Unique
    private static File globalOptionsFile;

    @Unique
    private static PackRepository packRepository = null;

    @Unique
    private static boolean hasInited = false;

    // holds every option ever in the file
    @Unique
    private static HashMap<String, String> optionsMap = new HashMap<>();

    @Unique
    private static void init() {
        // only run once hmm
        if(hasInited)
            return;

        hasInited = true;

        globalOptionsFile = new File(System.getProperty("user.home") + "/.config/syncy-winky/options.txt");
        // just need to create global config directory if it does not exist
        if(!globalOptionsFile.getParentFile().exists()) {
            // create all parent directories
            if(!globalOptionsFile.getParentFile().mkdirs()) {
                throw new RuntimeException("Could not create configuration directory \"" + globalOptionsFile.getParentFile().toString() + "\" for Syncy Winky !!!");
            }
            SyncyWinkyCommon.LOGGER.info("Created directory for config with path: " + globalOptionsFile.getParentFile().toString());
        }
        try {
            if(!globalOptionsFile.exists()) {
                globalOptionsFile.createNewFile();
                SyncyWinkyCommon.LOGGER.info("Created File: " + globalOptionsFile.getPath());
            }
        }catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    //? if (neoforge || fabric ){
    @Inject(method = "<init>", at = @At("HEAD"))
    private static void init(Minecraft minecraft, File workingDirectory, CallbackInfo ci) {
        init();
    }
    //?}

    @Inject(method = "load()V", at = @At("HEAD"))
    private void load(CallbackInfo ci) {
        // forge cant inject properly into constructors so have to do it on load which gets called at end of constructor
        //? if forge {
        /*init();
        *///? }

        // load in EVERY value present in config file manually
        try (BufferedReader reader = Files.newReader(globalOptionsFile, StandardCharsets.UTF_8)) {
            reader.lines().forEach((line) -> {
                    List<String> curOption = OPTION_SPLITTER.splitToList(line);
                    optionsMap.put(curOption.get(0), curOption.get(1));
            });
        }catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    //? if <=1.21.1 && (neoforge || forge) && > 1.18.2 {
    /*@Redirect(method = "load(Z)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;optionsFile:Ljava/io/File;", opcode = Opcodes.GETFIELD))
    *///? } else {
    @Redirect(method = "load", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;optionsFile:Ljava/io/File;", opcode = Opcodes.GETFIELD))
    //? }
    private File loadGetOptionsFile(Options options) {
        return globalOptionsFile;
    }

    @Redirect(method = "save", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;optionsFile:Ljava/io/File;", opcode = Opcodes.GETFIELD))
    private File saveGetOptionsFile(Options instance) {
        return globalOptionsFile;
    }

    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void save(CallbackInfo ci) {
        processOptions(new Options.FieldAccess() {
            //? if >=1.21.1 {
            public <T> void process(final String name, final OptionInstance<T> option) {
                option.codec().encodeStart(JsonOps.INSTANCE, option.get()).ifError((error)->SyncyWinkyCommon.LOGGER.error("Error saving option {}: {}", option, error.message())).ifSuccess((element)->{
                    optionsMap.put(name, Options.GSON.toJson(element));
                });
            }
            //? } elif >=1.19.2 {
            /*public <T> void process(final String name, final OptionInstance<T> option) {
                DataResult<JsonElement> dataResult = option.codec().encodeStart(JsonOps.INSTANCE, option.get());
                dataResult.error().ifPresent((partialResult) -> SyncyWinkyCommon.LOGGER.error("Error saving option " + option + ": " + partialResult));
                dataResult.result().ifPresent((jsonElement) -> {
                    optionsMap.put(name, Options.GSON.toJson(jsonElement));
                });
            }
            *///? }

            //? if <=1.18.2 {
            /*public <T> T process(String name, T value, IntFunction<T> intFunction, ToIntFunction<T> toIntFunction) {
                optionsMap.put(name, String.valueOf(toIntFunction.applyAsInt(value)));
                return value;
            }

            public double process(String name, double value) {
                optionsMap.put(name, String.valueOf(value));
                return value;
            }
            *///? }

            public int process(final String name, final int value) {
                optionsMap.put(name, String.valueOf(value));
                return value;
            }

            public boolean process(final String name, final boolean value) {
                optionsMap.put(name, String.valueOf(value));
                return value;
            }

            public String process(final String name, final String value) {
                optionsMap.put(name, value);
                return value;
            }

            public float process(final String name, final float value) {
                optionsMap.put(name, String.valueOf(value));
                return value;
            }

            public <T> T process(final String name, final T value, final Function<String, T> reader, final Function<T, String> converter) {
                optionsMap.put(name, (String)converter.apply(value));
                return value;
            }
        });
        //? if >=26.1 {
        String fullscreenVideoModeString = this.getFullscreenVideoModeString();
        if (fullscreenVideoModeString != null) {
            optionsMap.put("fullscreenResolution", fullscreenVideoModeString);
        }
        //? } else {
        /*if (this.minecraft.getWindow().getPreferredFullscreenVideoMode().isPresent()) {
            optionsMap.put("fullscreenResolution", ((VideoMode)this.minecraft.getWindow().getPreferredFullscreenVideoMode().get()).write());
        }
        *///? }

        // now write the map to the file, includes all
        try(final PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(globalOptionsFile), StandardCharsets.UTF_8))) {
            //? if >=26.1 {
            writer.println("version:" + SharedConstants.getCurrentVersion().dataVersion().version());
            //? } elif >=1.20.1 {
            /*writer.println("version:" + SharedConstants.getCurrentVersion().getDataVersion().getVersion());
            *///? } else {
            /*writer.println("version:" + SharedConstants.getCurrentVersion().getWorldVersion());
            *///? }
            for(Map.Entry<String, String> entry : optionsMap.entrySet()) {
                writer.print(entry.getKey() + ":" + entry.getValue());
                writer.println();
            }
        }catch(IOException e) {
            SyncyWinkyCommon.LOGGER.error("Failed to save options ", e);
        }

        this.broadcastOptions();
        ci.cancel();
    }

    // not used in Options class grrrrr but just in #case
    @Inject(method = "getFile", at = @At("HEAD"), cancellable = true)
    private void getFile(CallbackInfoReturnable<File> cir) {
        cir.setReturnValue(globalOptionsFile);
        cir.cancel();
    }

    @Inject(method = "processOptions", at = @At("TAIL"))
    private void processOptions(Options.FieldAccess fieldAccess, CallbackInfo ci) {
        try {
            ResourcePackUtil.loadResourcePacks();
        }catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    // dont modify the resourcepack list from the file, creates new list of actually applied packs
    //? if >=1.20.1 {
    @Inject(method = "updateResourcePacks", at = @At("HEAD"), cancellable = true)
    private void updateResourcePacks(PackRepository packRepository, CallbackInfo ci) {
        ResourcePackUtil.updateResourcePacks(packRepository, (Options)(Object)this, this.minecraft);
        ci.cancel();
    }
    //?}

    // have it apply packs from the proper list, instead of file list
    @ModifyVariable(method = "loadSelectedResourcePacks", at = @At("STORE"))
    private Iterator<String> loadPacksIterator(Iterator<String> iterator) {
        return ResourcePackUtil.appliedResourcePacks.iterator();
    }
}