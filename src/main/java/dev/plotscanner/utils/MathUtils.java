package dev.plotscanner.utils;

import net.minecraft.util.math.Vec3d;

public class MathUtils {
    public static double lerp(double a, double b, double f) {
        return a * (1.0 - f) + (b * f);
    }
    public static Vec3d align(Vec3d vec) {
        return new Vec3d(Math.floor(vec.x), Math.floor(vec.y), Math.floor(vec.z));
    }

    public static double roundToDecimalPlaces(double num, int decimal_places) {
        return ((int) (num * (Math.pow(10, decimal_places)))) / Math.pow(10, decimal_places);
    }
}
