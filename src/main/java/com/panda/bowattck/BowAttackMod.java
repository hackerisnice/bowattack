package com.panda.bowattck;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BowAttackMod implements ModInitializer {
    public static final String MOD_ID = "bowattack";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("BowAttack mod initialized! All hostile mobs are now armed with weakness arrows.");
    }
}
