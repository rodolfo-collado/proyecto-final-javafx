package com.practice.aulaequilibrio.model;
import jakarta.persistence.*;
@Entity
@Table(name="classroom")
public class Classroom {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;
    @Version private long revision;
    @Column(nullable=false,unique=true,length=20)
    private String codigo;
    @Column(nullable=false)
    private Integer capacidad;
    @ManyToOne(optional=false)
    @JoinColumn(name="type_id",nullable=false,foreignKey=@ForeignKey(name="fk_classroom_type"))
    private ClassroomType type;
    public Classroom() {}
    public Integer getId(){return id;}
    public long getRevision(){return revision;}
    public String getCodigo(){return codigo;}
    public void setCodigo(String codigo){this.codigo=codigo;}
    public Integer getCapacidad(){return capacidad;}
    public void setCapacidad(Integer capacidad){this.capacidad=capacidad;}
    public ClassroomType getType(){return type;}
    public void setType(ClassroomType type){this.type=type;}
    @Override public String toString(){return codigo;}
}
