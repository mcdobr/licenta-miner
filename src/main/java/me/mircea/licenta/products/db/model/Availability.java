package me.mircea.licenta.products.db.model;

/**
 * This is meant to mirror https://schema.org/ItemAvailability
 */
public enum Availability {
    DISCONTINUED("discontinued"),
    IN_STOCK("instock"),
    IN_STORE_ONLY("instoreonly"),
    LIMITED_AVAILABILITY("limitedavailability"),
    ONLINE_ONLY("onlineonly"),
    OUT_OF_STOCK("outofstock"),
    PREORDER("preorder"),
    PRESALE("presale"),
    SOLDOUT("soldout");

    String type;

    Availability(String type) {
        this.type = type.trim().toLowerCase();
    }
}