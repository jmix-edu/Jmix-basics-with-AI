package com.company.booking.entity;

import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

@JmixEntity
@Table(name = "ROOM", indexes = {
        @Index(name = "IDX_ROOM_FLOOR", columnList = "FLOOR_ID")
}, uniqueConstraints = {
        @UniqueConstraint(name = "UK_ROOM_FLOOR_NAME", columnNames = {"FLOOR_ID", "NAME"})
})
@Entity
public class Room {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @Column(name = "VERSION", nullable = false)
    @Version
    private Integer version;

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @JoinColumn(name = "FLOOR_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Floor floor;

    @InstanceName
    @NotNull
    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Positive
    @NotNull
    @Column(name = "CAPACITY", nullable = false)
    private Integer capacity;

    @JoinTable(name = "ROOM_AMENITY_LINK",
            joinColumns = @JoinColumn(name = "ROOM_ID", referencedColumnName = "ID"),
            inverseJoinColumns = @JoinColumn(name = "AMENITY_ID", referencedColumnName = "ID"))
    @ManyToMany
    private List<Amenity> amenities;

    public Floor getFloor() {
        return floor;
    }

    public void setFloor(Floor floor) {
        this.floor = floor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public List<Amenity> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<Amenity> amenities) {
        this.amenities = amenities;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

}
