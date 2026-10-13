package com.megaworld.computer;

/** Мост между блоком (общий код) и экраном (только клиент). */
public final class ComputerHooks {
    public static Runnable openComputer = () -> {};

    private ComputerHooks() {}
}
