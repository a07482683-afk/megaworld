package com.megaworld.block;

import net.minecraft.block.Block;

/**
 * Магнитный блок. Сама логика притяжения игрока к центру блока выполняется
 * на клиенте (см. MagnetClient), потому что движением игрока управляет клиент:
 * так притяжение плавное и не вызывает рывков и «кика за полёт».
 */
public class MagnetBlock extends Block {
    public MagnetBlock(Settings settings) {
        super(settings);
    }
}
