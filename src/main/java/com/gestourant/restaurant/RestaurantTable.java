package com.gestourant.restaurant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name = "restaurant_tables")
public class RestaurantTable {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="joined_table_id") private Long joinedTableId;
 @Column(name="table_number",nullable=false,unique=true) private Integer tableNumber;
 @Column(nullable=false) private Integer seats;
 @Column(name="floor_x",nullable=false,precision=5,scale=2) private BigDecimal floorX=BigDecimal.valueOf(15.0);
 @Column(name="floor_y",nullable=false,precision=5,scale=2) private BigDecimal floorY=BigDecimal.valueOf(15.0);
 @Column(name="qr_token",nullable=false,unique=true,updatable=false,length=36) private String qrToken=UUID.randomUUID().toString();
 @Column(name="bill_requested",nullable=false) private boolean billRequested;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private TableStatus status=TableStatus.LIBRE;
 protected RestaurantTable(){} public RestaurantTable(Integer n,Integer s){tableNumber=n;seats=s;}
 public Long getId(){return id;} public Integer getTableNumber(){return tableNumber;} public Integer getSeats(){return seats;} public TableStatus getStatus(){return status;} public BigDecimal getFloorX(){return floorX;} public BigDecimal getFloorY(){return floorY;} public String getQrToken(){return qrToken;} public boolean isBillRequested(){return billRequested;}
 public void update(Integer n,Integer s){tableNumber=n;seats=s;} public void occupy(){status=TableStatus.OCUPADA;} public void free(){status=TableStatus.LIBRE;}
 public void updateFloorPosition(Double x,Double y){floorX=BigDecimal.valueOf(x);floorY=BigDecimal.valueOf(y);} public void requestBill(){billRequested=true;} public void clearBillRequest(){billRequested=false;}
 public Long getJoinedTableId(){return joinedTableId;} public void joinWith(Long tableId){joinedTableId=tableId;} public void unjoin(){joinedTableId=null;}
}
