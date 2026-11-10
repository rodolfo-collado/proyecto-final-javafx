package com.practice.aulaequilibrio.model;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name="classroom_type")
public class ClassroomType {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;
    @Version private long revision;
    @Column(nullable=false,unique=true,length=60)
    private String name;
    @OneToMany(mappedBy="type")
    private List<Classroom> classrooms=new ArrayList<>();
    public ClassroomType() {}
    public Integer getId(){return id;}
    public long getRevision(){return revision;}
    public String getName(){return name;}
    public void setName(String name){this.name=name;}
    public List<Classroom> getClassrooms(){return classrooms;}
    @Override public String toString(){return name;}
}
