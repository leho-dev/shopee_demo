/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dev.repository.impl;

import com.dev.pojo.Rating;
import com.dev.pojo.User;
import com.dev.repository.UserReppository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author admin
 */
@Repository
@Transactional
public class UserRepositoryImpl implements UserReppository {

    @Autowired
    private LocalSessionFactoryBean factory;
    @Autowired
    private BCryptPasswordEncoder passEncoder;
    @Autowired
    private Environment env;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public User getUserById(int id) {
        Session s = this.factory.getObject().getCurrentSession();
        return s.get(User.class, id);
    }

    @Override
    public User getUserByUsername(String username) {
        Session s = this.factory.getObject().getCurrentSession();
        try {
            Query q = s.createQuery("FROM User WHERE username=:un");
            q.setParameter("un", username);

            return (User) q.getSingleResult();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean authUser(String username, String password) {
        User u = this.getUserByUsername(username);

        return this.passEncoder.matches(password, u.getPassword());
    }

    @Override
    public User addUser(User u) {
        Session s = this.factory.getObject().getCurrentSession();
        s.save(u);

        return u;
    }

    @Override
    public List<User> getUsers(Map<String, String> params) {
        Session session = this.factory.getObject().getCurrentSession();
        CriteriaBuilder b = session.getCriteriaBuilder();
        CriteriaQuery<User> q = b.createQuery(User.class);
        Root root = q.from(User.class);
        q.select(root);

        if (params != null) {
            List<Predicate> predicates = new ArrayList<>();

            String kw = params.get("kw");
            if (kw != null && !kw.isEmpty()) {
                predicates.add(b.like(root.get("username"), String.format("%%%s%%", kw)));
            }

            String role = params.get("role");
            if (role != null && !role.trim().isEmpty()) {
                predicates.add(b.equal(root.get("userRole"), role));
            }

            String status = params.get("status");
            if (status != null && !status.trim().isEmpty()) {
                boolean isActive = Boolean.parseBoolean(status);
                predicates.add(b.equal(root.get("active"), isActive));
            }

            q.where(predicates.toArray(Predicate[]::new));
        }

        q.orderBy(b.asc(root.get("userRole")));

        Query query = session.createQuery(q);

        if (params != null) {
            String page = params.get("page");
            if (page != null && !page.isEmpty()) {
                int p = Integer.parseInt(page);
                int pageSize = Integer.parseInt(this.env.getProperty("PAGE_SIZE"));
                query.setMaxResults(pageSize);
                query.setFirstResult((p - 1) * pageSize);
            }
        }

        return query.getResultList();
    }

    @Override
    public Long countUsers() {
        Session s = this.factory.getObject().getCurrentSession();
        Query q = s.createQuery("SELECT Count(*) FROM User");

        return Long.parseLong(q.getSingleResult().toString());
    }

    @Override
    public boolean addOrUpdateUser(User u) {
        Session s = this.factory.getObject().getCurrentSession();
        try {
            if (u.getId() == null) {
                u.setActive(Boolean.TRUE);
                u.setPassword(this.passwordEncoder.encode("Admin@123"));
                s.save(u);
            } else {
                s.update(u);
            }
            return true;
        } catch (HibernateException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateUserByAdmin(String username, String type) {
        Session s = this.factory.getObject().getCurrentSession();
        User u = getUserByUsername(username);
        try {
            if (type.equals("reset")) {
                u.setPassword(this.passwordEncoder.encode("Admin@123"));
                s.update(u);
            }
            if (type.equals("block")) {
                u.setActive(!u.getActive());
                s.update(u);
            }

            return true;
        } catch (HibernateException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public User loginUserWithOAuth2(Map<String, String> userReq) {
        Session s = this.factory.getObject().getCurrentSession();
        User u = null;
        try {
            Query q = s.createQuery("FROM User WHERE username=:un");
            q.setParameter("un", userReq.get("username"));
            u = (User) q.getSingleResult();
            if (userReq.get("userRole").equals(u.getUserRole())) {
                return u;
            }
            return null;
        } catch (NoResultException nre) {

        }

        if (u == null) {
            User newU = new User();
            newU.setUsername(userReq.get("username"));
            newU.setFirstName(userReq.get("firstName"));
            newU.setLastName(userReq.get("lastName"));
            newU.setEmail(userReq.get("email"));
            newU.setActive(!!Boolean.parseBoolean(userReq.get("active")));
            newU.setUserRole(userReq.get("userRole"));
            newU.setAvatar(userReq.get("avatar"));
            boolean userRes = this.addOrUpdateUser(newU);
            if (userRes) {
                return getUserByUsername(userReq.get("username"));
            }
        }
        return null;
    }

    @Override
    public List<User> getStores() {
        Session s = this.factory.getObject().getCurrentSession();
        Query q = s.createQuery("SELECT id, firstName, lastName, username FROM User where user_role like :role and active = :isActive");
        q.setParameter("role", "ROLE_STORE");
        q.setParameter("isActive", true);

        return q.getResultList();
    }

    @Override
    public User getStoreById(int id) {
        User u = getUserById(id);
        if (u.getUserRole().equals("ROLE_STORE")) {
            return u;
        }
        return null;
    }

    @Override
    public double getRateOfStore(int id) {
        Session s = this.factory.getObject().getCurrentSession();
        try {
            Query q = s.createQuery("SELECT AVG(number) FROM Rating where storeId = :user_id");
            q.setParameter("user_id", id, StandardBasicTypes.INTEGER);
            return Double.parseDouble(q.getSingleResult().toString());
        } catch (Exception ex) {
            return 0;
        }
    }

    @Override
    public int ratingStore(int id, int value) {
        Session s = this.factory.getObject().getCurrentSession();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Rating r = new Rating();

        Query q = s.createQuery("FROM Rating WHERE authorId=:author_id and storeId=:store_id");
        q.setParameter("author_id", this.getUserByUsername(auth.getName()));
        q.setParameter("store_id", id, StandardBasicTypes.INTEGER);

        try {
            r = (Rating) q.getSingleResult();
        } catch (NoResultException nre) {

        }

        try {
            if (r.getId() == null) {
                r.setNumber(value);
                r.setStoreId(this.getStoreById(id));
                r.setAuthorId(this.getUserByUsername(auth.getName()));
                s.save(r);
            } else {
                r.setNumber(value);
                s.update(r);
            }

            return value;
        } catch (Exception ex) {
            return 0;
        }
    }

    @Override
    public int getRatingStore(int storeId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Session s = this.factory.getObject().getCurrentSession();

        try {
            Query q = s.createQuery("SELECT number FROM Rating where authorId.id = :author_id and storeId.id = :store_id");
            q.setParameter("author_id", this.getUserByUsername(auth.getName()).getId());
            q.setParameter("store_id", storeId, StandardBasicTypes.INTEGER);
            return Integer.parseInt(q.getSingleResult().toString());
        } catch (Exception ex) {
            return -1;
        }
    }

    @Override
    public User updateUser(User u) {
        Session s = this.factory.getObject().getCurrentSession();
        try {
            s.update(u);
            return u;
        } catch (HibernateException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public User updateUserAvatar(MultipartFile image) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
