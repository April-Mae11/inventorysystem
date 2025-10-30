package com.nva.printing.inventory;

public enum Status {
    OUT_OF_STOCK("OUT OF STOCK", "status-out"),
    LOW_STOCK("LOW STOCK", "status-low"),
    NORMAL("NORMAL STOCK", "status-normal");

    private final String display;
    private final String cssClass;

    Status(String display, String cssClass) {
        this.display = display;
        this.cssClass = cssClass;
    }

    public String getDisplay() {
        return display;
    }

    public String getCssClass() {
        return cssClass;
    }

    public static Status fromItem(java.util.Optional<String> opt) {
        if (opt.isEmpty()) return NORMAL;
        String s = opt.get();
        if ("OUT OF STOCK".equalsIgnoreCase(s)) return OUT_OF_STOCK;
        if ("LOW STOCK".equalsIgnoreCase(s)) return LOW_STOCK;
        return NORMAL;
    }
}
