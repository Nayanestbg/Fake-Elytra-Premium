package nevercry.larp;

// safed vorherigen pres
public class PriceStore {

    private static volatile String price = "161M";

    private PriceStore() {}

    public static String getPrice() {
        return price;
    }

    public static void setPrice(String newPrice) {
        price = newPrice;
    }
}
