package com.nva.printing.inventory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Supplier {
    private String name;
    private String contactNumber;
    private String address;
    private String suppliedProduct;

    public Supplier() {}

    public Supplier(String name, String contactNumber, String address, String suppliedProduct) {
        this.name = name;
        this.contactNumber = contactNumber;
        this.address = address;
        this.suppliedProduct = suppliedProduct;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getSuppliedProduct() { return suppliedProduct; }
    public void setSuppliedProduct(String suppliedProduct) { this.suppliedProduct = suppliedProduct; }
}
