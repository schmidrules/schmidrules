package org.schmidrules.xmi;

public final class IdGenerator {

    private static final String ID_PREFIX = "EAID";

    private IdGenerator() {
        // Utility class should have no public constructor
    }

    /**
     * Creates random ids. Example: EAID_7B555E75_4A58_46d5_A77B_401C0FA557C7
     * 
     * @return generated id with the predefined prefix
     */
    public static String createId() {
        String delimiter = "_";

        StringBuilder id = new StringBuilder(ID_PREFIX);
        id.append(delimiter).append(randomString(8));
        id.append(delimiter).append(randomString(4));
        id.append(delimiter).append(randomString(4));
        id.append(delimiter).append(randomString(4));
        id.append(delimiter).append(randomString(12));

        return id.toString();
    }

    private static String randomString(int length) {

        char[] charPool = { 'A', 'B', 'C', 'D', 'E', 'F', 'a', 'b', 'c', 'd', 'e', 'f', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };

        StringBuilder strBuilder = new StringBuilder();

        for (int i = 0; i < length; i++) {
            strBuilder.append(charPool[(int) Math.floor(Math.random() * charPool.length)]);
        }

        return strBuilder.toString();
    }

}
