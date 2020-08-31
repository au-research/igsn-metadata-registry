package au.edu.ardc.registry.igsn.entity;

import au.edu.ardc.registry.common.entity.Record;
import org.hibernate.annotations.DiscriminatorOptions;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("IGSN")
public class IGSNRecord extends Record {

}
