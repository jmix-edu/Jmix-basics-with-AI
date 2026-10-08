package com.company.booking.entity;

import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@JmixEntity
@Table(name = "BOOKING", indexes = {
        @Index(name = "IDX_BOOKING_USER", columnList = "USER_ID"),
        @Index(name = "IDX_BOOKING_ROOM_START_AT", columnList = "ROOM_ID, START_AT"),
        @Index(name = "IDX_BOOKING_DESK_START_AT", columnList = "DESK_ID, START_AT")
})
@Entity
public class Booking {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @Column(name = "VERSION", nullable = false)
    @Version
    private Integer version;

    @OnDeleteInverse(DeletePolicy.DENY)
    @JoinColumn(name = "USER_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @JoinColumn(name = "ROOM_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private Room room;

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @JoinColumn(name = "DESK_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private Desk desk;

    @NotNull
    @Column(name = "START_AT", nullable = false)
    private LocalDateTime startAt;

    @NotNull
    @Column(name = "END_AT", nullable = false)
    private LocalDateTime endAt;

    @NotNull
    @Column(name = "STATUS", nullable = false, length = 50)
    private String status = BookingStatus.CONFIRMED.getId();

    @Column(name = "COMMENT_", length = 500)
    private String comment;

    @InstanceName
    @DependsOnProperties({"startAt", "endAt"})
    public String getDisplayName() {
        DateTimeFormatter dateTime = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        DateTimeFormatter time = DateTimeFormatter.ofPattern("HH:mm");
        String start = startAt != null ? startAt.format(dateTime) : "";
        String end = endAt != null ? endAt.format(time) : "";
        return start + " – " + end;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Desk getDesk() {
        return desk;
    }

    public void setDesk(Desk desk) {
        this.desk = desk;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public BookingStatus getStatus() {
        return status == null ? null : BookingStatus.fromId(status);
    }

    public void setStatus(BookingStatus status) {
        this.status = status == null ? null : status.getId();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
