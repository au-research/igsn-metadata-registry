package au.edu.ardc.igsn.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "prefix")
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")

public class Prefix {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String value;
    private String description;
    @ManyToMany(targetEntity = Registrant.class, mappedBy = "prefixes", cascade = {CascadeType.PERSIST, CascadeType.DETACH,CascadeType.MERGE,CascadeType.REFRESH})
    private List<Registrant> registrants;
	
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getValue() {
        return this.value;
    }
    public void setValue(String v) {
        this.value = v;
    }
    public String getDescription() {
        return this.description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public List<Registrant> getRegistrants() {
        return registrants;
    }

    public void setRegistrants(List<Registrant> registrants) {
        this.registrants = registrants;
    }
	
}
