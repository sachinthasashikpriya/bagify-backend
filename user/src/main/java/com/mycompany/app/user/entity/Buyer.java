package com.mycompany.app.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "buyers")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
public class Buyer extends User {
    private Integer totalOrders;
    private BigDecimal totalSpent;
    private Integer cartItems;
    private Integer wishList;
}
