package com.practice.aulaequilibrio.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@NoArgsConstructor
@Entity
@Table(name="classroom")
public class Classroom {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;
    @Version
    private long revision;
    @Column(nullable=false,unique=true,length=20)
    @Setter
    private String codigo;
    @Column(nullable=false)
    @Setter
    private Integer capacidad;
    @ManyToOne(optional=false)
    @JoinColumn(name="type_id",nullable=false,foreignKey=@ForeignKey(name="fk_classroom_type"))
    @Setter
    private ClassroomType type;
    @Override
    public String toString() {
        return codigo;
    }
}
