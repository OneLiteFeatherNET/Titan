package net.onelitefeather.titan.blockhandler.objects;

import java.util.List;

public record SignSide(boolean glowing, String color, List<String> message) {

    public static SignSide of(boolean glowing, String color, List<String> message) {
        return new SignSide(glowing, color, message);
    }
}
