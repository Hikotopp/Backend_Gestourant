package com.gestourant.restaurant;
import jakarta.validation.Valid; import jakarta.validation.constraints.Min; import org.springframework.http.HttpStatus; import org.springframework.web.bind.annotation.*; import java.util.List;
@RestController @RequestMapping("/api/tables")
public class TableController { private final RestaurantTableRepository tables; public TableController(RestaurantTableRepository t){tables=t;}
 @GetMapping public List<RestaurantTable> list(){return tables.findAll();}
 @PostMapping @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')") @ResponseStatus(HttpStatus.CREATED) public RestaurantTable create(@Valid @RequestBody TableRequest r){if(tables.existsByTableNumber(r.tableNumber()))throw new IllegalArgumentException("El número de mesa ya existe");return tables.save(new RestaurantTable(r.tableNumber(),r.seats()));}
 @PutMapping("/{id}") @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')") public RestaurantTable update(@PathVariable Long id,@Valid @RequestBody TableRequest r){RestaurantTable t=tables.findById(id).orElseThrow(()->new IllegalArgumentException("Mesa no encontrada")); if(!t.getTableNumber().equals(r.tableNumber())&&tables.existsByTableNumber(r.tableNumber()))throw new IllegalArgumentException("El número de mesa ya existe");t.update(r.tableNumber(),r.seats());return tables.save(t);}
 @DeleteMapping("/{id}") @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id){tables.delete(tables.findById(id).orElseThrow(()->new IllegalArgumentException("Mesa no encontrada")));}
 public record TableRequest(@Min(1) Integer tableNumber,@Min(1) Integer seats){}
}
