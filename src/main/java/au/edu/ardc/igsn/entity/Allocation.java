package au.edu.ardc.igsn.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import javax.persistence.*;

@Entity
@Table(name = "allocations")
public class Allocation {

    @Id
    private Long id;

    private String name;

    public Long getId() {
        return this.id;
    }

}
