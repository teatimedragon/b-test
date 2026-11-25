package com.somecompany.marketplace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A listing exactly as received (all values as strings), written before validation. */
@Entity
@Table(name = "ListingLog")
public class ListingLogEntity {

    @Id
    @Column(name = "LogId", nullable = false)
    private UUID logId;

    @Column(name = "LogTime", nullable = false)
    private Instant logTime;

    @Column(name = "SourceId", length = 50)
    private String sourceId;

    @Column(name = "SourceCode", length = 100)
    private String sourceCode;

    @Column(name = "Address", length = 100)
    private String address;

    @Column(name = "City", length = 100)
    private String city;

    @Column(name = "State", length = 2)
    private String state;

    @Column(name = "Zip", length = 50)
    private String zip;

    @Column(name = "Price", length = 100)
    private String price;

    @Column(name = "CurrencyCode", length = 3)
    private String currencyCode = "USD";

    @Column(name = "Bedrooms", length = 50)
    private String bedrooms;

    @Column(name = "Bathrooms", length = 50)
    private String bathrooms;

    @Column(name = "SquareFeet", length = 100)
    private String squareFeet;

    @Column(name = "Latitude", length = 20)
    private String latitude;

    @Column(name = "Longitude", length = 20)
    private String longitude;

    @Column(name = "ListedDate", length = 100)
    private String listedDate;

    @Column(name = "Status", length = 50)
    private String status;

    @Column(name = "Description", length = 250)
    private String description;

    public UUID getLogId() { return logId; }
    public void setLogId(UUID logId) { this.logId = logId; }
    public Instant getLogTime() { return logTime; }
    public void setLogTime(Instant logTime) { this.logTime = logTime; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getBedrooms() { return bedrooms; }
    public void setBedrooms(String bedrooms) { this.bedrooms = bedrooms; }
    public String getBathrooms() { return bathrooms; }
    public void setBathrooms(String bathrooms) { this.bathrooms = bathrooms; }
    public String getSquareFeet() { return squareFeet; }
    public void setSquareFeet(String squareFeet) { this.squareFeet = squareFeet; }
    public String getLatitude() { return latitude; }
    public void setLatitude(String latitude) { this.latitude = latitude; }
    public String getLongitude() { return longitude; }
    public void setLongitude(String longitude) { this.longitude = longitude; }
    public String getListedDate() { return listedDate; }
    public void setListedDate(String listedDate) { this.listedDate = listedDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
