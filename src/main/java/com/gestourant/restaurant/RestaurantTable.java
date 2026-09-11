package com.gestourant.restaurant;
import jakarta.persistence.*;
@Entity @Table(name = "restaurant_tables")
public class RestaurantTable {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="table_number",nullable=false,unique=true) private Integer tableNumber;
 @Column(nullable=false) private Integer seats;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private TableStatus status=TableStatus.LIBRE;
 protected RestaurantTable(){} public RestaurantTable(Integer n,Integer s){tableNumber=n;seats=s;}
 public Long getId(){return id;} public Integer getTableNumber(){return tableNumber;} public Integer getSeats(){return seats;} public TableStatus getStatus(){return status;}
 public void update(Integer n,Integer s){tableNumber=n;seats=s;} public void occupy(){status=TableStatus.OCUPADA;} public void free(){status=TableStatus.LIBRE;}
}
