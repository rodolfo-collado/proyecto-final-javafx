package com.practice.aulaequilibrio.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;
@Getter
@NoArgsConstructor
@Entity
@Table(name="classroom_type")
public class ClassroomType {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;
    @Version
    private long revision;
    @Column(nullable=false,unique=true,length=60)
    @Setter
    private String name;
    @OneToMany(mappedBy="type")
    private List<Classroom> classrooms=new ArrayList<>();
    @Override
    public String toString() {
        return name;
    }
}
