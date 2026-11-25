package com.somecompany.marketplace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** A validated listing. Primary key is {@code SourceCode + ":" + SourceId}. */
@Entity
@Table(name = "Listing")
public class ListingEntity {

    @Id
    @Column(name = "ListingId", length = 150, nullable = false)
    private String listingId;

    @Column(name = "SourceId", length = 50, nullable = false)
    private String sourceId;

    @Column(name = "SourceCode", length = 100, nullable = false)
    private String sourceCode;

    @Column(name = "Address", length = 100, nullable = false)
    private String address;

    @Column(name = "City", length = 100, nullable = false)
    private String city;

    @Column(name = "State", length = 2, nullable = false)
    private String state;

    @Column(name = "Zip", length = 50, nullable = false)
    private String zip;

    @Column(name = "Price", precision = 19, scale = 4, nullable = false)
    private BigDecimal price;

    @Column(name = "CurrencyCode", length = 3, nullable = false)
    private String currencyCode = "USD";

    @Column(name = "Bedrooms", nullable = false)
    private Short bedrooms;

    @Column(name = "Bathrooms", precision = 6, scale = 2, nullable = false)
    private BigDecimal bathrooms;

    @Column(name = "SquareFeet", nullable = false)
    private Integer squareFeet;

    @Column(name = "Latitude", precision = 9, scale = 6, nullable = false)
    private BigDecimal latitude;

    @Column(name = "Longitude", precision = 9, scale = 6, nullable = false)
    private BigDecimal longitude;

    @Column(name = "ListedDate", length = 100, nullable = false)
    private String listedDate;

    @Column(name = "Status", length = 50, nullable = false)
    private String status;

    @Column(name = "Description", length = 250, nullable = false)
    private String description;

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }
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
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public Short getBedrooms() { return bedrooms; }
    public void setBedrooms(Short bedrooms) { this.bedrooms = bedrooms; }
    public BigDecimal getBathrooms() { return bathrooms; }
    public void setBathrooms(BigDecimal bathrooms) { this.bathrooms = bathrooms; }
    public Integer getSquareFeet() { return squareFeet; }
    public void setSquareFeet(Integer squareFeet) { this.squareFeet = squareFeet; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getListedDate() { return listedDate; }
    public void setListedDate(String listedDate) { this.listedDate = listedDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
