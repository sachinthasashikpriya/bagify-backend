package com.mycompany.app.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "sellers")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
public class Seller extends User {
    private Integer totalProducts;
    private Integer itemsSold;
    private BigDecimal revenue;
    private Float rating;
}
