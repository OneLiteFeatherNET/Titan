package net.onelitefeather.titan.blockhandler.objects;

public record BannerPattern(int color, String pattern) {

    public static BannerPattern from(int color, String pattern) {
        return new BannerPattern(color, pattern);
    }

}
