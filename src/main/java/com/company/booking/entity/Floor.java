package com.company.booking.entity;

import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@JmixEntity
@Table(name = "FLOOR_", indexes = {
        @Index(name = "IDX_FLOOR__BUILDING", columnList = "BUILDING_ID")
}, uniqueConstraints = {
        @UniqueConstraint(name = "UK_FLOOR__BUILDING_NUMBER", columnNames = {"BUILDING_ID", "NUMBER_"})
})
@Entity(name = "Floor_")
public class Floor {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @Column(name = "VERSION", nullable = false)
    @Version
    private Integer version;

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @JoinColumn(name = "BUILDING_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Building building;

    @NotNull
    @Column(name = "NUMBER_", nullable = false)
    private Integer number;

    @Column(name = "NAME", length = 100)
    private String name;

    @Composition
    @OneToMany(mappedBy = "floor")
    private List<Room> rooms;

    @Composition
    @OneToMany(mappedBy = "floor")
    private List<Desk> desks;

    @InstanceName
    @DependsOnProperties({"number", "name"})
    public String getDisplayName() {
        if (name == null || name.isBlank()) {
            return String.valueOf(number);
        }
        return number + " — " + name;
    }

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }

    public List<Desk> getDesks() {
        return desks;
    }

    public void setDesks(List<Desk> desks) {
        this.desks = desks;
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
