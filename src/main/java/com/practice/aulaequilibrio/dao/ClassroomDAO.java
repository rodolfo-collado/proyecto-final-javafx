package com.practice.aulaequilibrio.dao;
import com.practice.aulaequilibrio.config.HibernateUtil;
import com.practice.aulaequilibrio.model.*;
import jakarta.persistence.LockModeType;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.List;
import java.util.Locale;
public class ClassroomDAO {
    public List<Classroom> listar() {
        // HQL con relación: trae el tipo para mostrarlo en JavaFX después de cerrar la sesión.
        try(Session s=HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery("select c from Classroom c join fetch c.type t order by c.codigo",Classroom.class).getResultList();
        }
    }
    public List<Classroom> listarPorTipo(Integer typeId) {
        try(Session s=HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery("select c from Classroom c join fetch c.type t where t.id=:type order by c.codigo",Classroom.class).setParameter("type",typeId).getResultList();
        }
    }
    public void guardar(Classroom value) {
        String code=value.getCodigo()==null?"":value.getCodigo().trim().toUpperCase(Locale.ROOT);
        if(code.isBlank() || code.length()>20)throw new IllegalArgumentException("El código del aula es obligatorio y admite hasta 20 caracteres.");
        if(value.getCapacidad()==null || value.getCapacidad()<=0)throw new IllegalArgumentException("La capacidad debe ser un número entero mayor que cero.");
        if(value.getType()==null || value.getType().getId()==null)throw new IllegalArgumentException("Seleccione un tipo de aula existente.");
        try(Session s=HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx=s.beginTransaction();
            try {
                ClassroomType type=s.find(ClassroomType.class,value.getType().getId(),LockModeType.PESSIMISTIC_READ);
                if(type==null)throw new IllegalArgumentException("El tipo seleccionado ya no existe. Actualice la lista.");
                long duplicates=s.createQuery("select count(c) from Classroom c where upper(c.codigo)=:code and (:id is null or c.id<>:id)",Long.class)
                    .setParameter("code",code).setParameter("id",value.getId()).getSingleResult();
                if(duplicates>0)throw new IllegalArgumentException("Ya existe un aula con ese código.");
                Classroom current;
                if(value.getId()==null) {current=new Classroom();current.setCodigo(code);current.setCapacidad(value.getCapacidad());current.setType(type);s.persist(current);}
                else {
                    current=s.find(Classroom.class,value.getId(),LockModeType.PESSIMISTIC_WRITE);
                    if(current==null)throw new IllegalArgumentException("El aula ya no existe. Actualice la tabla.");
                    if(current.getRevision()!=value.getRevision())throw new IllegalArgumentException("El aula fue modificada por otra persona. Actualice la tabla.");
                    current.setCodigo(code);current.setCapacidad(value.getCapacidad());current.setType(type);
                }
                tx.commit();
            }catch(RuntimeException ex){if(tx.isActive())tx.rollback();throw ex;}
        }
    }
    public void eliminar(Classroom value) {
        try(Session s=HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx=s.beginTransaction();
            try {
                Classroom current=s.find(Classroom.class,value.getId(),LockModeType.PESSIMISTIC_WRITE);
                if(current==null)throw new IllegalArgumentException("El aula ya no existe.");
                if(current.getRevision()!=value.getRevision())throw new IllegalArgumentException("El aula cambió; actualice la tabla.");
                s.remove(current);tx.commit();
            }catch(RuntimeException ex){if(tx.isActive())tx.rollback();throw ex;}
        }
    }
}
