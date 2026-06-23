package com.adam.firemagic.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

public class ModPackets {
    public static final PacketByteBuf ADD_RESOURCES_PACKET = PacketByteBufs.create();
    public static final PacketByteBuf EXTRACT_RESOURCES_PACKET = PacketByteBufs.create();
    public static final PacketByteBuf APPLY_UPGRADES_PACKET = PacketByteBufs.create();
}