package com.practice.aulaequilibrio.dao;
import com.practice.aulaequilibrio.config.HibernateUtil;
import com.practice.aulaequilibrio.model.ClassroomType;
import jakarta.persistence.LockModeType;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.List;
public class ClassroomTypeDAO {
    public List<ClassroomType> listar() {
        try(Session s=HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery("from ClassroomType t order by t.name",ClassroomType.class).getResultList();
        }
    }
    public void guardar(ClassroomType value) {
        String name=value.getName()==null?"":value.getName().trim();
        if(name.isBlank() || name.length()>60)throw new IllegalArgumentException("El nombre del tipo es obligatorio y admite hasta 60 caracteres.");
        try(Session s=HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx=s.beginTransaction();
            try {
                long duplicates=s.createQuery("select count(t) from ClassroomType t where lower(t.name)=:name and (:id is null or t.id<>:id)",Long.class)
                    .setParameter("name",name.toLowerCase(java.util.Locale.ROOT)).setParameter("id",value.getId()).getSingleResult();
                if(duplicates>0)throw new IllegalArgumentException("Ya existe un tipo de aula con ese nombre.");
                if(value.getId()==null) {
                    ClassroomType created=new ClassroomType();created.setName(name);s.persist(created);
                }else {
                    ClassroomType current=s.find(ClassroomType.class,value.getId(),LockModeType.PESSIMISTIC_WRITE);
                    if(current==null)throw new IllegalArgumentException("El tipo ya no existe. Actualice la tabla.");
                    if(current.getRevision()!=value.getRevision())throw new IllegalArgumentException("El tipo fue modificado por otra persona. Actualice la tabla.");
                    current.setName(name);
                }
                tx.commit();
            }catch(RuntimeException ex){if(tx.isActive())tx.rollback();throw ex;}
        }
    }
    public void eliminar(ClassroomType value) {
        try(Session s=HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx=s.beginTransaction();
            try {
                ClassroomType current=s.find(ClassroomType.class,value.getId(),LockModeType.PESSIMISTIC_WRITE);
                if(current==null)throw new IllegalArgumentException("El tipo ya no existe.");
                if(current.getRevision()!=value.getRevision())throw new IllegalArgumentException("El tipo cambió; actualice la tabla.");
                long classrooms=s.createQuery("select count(c) from Classroom c join c.type t where t.id=:id",Long.class).setParameter("id",value.getId()).getSingleResult();
                if(classrooms>0)throw new IllegalArgumentException("No se puede eliminar el tipo porque todavía tiene aulas asociadas. Cambie el tipo de esas aulas o elimínelas primero.");
                s.remove(current);tx.commit();
            }catch(RuntimeException ex){if(tx.isActive())tx.rollback();throw ex;}
        }
    }
}
